param(
    [ValidateSet('preflight','validate','test','package','completo')]
    [string]$Modo = 'preflight'
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $Root
try {
    $javaOutput = (& java -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Java não encontrado no PATH. Instale/configure JDK 17 ou 21.' }
    if ($javaOutput -notmatch 'version "(?:17|21)(?:\.|\")') {
        throw "Use JDK 17 ou 21. Saída encontrada: $javaOutput"
    }
    if (-not (Test-Path 'pom.xml')) { throw 'pom.xml não encontrado.' }
    if (-not (Test-Path 'mvnw.cmd')) { throw 'mvnw.cmd não encontrado.' }

    if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = '-Xmx512m -XX:MaxMetaspaceSize=256m' }
    Write-Host 'Preflight OK: JDK 17/21 e Maven Wrapper disponíveis.'

    switch ($Modo) {
        'preflight' { }
        'validate' { & .\mvnw.cmd -B -ntp validate; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
        'test' { & .\mvnw.cmd -B -ntp clean test; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
        'package' { & .\mvnw.cmd -B -ntp clean package; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
        'completo' {
            if ($env:INTEGRATION_MYSQL_URL -notmatch '^jdbc:mysql://(127\.0\.0\.1|localhost):[0-9]+/(sgt_impl_[A-Za-z0-9_]+|sgt_audit_[A-Za-z0-9_]+|sgt_correcao_exec|sgt8045_(baseline|candidata|upgrade|http|409_red|regressao))(\?.*)?$') {
                throw 'Defina INTEGRATION_MYSQL_URL para um schema sintético local autorizado; sgt8045 aceita somente baseline, candidata, upgrade, http, 409_red ou regressao.'
            }
            & .\mvnw.cmd -B -ntp -Pintegration clean verify jacoco:report
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            & .\mvnw.cmd -B -ntp -Pdb-load verify jacoco:report
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    }
}
finally {
    Pop-Location
}
