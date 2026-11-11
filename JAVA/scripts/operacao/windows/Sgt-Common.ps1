Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-SgtPrivateFile {
    param([Parameter(Mandatory=$true)][string]$Path)
    $file = Get-Item -LiteralPath $Path
    if ($file.PSIsContainer -or ($file.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'SGT_WINDOWS_PRIVATE_FILE_INVALID'
    }
    $me = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $allowed = @($me, 'S-1-5-18', 'S-1-5-32-544')
    $acl = Get-Acl -LiteralPath $file.FullName
    foreach ($rule in $acl.Access) {
        $sid = $rule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value
        if ($rule.AccessControlType -eq 'Allow' -and $sid -notin $allowed) {
            throw 'SGT_WINDOWS_PRIVATE_ACL_REQUIRED'
        }
    }
    return $file.FullName
}

function Get-SgtCheckedInputs {
    param([string]$Jar, [string]$Config, [string]$ExpectedSha256, [string]$JavaExe)
    if ($env:OS -ne 'Windows_NT') { throw 'SGT_WINDOWS_NATIVE_TEST_REQUIRED' }
    if ($ExpectedSha256 -notmatch '^[0-9a-fA-F]{64}$') { throw 'SGT_WINDOWS_EXPECTED_HASH_REQUIRED' }
    $jarFile = Get-Item -LiteralPath $Jar
    if ($jarFile.PSIsContainer -or ($jarFile.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'SGT_WINDOWS_JAR_INVALID'
    }
    $actualHash = (Get-FileHash -LiteralPath $jarFile.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $ExpectedSha256.ToLowerInvariant()) { throw 'SGT_WINDOWS_JAR_HASH_MISMATCH' }
    $privateConfig = Assert-SgtPrivateFile -Path $Config
    $javaCommand = Get-Command $JavaExe -CommandType Application -ErrorAction Stop
    $versionLines = @(& $javaCommand.Source --version)
    if ($LASTEXITCODE -ne 0 -or $versionLines.Count -eq 0 -or $versionLines[0] -notmatch '^(openjdk|java) 25([. ]|$)') {
        throw 'SGT_WINDOWS_JAVA25_REQUIRED'
    }
    return [pscustomobject]@{
        Jar = $jarFile.FullName
        Hash = $actualHash
        ConfigUri = ([uri]$privateConfig).AbsoluteUri
        ConfigPath = $privateConfig.Replace('\', '/')
        Java = $javaCommand.Source
    }
}

