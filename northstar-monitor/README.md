# Northstar 盈富量化平台网页监控端

## 开发环境要求
使用nodejs14版本，其他版本会有各种奇怪问题。
可使用国内npm镜像加速
```
npm config set registry https://registry.npmmirror.com
```

## 开发环境启动
注意在开发环境下，通讯没有加密，协议是HTTP与ws
```
npm start
```
服务端也同样需要采用dev的profile启动。否则会“出现服务端未启动”提示

## 生产环境构建
在生产环境下，项目会统一打包进springboot的JAR包中，并不需要额外配置其他HTTP服务来加载静态资源。通讯协议会加TLS加密，因此使用HTTPS访问（wss同理）

## 参考项目
[KLineChart](https://github.com/liihuu/KLineChart)，一个很牛逼的K线控件库。
- 参考示例：https://liihuu.github.io/KLineChart/ （可能需要翻墙）
- 代码示例：https://github.com/liihuu/KLineChartSample
- 参考文档：https://www.yuque.com/liihuu/klinechart
