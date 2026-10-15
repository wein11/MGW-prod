/* ===========================================================================
   mgw-prod — Etapa 2
   Wrapper unico de fetch + helpers de sesion. Lo comparten todas las paginas.
   =========================================================================== */

const TOKEN_KEY = 'mgw_token';
const USER_ID_KEY = 'mgw_user_id';
const ROLE_KEY = 'mgw_role';
const NAME_KEY = 'mgw_display_name';

/* --- Sesion: se guarda en localStorage al loguear --- */

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getUserId() {
  const id = localStorage.getItem(USER_ID_KEY);
  return id ? Number(id) : null;
}

function getRole() {
  return localStorage.getItem(ROLE_KEY);
}

function getDisplayName() {
  return localStorage.getItem(NAME_KEY);
}

function isLoggedIn() {
  return getToken() !== null;
}

// El backend devuelve la Session entera: { token, expiresAt, user: { id, role, ... } }
function saveSession(session) {
  localStorage.setItem(TOKEN_KEY, session.token);
  localStorage.setItem(USER_ID_KEY, session.user.id);
  localStorage.setItem(ROLE_KEY, session.user.role);
  localStorage.setItem(NAME_KEY, session.user.displayName);
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_ID_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(NAME_KEY);
}

function logout() {
  clearSession();
  window.location.href = 'login.html';
}

// Las paginas privadas la llaman al cargar. El 401 real lo sigue decidiendo el
// backend: esto solo evita mostrar una pantalla vacia al que no inicio sesion.
function requireLogin() {
  if (!isLoggedIn()) {
    window.location.href = 'login.html';
    return false;
  }
  return true;
}

/* --- Mensajes por codigo HTTP ---
   El backend responde con el cuerpo vacio, asi que el texto lo arma el cliente. */

function messageForStatus(status) {
  if (status === 400) return 'Faltan datos o estan mal cargados.';
  if (status === 401) return 'Necesitas iniciar sesion.';
  if (status === 403) return 'No tenes permiso para hacer esto.';
  if (status === 404) return 'No se encontro lo que buscabas.';
  if (status === 409) return 'La operacion choca con datos que ya existen.';
  return 'Error del servidor (' + status + ').';
}

/* --- El wrapper --- */

async function apiFetch(path, options) {
  const config = options || {};
  const method = config.method || 'GET';
  const auth = config.auth !== false;

  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (auth && token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  const response = await fetch(path, {
    method: method,
    headers: headers,
    body: config.body ? JSON.stringify(config.body) : undefined
  });

  // 204 No Content: el DELETE no manda cuerpo, no hay nada que parsear.
  if (response.status === 204) {
    return null;
  }

  if (!response.ok) {
    // Si el token vencio, la sesion guardada ya no sirve.
    if (response.status === 401 && token) {
      clearSession();
    }
    throw new Error(messageForStatus(response.status));
  }

  return response.json();
}

/* --- Helpers de UI compartidos --- */

function showError(element, text) {
  element.textContent = text;
  element.className = 'message message--error';
  element.hidden = false;
}

function showSuccess(element, text) {
  element.textContent = text;
  element.className = 'message message--ok';
  element.hidden = false;
}

function hideMessage(element) {
  element.hidden = true;
}

// Deshabilita el boton mientras la promesa esta pendiente.
function setLoading(button, loading, idleText) {
  button.disabled = loading;
  button.textContent = loading ? 'Enviando...' : idleText;
}

function formatDate(isoString) {
  if (!isoString) return '-';
  const date = new Date(isoString);
  return date.toLocaleDateString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
}

// Pinta el estado de la sesion en el header y engancha el boton de salir.
function renderSession() {
  const label = document.querySelector('[data-session-label]');
  const logoutButton = document.querySelector('[data-logout]');
  const loginLink = document.querySelector('[data-login-link]');

  if (isLoggedIn()) {
    if (label) label.textContent = getDisplayName() + ' · ' + getRole();
    if (logoutButton) {
      logoutButton.hidden = false;
      logoutButton.addEventListener('click', logout);
    }
    if (loginLink) loginLink.hidden = true;
  } else {
    if (label) label.textContent = '';
    if (logoutButton) logoutButton.hidden = true;
    if (loginLink) loginLink.hidden = false;
  }
}

/* --- Ecualizador: lo comparten todas las paginas --- */

// Diecisiete alturas que se repiten. Se eligio 17 (primo) a proposito: con un
// ciclo mas corto el patron se lee repetido a simple vista.
const BAR_HEIGHTS = [35, 72, 48, 90, 28, 64, 82, 42, 96, 56, 24, 78, 38, 88, 52, 68, 32];

// Degrade calido: rojo encendido a ambar. Sobre el papel crema el extremo
// claro se oscurece un poco para que siga teniendo contraste.
function spectrumColor(i, total) {
  const t = total > 1 ? i / (total - 1) : 0;
  return 'hsl(' + (4 + t * 34) + ' 82% ' + (44 + t * 8) + '%)';
}

function fillBand(element, total, offset) {
  element.innerHTML = '';
  for (let i = 0; i < total; i++) {
    const bar = document.createElement('span');
    bar.style.height = BAR_HEIGHTS[(i + offset) % BAR_HEIGHTS.length] + '%';
    bar.style.background = spectrumColor(i, total);
    element.appendChild(bar);
  }
}
