[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$BaseUrl)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$uri = [uri]$BaseUrl
if ($uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -notin @('', '/')) {
    throw 'SGT_WINDOWS_HTTPS_ORIGIN_REQUIRED'
}
$origin = $uri.GetLeftPart([UriPartial]::Authority)
$oldProtocols = [Net.ServicePointManager]::SecurityProtocol
try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    # Validacao normal de cadeia e hostname do Windows: nao instala certificados
    # nem altera callbacks de validacao, politicas ou firewall.
    $root = Invoke-WebRequest -Uri "$origin/" -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 15
    $csrf = Invoke-WebRequest -Uri "$origin/api/autenticacao/csrf" -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 15
    $body = $csrf.Content | ConvertFrom-Json
    if ($null -ne $body.PSObject.Properties['data']) { $body = $body.data }
    if ($root.StatusCode -ne 200 -or $csrf.StatusCode -ne 200 -or $null -eq $body.PSObject.Properties['headerName']) {
        throw 'SGT_WINDOWS_HTTPS_SMOKE_FAILED'
    }
    if (-not $csrf.Headers['Strict-Transport-Security']) { throw 'SGT_WINDOWS_HSTS_REQUIRED' }
    [pscustomobject]@{ Verified = $true; HttpsStatus = 200; CsrfEndpoint = 200; HstsPresent = $true; CredentialsUsed = $false } | ConvertTo-Json
} finally {
    [Net.ServicePointManager]::SecurityProtocol = $oldProtocols
}

