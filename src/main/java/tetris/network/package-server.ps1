$ErrorActionPreference = 'Stop'

$repo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../../../..')).Path
$buildRoot = Join-Path $repo 'build/dedicated-server'
$build = Join-Path $buildRoot (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
$classes = Join-Path $build 'classes'
$inputDir = Join-Path $build 'input'
New-Item -ItemType Directory -Path $classes, $inputDir | Out-Null

$jdkBin = Split-Path -Parent (Get-Command jpackage.exe -CommandType Application).Source
$javac = Join-Path $jdkBin 'javac.exe'
$jar = Join-Path $jdkBin 'jar.exe'
$jpackage = Join-Path $jdkBin 'jpackage.exe'
$sources = @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java' | ForEach-Object FullName)
& $javac --release 21 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Server compilation failed.' }

& $jar --create --file (Join-Path $inputDir 'dedicated-server.jar') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Server JAR creation failed.' }

Push-Location -LiteralPath $build
try {
    @(
        'main-class=tetris.network.DedicatedServerConsole'
        'win-console=true'
    ) | Set-Content -LiteralPath 'console.properties' -Encoding ASCII
    & $jpackage '-J-Dfile.encoding=UTF-8' '-J-Dsun.jnu.encoding=UTF-8' `
        --type app-image --name 'Tetris Dedicated Server' --dest '.' --input 'input' `
        --main-jar dedicated-server.jar --main-class tetris.network.DedicatedServerDesktop `
        --add-launcher 'Tetris Server Console=console.properties' `
        --add-modules java.desktop,jdk.crypto.ec --java-options '-Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw 'Windows application packaging failed.' }
} finally {
    Pop-Location
}

$exe = Join-Path $build 'Tetris Dedicated Server/Tetris Dedicated Server.exe'
if (!(Test-Path -LiteralPath $exe -PathType Leaf)) { throw 'Expected server EXE was not created.' }
Write-Output $exe
$consoleExe = Join-Path $build 'Tetris Dedicated Server/Tetris Server Console.exe'
if (!(Test-Path -LiteralPath $consoleExe -PathType Leaf)) { throw 'Expected console EXE was not created.' }
Write-Output $consoleExe
