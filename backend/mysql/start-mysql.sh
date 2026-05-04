#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_DIR="$ROOT_DIR/.mysql"
CONFIG_FILE="$ROOT_DIR/mysql/my.cnf"

mkdir -p "$MYSQL_DIR/run" "$MYSQL_DIR/tmp" "$MYSQL_DIR/logs"

TMPDIR="$MYSQL_DIR/tmp" exec mariadbd --defaults-file="$CONFIG_FILE" --console
