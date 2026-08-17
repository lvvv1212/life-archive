@REM Maven Wrapper for Windows
@setlocal

@if not defined JAVA_HOME (
  @echo JAVA_HOME must be set
  @exit /b 1
)

@set "MAVEN_PROJECTBASEDIR=%~dp0"

@"%JAVA_HOME%\bin\java.exe" ^
  -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
  -classpath "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*

@endlocal
