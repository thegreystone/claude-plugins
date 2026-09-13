# hirt-plugins

My Claude plugin marketplace. Works in both Claude Code and Claude Desktop.

## Add the marketplace

**Claude Code:**

```
/plugin marketplace add thegreystone/claude-plugins
```

**Claude Desktop:** *Settings → Extensions → Browse → Add marketplace*, paste
`https://github.com/thegreystone/claude-plugins`.

## Plugins

| Plugin | What it does | Install |
|--------|--------------|---------|
| [`rpg-mcp`](rpg-mcp) | Turns Claude into a Dungeon Master: a persistent, rules-aware RPG engine (SRD 5.2.1) whose campaigns live in a local database and can run for years across conversations. Adds `/rpg new` and `/rpg continue`. | `/plugin install rpg-mcp@hirt-plugins` |

The `rpg-mcp` plugin references the universal MCP Bundle of the matching
[rpg-mcp release](https://github.com/thegreystone/rpg-mcp/releases): one download with native builds for
macOS (Apple Silicon, signed and notarized), Windows (x86_64, signed) and Linux (x86_64, aarch64). No Java,
no network access in play, no account. Campaigns are stored under `~/.rpg-mcp` by default.

## License

BSD-3 (see [`LICENSE`](LICENSE)).
