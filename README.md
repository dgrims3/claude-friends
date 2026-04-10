# Claude Friends

A mobile chat interface for talking to persistent [Claude Code](https://docs.anthropic.com/en/docs/claude-code) agents from your Android phone -- no terminal needed.

## How It Works

```
Android App  <--WebSocket-->  Node.js Daemon  <--stdin/stdout-->  Claude Code CLI
  (phone)                      (your server)                      (per-agent process)
```

Each "friend" is a Claude Code agent tied to a project directory on your server. Sessions persist on the server even when you close the app, so you can pick up conversations where you left off.

## Features

- Chat with multiple Claude Code agents from your phone
- Each agent works in its own project directory
- Sessions survive app close -- the agent keeps running on your server
- Real-time streaming via WebSocket
- Tool use and tool results displayed inline
- Dark theme optimized for mobile

## Prerequisites

- A Linux/macOS server on your local network (or accessible remotely)
- [Node.js](https://nodejs.org/) 18+ installed on the server
- [Claude Code CLI](https://docs.anthropic.com/en/docs/claude-code) installed and authenticated on the server
- An Android phone (Android 9+)

## Server Setup

1. Clone this repo onto your server:

   ```bash
   git clone https://github.com/dgrims3/claude-friends.git
   cd claude-friends/server
   ```

2. Run the setup script (installs deps, configures systemd, opens firewall):

   ```bash
   chmod +x setup.sh
   ./setup.sh
   ```

   Or set up manually:

   ```bash
   npm install
   cp .env.example .env    # edit PORT/HOST if needed
   node src/index.js
   ```

3. Note your server's IP address -- you'll enter it in the Android app.

## Android App Setup

### Build from source

1. Open the `android/` directory in Android Studio
2. Build and install to your device
3. On first launch, enter your server IP and port (default: 3456)

### Install a prebuilt APK

If a release APK is available, download it from the [Releases](https://github.com/dgrims3/claude-friends/releases) page and sideload it.

## Usage

1. Open the app and enter your server's IP address on the setup screen
2. Tap **+** to add an agent -- give it a name and the absolute path to a project directory on your server
3. Tap the agent to start chatting
4. The agent runs Claude Code in that directory, so it has full access to the project files

## API

The server exposes a REST + WebSocket API:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/friends` | List all agents |
| `POST` | `/friends` | Add an agent (`{name, path}`) |
| `DELETE` | `/friends/:id` | Remove an agent |
| `POST` | `/sessions/:id/start` | Start or resume a session |
| `GET` | `/sessions/:id/status` | Check session status |
| `GET` | `/sessions/:id/history` | Get message history |
| `DELETE` | `/sessions/:id` | Kill a session |
| `WS` | `/sessions/:id/ws` | Real-time chat WebSocket |

## Configuration

### Server

Edit `server/.env`:

```
PORT=3456
HOST=0.0.0.0
FRIENDS_FILE=./friends.json
```

### Android

Server connection is configured in the app's Settings screen (gear icon on the friends list).

## Security Notes

- The server uses **cleartext HTTP/WS** -- designed for trusted local networks
- There is no authentication between the app and server
- Claude Code agents run with the permissions of the server user
- Do not expose the server port to the public internet without adding authentication

## Tech Stack

**Android**: Kotlin, Jetpack Compose, OkHttp3, DataStore, Navigation Compose

**Server**: Node.js, Express, WebSocket (ws), Claude Code CLI

## License

MIT
