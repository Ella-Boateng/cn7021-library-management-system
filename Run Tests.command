#!/bin/sh
cd "$(dirname "$0")" || exit 1
sh ./test.sh
result=$?
echo "Press Return to close."
read -r answer
exit "$result"
