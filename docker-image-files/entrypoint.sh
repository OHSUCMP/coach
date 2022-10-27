#!/bin/bash
set -e

APP_PATH="/opt/app"
CONFIG_DIR="$APP_PATH/config"
DEFAULTS_DIR="$APP_PATH/defaults"
LOGBACK_CONFIG="logback.xml"

# copy logging configuration if the directory is overridden but logback.xml is missing
if ! [ -f "$CONFIG_DIR/$LOGBACK_CONFIG" ]; then
    cp $DEFAULTS_DIR/$LOGBACK_CONFIG $CONFIG_DIR/$LOGBACK_CONFIG
fi

exec $@
