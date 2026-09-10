# Comprehensive fix script v2 - CAREFUL and targeted
$ErrorActionPreference = "Continue"
$javaSrcDir = "C:\Users\Clay\Qoder\bsball-backend\src\main\java\com\bsball"
$files = Get-ChildItem -Path $javaSrcDir -Recurse -Filter "*.java"

$totalFixed = 0

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    $origLen = $content.Length
    
    # ---- SAFE FIXES ----
    
    # Fix 1: "getLogger(Xxx.class)" without static import -> "LoggerFactory.getLogger(Xxx.class)"
    $content = $content -replace '(?<![.\w])getLogger\(', 'LoggerFactory.getLogger('
    
    # Fix 2: Remove remaining "ParentClass.InnerClass" references in SAME file
    # Only fix references that match the file's own inner classes
    # Pattern: AuthCaptchaService.CaptchaEntry (where AuthCaptchaService is the outer class)
    # We can detect the outer class name from the file name
    $className = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    
    # Fix references like "OuterClass.InnerClass" -> "InnerClass" 
    # But ONLY when it's NOT a method call, import, or qualified reference
    # Safe patterns: "new OuterClass.InnerClass(" -> "new InnerClass("
    $content = $content -replace "new\s+$className\.(\w+)\(", 'new $1('
    
    # "return OuterClass.InnerClass(" -> "return InnerClass("
    $content = $content -replace "return\s+$className\.(\w+)\(", 'return $1('
    
    # "(OuterClass.InnerClass)" -> "(InnerClass)"  
    $content = $content -replace "\($className\.(\w+)\)", '($1)'
    
    # "instanceof OuterClass.InnerClass" -> "instanceof InnerClass"
    $content = $content -replace "instanceof\s+$className\.(\w+)", 'instanceof $1'
    
    # "List<OuterClass.InnerClass>" -> "List<InnerClass>"
    $content = $content -replace "<$className\.(\w+)>", '<$1>'
    
    # Fix 3: AuthApi.java specific - "v0" variable issue
    if ($f.Name -eq "AuthApi.java") {
        # Replace v0 with userId in the specific context
        $content = $content -replace '\bv0\b', 'userId'
    }
    
    # Fix 4: Remove unnecessary (Object) casts in Map.of ONLY
    # These break generic inference by widening Map<K,V> to Map<Object,Object>
    $content = $content -replace 'Map\.of\(\(Object\)(\w+)', 'Map.of($1'
    $content = $content -replace 'Map\.of\(\(Object\)"([^"]*)"', 'Map.of("$1"'
    # For multi-arg Map.of: Map.of(key, (Object)val) -> Map.of(key, val)
    $content = $content -replace '(Map\.of\([^,]+),\s*\(Object\)', '$1, '
    
    if ($content.Length -ne $origLen) {
        [System.IO.File]::WriteAllText($f.FullName, $content, (New-Object System.Text.UTF8Encoding $false))
        $totalFixed++
    }
}

Write-Output "Total files modified: $totalFixed"
