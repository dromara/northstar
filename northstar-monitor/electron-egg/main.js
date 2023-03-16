const Appliaction = require('ee-core').Appliaction;
const { app, globalShortcut } = require('electron')
require('@electron/remote/main').initialize()
app.commandLine.appendSwitch('ignore-certificate-errors')    //忽略证书的检测

class Main extends Appliaction {

  constructor() {
    super();
    // this === eeApp;
  }

  /**
   * core app have been loaded
   */
  async ready () {
    // do some things
  }

  /**
   * electron app ready
   */
  async electronAppReady () {
    // do some things
  }

  /**
   * main window have been loaded
   */
  async windowReady () {
    // do some things
    // 延迟加载，无白屏
    const winOpt = this.config.windowsOption;
    const win = this.electron.mainWindow;
    if (winOpt.show == false) {
      win.once('ready-to-show', () => {
        win.show();
      })
    }

    win.on('focus', () => {
      globalShortcut.register('Shift+F5', () => {
        win.reload()
      })

      globalShortcut.register('Shift+Ctrl+I', () => {
        win.webContents.openDevTools()
      })

      globalShortcut.register('Ctrl+F', () => {
        win.webContents.send('on-find', '')
      })
    })

    win.on('blur', () => {
      globalShortcut.unregister('Shift+F5')
      globalShortcut.unregister('Ctrl+F')
      globalShortcut.unregister('Shift+Ctrl+I')
    })
  }

  /**
   * before app close
   */  
  async beforeClose () {
    // do some things

  }
}

new Main();
 
