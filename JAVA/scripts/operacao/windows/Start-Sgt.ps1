[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [Parameter(Mandatory=$true)][string]$Config,
    [Parameter(Mandatory=$true)][string]$ExpectedSha256,
    [string]$JavaExe = 'java',
    [ValidateRange(128,768)][int]$HeapMiB = 512,
    [switch]$CheckOnly
)
. "$PSScriptRoot/Sgt-Common.ps1"
$checked = Get-SgtCheckedInputs -Jar $Jar -Config $Config -ExpectedSha256 $ExpectedSha256 -JavaExe $JavaExe
if ($CheckOnly) {
    [pscustomobject]@{ Verified = $true; JarSha256 = $checked.Hash; JavaMajor = 25; ApplicationStarted = $false } | ConvertTo-Json
    return
}
# Configuracao obrigatoria: sem fallback para .env e sem segredos na linha de comando.
$arguments = @("-Xmx${HeapMiB}m", '-XX:MaxMetaspaceSize=256m', '-Duser.timezone=America/Sao_Paulo',
    '-jar', $checked.Jar, '--spring.profiles.active=prod', "--env-file=$($checked.ConfigPath)", "--spring.config.import=$($checked.ConfigUri)")
& $checked.Java @arguments
if ($LASTEXITCODE -ne 0) { throw 'SGT_WINDOWS_PROCESS_FAILED: confira o log privado' }

