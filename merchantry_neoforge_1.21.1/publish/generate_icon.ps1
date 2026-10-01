# Generuje ikonę moda Merchantry jako pixel art 32x32, powiększony bez wygładzania
param([string]$OutDir)
Add-Type -AssemblyName System.Drawing

function C($hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
$pal = @{
  bg = C '#243447'; bgD = C '#1a2635'; bgL = C '#2c4057'
  R = C '#c0392b'; Rd = C '#922b21'; W = C '#f4e9cf'; Wd = C '#d6c8a6'
  B = C '#9a6534'; Bd = C '#6b4423'; Bdd = C '#4a2e17'
  Y = C '#f5c542'; Yd = C '#d9a520'; L = C '#fff3b0'; O = C '#7a5a0e'; D = C '#6b4a0a'
  E = C '#3ddc84'; Ed = C '#1b8a4b'; G = C '#b6f7d2'; S = C '#ffffff'
}
$N = 32
$px = New-Object 'System.Drawing.Color[,]' $N, $N

# Tło z delikatnym gradientem i ramką
for ($y = 0; $y -lt $N; $y++) { for ($x = 0; $x -lt $N; $x++) {
  $px[$x, $y] = if ($y -lt 12) { $pal.bgL } else { $pal.bg }
  if ($x -eq 0 -or $y -eq 0 -or $x -eq 31 -or $y -eq 31) { $px[$x, $y] = $pal.bgD }
} }

# Słupki straganu
for ($y = 8; $y -le 25; $y++) { foreach ($x in 3, 28) { $px[$x, $y] = $pal.B; $px[($x + 1), $y] = $pal.Bd } }

# Daszek: górna belka, pasy czerwono-kremowe, zaokrąglone dolne krawędzie
for ($x = 2; $x -le 29; $x++) { $px[$x, 2] = $pal.Bdd }
for ($y = 3; $y -le 7; $y++) { for ($x = 2; $x -le 29; $x++) {
  $stripe = [math]::Floor(($x - 2) / 4)
  $red = ($stripe % 2) -eq 0
  $c = if ($red) { $pal.R } else { $pal.W }
  if ($y -eq 3) { $c = if ($red) { $pal.Rd } else { $pal.Wd } }
  $px[$x, $y] = $c
} }
for ($s = 0; $s -lt 7; $s++) {
  $x0 = 2 + $s * 4; $red = ($s % 2) -eq 0
  $c = if ($red) { $pal.Rd } else { $pal.Wd }
  $px[($x0 + 1), 8] = $c; $px[($x0 + 2), 8] = $c
}

# Lada
for ($x = 2; $x -le 29; $x++) { $px[$x, 24] = $pal.B; $px[$x, 25] = $pal.Bd; $px[$x, 26] = $pal.Bd; $px[$x, 27] = $pal.Bdd }

# Duża złota moneta z cieniowaniem
$cx = 15.5; $cy = 15.5; $rad = 7.45
for ($y = 7; $y -le 24; $y++) { for ($x = 7; $x -le 24; $x++) {
  $dx = $x - $cx; $dy = $y - $cy; $d = [math]::Sqrt($dx * $dx + $dy * $dy)
  if ($d -le $rad) {
    $c = $pal.Y
    if ($d -gt $rad - 1.1) { $c = $pal.O }
    elseif ($d -gt $rad - 2.1) { $c = $pal.Yd }
    elseif (($dx + $dy) -lt -5 -and $d -gt $rad - 3.6) { $c = $pal.L }
    $px[$x, $y] = $c
  }
} }

# Znak $ na monecie
$glyph = @('..X..', '.XXXX', 'X.X..', 'X.X..', '.XXX.', '..X.X', '..X.X', 'XXXX.', '..X..')
for ($gy = 0; $gy -lt $glyph.Count; $gy++) { for ($gx = 0; $gx -lt 5; $gx++) {
  if ($glyph[$gy][$gx] -eq 'X') { $px[(13 + $gx), (11 + $gy)] = $pal.D }
} }

# Błysk na monecie
$px[22, 8] = $pal.S; $px[21, 8] = $pal.L; $px[23, 8] = $pal.L; $px[22, 7] = $pal.L; $px[22, 9] = $pal.L

# Szmaragd (romb) i stosik płaskich monet na ladzie
$px[6, 19] = $pal.E; $px[5, 20] = $pal.E; $px[6, 20] = $pal.G; $px[7, 20] = $pal.E
$px[5, 21] = $pal.Ed; $px[6, 21] = $pal.E; $px[7, 21] = $pal.Ed; $px[5, 22] = $pal.Ed; $px[6, 22] = $pal.E; $px[7, 22] = $pal.Ed; $px[6, 23] = $pal.Ed
foreach ($row in 18, 20, 22) { for ($x = 23; $x -le 27; $x++) { $px[$x, $row] = if ($x -eq 23) { $pal.L } elseif ($x -eq 27) { $pal.Yd } else { $pal.Y } } }
foreach ($row in 19, 21, 23) { for ($x = 23; $x -le 27; $x++) { $px[$x, $row] = $pal.O } }

function Save($scale, $path) {
  $size = $N * $scale
  $bmp = New-Object System.Drawing.Bitmap $size, $size
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  for ($y = 0; $y -lt $N; $y++) { for ($x = 0; $x -lt $N; $x++) {
    $brush = New-Object System.Drawing.SolidBrush $px[$x, $y]
    $g.FillRectangle($brush, $x * $scale, $y * $scale, $scale, $scale)
    $brush.Dispose()
  } }
  $g.Dispose()
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

New-Item -ItemType Directory -Force $OutDir | Out-Null
Save 16 (Join-Path $OutDir 'merchantry_512.png')
Save 4 (Join-Path $OutDir 'merchantry_128.png')
