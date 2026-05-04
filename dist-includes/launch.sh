#!/usr/bin/env sh
JVM_OPT="-XX:MaxMetaspaceSize=256M --add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED --enable-native-access=javafx.graphics,ALL-UNNAMED"
java $JVM_OPT -Dlogbook_kai.config_dir=./config -Dlogbook_kai.data_dir=. -jar logbook-kai.jar
