param([string]$StagedRoot = '')
$ErrorActionPreference = 'Stop'
$backend = (Resolve-Path "$PSScriptRoot/../../cloud_api_serve_git").Path
$cache = Join-Path $env:TEMP 'construction-wayline-check'
$output = Join-Path $env:TEMP 'mobile-dock-calibration-check'
New-Item -ItemType Directory -Force $output | Out-Null
$libs = (Get-ChildItem "$cache/lib" -Filter '*.jar' | ForEach-Object FullName) -join ';'
$cp = "$output;$backend/sample/target/classes;$backend/cloud-sdk/target/classes;$cache/classes;$libs"
if ($StagedRoot) { $sources = @(Get-ChildItem $StagedRoot -Filter '*.java' | ForEach-Object FullName) }
else {
  $sources = @(Get-ChildItem "$backend/sample/src/main/java/com/dji/sample/control/service/impl" -Filter 'MobileDockCalibration*.java' | ForEach-Object FullName)
  $sources += "$backend/sample/src/main/java/com/dji/sample/control/controller/MobileDockCalibrationController.java"
  $sources += "$backend/sample/src/main/java/com/dji/sample/manage/service/impl/DeviceRedisServiceImpl.java"
  $sources += "$backend/cloud-sdk/src/main/java/com/dji/sdk/mqtt/events/EventsMethodEnum.java"
}
$sources += "$PSScriptRoot/MobileDockCalibrationChecks.java"
$arguments = @('--release','11','-encoding','UTF-8','-cp', ('"'+$cp.Replace('\','/')+'"'),'-d', ('"'+$output.Replace('\','/')+'"'))
$arguments += $sources | ForEach-Object { '"'+$_.Replace('\','/')+'"' }
$argsFile = Join-Path $output 'javac.args'
[IO.File]::WriteAllLines($argsFile,$arguments,(New-Object Text.UTF8Encoding($false)))
& "$env:JAVA_HOME/bin/javac.exe" "@$argsFile"
if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
& "$env:JAVA_HOME/bin/java.exe" '-cp' $cp 'com.dji.sample.control.service.impl.MobileDockCalibrationChecks'
exit $LASTEXITCODE
