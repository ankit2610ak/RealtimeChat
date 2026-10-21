#!/usr/bin/env bash
# Script to idempotently create or update PR status labels using GitHub CLI (gh)

set -e

labels=(
  "status: pending|fbca04|Review items pending action"
  "status: in-progress|1d76db|Review items currently being addressed"
  "status: resolved|0e8a16|Review items successfully resolved"
  "status: closed|6c757d|Review items closed or out of scope"
)

for label_info in "${labels[@]}"; do
  IFS="|" read -r name color desc <<< "$label_info"

  if gh label list --search "$name" 2>/dev/null | grep -q "$name"; then
    echo "Updating label: $name"
    gh label edit "$name" --color "$color" --description "$desc"
  else
    echo "Creating label: $name"
    gh label create "$name" --color "$color" --description "$desc"
  fi
done

echo "PR status labels created/updated successfully!"
