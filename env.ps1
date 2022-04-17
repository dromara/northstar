# 注意：防止文件访问权限问题,请先在Power shell 执行
# set-executionpolicy remotesigned
# 选Y

Add-Type -AssemblyName System.IO
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

#下载到northstar_env目录，如无该目录则创建;   
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

# 检查环境  
function checkCommand([string] $name, [string] $checkPattern){
	$results = Get-Command $name -ErrorAction SilentlyContinue | Where-Object {$_.Version -like $checkPattern}
	return $results.Count -gt 0
}
# 检查服务   
function checkService([string] $name){
	$results = Get-Service $name -ErrorAction SilentlyContinue | Where-Object {$_.Version -like $checkPattern}
	return $results.Count -gt 0
}

# 下载安装   
function downloadAndInstallMSI([string] $url, [string] $destPath, [string] $fileName){
	$error.Clear()
	if(!(test-path $destPath$fileName)){
		"Start downloading $fileName"
		Invoke-WebRequest -Uri $url -OutFile "$destPath$fileName"
	}
	if($error.Count -eq 0){	
		"Start installing $fileName"
		Start-Process msiexec.exe -ArgumentList "/log log_$fileName.txt /i $destPath$fileName /qr $args" -wait
		"$fileName installed"
	} else {
		"Something wrong with $fileName installing. Retry later."
	}
}

# 设置环境变量  
function setEnvironment([string] $name, [string] $path){
	if([Environment]::getEnvironmentVariable("Path", "User") -like "*$path*"){	
		"$Name environment is ready"
	} else {
		$fullPath = "$path;" + [Environment]::getEnvironmentVariable("Path", "User")
		[Environment]::SetEnvironmentVariable("Path", $fullPath, 'User')
	}
}

#定位安装目录  
function getInstallPath([string] $basePath, [string] $pattern){
	$path = Get-ChildItem $basePath | Where-Object {$_.Name -like $pattern}
	return $path.fullName
}

#JDK17环境安装  
If(checkCommand java.exe 17*){
    "JDK17 installed"
} else {
	downloadAndInstallMSI $JDK17DownloadUrl $BasePath jdk-17_windows-x64_bin.msi
	$programPath = "C:\Program Files\Java"
	$jdkPath = getInstallPath $programPath jdk-17*
	setEnvironment Java "$jdkPath\bin"
}

#Node14环境安装  
If(checkCommand node.exe 14*){
	"Node14 installed"
} else {
	downloadAndInstallMSI $Node14DownloadUrl $BasePath node-v14.19.0-x64.msi
	$nodePath = "C:\Program Files\nodejs"
	setEnvironment Node $nodePath
}

#MongoDB环境安装  
If(checkService *mongo*){
	"MongoDB installed"
} else {
	$settings = "ADDLOCAL=ServerService,Server,ProductFeature,Client,MonitoringTools,ImportExportTools,Router,MiscellaneousTools"
	downloadAndInstallMSI $MongoDownloadUrl $BasePath mongodb-win32-x86_64-2008plus-ssl-4.0.22-signed.msi $settings
}

#Maven环境安装  
If(checkCommand mvn *){
	"Maven installed"
} else {
	$targetFile = "apache-maven-3.6.3-bin.zip"
	if(!(test-path "C:\northstar_env\apache-maven-3.6.3")){
		"Start downloading $targetFile"
		Invoke-WebRequest -Uri $MavenDownloadUrl -OutFile "$BasePath$targetFile"
		Expand-Archive $BasePath$targetFile -DestinationPath $BasePath
		"Unzipped $targetFile"
		Remove-Item "$BasePath$targetFile"
	}
	$mvnPath = getInstallPath $BasePath *maven*
	setEnvironment Maven "$mvnPath\bin"
}

refreshenv

