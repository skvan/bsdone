# Fix record/enum/interface declarations in merged inner classes
$ErrorActionPreference = "Stop"
$javaSrcDir = "C:\Users\Clay\Qoder\bsball-backend\src\main\java\com\bsball"

# Get all Java files that have been modified (have inner class content merged)
$files = Get-ChildItem -Path $javaSrcDir -Recurse -Filter "*.java"

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    $orig = $content
    
    # Fix patterns like "ClassName.ParentClass.InnerName" -> "InnerName" in various contexts
    
    # Fix: "record ParentClass.InnerName" -> "record InnerName"
    $content = $content -replace '\brecord\s+(\w+)\.(\w+)', 'record $2'
    
    # Fix: "class ParentClass.InnerName" -> "class InnerName" (catch any remaining)
    $content = $content -replace '\bclass\s+(\w+)\.(\w+)', 'class $2'
    
    # Fix: "interface ParentClass.InnerName" -> "interface InnerName"
    $content = $content -replace '\binterface\s+(\w+)\.(\w+)', 'interface $2'
    
    # Fix: "enum ParentClass.InnerName" -> "enum InnerName"
    $content = $content -replace '\benum\s+(\w+)\.(\w+)', 'enum $2'
    
    # Fix: "new ParentClass.InnerName" -> "new InnerName"
    $content = $content -replace '\bnew\s+(\w+)\.(\w+)', 'new $2'
    
    # Fix: "(ParentClass.InnerName)" -> "(InnerName)"
    $content = $content -replace '\((\w+)\.(\w+)\)', '($2)'
    
    # Fix: "instanceof ParentClass.InnerName" -> "instanceof InnerName"
    $content = $content -replace '\binstanceof\s+(\w+)\.(\w+)', 'instanceof $2'
    
    # Fix: return type "ParentClass.InnerName" -> "InnerName" (single-word identifiers before method names)
    # Too aggressive - skip this generic case
    
    if ($content -ne $orig) {
        Set-Content -Path $f.FullName -Value $content -Encoding UTF8 -NoNewline:$false
        Write-Output "FIXED: $($f.Name)"
    }
}

Write-Output "`nDone fixing record/enum/interface declarations"
