# 检查JDK环境
# Get-CimInstance -Class Win32_Product |
#   Where-Object Name -eq "JDK17"
#
# Get-CimInstance -Class Win32_Product |
#   Where-Object Name -eq "JDK17" |
#     Format-List -Property *
# invoke-command -computername machine1, machine2 -filepath c:\Script\script.ps1
Add-Type -AssemblyName System.IO
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

#下载到Temp目录
$TempPath = $env:TEMP
#==============================全局变量区=========================#
[bool]$isDownloaded=$False
#==============================下载函数===========================#
Function Download([String]$url, [String]$fullFileName)
{
    #存储的完整文件路径
    $FullPath = "$TempPath\$fullFileName"
    if([String]::IsNullOrEmpty($url) -or [String]::IsNullOrEmpty($FullPath)){
        return $false;
    }

    try {
        $client = New-Object System.Net.WebClient
        $client.UseDefaultCredentials = $True

        #监视WebClient 的下载完成事件
        Register-ObjectEvent -InputObject $client -EventName DownloadFileCompleted `
        -SourceIdentifier Web.DownloadFileCompleted -Action {
            #下载完成，结束下载
            $Global:isDownloaded = $True
        }
        #监视WebClient 的进度事件
        Register-ObjectEvent -InputObject $client -EventName DownloadProgressChanged `
        -SourceIdentifier Web.DownloadProgressChanged -Action {
            #将下载的进度信息记录到全局的Data对象中
            $Global:Data = $event
        }

        $Global:isDownloaded =$False

        #监视PowerShell退出事件
        Register-EngineEvent -SourceIdentifier ([System.Management.Automation.PSEngineEvent]::Exiting) -Action {
            #PowerShell 结束事件
            Get-EventSubscriber | Unregister-Event
            Get-Job | Remove-Job -Force
        }

         #启用定时器，设置1秒一次输出下载进度
        $timer = New-Object timers.timer
        # 1 second interval
        $timer.Interval = 1000
        #Create the event subscription
        Register-ObjectEvent -InputObject $timer -EventName Elapsed -SourceIdentifier Timer.Output -Action {
            $percent = $Global:Data.SourceArgs.ProgressPercentage
            $totalBytes = $Global:Data.SourceArgs.TotalBytesToReceive
            $receivedBytes = $Global:Data.SourceArgs.BytesReceived
            If ($percent -ne $null) {
                 #这里你可以选择将进度显示到命令行 也可以选择将进度写到文件，具体看自己需求
                 #我这里选择将进度输出到命令行
                    Write-Host "current download percent: $percent % downloaded : $receivedBytes totalBytes: $totalBytes"
                    If ($percent -eq 100) {
                        Write-Host "download complete!"
                        $isDownloaded = $True

                         Write-Host "Finish 1 "
                         #清除监视
#                          Get-EventSubscriber | Unregister-Event
                         Write-Host "Finish 2 "
                         Get-Job | Remove-Job -Force
                         Write-Host "Finish 3"
                         #关闭下载线程
                         $client.Dispose()
                         Write-Host "Finish 4"
                         Remove-Variable client
                         Write-Host "Finish "

                        $timer.Enabled = $False

                    }
            }
        }
        If (-Not $isDownloaded) {
            $timer.Enabled = $True
        }

        #使用异步方式下载文件
        $client.DownloadFileAsync($url, $FullPath)
        While (-Not $isDownloaded)
        {
            #等待下载线程结束
            Start-Sleep -m 100
        }
    } catch {
        return $false;
    }
    return $true;
}

#JDK17下载地址
$JDK17DownloadUrl = "https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe"
#Node14下载地址
$Node14DownloadUrl = "https://registry.npmmirror.com/-/binary/node/latest-v14.x/node-v14.19.0-x64.msi"
#MavenDownloadUrl下载地址
$MavenDownloadUrl = "https://mirrors.bfsu.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz"
#MongoDB下载地址
$MongoDownloadUrl = "https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe"

"Start download JDK17..."
#下载的文件名
$FileName = "jdk-17_windows-x64_bin.exe"
Download $JDK17DownloadUrl $FileName
"Download JDK17 finished..."

"Start install JDK17..."
Invoke-CimMethod -ClassName Win32_Product -MethodName Install -Arguments @{PackageLocation=$FullPath}
"Install JDK17 finished..."

