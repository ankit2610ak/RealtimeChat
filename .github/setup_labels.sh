#!/usr/bin/env bash
# Script to idempotently create or update PR status labels using GitHub CLI (gh)

set -e

create_label() {
  name="$1"
  color="$2"
  description="$3"

  if gh label list --search "$name" 2>/dev/null | grep -q "$name"; then
    echo "Updating label: $name"
    gh label edit "$name" --color "$color" --description "$description"
  else
    echo "Creating label: $name"
    gh label create "$name" --color "$color" --description "$description"
  fi
}

create_label "status: pending" "e99695" "Awaiting initial review"
create_label "status: in-progress" "fef2c0" "Addressing review comments"
create_label "status: resolved" "c2e0c6" "All comments addressed"
create_label "status: closed" "d4d7d6" "Review concluded"

echo "PR status labels created/updated successfully!"
