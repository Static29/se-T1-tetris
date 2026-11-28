param([switch]$Configure)
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'This launcher requires Windows user-bound credential encryption.' }
$settingsDirectory = Join-Path $env:LOCALAPPDATA 'T1-Tetris'
$settingsFile = Join-Path $settingsDirectory 'server-launch.xml'

function Read-Default([string]$Prompt, [string]$Default) {
    $answer = Read-Host "$Prompt [$Default]"
    if ([string]::IsNullOrWhiteSpace($answer)) { return $Default }
    return $answer.Trim()
}

if ($Configure -or !(Test-Path -LiteralPath $settingsFile)) {
    $bindAddress = Read-Default 'Bind address (0.0.0.0 allows other PCs)' '0.0.0.0'
    $portText = Read-Default 'Port' '7777'
    $portNumber = 0
    if (![int]::TryParse($portText, [ref]$portNumber) -or $portNumber -lt 1 -or $portNumber -gt 65535) {
        throw 'Port must be 1-65535.'
    }
    $keyPath = Read-Default 'Server .p12 path' ''
    if (!(Test-Path -LiteralPath $keyPath -PathType Leaf)) { throw 'Server .p12 file was not found.' }
    $keyPath = (Resolve-Path -LiteralPath $keyPath).Path
    $secret = Read-Host 'Certificate password' -AsSecureString
    New-Item -ItemType Directory -Path $settingsDirectory -Force | Out-Null
    # SecureString in Export-Clixml uses Windows DPAPI (same Windows user/PC only).
    [pscustomobject]@{
        Bind = $bindAddress; Port = $portNumber; Key = $keyPath; Password = $secret
    } | Export-Clixml -LiteralPath $settingsFile
}
$settings = Import-Clixml -LiteralPath $settingsFile
if ($settings.Password -isnot [Security.SecureString]) { throw 'Invalid saved credential. Run with -Configure.' }
if (!(Test-Path -LiteralPath $settings.Key -PathType Leaf)) { throw 'Saved certificate is missing. Run with -Configure.' }
$javac = (Get-Command javac.exe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
$java = Join-Path (Split-Path -Parent $javac) 'java.exe'
$classes = Join-Path ([IO.Path]::GetTempPath()) ('tetris-server-run-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $classes | Out-Null
$sources = @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java' | ForEach-Object FullName)
& $javac --release 21 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Server compilation failed. JDK 21+ is required.' }

$names = @('TETRIS_SERVER_BIND','TETRIS_SERVER_PORT','TETRIS_SERVER_KEY','TETRIS_SERVER_PASSWORD','TETRIS_SERVER_AUTOSTART')
$previousEnvironment = @{}
foreach ($name in $names) { $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $env:TETRIS_SERVER_BIND = [string]$settings.Bind
    $env:TETRIS_SERVER_PORT = [string]$settings.Port
    $env:TETRIS_SERVER_KEY = [string]$settings.Key
    $env:TETRIS_SERVER_PASSWORD = [Net.NetworkCredential]::new('', $settings.Password).Password
    $env:TETRIS_SERVER_AUTOSTART = 'true'
    & $java -cp $classes tetris.network.DedicatedServerDesktop
    if ($LASTEXITCODE -ne 0) { throw 'Server process failed.' }
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process') }
}
