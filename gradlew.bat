@echo off
set DIRNAME=%~dp0
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set CLASSPATH=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_HOME%\bin\java" -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*