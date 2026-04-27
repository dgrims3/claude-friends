const { spawn } = require('child_process');
const os = require('os');
const path = require('path');

function expandHome(p) {
  if (p.startsWith('~/') || p === '~') {
    return path.join(os.homedir(), p.slice(1));
  }
  return p;
}

class SessionManager {
  constructor() {
    // Map<friendId, SessionState>
    this.sessions = new Map();
    // Map<friendId, Map<callback, ws>>  — tracks which callback belongs to which ws
    this.listeners = new Map();
  }

  // ──────────────────────────────────────────────
  // Session lifecycle
  // ──────────────────────────────────────────────

  async start(friend) {
    const { id, path: rawPath } = friend;
    const cwd = expandHome(rawPath);

    // Already registered? Just reattach listeners
    if (this.isActive(id)) {
      console.log(`[Session] ${id} already active, reattaching`);
      return;
    }

    console.log(`[Session] Registering session for "${friend.name}" in ${cwd}`);

    const session = {
      id,
      cwd,
      history: [],
      busy: false,  // true while a claude process is running
      claudeSessionId: null,  // Claude Code session ID for --resume
    };

    this.sessions.set(id, session);
  }

  async sendMessage(friendId, content) {
    const session = this.sessions.get(friendId);
    if (!session) throw new Error('Session not active');
    if (session.busy) throw new Error('Claude is still responding');

    // Record user message
    const userMsg = {
      type: 'user',
      content,
      timestamp: new Date().toISOString(),
    };
    session.history.push(userMsg);
    this._emit(friendId, userMsg);

    // Spawn claude for this message, using --session-id for conversation continuity
    session.busy = true;
    this._emit(friendId, { type: 'status', content: 'thinking' });

    const extraPath = path.join(os.homedir(), '.local', 'bin');
    const currentPath = process.env.PATH || '';
    const augmentedPath = currentPath.split(':').includes(extraPath)
      ? currentPath
      : `${extraPath}:${currentPath}`;

    const args = [
      '-p',
      '--output-format', 'stream-json',
      '--verbose',
      '--dangerously-skip-permissions',
    ];

    if (session.claudeSessionId) {
      args.push('--resume', session.claudeSessionId);
    }

    args.push(content);

    const claudeProc = spawn('claude', args, {
      cwd: session.cwd,
      stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env, PATH: augmentedPath, TERM: 'dumb' },
    });

    let buffer = '';

    claudeProc.stdout.on('data', (chunk) => {
      buffer += chunk.toString();
      const lines = buffer.split('\n');
      buffer = lines.pop(); // keep incomplete line in buffer

      for (const line of lines) {
        if (!line.trim()) continue;
        try {
          const event = JSON.parse(line);
          this._handleEvent(friendId, event);
        } catch {
          // Non-JSON output, wrap as raw
          this._emit(friendId, { type: 'raw', content: line });
        }
      }
    });

    claudeProc.stderr.on('data', (chunk) => {
      const text = chunk.toString().trim();
      if (text) {
        this._emit(friendId, { type: 'stderr', content: text });
      }
    });

    claudeProc.on('error', (err) => {
      console.error(`[Session] ${friendId} spawn error: ${err.message}`);
      this._emit(friendId, { type: 'error', content: `Failed to start: ${err.message}` });
      session.busy = false;
    });

    claudeProc.on('exit', (code) => {
      console.log(`[Session] ${friendId} claude process exited with code ${code}`);
      session.busy = false;
      // Flush remaining buffer
      if (buffer.trim()) {
        try {
          const event = JSON.parse(buffer);
          this._handleEvent(friendId, event);
        } catch {
          this._emit(friendId, { type: 'raw', content: buffer });
        }
      }
      this._emit(friendId, { type: 'status', content: 'ready' });
    });
  }

  async kill(friendId) {
    const session = this.sessions.get(friendId);
    if (!session) return;

    console.log(`[Session] Removing session ${friendId}`);
    // Clear history so reconnecting starts fresh
    session.history = [];
    this.sessions.delete(friendId);
  }

  // ──────────────────────────────────────────────
  // Event handling from Claude Code stream-json
  // ──────────────────────────────────────────────

  _handleEvent(friendId, event) {
    const session = this.sessions.get(friendId);
    if (!session) return;

    // Capture Claude Code session ID from the init event
    if (event.type === 'system' && event.subtype === 'init' && event.session_id) {
      session.claudeSessionId = event.session_id;
      console.log(`[Session] ${friendId} claude session: ${event.session_id}`);
    }

    const msg = {
      ...event,
      timestamp: new Date().toISOString(),
    };

    session.history.push(msg);
    this._emit(friendId, msg);
  }

  // ──────────────────────────────────────────────
  // Listener management (WebSocket clients)
  // ──────────────────────────────────────────────

  addListener(friendId, callback, ws) {
    if (!this.listeners.has(friendId)) {
      this.listeners.set(friendId, new Map());
    }
    this.listeners.get(friendId).set(callback, ws);
  }

  removeListener(friendId, ws) {
    const map = this.listeners.get(friendId);
    if (map) {
      // Remove only the callback associated with this specific ws
      for (const [cb, owner] of map) {
        if (owner === ws) {
          map.delete(cb);
          break;
        }
      }
    }
  }

  _emit(friendId, message) {
    const map = this.listeners.get(friendId);
    if (map) {
      for (const [cb] of map) {
        try { cb(message); } catch {}
      }
    }
  }

  // ──────────────────────────────────────────────
  // Queries
  // ──────────────────────────────────────────────

  isActive(friendId) {
    return this.sessions.has(friendId);
  }

  getMessageCount(friendId) {
    const session = this.sessions.get(friendId);
    return session ? session.history.length : 0;
  }

  getHistory(friendId) {
    const session = this.sessions.get(friendId);
    return session ? session.history : [];
  }
}

module.exports = { SessionManager };
