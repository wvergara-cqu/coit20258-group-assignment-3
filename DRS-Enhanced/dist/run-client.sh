#!/bin/sh
# Starts the JavaFX client. Set JAVAFX_HOME to an unzipped JavaFX 21 SDK (https://openjfx.io).
cd "$(dirname "$0")"
: "${JAVAFX_HOME:?Set JAVAFX_HOME to your JavaFX SDK folder, e.g. export JAVAFX_HOME=~/javafx-sdk-21.0.4}"
java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml -jar drs-enhanced-client.jar
