param([string]$SourceRoot = '')
$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path "$PSScriptRoot/../../cloud_api_serve_git").Path
$checkRoot = Join-Path $env:TEMP 'construction-wayline-check'
$libsRoot = Join-Path $checkRoot 'lib'
$classesRoot = Join-Path $checkRoot 'classes'
New-Item -ItemType Directory -Force $libsRoot,$classesRoot | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead("$backendRoot/sample/target/dfe-patrol-uav.jar")
try {
  foreach ($entry in $archive.Entries) {
    if ($entry.FullName -like 'BOOT-INF/lib/*.jar') {
      $destination = Join-Path $libsRoot ([IO.Path]::GetFileName($entry.FullName))
      if (!(Test-Path -LiteralPath $destination)) { [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination) }
    }
  }
} finally { $archive.Dispose() }
$javaRoot = "$backendRoot/sample/src/main/java/com/dji/sample"
if (!$SourceRoot) { $SourceRoot = "$javaRoot/df/constructionDf" }
$sources = @(Get-ChildItem -LiteralPath $SourceRoot -Filter '*.java' | Where-Object { $_.Name -match '^Construction(Kmz|Wayline)' } | ForEach-Object FullName)
if (!(Test-Path (Join-Path $SourceRoot 'ConstructionWaylineChecks.java'))) {
  $sources += @(Get-ChildItem "$backendRoot/sample/src/test/java/com/dji/sample/df/constructionDf" -Filter 'ConstructionWayline*Checks.java' | ForEach-Object FullName)
}
$sources += @(Get-ChildItem "$javaRoot/df/constructionDf" -Recurse -Filter '*.java' | Where-Object { $_.Name -match '^ConstructionArea' } | ForEach-Object FullName)
$sources += "$javaRoot/component/AuthInterceptor.java"
foreach ($relative in @('wayline/service/IWaylineFileService.java','wayline/service/impl/WaylineFileServiceImpl.java')) {
  $staged = Join-Path $SourceRoot ([IO.Path]::GetFileName($relative))
  if (Test-Path $staged) { $sources += $staged } else { $sources += "$javaRoot/$relative" }
}
$jarPaths = (Get-ChildItem -LiteralPath $libsRoot -Filter '*.jar' | ForEach-Object FullName) -join ';'
$lombokJar = Get-ChildItem "$env:USERPROFILE/.m2/repository/org/projectlombok/lombok" -Recurse -Filter '*.jar' | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
$jarPaths += ";$lombokJar"
$classpath = "$classesRoot;$backendRoot/sample/target/classes;$backendRoot/sample/target/dfe-patrol-uav.jar.original;$backendRoot/cloud-sdk/target/classes;$jarPaths"
$arguments = @('--release','11','-encoding','UTF-8','-cp', ('"' + $classpath.Replace('\','/') + '"'), '-d', ('"' + $classesRoot.Replace('\','/') + '"'))
$arguments += $sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' }
$argFile = Join-Path $checkRoot 'javac.args'
[IO.File]::WriteAllLines($argFile, $arguments, (New-Object Text.UTF8Encoding($false)))
& 'D:/JDK-17/bin/javac.exe' "@$argFile"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (Test-Path "$classesRoot/com/dji/sample/df/constructionDf/ConstructionWaylineChecks.class") {
  & 'D:/JDK-17/bin/java.exe' '-cp' $classpath 'com.dji.sample.df.constructionDf.ConstructionWaylineChecks'
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
Write-Output 'Construction wayline backend compilation passed.'
