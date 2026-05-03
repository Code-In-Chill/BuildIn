#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

COMMIT_MSG="${1:-"Update BuildIn plugin"}"

echo "Adding changes to git..."
git add .

echo "Committing with message: '$COMMIT_MSG'..."
git commit -m "$COMMIT_MSG" || echo "No changes to commit."

echo "Pushing to origin main..."
git push origin main

echo "Done! 🎉"
