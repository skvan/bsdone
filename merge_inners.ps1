# Merge inner class files into parent Java source files
$ErrorActionPreference = "Stop"
$innerSrcDir = "C:\Users\Clay\Qoder\bsball-inner-src"
$javaSrcDir = "C:\Users\Clay\Qoder\bsball-backend\src\main\java\com\bsball"

# Map: inner class basename -> parent relative path under com/bsball
$innerFiles = Get-ChildItem -Path $innerSrcDir -Filter "*.java" | Where-Object { $_.Name -match '\$' }

foreach ($f in $innerFiles) {
    $innerName = $f.Name -replace '\.java$', ''
    # Skip anonymous inner classes ($1) - already handled manually
    if ($innerName -match '\$\d+$') {
        Write-Output "SKIP anonymous: $innerName"
        continue
    }
    
    # Parse: e.g. "TenantProperties$HostMapping" -> parent="TenantProperties", inner="HostMapping"
    $parts = $innerName -split '\$'
    $parentName = $parts[0]
    $simpleInnerName = $parts[1]
    
    # Read the inner class content
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    
    # Remove CFR header comments (lines starting with /* or * or */)
    $content = $content -replace '(?s)^/\*.*?\*/\s*', ''
    
    # Remove package declaration
    $content = $content -replace '(?m)^package\s+[\w.]+;\s*', ''
    
    # Remove import statements (inner class shares parent's imports)
    $content = $content -replace '(?m)^import\s+[\w.*]+;\s*', ''
    
    # Fix class declaration: "public static class ParentClass.InnerName" -> "public static class InnerName"
    $content = $content -replace "class\s+$parentName\.", 'class '
    
    # Fix instanceof: "instanceof ParentClass.InnerName" -> "instanceof InnerName"
    $content = $content -replace "instanceof\s+$parentName\.", 'instanceof '
    
    # Fix cast: "(ParentClass.InnerName)" -> "(InnerName)"
    $content = $content -replace "\($parentName\.", '('
    
    # Fix constructor name: "public ParentClass.InnerName(" -> "public InnerName("
    $content = $content -replace "public\s+$parentName\.", 'public '
    
    # Fix references like "return ParentClass.InnerName(...)" 
    # (already handled by the generic fixes above in most cases)
    
    # Find the parent Java file
    $parentFile = Get-ChildItem -Path $javaSrcDir -Recurse -Filter "$parentName.java" | Select-Object -First 1
    
    if (-not $parentFile) {
        Write-Output "WARN: Parent file not found for $parentName"
        continue
    }
    
    # Read parent file
    $parentContent = Get-Content $parentFile.FullName -Raw -Encoding UTF8
    
    # Check if inner class already exists in parent
    if ($parentContent -match "class\s+$simpleInnerName\b") {
        Write-Output "SKIP already merged: $simpleInnerName in $parentName"
        continue
    }
    
    # Insert inner class before the last closing brace of the parent class
    $lastBracePos = $parentContent.LastIndexOf('}')
    if ($lastBracePos -lt 0) {
        Write-Output "WARN: No closing brace found in $parentName"
        continue
    }
    
    # Indent the inner class content (add 4 spaces to each line)
    $indentedLines = @()
    foreach ($line in ($content -split "`n")) {
        $trimmed = $line.TrimEnd("`r")
        if ($trimmed.Trim() -ne '') {
            $indentedLines += "    " + $trimmed
        } else {
            $indentedLines += ""
        }
    }
    $indentedContent = ($indentedLines -join "`r`n").TrimEnd()
    
    # Insert before last }
    $newParent = $parentContent.Substring(0, $lastBracePos) + "`r`n" + $indentedContent + "`r`n" + $parentContent.Substring($lastBracePos)
    
    # Write back
    Set-Content -Path $parentFile.FullName -Value $newParent -Encoding UTF8 -NoNewline:$false
    
    Write-Output "MERGED: $simpleInnerName -> $parentName"
}

Write-Output "`n=== DONE ==="
