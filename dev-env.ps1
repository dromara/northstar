#检查env.ps1脚本是否已经运行过，没有的话就运行，确保环境准备好   
Invoke-WebRequest https://gitee.com/dromara/northstar/raw/master/env.ps1 -OutFile env.ps1;
. .\env.ps1
$BasePath = "c:\northstar_env\"
$WorkspacePath = "c:\northstar_workspace\"
If(!(test-path $WorkspacePath))
{
   New-Item -Path $WorkspacePath -ItemType Directory
}
#Git下载地址  
$GitDownloadUrl = "https://npm.taobao.org/mirrors/git-for-windows/v2.32.0.windows.1/Git-2.32.0-64-bit.exe"
#Eclipse地址  
$EclipseDownloadUrl = "https://download.springsource.com/release/STS4/4.14.0.RELEASE/dist/e4.23/spring-tool-suite-4-4.14.0.RELEASE-e4.23.0-win32.win32.x86_64.self-extracting.jar"
#northstar仓库地址  
$NorthstarRepository = "https://gitee.com/dromara/northstar.git"

If(checkCommand git.exe *){
	"Git installed"
} else {
	$gitFile = "Git-2.35.1.2-64-bit.exe"
	if(!(test-path $BasePath$gitFile)){
		"Start downloading Git"
		Invoke-WebRequest $GitDownloadUrl -OutFile $BasePath$gitFile
	}
	"Start installing Git"
	Start-Process $BasePath$gitFile -ArgumentList "/silent" -wait
	$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User") 
}

"Cloning repository"
git clone $NorthstarRepository $WorkspacePath\northstar
cd $WorkspacePath\northstar
Start-Process mvn -ArgumentList "clean install -Dmaven.test.skip=true"

$stsJarName = "spring-tool-suite-4-4.14.0.RELEASE-e4.23.0-win32.win32.x86_64.self-extracting.jar"
if(test-path $BasePath$stsJarName){
	"STS package is ready"
} else {
	"Start downloading STS"
	Invoke-WebRequest $EclipseDownloadUrl -OutFile $BasePath$stsJarName
}

if(test-path "$BasePath\sts-4.14.0.RELEASE"){
	"STS is installed"
} else {
	cd $BasePath
	java -jar $BasePath$stsJarName
	"--add-opens=java.base/java.lang=ALL-UNNAMED" >>$BasePath\sts-4.14.0.RELEASE\SpringToolSuite4.ini
}


