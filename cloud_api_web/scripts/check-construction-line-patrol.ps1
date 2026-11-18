$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path "$PSScriptRoot/../../cloud_api_serve_git").Path
$checkRoot = Join-Path $env:TEMP 'construction-wayline-check'
$output = Join-Path $env:TEMP 'construction-line-patrol-check'
New-Item -ItemType Directory -Force $output | Out-Null
$libs = (Get-ChildItem "$checkRoot/lib" -Filter '*.jar' | ForEach-Object FullName) -join ';'
if (!$libs) { throw 'Run check-construction-plans-backend.ps1 first to prepare local dependencies.' }
$classpath = "$output;$backendRoot/sample/target/classes;$backendRoot/cloud-sdk/target/classes;$libs"
$src = "$backendRoot/sample/src/main/java/com/dji/sample/df/constructionDf"
$test = "$backendRoot/sample/src/test/java/com/dji/sample/df/constructionDf/ConstructionLinePatrolChecks.java"
$sources = @('ConstructionLineTarget.java','ConstructionWaylineService.java','ConstructionPlanGuard.java','ConstructionPlanService.java','ConstructionOverallService.java','ConstructionAnalysisConfigService.java') | ForEach-Object { "$src/$_" }
$arguments = @('--release','11','-encoding','UTF-8','-cp', ('"' + $classpath.Replace('\','/') + '"'), '-d', ('"' + $output.Replace('\','/') + '"'))
$arguments += @($sources + $test) | ForEach-Object { '"' + $_.Replace('\','/') + '"' }
$argFile = Join-Path $output 'javac.args'
[IO.File]::WriteAllLines($argFile, $arguments, (New-Object Text.UTF8Encoding($false)))
& (Join-Path $env:JAVA_HOME 'bin/javac.exe') "@$argFile"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $env:JAVA_HOME 'bin/java.exe') '-cp' $classpath 'com.dji.sample.df.constructionDf.ConstructionLinePatrolChecks'
exit $LASTEXITCODE
