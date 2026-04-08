// Authentication logic — used on login.html

function setupAuthListeners() {
  document.getElementById('show-register').addEventListener('click', () => {
    document.getElementById('login-form-container').classList.add('hidden');
    document.getElementById('register-form-container').classList.remove('hidden');
  });

  document.getElementById('show-login').addEventListener('click', () => {
    document.getElementById('register-form-container').classList.add('hidden');
    document.getElementById('login-form-container').classList.remove('hidden');
  });

  document.getElementById('login-btn').addEventListener('click', handleLogin);
  document.getElementById('register-btn').addEventListener('click', handleRegister);
}

function handleLogin() {
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;
  const errorEl = document.getElementById('login-error');

  if (!username || !password) {
    errorEl.textContent = 'Bitte alle Felder ausfüllen';
    errorEl.classList.remove('hidden');
    return;
  }

  const user = allData.find(
    d => d.type === 'user' && d.username === username && d.password === password
  );

  if (user) {
    currentUser = user;
    saveCurrentUser();
    window.location.href = 'index.html';
  } else {
    errorEl.textContent = 'Benutzername oder Passwort falsch';
    errorEl.classList.remove('hidden');
  }
}

async function handleRegister() {
  const username = document.getElementById('reg-username').value.trim();
  const password = document.getElementById('reg-password').value;
  const department = document.getElementById('reg-department').value;
  const role = document.getElementById('reg-role').value;
  const errorEl = document.getElementById('register-error');

  if (!username || !password || !department) {
    errorEl.textContent = 'Bitte alle Felder ausfüllen';
    errorEl.classList.remove('hidden');
    return;
  }

  if (password.length < 4) {
    errorEl.textContent = 'Passwort muss mindestens 4 Zeichen haben';
    errorEl.classList.remove('hidden');
    return;
  }

  const existingUser = allData.find(d => d.type === 'user' && d.username === username);
  if (existingUser) {
    errorEl.textContent = 'Benutzername bereits vergeben';
    errorEl.classList.remove('hidden');
    return;
  }

  const userCount = allData.filter(d => d.type === 'user').length;
  if (userCount >= 100) {
    errorEl.textContent = 'Maximale Benutzeranzahl erreicht';
    errorEl.classList.remove('hidden');
    return;
  }

  const btn = document.getElementById('register-btn');
  btn.disabled = true;
  btn.textContent = 'Registriere...';

  const newUser = {
    type: 'user',
    user_id: 'user_' + Date.now(),
    username,
    password,
    role,
    department,
    category: '',
    rating: 0,
    emoji: '',
    comment: '',
    created_at: new Date().toISOString()
  };

  sessionStore.users.push(newUser);
  saveSession();
  refreshAllData();

  btn.disabled = false;
  btn.textContent = 'Registrieren';
  errorEl.classList.add('hidden');

  // Switch back to login form and pre-fill username
  document.getElementById('register-form-container').classList.add('hidden');
  document.getElementById('login-form-container').classList.remove('hidden');
  document.getElementById('login-username').value = username;
  document.getElementById('login-password').value = '';
}

// Initialize
loadSession();
refreshAllData();
setupAuthListeners();
