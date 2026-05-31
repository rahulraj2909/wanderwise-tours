# Push application code only (respects .gitignore — no docs/images).
# Usage: .\scripts\push-code-only.ps1 -RemoteUrl "https://github.com/USER/REPO.git"
param(
    [Parameter(Mandatory = $true)]
    [string] $RemoteUrl,
    [string] $CommitMessage = "Initial commit: WanderWise catalog + booking services"
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

$git = Get-Command git -ErrorAction SilentlyContinue
if (-not $git) {
    Write-Error "Git not found. Install from https://git-scm.com/download/win and restart the terminal."
}

if (-not (Test-Path ".git")) {
    & git init
}

& git add .
Write-Host "`n--- Staged files (should be code only) ---"
& git status --short

$untracked = & git status --porcelain
if (-not $untracked) {
    Write-Host "Nothing to commit."
    exit 0
}

& git commit -m $CommitMessage
& git branch -M main

$remotes = & git remote 2>$null
if ($remotes -notcontains "origin") {
    & git remote add origin $RemoteUrl
} else {
    & git remote set-url origin $RemoteUrl
}

& git push -u origin main
Write-Host "`nDone. Code pushed to $RemoteUrl"
