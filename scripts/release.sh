#!/usr/bin/env bash
set -euo pipefail

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version-tag>"
    echo "Example: $0 v1.0.1"
    exit 1
fi

VERSION="$1"

if [[ ! "$VERSION" == v* ]]; then
    echo "Error: Version tag must start with 'v'"
    exit 1
fi

git add .
git commit -m "Prepare release $VERSION" || echo "No changes to commit"
git push origin main
git tag "$VERSION"
git push origin "$VERSION"

echo "Done! Release $VERSION created."
