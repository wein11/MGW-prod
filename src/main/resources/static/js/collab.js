/* ==========================================================================
   collab.js — funciones de toplines y colaboraciones.
   Lo usan beat.html (toplines de un beat) y collaborations.html (mis
   propuestas). Va despues de api.js porque usa apiFetch, getUser, etc.

   Como funciona el modulo en el backend:
   - Un topline es la voz de un artista sobre un beat.
   - Al crear un topline el backend crea solo una Collaboration en PENDING.
   - El dueño del beat la acepta o la rechaza (PUT /api/collaborations/{id}).
   ========================================================================== */

const STATUS_LABELS = {
  PENDING: 'Pendiente',
  ACCEPTED: 'Aceptada',
  REJECTED: 'Rechazada'
};

// El backend no manda la colaboracion adentro del topline: son dos listas
// separadas. Se pide la lista completa y se arma un objeto { toplineId: colaboracion }
// para encontrar la de cada topline sin recorrer la lista cada vez.
async function loadCollaborationsByTopline() {
  const collaborations = await apiFetch('/api/collaborations');
  const byTopline = {};
  for (const collaboration of collaborations) {
    byTopline[collaboration.toplineId] = collaboration;
  }
  return byTopline;
}

// Etiqueta de color con el estado. Si el topline no tiene colaboracion
// (por ejemplo, se borro la propuesta) se muestra "Sin propuesta".
function createStatusTag(collaboration) {
  const tag = document.createElement('span');
  if (collaboration === undefined || collaboration === null) {
    tag.className = 'tag';
    tag.textContent = 'Sin propuesta';
    return tag;
  }
  tag.className = 'tag tag-' + collaboration.status.toLowerCase();
  tag.textContent = STATUS_LABELS[collaboration.status];
  return tag;
}

// Misma regla que ToplineService.canModify: el artista dueño o un admin.
function canModifyTopline(topline) {
  const user = getUser();
  if (user === null) return false;
  return user.id === topline.artistId || user.role === 'ADMIN';
}

function createButton(text, className, onClick) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = className;
  button.textContent = text;
  button.addEventListener('click', onClick);
  return button;
}

// Boton de borrar con confirmacion en dos pasos: el primer click solo cambia el
// boton por "¿Seguro?"; el borrado real pasa recien en el segundo click.
// onConfirm es la funcion que hace el DELETE; recibe el boton para poder
// deshabilitarlo mientras espera.
function createDeleteControl(text, onConfirm) {
  const box = document.createElement('span');

  function showFirstStep() {
    box.innerHTML = '';
    box.appendChild(createButton(text, 'btn btn-small btn-danger', showSecondStep));
  }

  function showSecondStep() {
    box.innerHTML = '';
    const question = document.createElement('span');
    question.textContent = '¿Seguro? ';
    const yes = createButton('Si, borrar', 'btn btn-small btn-danger', function () {
      onConfirm(yes);
    });
    const no = createButton('Cancelar', 'btn btn-small btn-outline', showFirstStep);
    box.appendChild(question);
    box.appendChild(yes);
    box.appendChild(no);
  }

  showFirstStep();
  return box;
}

// Agrega al contenedor una linea "Etiqueta: valor".
function addInfo(container, label, value) {
  const p = document.createElement('p');
  const b = document.createElement('b');
  b.textContent = label + ': ';
  p.appendChild(b);
  p.appendChild(document.createTextNode(value));
  container.appendChild(p);
  return p;
}

// Boton Aceptar / Rechazar de una colaboracion pendiente.
// decision es 'ACCEPTED' o 'REJECTED'. onDone se llama cuando el backend
// confirma, para que la pagina vuelva a dibujar la lista.
function createDecisionButton(collaboration, decision, onDone, onError) {
  const accept = decision === 'ACCEPTED';
  const text = accept ? 'Aceptar' : 'Rechazar';
  const className = accept ? 'btn btn-small' : 'btn btn-small btn-outline';
  const button = createButton(text, className, async function () {
    setLoading(button, true, text);
    try {
      await apiFetch('/api/collaborations/' + collaboration.id + '?status=' + decision, 'PUT');
      onDone();
    } catch (error) {
      setLoading(button, false, text);
      onError(error.message);
    }
  });
  return button;
}

// Los links de audio los carga cada usuario. Un link que empiece con
// "javascript:" ejecutaria codigo al hacer click, asi que solo se acepta http o https.
function safeUrl(url) {
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  return '#';
}
