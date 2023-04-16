SET JAVA_HOME="C:\Program Files\Zulu\zulu-17"
SET PATH=%JAVA_HOME%\bin;%PATH%
SET JVM_OPT=-XX:MaxMetaspaceSize=256M
START javaw.exe %JVM_OPT% -jar logbook-kai.jar
