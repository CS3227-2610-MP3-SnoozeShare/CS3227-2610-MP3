#!/usr/bin/env bash
# Fails when a branch state is not ready to merge into development (AGENTS.md § 2, approval gate):
# its Status must be Approved, its Approved line must name the owner, and its session table must be empty.
# Usage: check-branch-states.sh <branch-state.md>...
set -u

status=0
for file in "$@"; do
  if [ ! -f "$file" ]; then
    echo "::error file=$file::branch state not found"
    status=1
    continue
  fi
  text=$(tr -d '\r' < "$file")
  state=$(printf '%s\n' "$text" | sed -n 's/^- \*\*Status:\*\* *//p' | head -1 | sed 's/ *$//')
  approved=$(printf '%s\n' "$text" | sed -n 's/^- \*\*Approved:\*\* *//p' | head -1 | sed 's/ *$//')
  # Pipe lines between **Sessions** and the next marker: the header and separator rows are 2 of them.
  table_lines=$(printf '%s\n' "$text" | awk '
    /^\*\*Sessions\*\*/ { in_table = 1; next }
    /^\*\*Remaining\*\*/ || /^## / { in_table = 0 }
    in_table && /^\|/ { rows++ }
    END { print rows + 0 }')
  live_rows=$((table_lines > 2 ? table_lines - 2 : 0))
  bad=0

  if [ "$state" != "Approved" ]; then
    echo "::error file=$file::Status is '${state:-missing}', not 'Approved'. Only the project owner sets Approved (AGENTS.md § 2); do not merge before then."
    bad=1
  fi
  case "$approved" in
    "" | "—" | "-" | "--")
      echo "::error file=$file::The Approved line is empty. The project owner records their name and the date there."
      bad=1
      ;;
  esac
  if [ "$live_rows" -gt 0 ]; then
    echo "::error file=$file::The session table has $live_rows live row(s). Empty it before merging."
    bad=1
  fi
  [ "$bad" -eq 0 ] || status=1
  if [ "$bad" -eq 0 ]; then
    echo "OK $file: Approved ($approved), no live sessions"
  fi
done
exit "$status"
