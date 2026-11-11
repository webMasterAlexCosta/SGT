[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [Parameter(Mandatory=$true)][string]$Config,
    [Parameter(Mandatory=$true)][string]$ExpectedSha256,
    [string]$JavaExe = 'java',
    [switch]$AuthorizeInitialAdministrator
)
. "$PSScriptRoot/Sgt-Common.ps1"
if (-not $AuthorizeInitialAdministrator) { throw 'SGT_WINDOWS_EXPLICIT_ADMIN_AUTHORIZATION_REQUIRED' }
$checked = Get-SgtCheckedInputs -Jar $Jar -Config $Config -ExpectedSha256 $ExpectedSha256 -JavaExe $JavaExe
$arguments = @('-Xmx512m', '-XX:MaxMetaspaceSize=256m', '-Duser.timezone=America/Sao_Paulo',
    '-Dloader.main=com.example.sgtoperacao.ComissionadorAdministradorInicial', '-cp', $checked.Jar,
    'org.springframework.boot.loader.launch.PropertiesLauncher', '--spring.profiles.active=sgt-admin',
    "--env-file=$($checked.ConfigPath)", "--spring.config.import=$($checked.ConfigUri)", '--sgt.admin.autorizado=true')
& $checked.Java @arguments
if ($LASTEXITCODE -ne 0) { throw 'SGT_WINDOWS_ADMIN_COMMISSIONING_FAILED: nao repetir sobre banco com dados' }

