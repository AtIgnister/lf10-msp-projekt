// Shared session store — included on both login.html and index.html

const sessionStore = {
  users: [],
  votes: [],
  channels: [],
  messages: [],
  channelFeedback: [],
  channelInvites: []
};

let currentUser = null;
let allData = [];

function loadSession() {
  try {
    const raw = sessionStorage.getItem('tsb_data');
    if (raw) {
      const parsed = JSON.parse(raw);
      sessionStore.users = Array.isArray(parsed.users) ? parsed.users : [];
      sessionStore.votes = Array.isArray(parsed.votes) ? parsed.votes : [];
      sessionStore.channels = Array.isArray(parsed.channels) ? parsed.channels : [];
      sessionStore.messages = Array.isArray(parsed.messages) ? parsed.messages : [];
      sessionStore.channelFeedback = Array.isArray(parsed.channelFeedback) ? parsed.channelFeedback : [];
      sessionStore.channelInvites = Array.isArray(parsed.channelInvites) ? parsed.channelInvites : [];
    }
  } catch (err) {
    console.warn('Session data corrupted, resetting.');
    sessionStore.users = [];
    sessionStore.votes = [];
    sessionStore.channels = [];
    sessionStore.messages = [];
    sessionStore.channelFeedback = [];
    sessionStore.channelInvites = [];
  }
}

function saveSession() {
  sessionStorage.setItem('tsb_data', JSON.stringify(sessionStore));
}

function loadCurrentUser() {
  try {
    const raw = sessionStorage.getItem('tsb_current_user');
    if (raw) currentUser = JSON.parse(raw);
  } catch {
    currentUser = null;
  }
}

function saveCurrentUser() {
  if (currentUser) {
    sessionStorage.setItem('tsb_current_user', JSON.stringify(currentUser));
  } else {
    sessionStorage.removeItem('tsb_current_user');
  }
}

function clearCurrentUser() {
  currentUser = null;
  sessionStorage.removeItem('tsb_current_user');
}

function refreshAllData() {
  allData = [
    ...sessionStore.users,
    ...sessionStore.votes,
    ...sessionStore.channels,
    ...sessionStore.messages,
    ...sessionStore.channelFeedback,
    ...sessionStore.channelInvites
  ];
}

// ── Channel functions ──────────────────────────────────────────────────────

function createChannel(name, description, type) {
  if (!currentUser || currentUser.role !== 'manager') {
    return null;
  }

  const channel = {
    id: 'ch_' + Date.now() + Math.random().toString(36).substr(2, 9),
    name,
    description,
    type, // 'open' or 'invite-only'
    createdBy: currentUser.username,
    createdAt: new Date().toISOString(),
    members: [currentUser.username]
  };
  sessionStore.channels.push(channel);
  saveSession();
  return channel;
}

function getChannels() {
  return sessionStore.channels;
}

function getUserChannels() {
  if (!currentUser) return [];
  return sessionStore.channels.filter(ch => ch.members.includes(currentUser.username));
}

function joinChannel(channelId) {
  const channel = sessionStore.channels.find(ch => ch.id === channelId);
  if (channel && !channel.members.includes(currentUser.username)) {
    channel.members.push(currentUser.username);
    saveSession();
    return true;
  }
  return false;
}

function leaveChannel(channelId) {
  const channel = sessionStore.channels.find(ch => ch.id === channelId);
  if (!channel) return false;

  const beforeCount = channel.members.length;
  channel.members = channel.members.filter(member => member !== currentUser.username);

  if (channel.members.length !== beforeCount) {
    saveSession();
    return true;
  }

  return false;
}

function addChannelMessage(channelId, message, senderUsername) {
  const msg = {
    id: 'msg_' + Date.now() + Math.random().toString(36).substr(2, 9),
    channelId,
    sender: senderUsername,
    text: message,
    timestamp: new Date().toISOString()
  };
  sessionStore.messages.push(msg);
  saveSession();
  return msg;
}

function getChannelMessages(channelId) {
  return sessionStore.messages
    .filter(msg => msg.channelId === channelId)
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
}

function addChannelFeedback(channelId, text, senderUsername, emoji = null) {
  const feedback = {
    id: 'fb_' + Date.now() + Math.random().toString(36).substr(2, 9),
    channelId,
    sender: senderUsername,
    text,
    emoji,
    timestamp: new Date().toISOString()
  };
  sessionStore.channelFeedback.push(feedback);
  saveSession();
  return feedback;
}

function getChannelFeedback(channelId) {
  return sessionStore.channelFeedback
    .filter(entry => entry.channelId === channelId)
    .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
}

function createChannelInvite(channelId, inviteeUsername, invitedByUsername) {
  const channel = sessionStore.channels.find(ch => ch.id === channelId);
  if (!channel) return { ok: false, reason: 'channel-not-found' };

  const userExists = sessionStore.users.some(u => u.type === 'user' && u.username === inviteeUsername);
  if (!userExists) return { ok: false, reason: 'user-not-found' };

  if (channel.members.includes(inviteeUsername)) {
    return { ok: false, reason: 'already-member' };
  }

  const alreadyPending = sessionStore.channelInvites.some(inv =>
    inv.channelId === channelId &&
    inv.invitee === inviteeUsername &&
    inv.status === 'pending'
  );

  if (alreadyPending) return { ok: false, reason: 'already-invited' };

  const invite = {
    id: 'inv_' + Date.now() + Math.random().toString(36).substr(2, 9),
    channelId,
    invitee: inviteeUsername,
    invitedBy: invitedByUsername,
    status: 'pending',
    createdAt: new Date().toISOString(),
    respondedAt: null
  };

  sessionStore.channelInvites.push(invite);
  saveSession();
  return { ok: true, invite };
}

function getUserPendingInvites(username = currentUser?.username) {
  if (!username) return [];
  return sessionStore.channelInvites
    .filter(inv => inv.invitee === username && inv.status === 'pending')
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
}

function getChannelPendingInvites(channelId) {
  return sessionStore.channelInvites
    .filter(inv => inv.channelId === channelId && inv.status === 'pending')
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
}

function respondToChannelInvite(inviteId, accept) {
  const invite = sessionStore.channelInvites.find(inv => inv.id === inviteId);
  if (!invite || invite.status !== 'pending') return false;
  if (!currentUser || invite.invitee !== currentUser.username) return false;

  invite.status = accept ? 'accepted' : 'declined';
  invite.respondedAt = new Date().toISOString();

  if (accept) {
    const channel = sessionStore.channels.find(ch => ch.id === invite.channelId);
    if (channel && !channel.members.includes(currentUser.username)) {
      channel.members.push(currentUser.username);
    }
  }

  saveSession();
  return true;
}
