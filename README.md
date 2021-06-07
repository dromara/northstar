# Northstar程序化交易平台
基于JAVA的程序化交易平台: Northstar  
本项目基于github上另一个知名的同类项目redtorch演化而来，感谢redtorch项目作者的启发。  
本人几乎从头到尾90%重构了两次，深感开源不易。  
详细文档请参考wiki  

## 运行环境
建议使用Linux云服务器

## 程序架构
- B/S架构
- northstar项目为服务端
- northstar-monitor项目为web网页监控端
- 交互协议HTTP + websocket
- 数据库为MongoDB
- 前端采用nodejs + vue
- 服务端采用java + springboot

## 启动步骤
假设当前环境是全新的服务器  

下载项目
```
cd ~
git clone https://gitee.com/KevinHuangwl/northstar.git
```

准备环境（只需要运行一次）
```
cd ~/northstar
bash env.sh
```

构建程序（每次代码更新后运行）
```
cd ~/northstar
bash build.sh
```

运行程序
```
nohup java -jar -DwsHost=<这里填云服务器内网IP> -Dnsuser=<登陆用户名> -Dnspwd=<登陆密码> ～/northstar.jar &
```

查询日志
```
cd ~/logs/
```

终止程序
```
kill `pgrep java`
```


## 注意事项
- 服务器时间校正
- 本项目为量化爱好者及JAVA开发者搭建，对交易行为并不负责
- 当前项目只能进行手工交易，若要开发量化策略，需要一定的JAVA编程基础