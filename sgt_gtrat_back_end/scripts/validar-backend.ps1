param(
    [ValidateSet('preflight','validate','test','package','completo')]
    [string]$Modo = 'preflight'
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $Root
try {
    $javaOutput = (& java -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Java não encontrado no PATH. Instale/configure JDK 17.' }
    if ($javaOutput -notmatch 'version "17(?:\.|\")') {
        throw "JDK 17 é obrigatório. Saída encontrada: $javaOutput"
    }
    if (-not (Test-Path 'pom.xml')) { throw 'pom.xml não encontrado.' }
    if (-not (Test-Path 'mvnw.cmd')) { throw 'mvnw.cmd não encontrado.' }

    Write-Host 'Preflight OK: JDK 17 e Maven Wrapper disponíveis.'

    switch ($Modo) {
        'preflight' { }
        'validate' { & .\mvnw.cmd -B -ntp validate; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
        'test' { & .\mvnw.cmd -B -ntp clean test; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
        'package' { & .\mvnw.cmd -B -ntp clean package; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
        'completo' {
            & .\mvnw.cmd -B -ntp clean test
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            & .\mvnw.cmd -B -ntp -DskipTests package
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    }
}
finally {
    Pop-Location
}
