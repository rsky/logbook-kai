#!/usr/bin/env sh
JVM_OPT=-XX:MaxMetaspaceSize=256M
LOGBOOK_KAI_CONFIG_DIR=./config \
LOGBOOK_KAI_DATA_DIR=. \
java $JVM_OPT -Djavafx.allowjs=true -jar logbook-kai.jar
