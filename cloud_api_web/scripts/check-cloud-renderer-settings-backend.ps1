param([string]$SourceRoot = '')
$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path "$PSScriptRoot/../../cloud_api_serve_git").Path
if (!$SourceRoot) { $SourceRoot = "$backendRoot/sample/src/main/java/com/dji/sample/df/cloudRendererDf" }
$checkRoot = Join-Path $env:TEMP 'construction-wayline-check'
$classesRoot = Join-Path $env:TEMP 'cloud-renderer-settings-classes'
New-Item -ItemType Directory -Force $classesRoot | Out-Null
if (!(Test-Path "$checkRoot/lib")) { throw 'Run check-construction-waylines-backend.ps1 first to extract local dependencies.' }
$jarPaths = (Get-ChildItem "$checkRoot/lib" -Filter '*.jar' | ForEach-Object FullName) -join ';'
$classpath = "$classesRoot;$backendRoot/sample/target/classes;$backendRoot/sample/target/dfe-patrol-uav.jar.original;$backendRoot/cloud-sdk/target/classes;$jarPaths"
$sources = @(Get-ChildItem $SourceRoot -Filter '*.java' | ForEach-Object FullName)
if (!(Test-Path "$SourceRoot/CloudRendererSettingsChecks.java")) { $sources += "$backendRoot/sample/src/test/java/com/dji/sample/df/cloudRendererDf/CloudRendererSettingsChecks.java" }
$arguments = @('--release','11','-encoding','UTF-8','-proc:none','-cp', ('"' + $classpath.Replace('\','/') + '"'), '-d', ('"' + $classesRoot.Replace('\','/') + '"'))
$arguments += $sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' }
$argFile = Join-Path $classesRoot 'javac.args'
[IO.File]::WriteAllLines($argFile,$arguments,(New-Object Text.UTF8Encoding($false)))
& (Join-Path $env:JAVA_HOME 'bin/javac.exe') "@$argFile"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $env:JAVA_HOME 'bin/java.exe') '-cp' $classpath 'com.dji.sample.df.cloudRendererDf.CloudRendererSettingsChecks'
exit $LASTEXITCODE
