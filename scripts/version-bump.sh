#!/bin/bash

# Fetch tags to ensure we have the latest
git fetch --tags

# Get the latest numeric tag (vX.Y.Z), default to v1.0.0 if none exists
LATEST_TAG=$(git tag -l "v[0-9]*.[0-9]*.[0-9]*" | sort -V | tail -n1)
LATEST_TAG=${LATEST_TAG:-v1.0.0}

# Remove 'v' prefix
VERSION=${LATEST_TAG#v}

# Determine the search range for commit messages
if git rev-parse "$LATEST_TAG" >/dev/null 2>&1; then
    RANGE="$LATEST_TAG..HEAD"
else
    RANGE="HEAD"
fi

MESSAGES=$(git log "$RANGE" --format=%B)
MESSAGES="$MESSAGES $1"

IFS='.' read -ra ADDR <<< "$VERSION"
MAJOR=${ADDR[0]:-1}
MINOR=${ADDR[1]:-0}
PATCH=${ADDR[2]:-0}

if echo "$MESSAGES" | grep -iq "\[major\]"; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
elif echo "$MESSAGES" | grep -iq "\[minor\]"; then
    MINOR=$((MINOR + 1))
    PATCH=0
else
    PATCH=$((PATCH + 1))
fi

NEW_VERSION="$MAJOR.$MINOR.$PATCH"

echo "Latest Tag: $LATEST_TAG"
echo "Next Version: $NEW_VERSION"

mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false
