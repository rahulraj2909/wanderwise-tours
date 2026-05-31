# Update TOC, List of Figures page numbers, and all fields in the report.
$paths = @(
    "$PSScriptRoot\..\docs\WanderWise_Applied_Project_Report.docx",
    "$env:USERPROFILE\Downloads\WanderWise_Applied_Project_Report.docx"
)

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

foreach ($path in $paths) {
    $full = (Resolve-Path $path -ErrorAction SilentlyContinue)
    if (-not $full) { Write-Host "Skip (not found): $path"; continue }
    $full = $full.Path
    Write-Host "Updating: $full"
    $doc = $word.Documents.Open($full)
    if ($doc.TablesOfContents.Count -gt 0) {
        $doc.TablesOfContents.Item(1).Update()
    }
    $doc.Fields.Update()
    $doc.Repaginate()
    $doc.Save()
    $doc.Close()
    Write-Host "  Done."
}

$word.Quit()
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($word) | Out-Null
Write-Host "All fields updated."
