#!/bin/bash
BIN_DIR=$(dirname $0)
if [ $# -eq 0 ]; then
  echo "Usage: pipe.sh fifo-name"
  exit 1
fi
FIFO="$1"
if [ ! -p "$FIFO" ]; then
  mkfifo "$FIFO"
fi
shift

$BIN_DIR/sender.sh -p $FIFO $*
