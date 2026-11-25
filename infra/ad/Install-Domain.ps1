# Promotes the Windows Server instance of A1 to the domain controller of the staff directory and
# installs the LDAPS certificate issued by the environment CA (KAN-31; KAN-2 ADR-100, ADR-088).
# Run by the SSM document solidaria-<env>-<slot>-ad-bootstrap (infra/terraform/modules/directory).
# Idempotent: re-running it on a domain controller only refreshes the LDAPS certificate.
# Exit code 3010 asks Systems Manager to reboot the instance and run the document again.
param(
    [Parameter(Mandatory = $true)][string] $DomainName,
    [Parameter(Mandatory = $true)][string] $NetbiosName,
    [Parameter(Mandatory = $true)][string] $SecretPrefix,
    [Parameter(Mandatory = $true)][string] $Region
)

$ErrorActionPreference = 'Stop'
Import-Module AWSPowerShell

# Secrets are SecureString parameters: the data subnet reaches only the Systems Manager endpoints
# (ADR-120), and Parameter Store decrypts them on the service side.
function Get-Secret([string] $Name) {
    (Get-SSMParameter -Name "$SecretPrefix/$Name" -WithDecryption $true -Region $Region).Value
}

$isDomainController = (Get-CimInstance Win32_ComputerSystem).DomainRole -ge 4

if (-not $isDomainController) {
    Write-Output 'Installing AD DS and promoting the server to a new forest'
    Install-WindowsFeature AD-Domain-Services -IncludeManagementTools | Out-Null
    $safeMode = ConvertTo-SecureString (Get-Secret 'safe-mode-password') -AsPlainText -Force
    Install-ADDSForest `
        -DomainName $DomainName `
        -DomainNetbiosName $NetbiosName `
        -SafeModeAdministratorPassword $safeMode `
        -InstallDns `
        -NoRebootOnCompletion `
        -Force | Out-Null
    Write-Output 'Promotion finished; rebooting'
    exit 3010
}

Write-Output 'Domain controller found; installing the LDAPS certificate'
$caPem = (Get-SSMParameter -Name ($SecretPrefix -replace '/ad$', '/pki/ca-certificate') -Region $Region).Value
$caFile = Join-Path $env:TEMP 'solidaria-ca.cer'
Set-Content -Path $caFile -Value $caPem -Encoding ascii
Import-Certificate -FilePath $caFile -CertStoreLocation Cert:\LocalMachine\Root | Out-Null

$pfxFile = Join-Path $env:TEMP 'ldaps.pfx'
[IO.File]::WriteAllBytes($pfxFile, [Convert]::FromBase64String((Get-Secret 'ldaps-pfx')))
$pfxPassword = ConvertTo-SecureString (Get-Secret 'ldaps-pfx-password') -AsPlainText -Force
$certificate = Import-PfxCertificate -FilePath $pfxFile -Password $pfxPassword -CertStoreLocation Cert:\LocalMachine\My
Remove-Item $pfxFile, $caFile -Force

# Remove older LDAPS certificates of this server so that AD DS picks the new one.
Get-ChildItem Cert:\LocalMachine\My |
    Where-Object { $_.Subject -eq $certificate.Subject -and $_.Thumbprint -ne $certificate.Thumbprint } |
    Remove-Item

# Ask AD DS to reload its server certificate without a restart.
$rootDse = [ADSI]'LDAP://RootDSE'
$rootDse.Put('renewServerCertificate', 1)
$rootDse.SetInfo()
Write-Output "LDAPS certificate $($certificate.Thumbprint) installed"
