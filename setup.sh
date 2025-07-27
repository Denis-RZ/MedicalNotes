#!/usr/bin/env bash
set -e

# Simple setup script for CI environment
# Configure local.properties with Android SDK path

SDK_DIR="/root/android-sdk"

echo "sdk.dir=${SDK_DIR}" > local.properties

echo "Setup complete"
