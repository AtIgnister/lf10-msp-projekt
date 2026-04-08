// Main application logic — used on index.html

let selectedRatings = {
  climate: { rating: null, emoji: null },
  workload: { rating: null, emoji: null },
  salary:   { rating: null, emoji: null }
};

const defaultConfig = {
  app_title: 'Team Stimmungsbarometer',
  welcome_message: 'Wie geht es dir heute?'
};

let trendChart = null;
let currentChartDays = 30;
let selectedDashboardChannelId = '';

// ── Bootstrap ──────────────────────────────────────────────────────────────

function initApp() {
  loadSession();
  loadCurrentUser();
  refreshAllData();

  // Guard: redirect to login if no session
  if (!currentUser) {
    window.location.href = 'login.html';
    return;
  }

  // Initialize sample channels if none exist
  if (sessionStore.channels.length === 0) {
    createChannel('Pausenzeiten', 'Diskussionen über Pausenzeiten und Kaffeepausen', 'open');
    createChannel('Wie geht\'s dem Essen?', 'Austausch über Mittagessen und Getränke in der Kantine', 'open');
    // Invite-only example
    sessionStore.channels.push({
      id: 'ch_a400m_project',
      name: 'A400M Projekt',
      description: 'Projektarbeit für den A400M - Nur für Projektmitglieder',
      type: 'invite-only',
      createdBy: 'admin',
      createdAt: new Date().toISOString(),
      members: ['admin']
    });
    saveSession();
  }

  // Element SDK integration (optional — only present in the embed environment)
  if (window.elementSdk) {
    window.elementSdk.init({
      defaultConfig,
      onConfigChange: async (config) => {
        const title   = config.app_title       || defaultConfig.app_title;
        const welcome = config.welcome_message  || defaultConfig.welcome_message;
        document.getElementById('app-title').textContent  = title;
        document.getElementById('welcome-msg').textContent = welcome;
      },
      mapToCapabilities: () => ({
        recolorables:  [],
        borderables:   [],
        fontEditable:  undefined,
        fontSizeable:  undefined
      }),
      mapToEditPanelValues: (config) => new Map([
        ['app_title',       config.app_title       || defaultConfig.app_title],
        ['welcome_message', config.welcome_message  || defaultConfig.welcome_message]
      ])
    });
  }

  showMainScreen();
  setupEventListeners();
}

// ── Event listeners ────────────────────────────────────────────────────────

function setupEventListeners() {
  // Logout
  document.getElementById('logout-btn').addEventListener('click', handleLogout);

  // Tab navigation
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const tabId = e.target.id.replace('tab-', '');
      if (tabId === 'channels') {
        renderChannels();
      }
      if (tabId === 'dashboard' && currentUser?.role === 'manager') {
        renderDashboard();
      }
      showSection(tabId);
    });
  });

  // Dashboard channel selector
  document.getElementById('dashboard-channel-select')?.addEventListener('change', (e) => {
    selectedDashboardChannelId = e.target.value;
    if (currentUser?.role === 'manager') renderDashboard();
  });

  // Emoji selection
  ['climate', 'workload', 'salary'].forEach(category => {
    const container = document.getElementById(`emoji-${category}`);
    container.querySelectorAll('.emoji-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        container.querySelectorAll('.emoji-btn').forEach(b => b.classList.remove('selected'));
        btn.classList.add('selected');
        selectedRatings[category] = {
          rating: parseInt(btn.dataset.rating),
          emoji:  btn.dataset.emoji
        };
      });
    });
  });

  // Chart time-range filter
  document.querySelectorAll('.chart-filter').forEach(btn => {
    btn.addEventListener('click', (e) => {
      document.querySelectorAll('.chart-filter').forEach(b => {
        b.classList.remove('bg-indigo-600', 'text-white');
        b.classList.add('bg-white', 'text-teal-900', 'border', 'border-teal-200', 'shadow-sm', 'hover:bg-teal-50');
      });
      e.target.classList.add('bg-indigo-600', 'text-white');
      e.target.classList.remove('bg-white', 'text-teal-900', 'border', 'border-teal-200', 'shadow-sm', 'hover:bg-teal-50');
      currentChartDays = parseInt(e.target.dataset.days);
      renderTrendChart(currentChartDays);
    });
  });

  // Submit vote
  document.getElementById('submit-vote').addEventListener('click', handleSubmitVote);

  // Channels
  setupChannelEventListeners();
}

// ── Auth ───────────────────────────────────────────────────────────────────

function handleLogout() {
  clearCurrentUser();
  window.location.href = 'login.html';
}

// ── Screen & section helpers ───────────────────────────────────────────────

function showMainScreen() {
  document.getElementById('user-display').textContent = currentUser.username;
  document.getElementById('role-display').textContent =
    currentUser.role === 'manager'
      ? 'Vorgesetzter • ' + currentUser.department
      : 'Mitarbeiter • '  + currentUser.department;

  const createChannelBtn = document.getElementById('btn-create-channel');
  if (createChannelBtn) {
    createChannelBtn.classList.toggle('hidden', currentUser.role !== 'manager');
  }

  if (currentUser.role === 'manager') {
    document.getElementById('tab-dashboard').classList.remove('hidden');
  } else {
    document.getElementById('tab-dashboard').classList.add('hidden');
  }

  showSection('vote');
  renderHistory();
  renderInvites();
  if (currentUser.role === 'manager') renderDashboard();
}

function showSection(section) {
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  const tabSection = section === 'channel-detail' ? 'channels' : section;
  const tabEl = document.getElementById(`tab-${tabSection}`);
  if (tabEl) tabEl.classList.add('active');

  ['vote', 'history', 'channels', 'channel-detail', 'dashboard'].forEach(s => {
    document.getElementById(`section-${s}`)?.classList.add('hidden');
  });
  document.getElementById(`section-${section}`).classList.remove('hidden');
}

// ── Voting ─────────────────────────────────────────────────────────────────

async function handleSubmitVote() {
  const errorEl   = document.getElementById('vote-error');
  const successEl = document.getElementById('vote-success');

  if (
    !selectedRatings.climate.rating ||
    !selectedRatings.workload.rating ||
    !selectedRatings.salary.rating
  ) {
    errorEl.textContent = 'Bitte wähle für jeden Bereich ein Emoji aus';
    errorEl.classList.remove('hidden');
    successEl.classList.add('hidden');
    return;
  }

  const voteCount = allData.filter(d => d.type === 'vote').length;
  if (voteCount >= 800) {
    errorEl.textContent = 'Maximale Abstimmungsanzahl erreicht';
    errorEl.classList.remove('hidden');
    return;
  }

  const btn = document.getElementById('submit-vote');
  btn.disabled = true;
  btn.textContent = 'Wird gespeichert...';
  errorEl.classList.add('hidden');

  const categories = [
    { key: 'climate',  name: 'Arbeitsklima',  comment: document.getElementById('comment-climate').value },
    { key: 'workload', name: 'Arbeitspensum',  comment: document.getElementById('comment-workload').value },
    { key: 'salary',   name: 'Bezahlung',      comment: document.getElementById('comment-salary').value }
  ];

  for (const cat of categories) {
    sessionStore.votes.push({
      type:       'vote',
      user_id:    currentUser.user_id,
      username:   '',
      password:   '',
      role:       '',
      department: currentUser.department,
      category:   cat.name,
      rating:     selectedRatings[cat.key].rating,
      emoji:      selectedRatings[cat.key].emoji,
      comment:    cat.comment.trim(),
      created_at: new Date().toISOString()
    });
  }

  saveSession();
  refreshAllData();

  btn.disabled = false;
  btn.textContent = 'Abstimmung abschicken';

  successEl.textContent = '✓ Deine Stimmung wurde erfasst!';
  successEl.classList.remove('hidden');
  resetVoteForm();
  setTimeout(() => successEl.classList.add('hidden'), 3000);
}

function resetVoteForm() {
  selectedRatings = {
    climate:  { rating: null, emoji: null },
    workload: { rating: null, emoji: null },
    salary:   { rating: null, emoji: null }
  };
  document.querySelectorAll('.emoji-btn').forEach(btn => btn.classList.remove('selected'));
  document.getElementById('comment-climate').value  = '';
  document.getElementById('comment-workload').value = '';
  document.getElementById('comment-salary').value   = '';
}

// ── History ────────────────────────────────────────────────────────────────

function renderHistory() {
  const container = document.getElementById('history-list');
  const userVotes = allData.filter(d => d.type === 'vote' && d.user_id === currentUser.user_id);
  const userFeedbacks = allData.filter(d => d.id && d.id.startsWith('fb_') && d.sender === currentUser.username);

  if (userVotes.length === 0 && userFeedbacks.length === 0) {
    container.innerHTML = '<p class="text-secondary-dark text-center py-8">Noch keine Abstimmungen vorhanden.</p>';
    return;
  }

  // Group by local date string
  const grouped = {};
  
  userVotes.forEach(vote => {
    const date = new Date(vote.created_at).toLocaleDateString('de-DE', {
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
    if (!grouped[date]) grouped[date] = [];
    grouped[date].push({
      category: vote.category,
      emoji: vote.emoji,
      comment: vote.comment
    });
  });

  userFeedbacks.forEach(fb => {
    const date = new Date(fb.timestamp).toLocaleDateString('de-DE', {
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
    if (!grouped[date]) grouped[date] = [];
    const channel = allData.find(d => d.id === fb.channelId && d.name);
    grouped[date].push({
      category: `Kanal: ${channel ? channel.name : 'Unbekannt'}`,
      emoji: fb.emoji || '💬',
      comment: fb.text
    });
  });

  const sortedDates = Object.keys(grouped).sort((a, b) => {
    const [dayA, monthA, yearA] = a.split('.');
    const [dayB, monthB, yearB] = b.split('.');
    return new Date(yearB, monthB - 1, dayB) - new Date(yearA, monthA - 1, dayA);
  });

  let html = '';
  sortedDates.slice(0, 10).forEach(date => {
    html += `
      <div class="bg-slate-50/90 backdrop-blur-sm rounded-xl p-4 border border-teal-200 shadow-sm mb-4">
        <p class="text-sm text-teal-900 mb-3">${date}</p>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">`;

    grouped[date].forEach(item => {
      html += `
        <div class="text-center bg-white p-2 rounded-lg border border-teal-100 shadow-sm">
          <p class="text-xs text-secondary-dark mb-1 truncate" title="${escapeHtml(item.category)}">${escapeHtml(item.category)}</p>
          <span class="text-2xl">${item.emoji}</span>
          ${item.comment
            ? `<p class="text-xs text-teal-900 mt-1 truncate" title="${escapeHtml(item.comment)}">${escapeHtml(item.comment)}</p>`
            : ''}
        </div>`;
    });

    html += '</div></div>';
  });

  container.innerHTML = html;
}

// ── Dashboard ──────────────────────────────────────────────────────────────

function renderDashboard() {
  const votes     = allData.filter(d => d.type === 'vote');
  const deptVotes = votes.filter(v => v.department === currentUser.department);
  const memberChannels = getUserChannels();

  document.getElementById('dept-name').textContent = currentUser.department;

  // Dashboard channel selector: only channels where the current user is a member.
  const channelSelect = document.getElementById('dashboard-channel-select');
  const channelNote = document.getElementById('dashboard-channel-note');
  if (channelSelect) {
    const exists = memberChannels.some(ch => ch.id === selectedDashboardChannelId);
    if (!exists) selectedDashboardChannelId = '';

    channelSelect.innerHTML = '<option value="">Kanal waehlen</option>' + memberChannels
      .map(ch => `<option value="${ch.id}" ${selectedDashboardChannelId === ch.id ? 'selected' : ''}>${escapeHtml(ch.name)}</option>`)
      .join('');

    channelSelect.disabled = memberChannels.length === 0;
    if (channelNote) {
      channelNote.textContent = memberChannels.length === 0
        ? 'Du bist aktuell in keinem Kanal Mitglied.'
        : 'Nur Kanaele, in denen du Mitglied bist, sind auswaehlbar.';
    }
  }

  const calcAvg = (voteList, category) => {
    const catVotes = voteList.filter(v => v.category === category);
    if (catVotes.length === 0) return null;
    const sum = catVotes.reduce((acc, v) => acc + v.rating, 0);
    return (sum / catVotes.length).toFixed(1);
  };

  const renderStats = (voteList) => {
    const climate  = calcAvg(voteList, 'Arbeitsklima');
    const workload = calcAvg(voteList, 'Arbeitspensum');
    const salary   = calcAvg(voteList, 'Bezahlung');
    const total    = voteList.length / 3;

    if (!climate && !workload && !salary) {
      return '<p class="text-slate-800">Keine Daten vorhanden.</p>';
    }

    const getBar = (val) => {
      if (!val) return '';
      const pct   = (val / 5) * 100;
      const color = val >= 4 ? 'bg-emerald-500' : val >= 3 ? 'bg-yellow-500' : 'bg-red-500';
      return `<div class="w-full bg-slate-200 rounded-full h-2 mt-1">
                <div class="${color} h-2 rounded-full" style="width: ${pct}%"></div>
              </div>`;
    };

    return `
      <div class="space-y-3">
        <div>
          <div class="flex justify-between text-sm">
            <span>🏢 Arbeitsklima</span>
            <span class="font-semibold">${climate || '-'}/5</span>
          </div>
          ${getBar(climate)}
        </div>
        <div>
          <div class="flex justify-between text-sm">
            <span>⚖️ Arbeitspensum</span>
            <span class="font-semibold">${workload || '-'}/5</span>
          </div>
          ${getBar(workload)}
        </div>
        <div>
          <div class="flex justify-between text-sm">
            <span>💰 Bezahlung</span>
            <span class="font-semibold">${salary || '-'}/5</span>
          </div>
          ${getBar(salary)}
        </div>
        <p class="text-xs text-slate-500 pt-2 border-t border-slate-700">${Math.round(total)} Abstimmungen</p>
      </div>`;
  };

  document.getElementById('dept-stats').innerHTML    = renderStats(deptVotes);
  document.getElementById('company-stats').innerHTML = renderStats(votes);

  const commentsTitle = document.getElementById('dashboard-comments-title');
  const commentsContainer = document.getElementById('recent-comments');

  if (selectedDashboardChannelId) {
    const selectedChannel = memberChannels.find(ch => ch.id === selectedDashboardChannelId);
    const feedback = getChannelFeedback(selectedDashboardChannelId).slice(0, 5);
    const messages = getChannelMessages(selectedDashboardChannelId)
      .slice(-5)
      .reverse();

    if (commentsTitle) {
      commentsTitle.textContent = selectedChannel
        ? `Kanalansicht: ${selectedChannel.name}`
        : 'Kanalansicht';
    }

    const feedbackHtml = feedback.map(entry => {
      const date = new Date(entry.timestamp).toLocaleString('de-DE', {
        day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
      });
      return `
        <div class="bg-white rounded-lg p-3 border border-teal-200">
          <div class="flex items-center justify-between mb-1">
            <span class="text-xs text-slate-800">📝 Feedback von ${escapeHtml(entry.sender)}</span>
            <span class="text-xs text-slate-500">${date}</span>
          </div>
          <p class="text-sm text-teal-900">${entry.emoji ? `${escapeHtml(entry.emoji)} ` : ''}${escapeHtml(entry.text || '')}</p>
        </div>`;
    });

    const messagesHtml = messages.map(msg => {
      const time = new Date(msg.timestamp).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
      return `
        <div class="bg-white rounded-lg p-3 border border-teal-200">
          <div class="flex items-center justify-between mb-1">
            <span class="text-xs text-slate-800">💬 Nachricht von ${escapeHtml(msg.sender)}</span>
            <span class="text-xs text-slate-500">${time}</span>
          </div>
          <p class="text-sm text-teal-900">${escapeHtml(msg.text)}</p>
        </div>`;
    });

    const combined = [...feedbackHtml, ...messagesHtml].slice(0, 5);
    commentsContainer.innerHTML = combined.length > 0
      ? combined.join('')
      : '<p class="text-slate-800">Keine Eintraege fuer diesen Kanal vorhanden.</p>';
  } else {
    if (commentsTitle) commentsTitle.textContent = 'Aktuelle Kommentare (anonym)';

    // Default: vote comments overview
    const recentComments = votes
      .filter(v => v.comment && v.comment.trim())
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
      .slice(0, 5);

    if (recentComments.length === 0) {
      commentsContainer.innerHTML = '<p class="text-slate-800">Keine Kommentare vorhanden.</p>';
    } else {
      commentsContainer.innerHTML = recentComments.map(c => `
        <div class="bg-white rounded-lg p-3">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-lg">${c.emoji}</span>
            <span class="text-xs text-slate-500">${c.category} • ${c.department}</span>
          </div>
          <p class="text-sm text-teal-900">${c.comment}</p>
        </div>`).join('');
    }
  }

  renderTrendChart(currentChartDays);
}

// ── Trend chart ────────────────────────────────────────────────────────────

function renderTrendChart(days) {
  const ctx = document.getElementById('trend-chart');
  if (!ctx) return;
  const chartTitle = document.getElementById('dashboard-chart-title');

  const now       = new Date();
  const startDate = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);

  // Build date buckets for selected range.
  const dataByDate = {};
  for (let i = 0; i < days; i++) {
    const date    = new Date(startDate.getTime() + i * 24 * 60 * 60 * 1000);
    const dateStr = date.toLocaleDateString('de-DE', { month: '2-digit', day: '2-digit' });
    dataByDate[dateStr] = {
      climate: [],
      workload: [],
      salary: [],
      channelMood: [],
      channelMessages: 0
    };
  }

  const labels = Object.keys(dataByDate);

  const avg = (arr) =>
    arr.length > 0 ? (arr.reduce((a, b) => a + b) / arr.length).toFixed(2) : null;

  let datasets = [];
  let scales = {
    y: {
      min:   0,
      max:   5,
      ticks: { color: '#134e4a', stepSize: 1 },
      grid:  { color: 'rgba(15, 118, 110, 0.1)' }
    },
    x: {
      ticks: { color: '#134e4a' },
      grid:  { color: 'rgba(15, 118, 110, 0.1)' }
    }
  };

  if (selectedDashboardChannelId) {
    const selectedChannel = getUserChannels().find(ch => ch.id === selectedDashboardChannelId);
    const feedback = getChannelFeedback(selectedDashboardChannelId);
    const messages = getChannelMessages(selectedDashboardChannelId);

    const feedbackEmojiToRating = {
      '😡': 1,
      '😕': 2,
      '😐': 3,
      '🙂': 4,
      '😄': 5
    };

    feedback.forEach(entry => {
      const d = new Date(entry.timestamp);
      if (d >= startDate && d <= now && entry.emoji) {
        const dateStr = d.toLocaleDateString('de-DE', { month: '2-digit', day: '2-digit' });
        const rating = feedbackEmojiToRating[entry.emoji];
        if (rating && dataByDate[dateStr]) dataByDate[dateStr].channelMood.push(rating);
      }
    });

    messages.forEach(msg => {
      const d = new Date(msg.timestamp);
      if (d >= startDate && d <= now) {
        const dateStr = d.toLocaleDateString('de-DE', { month: '2-digit', day: '2-digit' });
        if (dataByDate[dateStr]) dataByDate[dateStr].channelMessages += 1;
      }
    });

    const moodData = labels.map(d => avg(dataByDate[d].channelMood));
    const messageData = labels.map(d => dataByDate[d].channelMessages);

    if (chartTitle) {
      chartTitle.innerHTML = `<span>📈</span> Kanalverlauf: ${escapeHtml(selectedChannel ? selectedChannel.name : 'Kanal')}`;
    }

    datasets = [
      {
        label:              '📝 Kanal-Stimmung',
        data:               moodData,
        borderColor:        '#F59E0B',
        backgroundColor:    'rgba(245, 158, 11, 0.12)',
        tension:            0.4,
        fill:               true,
        pointBackgroundColor: '#F59E0B',
        pointBorderColor:   '#031163',
        pointRadius:        4,
        pointHoverRadius:   6,
        yAxisID:            'y'
      },
      {
        label:              '💬 Nachrichten',
        data:               messageData,
        borderColor:        '#38BDF8',
        backgroundColor:    'rgba(56, 189, 248, 0.12)',
        tension:            0.35,
        fill:               false,
        pointBackgroundColor: '#38BDF8',
        pointBorderColor:   '#031163',
        pointRadius:        3,
        pointHoverRadius:   5,
        yAxisID:            'y1'
      }
    ];

    scales.y1 = {
      position: 'right',
      beginAtZero: true,
      ticks: { color: '#134e4a', precision: 0 },
      grid:  { drawOnChartArea: false, color: 'rgba(15, 118, 110, 0.1)' }
    };
  } else {
    const votes     = allData.filter(d => d.type === 'vote');
    const deptVotes = votes.filter(v => v.department === currentUser.department);

    deptVotes.forEach(vote => {
      const voteDate = new Date(vote.created_at);
      if (voteDate >= startDate && voteDate <= now) {
        const dateStr = voteDate.toLocaleDateString('de-DE', { month: '2-digit', day: '2-digit' });
        if (dataByDate[dateStr]) {
          const cat =
            vote.category === 'Arbeitsklima'  ? 'climate'  :
            vote.category === 'Arbeitspensum' ? 'workload' : 'salary';
          dataByDate[dateStr][cat].push(vote.rating);
        }
      }
    });

    const climateData  = labels.map(d => avg(dataByDate[d].climate));
    const workloadData = labels.map(d => avg(dataByDate[d].workload));
    const salaryData   = labels.map(d => avg(dataByDate[d].salary));

    if (chartTitle) chartTitle.innerHTML = '<span>📈</span> Stimmungsverlauf';

    datasets = [
      {
        label:              '🏢 Arbeitsklima',
        data:               climateData,
        borderColor:        '#1FBFB8',
        backgroundColor:    'rgba(31, 191, 184, 0.1)',
        tension:            0.4,
        fill:               true,
        pointBackgroundColor: '#1FBFB8',
        pointBorderColor:   '#031163',
        pointRadius:        4,
        pointHoverRadius:   6
      },
      {
        label:              '⚖️ Arbeitspensum',
        data:               workloadData,
        borderColor:        '#1978A5',
        backgroundColor:    'rgba(25, 120, 165, 0.1)',
        tension:            0.4,
        fill:               true,
        pointBackgroundColor: '#1978A5',
        pointBorderColor:   '#031163',
        pointRadius:        4,
        pointHoverRadius:   6
      },
      {
        label:              '💰 Bezahlung',
        data:               salaryData,
        borderColor:        '#05716C',
        backgroundColor:    'rgba(5, 113, 108, 0.1)',
        tension:            0.4,
        fill:               true,
        pointBackgroundColor: '#05716C',
        pointBorderColor:   '#031163',
        pointRadius:        4,
        pointHoverRadius:   6
      }
    ];
  }

  if (trendChart) trendChart.destroy();

  trendChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets
    },
    options: {
      responsive:          true,
      maintainAspectRatio: true,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { labels: { color: '#134e4a', font: { size: 12 } } },
        filler: { propagate: true }
      },
      scales
    }
  });
}

// ── Channels ──────────────────────────────────────────────────────────────

let currentChannel = null;
let selectedChannelFeedbackEmoji = null;

function setupChannelEventListeners() {
  // Create channel button
  document.getElementById('btn-create-channel')?.addEventListener('click', openCreateChannelModal);
  document.getElementById('btn-cancel-channel')?.addEventListener('click', closeCreateChannelModal);
  document.getElementById('btn-confirm-channel')?.addEventListener('click', handleCreateChannel);

  // Modal close on outside click
  document.getElementById('modal-create-channel')?.addEventListener('click', (e) => {
    if (e.target.id === 'modal-create-channel') closeCreateChannelModal();
  });

  // Back button from channel detail
  document.getElementById('btn-back-channels')?.addEventListener('click', () => {
    currentChannel = null;
    showSection('channels');
    renderChannels();
  });

  // Leave current channel (detail view)
  document.getElementById('btn-leave-channel')?.addEventListener('click', handleLeaveCurrentChannel);

  // Send message
  document.getElementById('btn-send-message')?.addEventListener('click', handleSendMessage);
  document.getElementById('message-input')?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handleSendMessage();
  });

  // Send channel feedback
  document.getElementById('btn-send-feedback')?.addEventListener('click', handleSendChannelFeedback);

  // Invite modal actions
  document.getElementById('btn-open-invite-modal')?.addEventListener('click', openInviteModal);
  document.getElementById('btn-cancel-invite-modal')?.addEventListener('click', closeInviteModal);
  document.getElementById('btn-confirm-invite-modal')?.addEventListener('click', handleSendChannelInvite);
  document.getElementById('modal-channel-invite')?.addEventListener('click', (e) => {
    if (e.target.id === 'modal-channel-invite') closeInviteModal();
  });

  // Channel feedback emoji selection
  document.querySelectorAll('.channel-feedback-emoji-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.channel-feedback-emoji-btn').forEach(b => b.classList.remove('selected'));
      btn.classList.add('selected');
      selectedChannelFeedbackEmoji = btn.dataset.emoji;
    });
  });
}

function openCreateChannelModal() {
  if (currentUser.role !== 'manager') {
    alert('Nur Vorgesetzte koennen Kanaele erstellen.');
    return;
  }
  document.getElementById('modal-create-channel').classList.remove('hidden');
  document.getElementById('channel-name').focus();
}

function closeCreateChannelModal() {
  document.getElementById('modal-create-channel').classList.add('hidden');
  document.getElementById('channel-name').value = '';
  document.getElementById('channel-description').value = '';
  document.querySelector('input[name="channel-type"][value="open"]').checked = true;
}

function handleCreateChannel() {
  if (currentUser.role !== 'manager') {
    alert('Nur Vorgesetzte koennen Kanaele erstellen.');
    return;
  }

  const name = document.getElementById('channel-name').value.trim();
  const description = document.getElementById('channel-description').value.trim();
  const type = document.querySelector('input[name="channel-type"]:checked').value;

  if (!name) {
    alert('Bitte gib einen Kanalnamen ein');
    return;
  }

  const created = createChannel(name, description, type);
  if (!created) {
    alert('Kanal konnte nicht erstellt werden.');
    return;
  }

  refreshAllData();
  closeCreateChannelModal();
  renderChannels();
}

function renderChannels() {
  const userChannels = getUserChannels();
  const allChannels = getChannels();
  const availableChannels = allChannels.filter(ch => !userChannels.includes(ch));

  // Render user's channels
  const channelsList = document.getElementById('channels-list');
  if (userChannels.length === 0) {
    channelsList.innerHTML = '<p class="text-slate-800 col-span-full text-center py-8">Noch keine Kanäle. Erstelle einen oder tritt einem bei!</p>';
  } else {
    channelsList.innerHTML = userChannels.map(channel => `
      <div class="bg-slate-50/90 backdrop-blur-sm rounded-xl p-6 border border-teal-200 card-hover cursor-pointer hover:border-teal-400 hover:bg-teal-50 shadow-sm transition-all" onclick="openChannelDetail('${channel.id}')">
        <div class="flex items-start justify-between mb-3">
          <div>
            <h3 class="text-lg font-semibold text-teal-900">${escapeHtml(channel.name)}</h3>
            <p class="text-sm text-secondary-dark">${channel.type === 'open' ? '🌐 Öffentlich' : '🔒 Nur auf Einladung'}</p>
          </div>
          <span class="text-xl">${channel.type === 'open' ? '🌐' : '🔒'}</span>
        </div>
        <p class="text-teal-900 text-sm mb-3">${escapeHtml(channel.description || 'Keine Beschreibung')}</p>
        <div class="flex items-center justify-between text-xs text-slate-800">
          <span>👥 ${channel.members.length} Mitglied${channel.members.length !== 1 ? 'er' : ''}</span>
          <span>von ${escapeHtml(channel.createdBy)}</span>
        </div>
        <div class="mt-3 flex justify-end">
          <button class="px-3 py-1 rounded-lg text-xs font-medium bg-rose-600 hover:bg-rose-700 transition-all"
            onclick="event.stopPropagation(); handleLeaveChannel('${channel.id}')">
            Verlassen
          </button>
        </div>
      </div>
    `).join('');
  }

  // Render available channels (for joining)
  const availableList = document.getElementById('available-channels-list');
  if (availableChannels.length === 0) {
    availableList.innerHTML = '<p class="text-slate-800 col-span-full text-center py-8">Alle öffentlichen Kanäle sind bereits beigetreten.</p>';
  } else {
    availableList.innerHTML = availableChannels
      .filter(ch => ch.type === 'open')
      .map(channel => `
      <div class="bg-slate-50/90 backdrop-blur-sm rounded-xl p-6 border border-teal-200 hover:border-teal-400 hover:bg-teal-50 shadow-sm transition-all">
        <h3 class="text-lg font-semibold text-teal-900 mb-2">${escapeHtml(channel.name)}</h3>
        <p class="text-secondary-dark text-sm mb-3">${escapeHtml(channel.description || 'Keine Beschreibung')}</p>
        <div class="flex items-center justify-between">
          <span class="text-xs text-secondary-dark">👥 ${channel.members.length} Mitglieder</span>
          <button onclick="handleJoinChannel('${channel.id}')" class="px-3 py-1 bg-teal-600 hover:bg-teal-700 text-white text-sm rounded-lg transition-all">
            Beitreten
          </button>
        </div>
      </div>
      `).join('');
  }

  renderInvites();
}

function handleJoinChannel(channelId) {
  if (joinChannel(channelId)) {
    refreshAllData();
    renderChannels();
    alert('Du bist dem Kanal beigetreten!');
  }
}

function renderInvites() {
  const container = document.getElementById('channel-invites-list');
  if (!container) return;

  const invites = getUserPendingInvites();
  if (invites.length === 0) {
    container.innerHTML = '<p class="text-slate-800">Keine offenen Einladungen.</p>';
    return;
  }

  container.innerHTML = invites.map(invite => {
    const channel = getChannels().find(ch => ch.id === invite.channelId);
    if (!channel) return '';

    return `
      <div class="bg-slate-50/90 backdrop-blur-sm rounded-xl p-4 border border-teal-200 shadow-sm">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-teal-900 font-medium">${escapeHtml(channel.name)}</p>
            <p class="text-xs text-slate-800">Einladung von ${escapeHtml(invite.invitedBy)} • ${channel.type === 'invite-only' ? '🔒 Nur auf Einladung' : '🌐 Oeffentlich'}</p>
          </div>
          <div class="flex gap-2">
            <button class="px-3 py-1 rounded-lg text-sm font-medium bg-emerald-600 hover:bg-emerald-700 transition-all" onclick="handleInviteResponse('${invite.id}', true)">
              Annehmen
            </button>
            <button class="px-3 py-1 rounded-lg text-sm font-medium bg-slate-200 hover:bg-slate-300 transition-all" onclick="handleInviteResponse('${invite.id}', false)">
              Ablehnen
            </button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function handleInviteResponse(inviteId, accept) {
  if (!respondToChannelInvite(inviteId, accept)) return;

  refreshAllData();
  renderChannels();

  if (accept) {
    alert('Einladung angenommen. Du bist jetzt Mitglied des Kanals.');
  }
}

function handleLeaveChannel(channelId) {
  const channel = getChannels().find(ch => ch.id === channelId);
  if (!channel) return;

  const confirmed = window.confirm(`Moechtest du den Kanal "${channel.name}" wirklich verlassen?`);
  if (!confirmed) return;

  if (leaveChannel(channelId)) {
    refreshAllData();
    renderChannels();
    if (currentChannel && currentChannel.id === channelId) {
      currentChannel = null;
      showSection('channels');
    }
    alert('Du hast den Kanal verlassen.');
  }
}

function handleLeaveCurrentChannel() {
  if (!currentChannel) return;
  handleLeaveChannel(currentChannel.id);
}

function openChannelDetail(channelId) {
  const channel = getChannels().find(ch => ch.id === channelId);
  if (!channel) return;

  currentChannel = channel;
  
  // Update header
  document.getElementById('channel-detail-name').textContent = channel.name;
  document.getElementById('channel-detail-description').textContent = channel.description || 'Keine Beschreibung';
  
  // Update info panel
  document.getElementById('channel-type-display').textContent = channel.type === 'open' ? '🌐 Öffentlich' : '🔒 Nur auf Einladung';
  document.getElementById('channel-creator-display').textContent = channel.createdBy;
  document.getElementById('channel-members-count').textContent = channel.members.length;
  document.getElementById('channel-members-list').innerHTML = channel.members
    .map(member => `<div class="text-slate-800 text-xs">👤 ${escapeHtml(member)}</div>`)
    .join('');

  updateInviteControls();

  // Clear and load messages
  document.getElementById('messages-list').innerHTML = '';
  document.getElementById('message-input').value = '';
  document.getElementById('channel-feedback-input').value = '';
  selectedChannelFeedbackEmoji = null;
  document.querySelectorAll('.channel-feedback-emoji-btn').forEach(btn => btn.classList.remove('selected'));
  
  renderChannelMessages();
  renderChannelFeedback();
  showSection('channel-detail');
}

function updateInviteControls() {
  const controls = document.getElementById('channel-invite-controls');
  const openBtn = document.getElementById('btn-open-invite-modal');
  const select = document.getElementById('channel-invite-user-modal');
  const pending = document.getElementById('channel-pending-invites');
  if (!controls || !openBtn || !select || !pending || !currentChannel) return;

  const canInvite = currentChannel.type === 'invite-only' && currentChannel.members.includes(currentUser.username);
  controls.classList.toggle('hidden', !canInvite);
  if (!canInvite) return;

  const pendingInvites = getChannelPendingInvites(currentChannel.id);
  const pendingUsers = pendingInvites.map(inv => inv.invitee);

  const candidates = sessionStore.users
    .filter(u => u.type === 'user')
    .map(u => u.username)
    .filter(username =>
      username !== currentUser.username &&
      !currentChannel.members.includes(username) &&
      !pendingUsers.includes(username)
    )
    .sort((a, b) => a.localeCompare(b, 'de'));

  if (candidates.length === 0) {
    select.innerHTML = '<option value="">Keine verfuegbaren Benutzer</option>';
    openBtn.disabled = true;
    openBtn.classList.add('opacity-50', 'cursor-not-allowed');
  } else {
    select.innerHTML = '<option value="">Benutzer waehlen</option>' + candidates
      .map(username => `<option value="${escapeHtml(username)}">${escapeHtml(username)}</option>`)
      .join('');
    openBtn.disabled = false;
    openBtn.classList.remove('opacity-50', 'cursor-not-allowed');
  }

  if (pendingInvites.length === 0) {
    pending.textContent = 'Keine ausstehenden Einladungen.';
  } else {
    pending.textContent = 'Ausstehend: ' + pendingInvites.map(inv => inv.invitee).join(', ');
  }
}

function openInviteModal() {
  const modal = document.getElementById('modal-channel-invite');
  if (!modal) return;
  updateInviteControls();
  modal.classList.remove('hidden');
}

function closeInviteModal() {
  const modal = document.getElementById('modal-channel-invite');
  if (!modal) return;
  modal.classList.add('hidden');
}

function handleSendChannelInvite() {
  if (!currentChannel) return;
  const select = document.getElementById('channel-invite-user-modal');
  if (!select) return;

  const invitee = select.value;
  if (!invitee) return;

  const result = createChannelInvite(currentChannel.id, invitee, currentUser.username);
  if (!result.ok) {
    alert('Einladung konnte nicht erstellt werden.');
    return;
  }

  refreshAllData();
  updateInviteControls();
  closeInviteModal();
  alert(`Einladung an ${invitee} gesendet.`);
}

function renderChannelMessages() {
  if (!currentChannel) return;

  const messages = getChannelMessages(currentChannel.id);
  const messagesList = document.getElementById('messages-list');

  if (messages.length === 0) {
    messagesList.innerHTML = '<p class="text-slate-800 text-center py-8">Noch keine Nachrichten in diesem Kanal.</p>';
    return;
  }

  messagesList.innerHTML = messages.map(msg => {
    const time = new Date(msg.timestamp).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
    const isOwn = msg.sender === currentUser.username;
    return `
      <div class="flex ${isOwn ? 'justify-end' : 'justify-start'}">
        <div class="max-w-xs px-4 py-2 rounded-lg ${isOwn ? 'bg-indigo-600 text-white' : 'bg-white text-teal-900 shadow-sm border border-teal-100'}">
          ${!isOwn ? `<p class="text-xs font-semibold mb-1 text-teal-900">${escapeHtml(msg.sender)}</p>` : ''}
          <p class="text-sm">${escapeHtml(msg.text)}</p>
          <p class="text-xs mt-1 ${isOwn ? 'text-indigo-200' : 'text-slate-800'}">${time}</p>
        </div>
      </div>
    `;
  }).join('');

  // Scroll to bottom
  messagesList.scrollTop = messagesList.scrollHeight;
}

function handleSendMessage() {
  const input = document.getElementById('message-input');
  const text = input.value.trim();

  if (!text || !currentChannel) return;

  addChannelMessage(currentChannel.id, text, currentUser.username);
  refreshAllData();
  input.value = '';
  renderChannelMessages();
}

function renderChannelFeedback() {
  if (!currentChannel) return;

  const feedbackEntries = getChannelFeedback(currentChannel.id);
  const list = document.getElementById('channel-feedback-list');
  if (!list) return;

  if (feedbackEntries.length === 0) {
    list.innerHTML = '<p class="text-slate-800">Noch kein Feedback in diesem Kanal.</p>';
    return;
  }

  list.innerHTML = feedbackEntries.map(entry => {
    const date = new Date(entry.timestamp).toLocaleString('de-DE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });

    return `
      <div class="bg-white rounded-lg p-3 border border-teal-200">
        <div class="flex items-center justify-between mb-1">
          <p class="text-xs text-slate-800">von ${escapeHtml(entry.sender)}</p>
          <p class="text-xs text-slate-500">${date}</p>
        </div>
        <div class="flex items-start gap-2">
          ${entry.emoji ? `<span class="text-lg leading-5">${escapeHtml(entry.emoji)}</span>` : ''}
          <p class="text-sm text-slate-200">${escapeHtml(entry.text)}</p>
        </div>
      </div>
    `;
  }).join('');
}

function handleSendChannelFeedback() {
  if (!currentChannel) return;

  const input = document.getElementById('channel-feedback-input');
  const text = input.value.trim();
  if (!text && !selectedChannelFeedbackEmoji) return;

  addChannelFeedback(currentChannel.id, text, currentUser.username, selectedChannelFeedbackEmoji);
  refreshAllData();
  input.value = '';
  selectedChannelFeedbackEmoji = null;
  document.querySelectorAll('.channel-feedback-emoji-btn').forEach(btn => btn.classList.remove('selected'));
  renderChannelFeedback();
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// ── Start ──────────────────────────────────────────────────────────────────
initApp();
