$ErrorActionPreference = 'Stop'
$javaRoot = 'C:\Users\Fatih\.jdks\corretto-19.0.2'
$env:JAVA_HOME = $javaRoot
$signingDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'Botluk-signing'
New-Item -ItemType Directory -Force $signingDir | Out-Null
$passwordFile = Join-Path $signingDir 'password.xml'
$keyFile = Join-Path $signingDir 'release.p12'
if (-not (Test-Path $passwordFile)) {
    $random = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Fill($random)
    ConvertTo-SecureString ([Convert]::ToBase64String($random)) -AsPlainText -Force | Export-Clixml $passwordFile
}
$secure = Import-Clixml $passwordFile
$env:HERMES_SIGN_PASS = [Net.NetworkCredential]::new('', $secure).Password
try {
    if (-not (Test-Path $keyFile)) {
        & (Join-Path $javaRoot 'bin\keytool.exe') -genkeypair -keystore $keyFile -storetype PKCS12 -storepass:env HERMES_SIGN_PASS -keypass:env HERMES_SIGN_PASS -alias botluk -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Botluk, O=Fatih, C=TR'
        if ($LASTEXITCODE -ne 0) { throw 'Signing key generation failed' }
    }
    $dist = Join-Path $PSScriptRoot 'dist'
    New-Item -ItemType Directory -Force $dist | Out-Null
    $signer = Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools\36.1.0\apksigner.bat'
    & $signer sign --ks $keyFile --ks-key-alias botluk --ks-pass env:HERMES_SIGN_PASS --out (Join-Path $dist 'Botluk-0.2.0.apk') (Join-Path $PSScriptRoot 'app\build\outputs\apk\release\app-release-unsigned.apk')
    if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }
    & $signer verify --verbose (Join-Path $dist 'Botluk-0.2.0.apk')
    if ($LASTEXITCODE -ne 0) { throw 'APK verification failed' }
    & $signer sign --ks $keyFile --ks-key-alias botluk --ks-pass env:HERMES_SIGN_PASS --out (Join-Path $dist 'test-only.apk') (Join-Path $PSScriptRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Test APK signing failed' }
} finally { Remove-Item Env:HERMES_SIGN_PASS -ErrorAction SilentlyContinue }
