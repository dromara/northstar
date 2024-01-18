# 注意：防止文件访问权限问题,请先在Power shell 执行
# set-executionpolicy remotesigned
# 选Y

Add-Type -AssemblyName System.IO
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12


# 下载到northstar_env目录，如无该目录则创建;
echo "Checking c:\northstar_env\"  
$BasePath = "c:\northstar_env\"
If(!(test-path $BasePath)){
   New-Item -Path $BasePath -ItemType Directory
}
# 程序包目录
echo "Checking c:\northstar_dist\"
$DistPath = "c:\northstar_dist\" 
If(!(test-path $DistPath)){
	New-Item -Path $DistPath -ItemType Directory
}
#JDK21下载地址
$JDK21DownloadUrl = "https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.msi"

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

# JDK21环境安装  
If(checkCommand java.exe 21*){
    "JDK21 installed"
} else {
	downloadAndInstallMSI $JDK21DownloadUrl $BasePath jdk-21_windows-x64_bin.msi
	$programPath = "C:\Program Files\Java"
	$jdkPath = getInstallPath $programPath jdk-21*
	setEnvironment Java "$jdkPath\bin"
}

$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User") 

