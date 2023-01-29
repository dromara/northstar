const Appliaction = require('ee-core').Appliaction;
const { app, globalShortcut } = require('electron')
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

    globalShortcut.register('Shift+Ctrl+I', () => {
      win.webContents.openDevTools()
    })

    globalShortcut.register('Shift+F5', () => {
      win.reload()
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
 
