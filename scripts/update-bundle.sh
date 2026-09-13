#!/usr/bin/env bash
# Pull a released universal MCP Bundle into this marketplace and bump the plugin's version.
#
# Claude Desktop refuses plugins whose mcpServers points at a URL ("MCPB URL references are not allowed in
# plugins because the target can change after review"), so each bundle is committed here and referenced by
# relative path. Run this after a release is complete, and again after that project's mcpb/Sign-Release.ps1
# has re-packed the universal bundle around the signed Windows binary.
#
#   scripts/update-bundle.sh rpg-mcp 0.1.5
#   scripts/update-bundle.sh mcp-email 1.0.13
set -euo pipefail

PLUGIN="${1:?usage: $0 <plugin> <version without v prefix>}"
VERSION="${2:?usage: $0 <plugin> <version without v prefix>}"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
LIMIT=50000000

# plugin -> GitHub repo and release asset prefix
case "$PLUGIN" in
	rpg-mcp)   REPO="thegreystone/rpg-mcp";   SERVER="rpg-mcp-server" ;;
	mcp-email) REPO="thegreystone/mcp-email"; SERVER="mcp-email-server" ;;
	*) echo "unknown plugin '$PLUGIN'; add it to the case list in $0" >&2; exit 2 ;;
esac
ASSET="${SERVER}-${VERSION}-universal.mcpb"
TARGET="$HERE/$PLUGIN/${SERVER}.mcpb"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
echo "downloading $ASSET from $REPO v$VERSION..."
gh release download "v$VERSION" --repo "$REPO" --pattern "$ASSET" --dir "$tmp"

size="$(wc -c < "$tmp/$ASSET")"
if [ "$size" -gt "$LIMIT" ]; then
	echo "$ASSET is $size bytes, over GitHub's $LIMIT byte warning threshold; not committing it" >&2
	exit 1
fi
unzip -p "$tmp/$ASSET" manifest.json | grep -q "\"version\": *\"$VERSION\"" \
	|| { echo "bundle manifest does not declare version $VERSION" >&2; exit 1; }

mv -f "$tmp/$ASSET" "$TARGET"

# Bump the version in the plugin manifest, and in this plugin's entry of the marketplace manifest.
sed -i -E "s/(\"version\": *\")[0-9]+\.[0-9]+\.[0-9]+(\")/\1${VERSION}\2/" "$HERE/$PLUGIN/.claude-plugin/plugin.json"
python - "$HERE/.claude-plugin/marketplace.json" "$PLUGIN" "$VERSION" <<'PY'
import json, sys
path, plugin, version = sys.argv[1:]
m = json.load(open(path, encoding="utf-8"))
next(p for p in m["plugins"] if p["name"] == plugin)["version"] = version
json.dump(m, open(path, "w", encoding="utf-8"), ensure_ascii=False)
PY
python "$HERE/scripts/format-json.py" "$HERE/.claude-plugin/marketplace.json"

echo "installed $TARGET ($size bytes); $PLUGIN manifests now at $VERSION"
echo "next: review 'git status', then commit and push"
