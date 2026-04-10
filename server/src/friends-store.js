const fs = require('fs');
const { v4: uuidv4 } = require('uuid');

class FriendsStore {
  constructor(filePath) {
    this.filePath = filePath;
    this.friends = this._load();
  }

  _load() {
    try {
      if (fs.existsSync(this.filePath)) {
        return JSON.parse(fs.readFileSync(this.filePath, 'utf-8'));
      }
    } catch (err) {
      console.error('Failed to load friends file:', err.message);
    }
    return [];
  }

  _save() {
    fs.writeFileSync(this.filePath, JSON.stringify(this.friends, null, 2));
  }

  getAll() {
    return this.friends;
  }

  get(id) {
    return this.friends.find(f => f.id === id) || null;
  }

  add({ name, path, avatar }) {
    const friend = {
      id: uuidv4().split('-')[0], // short id
      name,
      path,
      avatar: avatar || this._defaultAvatar(name),
      createdAt: new Date().toISOString(),
    };
    this.friends.push(friend);
    this._save();
    return friend;
  }

  remove(id) {
    this.friends = this.friends.filter(f => f.id !== id);
    this._save();
  }

  _defaultAvatar(name) {
    // Assign a color based on the name hash for the android app to use
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F'];
    let hash = 0;
    for (const ch of name) hash = ((hash << 5) - hash + ch.charCodeAt(0)) | 0;
    return colors[Math.abs(hash) % colors.length];
  }
}

module.exports = { FriendsStore };
