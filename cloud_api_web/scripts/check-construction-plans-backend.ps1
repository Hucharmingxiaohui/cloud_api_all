param([string]$SourceRoot = '')
$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path "$PSScriptRoot/../../cloud_api_serve_git").Path
$javaRoot = "$backendRoot/sample/src/main/java/com/dji/sample"
if (!$SourceRoot) { $SourceRoot = "$javaRoot/df/constructionDf" }
# Reuse the established local dependency extraction and geometry checks.
& "$PSScriptRoot/check-construction-waylines-backend.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$checkRoot = Join-Path $env:TEMP 'construction-wayline-check'
$classesRoot = Join-Path $checkRoot 'classes'
$jarPaths = (Get-ChildItem "$checkRoot/lib" -Filter '*.jar' | ForEach-Object FullName) -join ';'
$lombok = Get-ChildItem "$env:USERPROFILE/.m2/repository/org/projectlombok/lombok" -Recurse -Filter '*.jar' | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
$classpath = "$classesRoot;$backendRoot/sample/target/classes;$backendRoot/cloud-sdk/target/classes;$jarPaths;$lombok"
$sources = @(Get-ChildItem $SourceRoot -Filter 'ConstructionPlan*.java' | ForEach-Object FullName)
if (!(Test-Path (Join-Path $SourceRoot 'ConstructionPlanChecks.java'))) {
  $sources += "$backendRoot/sample/src/test/java/com/dji/sample/df/constructionDf/ConstructionPlanChecks.java"
}
$sources += "$javaRoot/df/constructionDf/ConstructionType.java"
foreach ($name in @('ConstructionDockCoverageController.java','ConstructionDockCoverageChecks.java')) {
  $staged = Join-Path $SourceRoot $name
  $installed = if ($name -like '*Checks.java') { "$backendRoot/sample/src/test/java/com/dji/sample/df/constructionDf/$name" } else { "$javaRoot/df/constructionDf/$name" }
  if (Test-Path $staged) { $sources += $staged } elseif (Test-Path $installed) { $sources += $installed }
}
foreach ($relative in @('df/electricInspectionDf/service/impl/PubWaylineJobPlanDfServiceImpl.java','wayline/service/impl/FlightTaskServiceImpl.java')) {
  $staged = Join-Path $SourceRoot ([IO.Path]::GetFileName($relative))
  if (Test-Path $staged) { $sources += $staged } else { $sources += "$javaRoot/$relative" }
}
$arguments = @('-encoding','UTF-8','-cp', ('"' + $classpath.Replace('\','/') + '"'), '-d', ('"' + $classesRoot.Replace('\','/') + '"'))
$arguments += $sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' }
$argFile = Join-Path $checkRoot 'plans-javac.args'
[IO.File]::WriteAllLines($argFile,$arguments,(New-Object Text.UTF8Encoding($false)))
& 'D:/JDK-17/bin/javac.exe' "@$argFile"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (Test-Path "$classesRoot/com/dji/sample/df/constructionDf/ConstructionPlanChecks.class") {
  & 'D:/JDK-17/bin/java.exe' '-cp' $classpath 'com.dji.sample.df.constructionDf.ConstructionPlanChecks'
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
Write-Output 'Construction plans backend compilation passed.'
if (Test-Path "$classesRoot/com/dji/sample/df/constructionDf/ConstructionDockCoverageChecks.class") {
  & 'D:/JDK-17/bin/java.exe' '-cp' $classpath 'com.dji.sample.df.constructionDf.ConstructionDockCoverageChecks'
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
