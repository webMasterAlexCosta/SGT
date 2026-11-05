$ErrorActionPreference = 'Stop'
$SgtArguments = $args
$SgtProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$SgtPropertiesPath = Join-Path $PSScriptRoot 'maven-wrapper.properties'
$SgtProperties = @{}
foreach ($SgtLine in Get-Content -LiteralPath $SgtPropertiesPath) {
    if ($SgtLine -match '^([^#=]+)=(.*)$') {
        $SgtProperties[$Matches[1].Trim()] = $Matches[2].Trim()
    }
}
$SgtUrl = $SgtProperties['distributionUrl']
$SgtExpectedHash = $SgtProperties['distributionSha256Sum']
if (-not $SgtUrl -or -not $SgtExpectedHash) {
    throw 'URL e SHA-256 da distribuição Maven são obrigatórios.'
}
if ($env:MVNW_REPOURL) {
    $SgtUrl = $SgtUrl.Replace('https://repo.maven.apache.org/maven2', $env:MVNW_REPOURL.TrimEnd('/'))
}
$SgtArchive = [IO.Path]::GetFileName(([Uri]$SgtUrl).AbsolutePath)
if ($SgtArchive -notmatch '^apache-maven-([0-9][^-]*)-bin\.zip$') {
    throw 'Nome de distribuição Maven inválido.'
}
$SgtVersion = $Matches[1]
$SgtUserRepository = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else {
    Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2'
}
$SgtDistributionRoot = Join-Path $SgtUserRepository "wrapper\dists\apache-maven-$SgtVersion-bin\sgt-gtrat"
$SgtMavenCommand = Join-Path $SgtDistributionRoot "apache-maven-$SgtVersion\bin\mvn.cmd"
if (-not (Test-Path -LiteralPath $SgtMavenCommand -PathType Leaf)) {
    $SgtTemporary = Join-Path ([IO.Path]::GetTempPath()) ('sgt-maven-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $SgtTemporary | Out-Null
    try {
        $SgtZip = Join-Path $SgtTemporary $SgtArchive
        Invoke-WebRequest -UseBasicParsing -Uri $SgtUrl -OutFile $SgtZip
        $SgtActualHash = (Get-FileHash -LiteralPath $SgtZip -Algorithm SHA256).Hash
        if (-not $SgtActualHash.Equals($SgtExpectedHash, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'SHA-256 da distribuição Maven não corresponde ao valor esperado.'
        }
        New-Item -ItemType Directory -Force -Path $SgtDistributionRoot | Out-Null
        Expand-Archive -LiteralPath $SgtZip -DestinationPath $SgtDistributionRoot -Force
    } finally {
        Remove-Item -LiteralPath $SgtTemporary -Recurse -Force
    }
}
if (-not (Test-Path -LiteralPath $SgtMavenCommand -PathType Leaf)) {
    throw 'Distribuição Maven não foi extraída corretamente.'
}
& $SgtMavenCommand @SgtArguments
exit $LASTEXITCODE
