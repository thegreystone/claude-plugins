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

| Plugin                   | What it does                                                                                                                                                                                                    | Install                                  |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------|
| [`rpg-mcp`](rpg-mcp)     | Turns Claude into a Dungeoun Master: a persistent, rules-aware RPG engine (SRD 5.2.1) whose campaigns live in a local database and can run for years across conversations. Adds `/rpg new` and `/rpg continue`. | `/plugin install rpg-mcp@hirt-plugins`   |
| [`mcp-email`](mcp-email) | Your mailbox as tools for Claude: read, search, triage, file and draft email over IMAP and SMTP, with several named accounts from one server. Sending and permanent deletion are off by default.                | `/plugin install mcp-email@hirt-plugins` |

Each plugin ships a copy of the universal MCP Bundle from the matching release of its project, committed
next to the plugin manifest: `rpg-mcp/rpg-mcp-server.mcpb` from
[rpg-mcp](https://github.com/thegreystone/rpg-mcp/releases) and `mcp-email/mcp-email-server.mcpb` from
[mcp-email](https://github.com/thegreystone/mcp-email/releases). A universal bundle is one file with a native
build per platform; macOS builds are signed and notarized, Windows builds are Authenticode-signed. `rpg-mcp` covers
macOS (Apple Silicon), Windows (x86_64) and Linux (x86_64); `mcp-email` covers macOS
(Apple Silicon) and Windows (x86_64). Other platforms take the bare binary from the project's releases.

Both clients read the bundle's settings: Claude Desktop shows them on the extension's settings page, Claude
Code asks for them when the plugin is enabled (`/plugin manage` to change them later). For `mcp-email` that is
the IMAP and SMTP server, username and password of one account; passwords go to the operating system keychain.
For `rpg-mcp` it is only the campaign data directory, `~/.rpg-mcp` by default.

The bundles are committed here rather than linked, because Claude Desktop only accepts plugins whose MCP bundle
is part of the reviewed marketplace content. To pick up new releases, run `scripts/update-to-latest.sh`
(or `scripts/update-to-latest.sh --check` to only see what is behind) and commit the result. It finds the
latest release of each project and calls `scripts/update-bundle.sh <plugin> <version>` for every plugin that
is out of date; that script can also be run by hand to pin a specific version.

## License

BSD-3 (see [`LICENSE`](LICENSE)).
