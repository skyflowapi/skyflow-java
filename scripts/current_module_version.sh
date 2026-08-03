#!/usr/bin/env bash
# Prints <module>'s own current <version> (skipping the inherited <parent>
# block, which has its own <version> tag), with any existing -dev.<sha>
# suffix stripped. Read-only - never modifies the pom.
#
# Used by internal releases to get a module's base version without touching
# git tags at all, so it can never accidentally pick up another module's tag.
set -euo pipefail

Module=$1
PomFile="$Module/pom.xml"

raw_line=$(awk '
    /<parent>/,/<\/parent>/ { next }
    /<version>/ { print; exit }
' "$PomFile")

version=$(echo "$raw_line" | sed -E 's#.*<version>([^<]+)</version>.*#\1#')
version=$(echo "$version" | sed -E 's/-dev\.[0-9a-f]+$//')

echo "$version"
