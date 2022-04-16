#检查env.ps1脚本是否已经运行过，没有的话就运行，确保环境准备好   
Invoke-WebRequest https://gitee.com/dromara/northstar/raw/master/env.ps1 -OutFile env.ps1;
powershell -noexit ".\env.ps1"
. $profile
$BasePath = "c:\northstar_env\"
$WorkspacePath = "c:\northstar_workspace\"

#Git下载地址  
$GitDownloadUrl = "https://github.com/git-for-windows/git/releases/download/v2.35.1.windows.2/Git-2.35.1.2-64-bit.exe"
#Eclipse地址  
$EclipseDownloadUrl = "https://download.springsource.com/release/STS4/4.14.0.RELEASE/dist/e4.23/spring-tool-suite-4-4.14.0.RELEASE-e4.23.0-win32.win32.x86_64.self-extracting.jar"
#northstar仓库地址  
$NorthstarRepository = "https://gitee.com/dromara/northstar.git"

cd $WorkspacePath
"Cloning repository"
git clone $NorthstarRepository
cd northstar
Start-Process mvn -ArgumentList "clean install -Dmaven.test.skip=true"

$stsJarName = "spring-tool-suite-4-4.14.0.RELEASE-e4.23.0-win32.win32.x86_64.self-extracting.jar"
if(!(test-path $BasePath$stsJarName)){
	"Start downloading STS"
	Invoke-WebRequest $EclipseDownloadUrl -OutFile $BasePath$stsJarName
	java -jar $BasePath$stsJarName
}