# =====================================================================
# Upload the local frontend build directory to the PROD deploy staging
# area. The NEXT "Deploy Production (manual)" workflow run will deploy
# it (STEP 4 in scripts/deploy-prod.sh) together with the backend jar.
#
# The package keeps a top-level "webapps/" root (same convention as the
# old test-env tarball). After a successful deploy the server renames it
# to webapps-prod.tar.gz.deployed-<stamp>, so a stale package is never
# redeployed silently.
#
# Usage (from anywhere):
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\upload-frontend-prod.ps1
# Optional:
#   -FrontendDir <dir>   (default: <repo>\bsball_project\webapps)
#   -Target <user@host>  (default: root@8.138.99.113)
# =====================================================================
param(
    [string]$FrontendDir = (Join-Path $PSScriptRoot "..\bsball_project\webapps"),
    [string]$Target      = "root@8.138.99.113",
    [string]$KeyPath     = (Join-Path $env:USERPROFILE ".ssh\bsdone_deploy"),
    [string]$RemoteStage = "/root/deploy-prod/staging"
)
$ErrorActionPreference = "Stop"

$FrontendDir = (Resolve-Path -LiteralPath $FrontendDir).Path
$leaf = Split-Path $FrontendDir -Leaf
if ($leaf -ne "webapps") { throw "FrontendDir must be a directory named 'webapps' (got: '$leaf')" }
if (-not (Test-Path (Join-Path $FrontendDir "index.html"))) { throw "index.html not found in $FrontendDir - wrong frontend dir?" }

$tmp    = Join-Path $env:TEMP "webapps-prod.tar.gz"
$parent = Split-Path $FrontendDir -Parent

Write-Host "packing  $FrontendDir ..."
if (Test-Path $tmp) { Remove-Item $tmp -Force }
tar -C "$parent" -czf "$tmp" "$leaf"
if ($LASTEXITCODE -ne 0) { throw "tar failed" }
$localMd5 = (Get-FileHash -LiteralPath $tmp -Algorithm MD5).Hash.ToLower()
$sizeMb   = [math]::Round((Get-Item $tmp).Length / 1MB, 1)
Write-Host ("package  {0} MB, md5 {1}" -f $sizeMb, $localMd5)

Write-Host "uploading to ${Target}:$RemoteStage/webapps-prod.tar.gz.new ..."
$ok = $false
for ($i = 1; $i -le 3 -and -not $ok; $i++) {
    if ($i -gt 1) { Write-Host "  retry $i/3 (previous attempt got reset - cross-border link) ..."; Start-Sleep -Seconds 5 }
    ssh -i "$KeyPath" -o BatchMode=yes -o StrictHostKeyChecking=accept-new $Target "mkdir -p $RemoteStage"
    if ($LASTEXITCODE -ne 0) { continue }
    scp -i "$KeyPath" -o BatchMode=yes -o ServerAliveInterval=15 -o ServerAliveCountMax=4 "$tmp" "${Target}:$RemoteStage/webapps-prod.tar.gz.new"
    if ($LASTEXITCODE -eq 0) { $ok = $true }
}
if (-not $ok) { throw "scp failed after 3 attempts - nothing staged (try again later or from a better network)" }

Write-Host "verifying remote copy ..."
$remote = ssh -i "$KeyPath" -o BatchMode=yes $Target "md5sum $RemoteStage/webapps-prod.tar.gz.new | cut -d' ' -f1; tar -tzf $RemoteStage/webapps-prod.tar.gz.new | wc -l"
$remoteMd5   = ("$(@($remote)[0])").Trim()
$remoteCount = ("$(@($remote)[1])").Trim()
Write-Host ("remote md5 {0}  ({1} files in package)" -f $remoteMd5, $remoteCount)
if ($remoteMd5 -ne $localMd5) { throw "REMOTE MD5 MISMATCH - upload corrupted, do NOT deploy" }
Write-Host "[OK] frontend package staged - it will be deployed by the next 'Deploy Production (manual)' run."
