#!/usr/bin/env bash
# Script to idempotently create or update PR status labels using GitHub CLI (gh)

set -e

create_or_edit_label() {
    name="$1"
    color="$2"
    description="$3"

    if gh label list --json name -q ".[].name" 2>/dev/null | grep -qx "$name"; then
        echo "Label '$name' exists, updating..."
        gh label edit "$name" --color "$color" --description "$description"
    else
        echo "Creating label '$name'..."
        gh label create "$name" --color "$color" --description "$description"
    fi
}

create_or_edit_label "status: pending" "fbca04" "Review items pending action"
create_or_edit_label "status: in-progress" "1d76db" "Review items currently being addressed"
create_or_edit_label "status: resolved" "0e8a16" "Review items successfully resolved"
create_or_edit_label "status: closed" "6c757d" "Review items closed or out of scope"

echo "PR status labels created/updated successfully!"
