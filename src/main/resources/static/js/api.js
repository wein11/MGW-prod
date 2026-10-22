/* ==========================================================================
   api.js — lo comparten todas las paginas.
   Tiene tres partes:
   1. Sesion: guardar y leer el token que devuelve el login.
   2. apiFetch: la unica funcion que habla con el backend por HTTP.
   3. Helpers de pantalla: barra de navegacion, mensajes y fechas.
   ========================================================================== */

/* ---------- 1. Sesion ----------
   El login devuelve { token, expiresAt, user: { id, displayName, role, ... } }.
   Lo guardamos en localStorage para que siga ahi al cambiar de pagina.
   localStorage solo guarda texto, por eso el usuario se guarda con
   JSON.stringify y se lee con JSON.parse. */

function saveSession(session) {
  localStorage.setItem('token', session.token);
  localStorage.setItem('user', JSON.stringify(session.user));
}

function getToken() {
  return localStorage.getItem('token');
}

// Devuelve el usuario logueado o null si no hay sesion.
function getUser() {
  const text = localStorage.getItem('user');
  if (text === null) {
    return null;
  }
  return JSON.parse(text);
}

function isLoggedIn() {
  return getToken() !== null;
}

function clearSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}

function logout() {
  clearSession();
  window.location.href = 'index.html';
}

/* ---------- 2. apiFetch ----------
   Todas las paginas llaman al backend con esta funcion. Hace tres cosas:
   - agrega el header Authorization si hay sesion (el backend lo necesita
     para saber quien hace el POST/PUT/DELETE),
   - convierte el body a JSON,
   - si la respuesta es un error, tira un Error con un mensaje en castellano
     para que la pagina lo muestre.
   El backend devuelve los errores con el cuerpo vacio, asi que el texto del
   error se arma aca segun el codigo HTTP. */

function messageForStatus(status) {
  if (status === 400) return 'Faltan datos o estan mal cargados.';
  if (status === 401) return 'Tenes que iniciar sesion.';
  if (status === 403) return 'No tenes permiso para hacer esto.';
  if (status === 404) return 'No se encontro lo que buscabas.';
  if (status === 409) return 'Ese dato ya existe.';
  return 'Error del servidor (' + status + ').';
}

async function apiFetch(path, method, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (isLoggedIn()) {
    headers['Authorization'] = 'Bearer ' + getToken();
  }

  const options = { method: method || 'GET', headers: headers };
  if (body !== undefined) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(path, options);

  if (!response.ok) {
    // 401 con sesion guardada = el token vencio. Se borra para no seguir
    // mandando un token que el backend ya no acepta.
    if (response.status === 401) {
      clearSession();
    }
    throw new Error(messageForStatus(response.status));
  }

  // 204 No Content (por ejemplo el DELETE) no trae cuerpo para leer.
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

/* ---------- 3. Helpers de pantalla ---------- */

// Completa el costado derecho de la barra de navegacion segun haya sesion o no.
// Cada pagina tiene un <div id="session-area"> vacio en el header.
function renderNav() {
  const area = document.getElementById('session-area');
  if (area === null) {
    return;
  }
  area.innerHTML = '';

  const user = getUser();
  if (user === null) {
    const login = document.createElement('a');
    login.href = 'login.html';
    login.textContent = 'Entrar';
    login.className = 'btn btn-small';
    area.appendChild(login);
    return;
  }

  const name = document.createElement('span');
  name.className = 'session-name';
  name.textContent = user.displayName + ' (' + user.role + ')';

  const exit = document.createElement('button');
  exit.type = 'button';
  exit.className = 'btn btn-small btn-outline';
  exit.textContent = 'Salir';
  exit.addEventListener('click', logout);

  area.appendChild(name);
  area.appendChild(exit);
}

// Muestra un mensaje en una caja. type es 'error' o 'ok'.
function showMessage(element, text, type) {
  element.textContent = text;
  element.className = 'message message-' + type;
  element.hidden = false;
}

function hideMessage(element) {
  element.hidden = true;
}

// Mientras se espera la respuesta el boton queda deshabilitado, asi el usuario
// no lo aprieta dos veces y manda el mismo pedido repetido.
function setLoading(button, loading, normalText) {
  button.disabled = loading;
  if (loading) {
    button.textContent = 'Enviando...';
  } else {
    button.textContent = normalText;
  }
}

// El backend manda las fechas en formato ISO ("2026-10-22T03:00:00Z").
function formatDate(isoText) {
  if (!isoText) {
    return '-';
  }
  return new Date(isoText).toLocaleDateString('es-AR');
}

// Lee un parametro de la URL. Ej: en beat.html?id=3, getParam('id') devuelve "3".
function getParam(name) {
  const params = new URLSearchParams(window.location.search);
  return params.get(name);
}

// Los beats y entregas solo traen producerId (un numero). Para mostrar el nombre
// hay que pedir el usuario. Se guarda en un objeto para no pedir dos veces el
// mismo usuario si aparece en varias filas.
const userNames = {};

async function getUserName(userId) {
  if (userNames[userId] !== undefined) {
    return userNames[userId];
  }
  try {
    const user = await apiFetch('/api/users/' + userId);
    userNames[userId] = user.displayName;
  } catch (error) {
    userNames[userId] = 'Usuario #' + userId;
  }
  return userNames[userId];
}
