# 注意：防止文件访问权限问题,请先在Power shell 执行
# set-executionpolicy remotesigned
# 选Y
#
# 功能：
#   1. 创建 c:\northstar_env\ 与 c:\northstar_dist\
#   2. 检查并安装 JDK 25，更新用户 PATH
#   3. 准备 RL 强化学习训练的 Python 环境：在 c:\northstar_env\rl-venv 创建虚拟环境，
#      安装 northstar-rl/training/requirements.txt 依赖（numpy/torch/gymnasium 等），
#      并写入用户级环境变量 NS_RL_PYTHON 指向该虚拟环境解释器，
#      应用启动时自动使用（未设置时默认的 python 为系统解释器，缺少依赖会导致训练失败）
#      依赖清单默认按候选路径自动查找（可用 REQUIREMENTS 环境变量指定），找不到时安装内置默认依赖
#      注意：venv 属于运行环境而非源码仓库，生产环境（northstar-dist）下没有 northstar-rl 目录

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
#JDK25下载地址
$JDK25DownloadUrl = "https://download.oracle.com/java/25/latest/jdk-25_windows-x64_bin.msi"

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

# JDK25环境安装  
If(checkCommand java.exe 25*){
    "JDK25 installed"
} else {
	downloadAndInstallMSI $JDK25DownloadUrl $BasePath jdk-25_windows-x64_bin.msi
	$programPath = "C:\Program Files\Java"
	$jdkPath = getInstallPath $programPath jdk-25*
	setEnvironment Java "$jdkPath\bin"
}

$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

# ---------- Python RL 训练环境 ----------
# RL 训练由 Python 子进程执行 training\train_round.py，依赖 numpy/torch/gymnasium；
# venv 与 JDK 一样属于运行环境，统一安装在 c:\northstar_env\rl-venv（生产环境无 northstar-rl 源码目录），
# 并持久化 NS_RL_PYTHON 告知应用。
$VenvDir = Join-Path $BasePath "rl-venv"
$VenvPython = Join-Path $VenvDir "Scripts\python.exe"

# 解析依赖清单：优先 REQUIREMENTS 环境变量；其次按候选路径查找（仓库布局 / 部署目录布局）；
# 均不存在时退回内置默认依赖（与 northstar-rl/training/requirements.txt 保持一致，新增依赖需同步两处）。
function resolveRequirements(){
	if(-not [string]::IsNullOrWhiteSpace($env:REQUIREMENTS) -and (Test-Path $env:REQUIREMENTS)){
		return (Resolve-Path $env:REQUIREMENTS).Path
	}
	$candidates = @(
		(Join-Path $PSScriptRoot "northstar-rl\training\requirements.txt"),	# 脚本在仓库根（开发）
		(Join-Path $PSScriptRoot "training\requirements.txt"),				# 脚本与 training 目录同级（部署）
		"northstar-rl\training\requirements.txt",							# 工作目录为仓库根
		"training\requirements.txt",										# 工作目录为部署目录（northstar-dist）
		"..\northstar-rl\training\requirements.txt"							# 工作目录为模块目录
	)
	foreach($c in $candidates){
		if(Test-Path $c){
			return (Resolve-Path $c).Path
		}
	}
	return $null
}

# 定位可用的 Python（优先 python，其次 Python Launcher 的 py -3），返回命令名与附加参数
function findPython(){
	if(checkCommand python.exe *){
		return @{ Cmd = "python"; Args = @() }
	}
	if(checkCommand py.exe *){
		return @{ Cmd = "py"; Args = @("-3") }
	}
	return $null
}

$systemPython = findPython
if($null -eq $systemPython){
	"WARNING: 未检测到 Python，跳过 RL 训练环境准备（强化学习训练需要 Python 3.10+，请安装后重跑本脚本）"
} else {
	if(!(Test-Path $VenvPython)){
		"Creating RL training venv: $VenvDir"
		& $systemPython.Cmd @($systemPython.Args) -m venv $VenvDir
	}
	if(Test-Path $VenvPython){
		# 可通过 PIP_INDEX_URL 环境变量指定 pip 镜像（如 https://pypi.tuna.tsinghua.edu.cn/simple）
		$pipArgs = @("install", "--disable-pip-version-check")
		if(-not [string]::IsNullOrWhiteSpace($env:PIP_INDEX_URL)){
			$pipArgs += @("-i", $env:PIP_INDEX_URL)
		}
		$Requirements = resolveRequirements
		if($null -eq $Requirements){
			"requirements.txt not found, installing default RL dependencies: torch gymnasium numpy pytest"
			$pipArgs += @("torch", "gymnasium", "numpy", "pytest")
		} else {
			"Installing RL training dependencies from $Requirements ..."
			$pipArgs += @("-r", $Requirements)
		}
		& $VenvPython -m pip install --upgrade pip --disable-pip-version-check -q
		& $VenvPython -m pip @pipArgs
		# 自检：训练脚本依赖 numpy/torch 必须可导入
		& $VenvPython -c "import numpy, torch" 2>$null
		if($LASTEXITCODE -eq 0){
			"RL training Python environment ready: $VenvPython"
			# 持久化到用户环境变量（幂等），并刷新当前会话
			if([Environment]::GetEnvironmentVariable("NS_RL_PYTHON", "User") -ne $VenvPython){
				[Environment]::SetEnvironmentVariable("NS_RL_PYTHON", $VenvPython, "User")
			}
			$env:NS_RL_PYTHON = $VenvPython
		} else {
			"WARNING: RL 依赖自检失败（numpy/torch 不可导入），强化学习训练将不可用。请检查网络后重跑本脚本；如官方源无法访问，可先执行 `$env:PIP_INDEX_URL='https://pypi.tuna.tsinghua.edu.cn/simple' 再重试"
		}
	} else {
		"WARNING: 虚拟环境创建失败，跳过 RL 依赖安装（强化学习训练需要可用的 Python venv）"
	}
}

