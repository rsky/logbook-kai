#!/usr/bin/env sh
JVM_OPT=-XX:MaxMetaspaceSize=256M
java $JVM_OPT -Djavafx.allowjs=true -Dlogbook_kai.config_dir=./config -Dlogbook_kai.data_dir=. -jar logbook-kai.jar
