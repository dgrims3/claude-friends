#!/bin/bash
# setup.sh — Deploy Claude Friends daemon on your server
# Run from the server/ directory of this project

set -e

echo "=== Claude Friends Server Setup ==="

# 1. Check prerequisites
echo "[1/5] Checking prerequisites..."
command -v node >/dev/null 2>&1 || { echo "Node.js required. Install: sudo apt install nodejs npm"; exit 1; }
command -v claude >/dev/null 2>&1 || { echo "Claude Code CLI required. Install: npm install -g @anthropic-ai/claude-code"; exit 1; }

echo "  node $(node --version)"
echo "  claude $(claude --version 2>/dev/null || echo 'installed')"

# 2. Install dependencies
echo "[2/5] Installing npm dependencies..."
npm install

# 3. Create env file
echo "[3/5] Setting up environment..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "  Created .env from template — edit PORT/HOST if needed"
else
    echo "  .env already exists, skipping"
fi

# Load port from .env for firewall rule
PORT=$(grep -oP '^PORT=\K.*' .env 2>/dev/null || echo "3456")

# 4. Open firewall port (optional — skip if ufw not installed)
echo "[4/5] Configuring firewall..."
if command -v ufw >/dev/null 2>&1; then
    sudo ufw allow "$PORT/tcp" comment "Claude Friends daemon"
    echo "  Port $PORT opened"
else
    echo "  ufw not found, skipping — make sure port $PORT is accessible on your network"
fi

# 5. Create systemd service for persistence
echo "[5/5] Creating systemd service..."
CURRENT_USER=$(whoami)
WORKING_DIR=$(pwd)
NODE_PATH=$(which node)

SERVICE_FILE=/etc/systemd/system/claude-friends.service
sudo tee $SERVICE_FILE > /dev/null <<EOF
[Unit]
Description=Claude Friends Session Daemon
After=network.target

[Service]
Type=simple
User=$CURRENT_USER
WorkingDirectory=$WORKING_DIR
ExecStart=$NODE_PATH src/index.js
Restart=on-failure
RestartSec=5
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable claude-friends
sudo systemctl start claude-friends

SERVER_IP=$(hostname -I | awk '{print $1}')

echo ""
echo "=== Setup Complete ==="
echo "Server running at http://$SERVER_IP:$PORT"
echo ""
echo "Commands:"
echo "  sudo systemctl status claude-friends   # check status"
echo "  sudo journalctl -u claude-friends -f    # view logs"
echo "  sudo systemctl restart claude-friends   # restart"
echo ""
echo "Next: Install the Android app and enter $SERVER_IP:$PORT in Settings."
