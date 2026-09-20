#!/bin/sh
cd "$(dirname "$0")" || exit 1
if ! command -v javac >/dev/null 2>&1; then
    echo "Install a Java Development Kit (JDK) version 17 or later, then try again."
    read -r answer
    exit 1
fi
if ! sh ./build.sh; then
    echo "Build failed. Check that Java 17 or later is installed."
    read -r answer
    exit 1
fi
java -jar dist/library-management.jar "$PWD/data"
