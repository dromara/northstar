'use strict';

const dayjs = require('dayjs');
const path = require('path');

/**
 * 默认配置
 */
module.exports = (appInfo) => {

  const config = {};

  /**
   * 应用模式配置
   */
  config.developmentMode = {
    default: 'vue',
    mode: {
      vue: {
        hostname: 'localhost',
        port: 8090
      },
      react: {
        hostname: 'localhost',
        port: 3000
      },
      html: {
        hostname: 'localhost',
        indexPage: 'index.html'
      },
    }
  };

  /**
   * 开发者工具
   */
  config.openDevTools = false;

  /**
   * 应用程序顶部菜单
   */
  config.openAppMenu = 'dev-show';

  /**
   * 主窗口
   */
  config.windowsOption = {
    title: 'Northstar盈富量化平台',
    width: 1920,
    height: 1080,
    minWidth: 1024,
    minHeight: 768,
    webPreferences: {
      //webSecurity: false, // 跨域问题 -> 打开注释
      contextIsolation: false, // false -> 可在渲染进程中使用electron的api，true->需要bridge.js(contextBridge)
      nodeIntegration: true,
      enableRemoteModule: true,
      //preload: path.join(appInfo.baseDir, 'preload', 'bridge.js'),
    },
    frame: true,
    show: true,
    icon: path.join(appInfo.home, 'public', 'images', 'logo-32.png'),
  };

  /**
   * ee框架日志
   */  
  config.logger = {
    appLogName: `ee-${dayjs().format('YYYY-MM-DD')}.log`, 
    errorLogName: `ee-error-${dayjs().format('YYYY-MM-DD')}.log` 
  }

  /**
   * 远程模式-web地址
   */    
  config.remoteUrl = {
    enable: false,
    url: 'https://discuz.chat/'
  };

  /**
   * 内置socket服务
   */   
  config.socketServer = {
    enable: false,
    port: 7070,
    path: "/socket.io/",
    connectTimeout: 45000,
    pingTimeout: 30000,
    pingInterval: 25000,
    maxHttpBufferSize: 1e8,
    transports: ["polling", "websocket"],
    cors: {
      origin: true,
    }
  };

  /**
   * 内置http服务
   */     
  config.httpServer = {
    enable: false,
    https: {
      enable: true, 
      key: '/public/ssl/localhost+1.key',
      cert: '/public/ssl/localhost+1.pem'
    },
    port: 7071,
    cors: {
      origin: "*"
    },
    body: {
      multipart: true,
      formidable: {
        keepExtensions: true
      }
    },
    filterRequest: {
      uris:  [
        'favicon.ico'
      ],
      returnData: ''
    }
  };

  /**
   * 主进程
   */     
  config.mainServer = {
    protocol: 'https://',
    host: '127.0.0.1',
    port: 7072,
    ssl: {
      key: '/public/ssl/localhost+1.key',
      cert: '/public/ssl/localhost+1.pem'
    }
  }; 

  /**
   * 硬件加速
   */
  config.hardGpu = {
    enable: false
  };

  /**
   * 插件功能
   */
  config.addons = {
    window: {
      enable: true,
    },
    tray: {
      enable: true,
      title: 'Northstar盈富量化平台',
      icon: '/public/images/tray_logo.png'
    },
    security: {
      enable: true,
    },
    awaken: {
      enable: true,
      protocol: 'ee',
      args: []
    },
    autoUpdater: {
      enable: true,
      windows: false, 
      macOS: false, 
      linux: false,
      options: {
        provider: 'generic', 
        url: 'http://kodo.qiniu.com/'
      },
      force: false,
    },
    javaServer: {
      enable: false,
      port: 18080,
      jreVersion: 'jre1.8.0_201',
      opt: '-server -Xms512M -Xmx512M -Xss512k -Dspring.profiles.active=prod -Dserver.port=${port} -Dlogging.file.path="${path}" ',
      name: 'java-app.jar'
    },
    example: {
      enable: true,
    },
  };

  return {
    ...config
  };
}
