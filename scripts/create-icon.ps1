$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetDirectory = Join-Path $PSScriptRoot '../src/main/resources/tetris'
[System.IO.Directory]::CreateDirectory($assetDirectory) | Out-Null
$bitmap = New-Object System.Drawing.Bitmap 256, 256
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::FromArgb(255, 20, 23, 30))
$red = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 235, 45, 62))
$light = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 102, 111))
$dark = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 166, 24, 42))
$pattern = @('1110110', '0100010', '0100010', '0100010', '0100111')
for ($row = 0; $row -lt 5; $row++) {
    for ($col = 0; $col -lt 7; $col++) {
        if ($pattern[$row][$col] -eq '1') {
            $x = 30 + $col * 28
            $y = 58 + $row * 28
            $graphics.FillRectangle($red, $x, $y, 24, 24)
            $graphics.FillRectangle($light, $x, $y, 24, 3)
            $graphics.FillRectangle($dark, $x, $y + 21, 24, 3)
        }
    }
}
$bitmap.Save((Join-Path $assetDirectory 'icon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
# Multi-resolution ICO: PNG payloads preserve clean edges at each Windows icon size.
$sizes = @(16, 24, 32, 48, 64, 128, 256)
$payloads = @()
foreach ($size in $sizes) {
    $scaled = New-Object System.Drawing.Bitmap $size, $size
    $canvas = [System.Drawing.Graphics]::FromImage($scaled)
    $canvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $canvas.DrawImage($bitmap, 0, 0, $size, $size)
    $stream = New-Object System.IO.MemoryStream
    $scaled.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
    $payloads += ,$stream.ToArray()
    $stream.Dispose(); $canvas.Dispose(); $scaled.Dispose()
}
$file = [System.IO.File]::Create((Join-Path $assetDirectory 'icon.ico'))
$writer = New-Object System.IO.BinaryWriter $file
$writer.Write([uint16]0); $writer.Write([uint16]1); $writer.Write([uint16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $dimension = if ($sizes[$i] -eq 256) { 0 } else { $sizes[$i] }
    $writer.Write([byte]$dimension); $writer.Write([byte]$dimension)
    $writer.Write([byte]0); $writer.Write([byte]0)
    $writer.Write([uint16]1); $writer.Write([uint16]32)
    $writer.Write([uint32]$payloads[$i].Length); $writer.Write([uint32]$offset)
    $offset += $payloads[$i].Length
}
foreach ($payload in $payloads) { $writer.Write([byte[]]$payload) }
$writer.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
$red.Dispose(); $light.Dispose(); $dark.Dispose()
