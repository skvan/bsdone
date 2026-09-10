# Batch fix raw types - maps repository names to entity types
$javaSrcDir = "C:\Users\Clay\Qoder\bsball-backend\src\main\java\com\bsball"

# Repository name -> Entity type mapping
$repoMap = @{
    'sysApiRepository' = 'SysApi'
    'sysRoleRepository' = 'SysRole'
    'sysMenuRepository' = 'SysMenu'
    'sysUserRepository' = 'SysUser'
    'sysConfigRepository' = 'SysConfig'
    'sysDictDataRepository' = 'SysDictData'
    'sysDictTypeRepository' = 'SysDictType'
    'sysRoleApiRepository' = 'SysRoleApi'
    'sysRoleMenuRepository' = 'SysRoleMenu'
    'sysMenuApiRepository' = 'SysMenuApi'
    'sysUserRoleRepository' = 'SysUserRole'
    'sysUserTenantRepository' = 'SysUserTenant'
    'sysNoticeRepository' = 'SysNotice'
    'sysArticleRepository' = 'SysArticle'
    'sysLoginLogRepository' = 'SysLoginLog'
    'sysOperationLogRepository' = 'SysOperationLog'
    'sysResourceRepository' = 'SysResource'
    'sysMediaIconRepository' = 'SysMediaIcon'
    'sysMediaGalleryItemRepository' = 'SysMediaGalleryItem'
    'coachRepository' = 'Coach'
    'teamRepository' = 'Team'
    'playerRepository' = 'Player'
    'gameRepository' = 'Game'
    'stadiumRepository' = 'Stadium'
    'teamManagerRepository' = 'TeamManager'
    'gamePlayerStatRepository' = 'GamePlayerStat'
    'highlightMomentRepository' = 'HighlightMoment'
    'historyRecordRepository' = 'HistoryRecord'
    'playerClaimRepository' = 'PlayerClaim'
    'portalFeedbackRepository' = 'PortalFeedback'
    'portalVisitHitRepository' = 'PortalVisitHit'
    'portalDevtoolsReportRepository' = 'PortalDevtoolsReport'
    'teamLineupTemplateRepository' = 'TeamLineupTemplate'
    'stadiumHomeTeamRepository' = 'StadiumHomeTeam'
    'eventRepository' = 'Event'
    'leagueRepository' = 'League'
    'ipLocationCacheRepository' = 'IpLocationCache'
}

$files = Get-ChildItem -Path $javaSrcDir -Recurse -Filter "*.java"
$totalFixed = 0

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    $orig = $content
    
    # Fix: "List xxx = this.yyyRepository.findAll()" -> "List<Entity> xxx = ..."
    foreach ($repo in $repoMap.Keys) {
        $entity = $repoMap[$repo]
        # findAll()
        $content = $content -replace "List (\w+) = this\.$repo\.findAll\(\)", "List<$entity> `$1 = this.$repo.findAll()"
        # findAll(spec, pageable)
        $content = $content -replace "List (\w+) = this\.$repo\.findAll\(", "List<$entity> `$1 = this.$repo.findAll("
        # findAllById
        $content = $content -replace "List (\w+) = this\.$repo\.findAllById\(", "List<$entity> `$1 = this.$repo.findAllById("
        # findBy... methods returning List
        $content = $content -replace "List (\w+) = this\.$repo\.findBy", "List<$entity> `$1 = this.$repo.findBy"
    }
    
    # Fix: "(Iterable)roleIds" -> "roleIds"
    $content = $content -replace '\(Iterable\)(\w+)', '$1'
    
    # Fix: "(Collection)xxx" -> "xxx" in List.copyOf/Set.copyOf context
    $content = $content -replace 'List\.copyOf\(\(Collection\)', 'List.copyOf('
    $content = $content -replace 'Set\.copyOf\(\(Collection\)', 'Set.copyOf('
    
    # Fix: "new ArrayList(xxx)" -> "new ArrayList<>(xxx)"
    $content = $content -replace 'new ArrayList\((\w+)\)', 'new ArrayList<>($1)'
    $content = $content -replace 'new HashSet\((\w+)\)', 'new HashSet<>($1)'
    $content = $content -replace 'new HashMap\((\w+)\)', 'new HashMap<>($1)'
    
    # Fix: remove unnecessary (String) casts on String method calls
    $content = $content -replace '\(String\)(\w+)\.getPath\(\)', '$1.getPath()'
    $content = $content -replace '\(String\)(\w+)\.getMethod\(\)', '$1.getMethod()'
    $content = $content -replace '\(String\)(\w+)\.getCode\(\)', '$1.getCode()'
    $content = $content -replace '\(String\)(\w+)\.getName\(\)', '$1.getName()'
    
    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, (New-Object System.Text.UTF8Encoding $false))
        $totalFixed++
        Write-Output "Fixed: $($f.Name)"
    }
}

Write-Output "Total files fixed: $totalFixed"
