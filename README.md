## Configuration

Create a `.env` file:
```env
DISCORD_TOKEN=your_discord_token
DB_URL=database.db

# Optional: Custom database path (default: database.db) -> I'll do this later
DB_PATH=
```
**Database Options:**
- Leave `DB_PATH` empty to use default `database.db`
- Set a custom filename: `DB_PATH=my_database.db`
- Set a custom path: `DB_PATH=data/server_db.db`
