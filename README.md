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

| Plugin                   | What it does                                                                                                                | Install                                  |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------|------------------------------------------|
| [`rpg-mcp`](rpg-mcp)     | Persistent, rules-aware RPG Game Master engine (SRD 5.2.1). Adds `/rpg` to start or continue a campaign.                    | `/plugin install rpg-mcp@hirt-plugins`   |
| [`mcp-email`](mcp-email) | Read, search, triage, file and draft email over IMAP and SMTP. Sending and permanent deletion are off by default.           | `/plugin install mcp-email@hirt-plugins` |
| [`mnemic`](mnemic)       | Long-term memory for Claude: what you tell it, kept as dated facts in one local SQLite file and recalled before it answers. | `/plugin install mnemic@hirt-plugins`    |

Each plugin ships a copy of the universal MCP Bundle from the matching release of its project, committed
next to the plugin manifest: `rpg-mcp/rpg-mcp-server.mcpb` from
[rpg-mcp](https://github.com/thegreystone/rpg-mcp/releases), `mcp-email/mcp-email-server.mcpb` from
[mcp-email](https://github.com/thegreystone/mcp-email/releases) and `mnemic/mnemic.mcpb` from
[mnemic](https://github.com/thegreystone/mnemic/releases). A universal bundle is one file with a native
build per platform; macOS builds are signed and notarized, Windows builds are Authenticode-signed. `rpg-mcp` covers
macOS (Apple Silicon), Windows (x86_64) and Linux (x86_64); `mcp-email` and `mnemic` cover macOS
(Apple Silicon) and Windows (x86_64). Other platforms take the bare binary from the project's releases.

Both clients read the bundle's settings: Claude Desktop shows them on the extension's settings page, Claude
Code asks for them when the plugin is enabled (`/plugin manage` to change them later). For `mcp-email` that is
the IMAP and SMTP server, username and password of one account; passwords go to the operating system keychain.
For `rpg-mcp` it is only the campaign data directory, `~/.rpg-mcp` by default. For `mnemic` it is your name,
so that "I" and "me" mean you, and optionally the data home (`~/.mnemic` by default) and the other names,
e-mail addresses and handles you go by. On first start `mnemic` downloads its embedding model (about 350 MB)
into a folder under your home directory; after that it works offline.

The bundles are committed here rather than linked, because Claude Desktop only accepts plugins whose MCP bundle
is part of the reviewed marketplace content. To pick up new releases, build the tool once with `mvn package` and run
`java -jar target/claude-plugins-tools.jar update` (or `update --check` to only see what is behind) and commit
the result. It asks GitHub for the latest release of each project and installs the bundle of every plugin
that is out of date; `java -jar target/claude-plugins-tools.jar install <plugin> <version>` does the same for
one plugin and a specific release. `mvn spotless:apply` formats the Java sources and the plugin manifests.

## License

BSD-3 (see [`LICENSE`](LICENSE)).
