'use strict'

import { app, BrowserWindow, protocol, globalShortcut, Tray, Menu} from 'electron'

const {getConfig} = require('./config.js')
const path = require('path')

// 创建窗口
function createWindow () {
  // 创建浏览器窗口
  const mainWindow = new BrowserWindow({
    // frame: false, // 去掉菜单栏 + 外壳 
    width: getConfig('width'),
    height: getConfig('height'),
    x: getConfig('x'),
    y: getConfig('y'),
    icon: path.join(__dirname, './logo.ico'),
    fullscreen: false // 全屏
  })
  // 加载的目标地址
  let chat_url = getConfig('chat_url')
  mainWindow.loadURL(chat_url)
  mainWindow.removeMenu()
  mainWindow.on('hide', () => {});

  // mainWindow.webContents.openDevTools()

  global.mainWindow = mainWindow
}

// 注册协议
// 在浏览器地址栏输入“videoPlug://”就可以打开插件
if (!app.isDefaultProtocolClient('videoPlug')) {
  app.setAsDefaultProtocolClient('videoPlug')
}

// 防止重复启动本程序
// 可以在'second-instance'事件中获取到传递的参数。
const gotTheLock = app.requestSingleInstanceLock()
 
if (!gotTheLock) {
  app.quit()
} else {
  app.on('second-instance', async (event, argv) => {
    console.log("window 准备执行网页端调起客户端逻辑");
    // Windows 下通过协议URL启动时，URL会作为参数，所以需要在这个事件里处理
    if (process.platform === 'win32') {
      console.log("window 准备执行网页端调起客户端逻辑");
      handleArgvFromWeb(argv);
      _handleOpenWindowByWeb();
    }
  });
}

//应用启动执行
app.whenReady().then(() => {
  // 创建窗口
  createWindow()
  // 初始化服务
  initServer()
  // 初始化web页面事件
  // initWebContentsEvent()
  // 初始化应用实践
  initAppEvent()
  // 快捷键支持
  bindShortCut()
  // 初始化托盘栏
  initTray()
})

// 使用http协议
protocol.registerSchemesAsPrivileged([
  {
    scheme: 'http', 
    privileges: {
      bypassCSP: true,
      secure: true,
      supportFetchAPI: true,
      corsEnabled: true
    }
  }
]);

// 初始化应用实践
function initAppEvent() {
  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
  app.on('window-all-closed', function () {
    if (process.platform !== 'darwin') app.quit()
  })
}

// 托盘栏
function initTray() {
  // 新建托盘
  let tray = new Tray(path.join(__dirname, './logo.ico'));
  // 托盘名称
  tray.setToolTip('Electron Tray');
  // 托盘菜单
  const contextMenu = Menu.buildFromTemplate([{
          label: '显示',
          click: () => { mainWindow.show() }
      },
      {
          label: '退出',
          click: () => { mainWindow.destroy() }
      }
  ]);
  // 载入托盘菜单
  tray.setContextMenu(contextMenu);
  // 双击触发
  tray.on('double-click', () => {
      // 双击通知区图标实现应用的显示或隐藏
      mainWindow.isVisible() ? mainWindow.hide() : mainWindow.show()
      tray.setContextMenu(contextMenu);
  });
}

// 快捷键支持
function bindShortCut() {
  globalShortcut.register('CommandOrControl+shift+D', () => {
    mainWindow.webContents.openDevTools()
  })
}