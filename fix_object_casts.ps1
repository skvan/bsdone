# Fix (Object) casts that break generic type inference
# Only remove (Object) from patterns where it widens the type and breaks generics

$javaSrcDir = "C:\Users\Clay\Qoder\bsball-backend\src\main\java\com\bsball"
$files = Get-ChildItem -Path $javaSrcDir -Recurse -Filter "*.java"

$fixedCount = 0
$totalChanges = 0

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    $orig = $content
    $changed = $false
    
    # Pattern 1: Map.of((Object)"str", (Object)val) -> Map.of("str", val)
    # This breaks Map<K,V> generic inference because args become Object
    if ($content -match '\(Object\)"[^"]*"') {
        $content = $content -replace 'Map\.of\(\(Object\)(\w+)', 'Map.of($1'
        $content = $content -replace 'Map\.of\(\(Object\)"([^"]*)"', 'Map.of("$1"'
        $content = $content -replace ',\s*\(Object\)(?=[\w"])', ', '
    }
    
    # Pattern 2: List.of((Object)...) breaks List<T> inference
    $content = $content -replace 'List\.of\(\(Object\)', 'List.of('
    
    # Pattern 3: Set.of((Object)...) breaks Set<T> inference
    $content = $content -replace 'Set\.of\(\(Object\)', 'Set.of('
    
    # Pattern 4: Collections.singletonList((Object)val) -> Collections.singletonList(val)
    $content = $content -replace 'Collections\.singletonList\(\(Object\)', 'Collections.singletonList('
    $content = $content -replace 'Collections\.singletonMap\(\(Object\)', 'Collections.singletonMap('
    
    # Pattern 5: Result.ok((Object)...) - already handled previously but double-check
    # Pattern 6: Optional.of((Object)...) 
    $content = $content -replace 'Optional\.of\(\(Object\)', 'Optional.of('
    
    # Pattern 7: Stream.of((Object)...)
    $content = $content -replace 'Stream\.of\(\(Object\)', 'Stream.of('
    
    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, (New-Object System.Text.UTF8Encoding $false))
        $fixedCount++
    }
}

Write-Output "Fixed $fixedCount files"
