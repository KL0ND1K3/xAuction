param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$tools = Join-Path $root ".tools"
$mavenVersion = "3.9.11"
$mavenHome = Join-Path $tools "apache-maven-$mavenVersion"
$sharedMaven = Join-Path (Split-Path $root) "plugin\.tools\apache-maven-$mavenVersion"
if (Test-Path (Join-Path $sharedMaven "bin\mvn.cmd")) {
    $mavenHome = $sharedMaven
}

$jdk25 = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
if (Test-Path (Join-Path $jdk25 "bin\java.exe")) {
    $env:JAVA_HOME = $jdk25
}

if (-not (Test-Path (Join-Path $mavenHome "bin\mvn.cmd"))) {
    New-Item -ItemType Directory -Force -Path $tools | Out-Null
    $zip = Join-Path $tools "maven.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    Write-Host "Downloading Maven $mavenVersion ..."
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    Expand-Archive -Path $zip -DestinationPath $tools -Force
    Remove-Item $zip
    $mavenHome = Join-Path $tools "apache-maven-$mavenVersion"
}

& (Join-Path $mavenHome "bin\mvn.cmd") -f (Join-Path $root "pom.xml") @MavenArgs
exit $LASTEXITCODE
