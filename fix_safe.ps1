# SAFE fix script - only uses patterns verified to not break code
$ErrorActionPreference = "Continue"
$javaSrcDir = "C:\Users\Clay\Qoder\bsball-backend\src\main\java\com\bsball"

Write-Output "=== Step 1: Merge inner classes ==="
# Already done via merge_inners.ps1 - skipping

Write-Output "=== Step 2: Fix BOM ==="
$files = Get-ChildItem -Path $javaSrcDir -Recurse -Filter "*.java"
foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $newBytes = $bytes[3..($bytes.Length-1)]
        [System.IO.File]::WriteAllBytes($f.FullName, $newBytes)
    }
}

Write-Output "=== Step 3: Fix declarations and references ==="
$fixCount = 0
foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    $orig = $content
    
    # Fix record/enum/class/interface declarations with parent prefix
    $content = $content -replace '\brecord\s+\w+\.(\w+)', 'record $1'
    $content = $content -replace '\bclass\s+\w+\.(\w+)', 'class $1'
    $content = $content -replace '\binterface\s+\w+\.(\w+)', 'interface $1'
    $content = $content -replace '\benum\s+\w+\.(\w+)', 'enum $1'
    $content = $content -replace '1PortalRole', 'PortalRole'
    
    # Fix getLogger static import issue: getLogger( -> LoggerFactory.getLogger(
    $content = $content -replace '(?<![.\w])getLogger\(', 'LoggerFactory.getLogger('
    
    # Fix remaining "OuterClass.InnerClass" references
    # SAFE: only replace when used as type (after 'new', 'instanceof', in cast, in generics)
    $className = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    
    # "new OuterClass.InnerClass(" -> "new InnerClass("
    $content = $content -replace "new\s+$className\.(\w+)\(", 'new $1('
    
    # "instanceof OuterClass.InnerClass" -> "instanceof InnerClass"
    $content = $content -replace "instanceof\s+$className\.(\w+)", 'instanceof $1'
    
    # "List<OuterClass.InnerClass>" -> "List<InnerClass>"
    $content = $content -replace "<$className\.(\w+)>", '<$1>'
    
    # Cast: "(OuterClass.InnerClass)" -> "(InnerClass)"  but NOT "(OuterClass.class)"!
    $content = $content -replace "\($className\.(?!class\b)(\w+)\)", '($1)'
    
    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, (New-Object System.Text.UTF8Encoding $false))
        $fixCount++
    }
}
Write-Output "  Fixed $fixCount files"

Write-Output "=== Step 4: Fix constructors in known files ==="
$druidFile = "$javaSrcDir\core\DruidAdFilter.java"
$authFile = "$javaSrcDir\service\AuthCaptchaService.java"
foreach ($fp in @($druidFile, $authFile)) {
    $content = Get-Content $fp -Raw -Encoding UTF8
    $orig = $content
    # Fix constructor pattern: leading spaces + "OuterClass.InnerClass("
    $content = $content -replace '^(\s+)(\w+)\.(\w+)\(', '$1$3('
    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($fp, $content, (New-Object System.Text.UTF8Encoding $false))
        Write-Output "  Constructor fix: $([System.IO.Path]::GetFileName($fp))"
    }
}

Write-Output "=== Step 5: Fix known specific issues ==="
# AuthApi.java: v0 variable
$authApiFile = "$javaSrcDir\api\AuthApi.java"
$content = Get-Content $authApiFile -Raw -Encoding UTF8
$content = $content -replace '\bv0\b', 'userId'
[System.IO.File]::WriteAllText($authApiFile, $content, (New-Object System.Text.UTF8Encoding $false))

# EmailService: add javax.mail imports
$emailFile = "$javaSrcDir\service\EmailService.java"
$content = Get-Content $emailFile -Raw -Encoding UTF8
$content = $content -replace 'import jakarta.mail.Message;', "import jakarta.mail.Authenticator;`r`nimport jakarta.mail.Message;`r`nimport jakarta.mail.PasswordAuthentication;"
[System.IO.File]::WriteAllText($emailFile, $content, (New-Object System.Text.UTF8Encoding $false))

Write-Output "=== DONE ==="