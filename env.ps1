# 注意：防止文件访问权限问题,请先在Power shell 执行
# set-executionpolicy remotesigned
# 选Y

Add-Type -AssemblyName System.IO
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# 程序包目录
$DistPath = "c:\\northstar_dist\" 
# 下载到northstar_env目录，如无该目录则创建;  
$BasePath = "c:\\northstar_env\"
If(!(test-path $BasePath)){
   New-Item -Path $BasePath -ItemType Directory
}
If(!(test-path $DistPath)){
	New-Item -Path $DistPath -ItemType Directory
}
#JDK17下载地址
$JDK17DownloadUrl = "https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.msi"
#Redis下载地址
$RedisDownloadUrl = "https://gitee.com/dromara/northstar/attach_files/1077290/download"

# 检查环境  
function checkCommand([string] $name, [string] $checkPattern){
	$results = Get-Command $name -ErrorAction SilentlyContinue | Where-Object {$_.Version -like $checkPattern}
	return $results.Count -gt 0
}
# 检查服务   
function checkService([string] $name){
	$results = Get-Service $name -ErrorAction SilentlyContinue 
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

# 定位安装目录  
function getInstallPath([string] $basePath, [string] $pattern){
	$path = Get-ChildItem $basePath | Where-Object {$_.Name -like $pattern}
	return $path.fullName
}

# JDK17环境安装  
If(checkCommand java.exe 17*){
    "JDK17 installed"
} else {
	downloadAndInstallMSI $JDK17DownloadUrl $BasePath jdk-17_windows-x64_bin.msi
	$programPath = "C:\Program Files\Java"
	$jdkPath = getInstallPath $programPath jdk-17*
	setEnvironment Java "$jdkPath\bin"
}

# Redis环境安装
If(checkService redis){
	"Redis installed"
} else {
	downloadAndInstallMSI $RedisDownloadUrl $BasePath Redis-x64-3.0.504.msi
}

$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User") 

