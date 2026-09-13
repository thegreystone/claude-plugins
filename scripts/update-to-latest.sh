#!/usr/bin/env bash
# Bring every plugin in this marketplace up to the latest release of its project.
#
# Reads the plugin list from .claude-plugin/marketplace.json, asks GitHub for the latest release of each
# plugin's repository, and runs scripts/update-bundle.sh for every plugin whose committed version is behind.
# Plugins already at the latest release are left alone.
#
#   scripts/update-to-latest.sh              # update all plugins
#   scripts/update-to-latest.sh rpg-mcp      # update only the named plugin(s)
#   scripts/update-to-latest.sh --check      # report what would change, touch nothing
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
MARKETPLACE="$HERE/.claude-plugin/marketplace.json"

CHECK=0
ONLY=()
for arg in "$@"; do
	case "$arg" in
		--check|-n) CHECK=1 ;;
		-h|--help) sed -n '2,10p' "$0"; exit 0 ;;
		-*) echo "unknown option '$arg'" >&2; exit 2 ;;
		*) ONLY+=("$arg") ;;
	esac
done

# One line per plugin: <name> <current version> <owner/repo>
plugins="$(python - "$MARKETPLACE" <<'PY' | tr -d '\r'
import json, re, sys
m = json.load(open(sys.argv[1], encoding="utf-8"))
for p in m["plugins"]:
    repo = re.sub(r"^https://github\.com/", "", p["repository"]).rstrip("/")
    print(p["name"], p["version"], repo)
PY
)"

for want in "${ONLY[@]+"${ONLY[@]}"}"; do
	case "$plugins" in "$want "*|*"
$want "*) ;; *) echo "unknown plugin '$want'; known: $(printf '%s' "$plugins" | cut -d' ' -f1 | paste -sd,)" >&2; exit 2 ;; esac
done

updated=0
failed=0
while read -r name current repo; do
	if [ "${#ONLY[@]}" -gt 0 ]; then
		case " ${ONLY[*]} " in *" $name "*) ;; *) continue ;; esac
	fi

	tag="$(gh release view --repo "$repo" --json tagName -q .tagName | tr -d '\r')"
	latest="${tag#v}"

	if [ "$latest" = "$current" ]; then
		echo "$name: $current is the latest release"
		continue
	fi
	if [ "$CHECK" = 1 ]; then
		echo "$name: $current -> $latest (would update)"
		updated=$((updated + 1))
		continue
	fi

	echo "$name: $current -> $latest"
	if "$HERE/scripts/update-bundle.sh" "$name" "$latest"; then
		updated=$((updated + 1))
	else
		echo "$name: update to $latest failed" >&2
		failed=$((failed + 1))
	fi
done <<< "$plugins"

if [ "$CHECK" = 1 ]; then
	echo "$updated plugin(s) behind the latest release"
else
	echo "$updated plugin(s) updated, $failed failed"
	[ "$updated" -gt 0 ] && echo "next: review 'git status', then commit and push"
fi
[ "$failed" -eq 0 ]
