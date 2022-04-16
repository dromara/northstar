# 注意：防止文件访问权限问题,请先在Power shell 执行
# set-executionpolicy remotesigned
# 选Y

Add-Type -AssemblyName System.IO
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
#下载到northstar_env目录，如无该目录则创建
$BasePath = "c:\\northstar_env\"
If(!(test-path $BasePath))
{
   New-Item -Path $BasePath -ItemType Directory
}

#JDK17下载地址
$JDK17DownloadUrl = "https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.msi"
#Node14下载地址
$Node14DownloadUrl = "https://registry.npmmirror.com/-/binary/node/latest-v14.x/node-v14.19.0-x64.msi"
#MongoDB下载地址
$MongoDownloadUrl = "http://downloads.mongodb.org/win32/mongodb-win32-x86_64-2008plus-ssl-4.0.22-signed.msi"
#Maven下载地址
$MavenDownloadUrl = "https://mirrors.bfsu.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip"

#检查环境
function checkExist([string] $name, [string] $checkPattern){
	$results = Get-Command $name -ErrorAction SilentlyContinue | Where-Object {$_.Version -like $checkPattern}
	return $results.Count -gt 0
}

#下载安装 
function downloadAndInstallMSI([string] $url, [string] $destPath, [string] $fileName){
	"Start download $fileName"
	Invoke-WebRequest -Uri $url -OutFile "$destPath$fileName"
	"Start install $fileName"
	msiexec.exe /log "$fileName.txt" /i "$destPath$fileName" /qr $args
	"$fileName installed"
}

#下载解压
function downloadAndUnzip([string] $url, [string] $targetFile, [string] $destPath){
	"Start download $targetFile"
	Invoke-WebRequest -Uri $url -OutFile "$destPath$targetFile"
	Expand-Archive $destPath$targetFile -DestinationPath $destPath
	"Unzipped $targetFile"
}

#定位安装目录
function getInstallPath([string] $basePath, [string] $pattern){
	$path = Get-ChildItem $basePath | Where-Object {$_.Name -like $pattern}
	return $path.fullName
}

#设置环境变量
function setEnvPath([string] $key, [string] $val){
	[Environment]::SetEnvironmentVariable($key, $val, 'User')
}

#JDK17环境安装
If(checkExist java.exe 17*){
    "JDK17 installed"
} else {
	downloadAndInstallMSI $JDK17DownloadUrl $BasePath jdk-17_windows-x64_bin.msi
}

#Node14环境安装
If(checkExist node.exe 14*){
	"Node14 installed"
} else {
	downloadAndInstallMSI $Node14DownloadUrl $BasePath node-v14.19.0-x64.msi
}

#MongoDB环境安装
If(checkExist mongo.exe 4*){
	"MongoDB installed"
} else {
	downloadAndInstallMSI $MongoDownloadUrl $BasePath mongodb-win32-x86_64-2008plus-ssl-4.0.22-signed.msi SHOULD_INSTALL_COMPASS=0 MONGO_SERVICE_INSTALL=1
}

#Maven环境安装
If(checkExist mvn.exe 3.6*){
	"Maven installed"
} else {
	downloadAndUnzip $MavenDownloadUrl apache-maven-3.6.3-bin.zip $BasePath
	Remove-Item $BasePath\apache-maven-3.6.3-bin.zip
	$mvnPath = getInstallPath $BasePath *maven*
	$path = "$mvnPath\bin;" + [Environment]::getEnvironmentVariable("Path")
	setEnvPath Path $path
}
