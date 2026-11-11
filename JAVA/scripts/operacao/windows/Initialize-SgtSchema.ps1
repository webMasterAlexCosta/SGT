[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [Parameter(Mandatory=$true)][string]$Config,
    [Parameter(Mandatory=$true)][string]$ExpectedSha256,
    [string]$JavaExe = 'java',
    [switch]$AuthorizeNewDatabase
)
. "$PSScriptRoot/Sgt-Common.ps1"
if (-not $AuthorizeNewDatabase) { throw 'SGT_WINDOWS_EXPLICIT_NEW_DATABASE_AUTHORIZATION_REQUIRED' }
$checked = Get-SgtCheckedInputs -Jar $Jar -Config $Config -ExpectedSha256 $ExpectedSha256 -JavaExe $JavaExe
$arguments = @('-Xmx512m', '-XX:MaxMetaspaceSize=256m', '-Duser.timezone=America/Sao_Paulo',
    '-Dloader.main=com.example.sgtoperacao.PreparadorSchemaNovo', '-cp', $checked.Jar,
    'org.springframework.boot.loader.launch.PropertiesLauncher', '--spring.profiles.active=sgt-schema',
    "--env-file=$($checked.ConfigPath)", "--spring.config.import=$($checked.ConfigUri)", '--sgt.schema.autorizado=true')
& $checked.Java @arguments
if ($LASTEXITCODE -ne 0) { throw 'SGT_WINDOWS_SCHEMA_PREPARATION_FAILED: nao apagar nem repetir sobre acervo existente' }
