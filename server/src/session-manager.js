const { spawn } = require('child_process');

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
    const { id, path } = friend;

    // Already running? Just reattach listeners
    if (this.isActive(id)) {
      console.log(`[Session] ${id} already active, reattaching`);
      return;
    }

    console.log(`[Session] Starting claude session for "${friend.name}" in ${path}`);

    const claudeProc = spawn('claude', ['--output-format', 'stream-json'], {
      cwd: path,
      stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env, TERM: 'dumb' },
    });

    const session = {
      id,
      process: claudeProc,
      history: [],
      buffer: '',
    };

    // Parse stream-json output line by line
    claudeProc.stdout.on('data', (chunk) => {
      session.buffer += chunk.toString();
      const lines = session.buffer.split('\n');
      session.buffer = lines.pop(); // keep incomplete line in buffer

      for (const line of lines) {
        if (!line.trim()) continue;
        try {
          const event = JSON.parse(line);
          this._handleEvent(id, event);
        } catch {
          // Non-JSON output, wrap as raw
          this._emit(id, { type: 'raw', content: line });
        }
      }
    });

    claudeProc.stderr.on('data', (chunk) => {
      const text = chunk.toString().trim();
      if (text) {
        this._emit(id, { type: 'stderr', content: text });
      }
    });

    claudeProc.on('exit', (code) => {
      console.log(`[Session] ${id} process exited with code ${code}`);
      this._emit(id, { type: 'session_ended', code });
      this.sessions.delete(id);
    });

    this.sessions.set(id, session);
  }

  async sendMessage(friendId, content) {
    const session = this.sessions.get(friendId);
    if (!session) throw new Error('Session not active');

    // Record user message
    const userMsg = {
      type: 'user',
      content,
      timestamp: new Date().toISOString(),
    };
    session.history.push(userMsg);
    this._emit(friendId, userMsg);

    // Send to claude's stdin
    session.process.stdin.write(content + '\n');
  }

  async kill(friendId) {
    const session = this.sessions.get(friendId);
    if (!session) return;

    console.log(`[Session] Killing session ${friendId}`);

    // Try graceful exit first
    try {
      session.process.stdin.write('/quit\n');
    } catch {}

    // Force kill after timeout
    setTimeout(() => {
      try { session.process.kill('SIGTERM'); } catch {}
    }, 3000);

    this.sessions.delete(friendId);
  }

  // ──────────────────────────────────────────────
  // Event handling from Claude Code stream-json
  // ──────────────────────────────────────────────

  _handleEvent(friendId, event) {
    const session = this.sessions.get(friendId);
    if (!session) return;

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
