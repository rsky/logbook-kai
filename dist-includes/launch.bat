SET JVM_OPT=-XX:MaxMetaspaceSize=256M
SET LOGBOOK_KAI_CONFIG_DIR=.\config
SET LOGBOOK_KAI_DATA_DIR=.
START javaw %JVM_OPT% -Djavafx.allowjs=true -jar logbook-kai.jar
