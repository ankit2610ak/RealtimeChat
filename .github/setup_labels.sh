#!/usr/bin/env bash
# Script to create PR status labels using GitHub CLI (gh)

gh label create "status: pending" --color "fbca04" --description "Review items pending action" --force
gh label create "status: in-progress" --color "1d76db" --description "Review items currently being addressed" --force
gh label create "status: resolved" --color "0e8a16" --description "Review items successfully resolved" --force
gh label create "status: closed" --color "b60205" --description "Review items closed or out of scope" --force

echo "PR status labels created successfully!"
