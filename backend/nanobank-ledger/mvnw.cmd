@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE SET "BASE_DIR=%__MVNW_ARG0_NAME__%"

@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%
IF NOT "%MAVEN_BASEDIR%"=="" SET "MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%"
@ECHO Using Maven project base directory: %MAVEN_PROJECTBASEDIR%

@SET WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties
@SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar

@SET DOWNLOAD_URL=
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%WRAPPER_PROPERTIES%") DO (
    IF "%%A"=="wrapperUrl" SET "DOWNLOAD_URL=%%B"
)

@IF NOT EXIST "%WRAPPER_JAR%" (
    IF NOT "%MVNW_REPOURL%"=="" SET "DOWNLOAD_URL=%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    IF "%DOWNLOAD_URL%"=="" SET "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    @ECHO Downloading %DOWNLOAD_URL%
    powershell -Command "&{"^
        "$webclient = new-object System.Net.WebClient;"^
        "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
        "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
        "}"^
        "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"^
        "}"
    IF "%ERRORLEVEL%"=="0" GOTO :execute
    DEL "%WRAPPER_JAR%"
    @ECHO Could not download maven-wrapper.jar, please run: mvn -N wrapper:wrapper
    GOTO :error
)

:execute
@SET JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
IF "%ERRORLEVEL%"=="0" GOTO :init
ECHO.
ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
ECHO.
GOTO :error

:init
@SET MAVEN_CMD_LINE_ARGS=%*

%JAVA_EXE% ^
  %JVM_CONFIG_MAVEN_PROPS% ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %MAVEN_CMD_LINE_ARGS%

IF "%ERRORLEVEL%"=="0" GOTO :end
GOTO :error

:error
SET ERROR_CODE=%ERRORLEVEL%
:end
@ENDLOCAL & SET ERROR_CODE=%ERROR_CODE%

IF NOT "%MVNW_NOTRUNC%"=="" GOTO :truncate
@ECHO.
@ECHO Maven wrapper script exited with error code %ERROR_CODE%

:truncate
EXIT /B %ERROR_CODE%
