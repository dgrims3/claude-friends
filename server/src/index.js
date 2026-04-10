require('dotenv').config();
const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const { SessionManager } = require('./session-manager');
const { FriendsStore } = require('./friends-store');

const PORT = process.env.PORT || 3456;
const HOST = process.env.HOST || '0.0.0.0';

const app = express();
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocketServer({ noServer: true });

const friends = new FriendsStore(process.env.FRIENDS_FILE || './friends.json');
const sessions = new SessionManager();

// ──────────────────────────────────────────────
// REST: Friends (agent) management
// ──────────────────────────────────────────────

app.get('/friends', (req, res) => {
  const list = friends.getAll().map(f => ({
    ...f,
    active: sessions.isActive(f.id),
  }));
  res.json(list);
});

app.post('/friends', (req, res) => {
  const { name, path, avatar } = req.body;
  if (!name || !path) {
    return res.status(400).json({ error: 'name and path are required' });
  }
  const friend = friends.add({ name, path, avatar: avatar || null });
  res.status(201).json(friend);
});

app.delete('/friends/:id', (req, res) => {
  const { id } = req.params;
  if (sessions.isActive(id)) {
    sessions.kill(id);
  }
  friends.remove(id);
  res.json({ ok: true });
});

// ──────────────────────────────────────────────
// REST: Session management
// ──────────────────────────────────────────────

app.post('/sessions/:id/start', async (req, res) => {
  const friend = friends.get(req.params.id);
  if (!friend) return res.status(404).json({ error: 'Friend not found' });

  try {
    await sessions.start(friend);
    res.json({ ok: true, sessionId: friend.id });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/sessions/:id/status', (req, res) => {
  res.json({
    active: sessions.isActive(req.params.id),
    messageCount: sessions.getMessageCount(req.params.id),
  });
});

app.delete('/sessions/:id', async (req, res) => {
  try {
    await sessions.kill(req.params.id);
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/sessions/:id/history', (req, res) => {
  const history = sessions.getHistory(req.params.id);
  res.json(history);
});

// ──────────────────────────────────────────────
// WebSocket: Real-time chat
// ──────────────────────────────────────────────

server.on('upgrade', (request, socket, head) => {
  const match = request.url.match(/^\/sessions\/([^/]+)\/ws$/);
  if (!match) {
    socket.destroy();
    return;
  }

  wss.handleUpgrade(request, socket, head, (ws) => {
    ws.friendId = match[1];
    wss.emit('connection', ws, request);
  });
});

wss.on('connection', (ws) => {
  const friendId = ws.friendId;
  console.log(`[WS] Client connected for friend: ${friendId}`);

  // Register this websocket to receive session output
  const listener = (message) => {
    if (ws.readyState === ws.OPEN) {
      ws.send(JSON.stringify(message));
    }
  };
  sessions.addListener(friendId, listener, ws);

  ws.on('message', async (data) => {
    try {
      const { type, content } = JSON.parse(data.toString());

      if (type === 'message') {
        await sessions.sendMessage(friendId, content);
      } else if (type === 'quit') {
        await sessions.kill(friendId);
        ws.send(JSON.stringify({ type: 'session_ended' }));
      }
    } catch (err) {
      ws.send(JSON.stringify({ type: 'error', content: err.message }));
    }
  });

  ws.on('close', () => {
    sessions.removeListener(friendId, ws);
    console.log(`[WS] Client disconnected for friend: ${friendId}`);
  });
});

// ──────────────────────────────────────────────
// Start
// ──────────────────────────────────────────────

server.listen(PORT, HOST, () => {
  console.log(`Claude Friends daemon running on ${HOST}:${PORT}`);
  console.log(`Active friends: ${friends.getAll().length}`);
});
