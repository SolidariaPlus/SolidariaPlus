# Creates the six staff groups and the 30 synthetic staff accounts of staff.csv (KAN-31;
# KAN-2 ADR-084, ADR-100). Synthetic data only: no real person is represented.
# MS-17 maps the groups to roles: sp-tellers (Teller), sp-board (Board), sp-board-president
# (Board, President function), sp-compliance-delegate (Board, Compliance function),
# sp-accountants (Accountant), sp-external-consultants (External Consultant).
# Idempotent: existing groups and users are kept, and missing memberships are added.
param(
    [Parameter(Mandatory = $true)][string] $CsvPath,
    [Parameter(Mandatory = $true)][string] $SecretPrefix,
    [Parameter(Mandatory = $true)][string] $Region
)

$ErrorActionPreference = 'Stop'

if ((Get-CimInstance Win32_ComputerSystem).DomainRole -lt 4) {
    Write-Output 'Not a domain controller yet; the accounts are created after the promotion'
    exit 0
}

Import-Module ActiveDirectory
Import-Module AWSPowerShell

$domain = Get-ADDomain
$ouPath = "OU=Staff,$($domain.DistinguishedName)"
if (-not (Get-ADOrganizationalUnit -Filter "DistinguishedName -eq '$ouPath'" -ErrorAction SilentlyContinue)) {
    New-ADOrganizationalUnit -Name 'Staff' -Path $domain.DistinguishedName -ProtectedFromAccidentalDeletion $true
}

$initialPassword = ConvertTo-SecureString `
    (Get-SSMParameter -Name "$SecretPrefix/staff-initial-password" -WithDecryption $true -Region $Region).Value `
    -AsPlainText -Force

$staff = Import-Csv -Path $CsvPath -Encoding UTF8
foreach ($groupName in ($staff.group | Sort-Object -Unique)) {
    if (-not (Get-ADGroup -Filter "Name -eq '$groupName'" -ErrorAction SilentlyContinue)) {
        New-ADGroup -Name $groupName -GroupScope Global -GroupCategory Security -Path $ouPath
        Write-Output "group  $groupName"
    }
}

foreach ($person in $staff) {
    if (-not (Get-ADUser -Filter "SamAccountName -eq '$($person.sam)'" -ErrorAction SilentlyContinue)) {
        New-ADUser `
            -SamAccountName $person.sam `
            -UserPrincipalName "$($person.sam)@$($domain.DNSRoot)" `
            -Name "$($person.given_name) $($person.surname) ($($person.sam))" `
            -GivenName $person.given_name `
            -Surname $person.surname `
            -DisplayName "$($person.given_name) $($person.surname)" `
            -Path $ouPath `
            -AccountPassword $initialPassword `
            -Enabled $true `
            -ChangePasswordAtLogon $false
        Write-Output "user   $($person.sam)"
    }
    Add-ADGroupMember -Identity $person.group -Members $person.sam
}

Write-Output "Staff directory ready: $($staff.Count) accounts"
