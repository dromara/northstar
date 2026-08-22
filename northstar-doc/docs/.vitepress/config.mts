import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'Northstar 使用文档',
  description: 'Northstar 盈富量化交易平台使用文档：历史回放、模拟盘交易、程序化交易、多账户交易、操盘手训练',
  cleanUrls: true,
  lastUpdated: true,

  themeConfig: {
    logo: '📈',
    siteTitle: 'Northstar 使用文档',

    nav: [
      { text: '首页', link: '/' },
      { text: '安装与启动', link: '/guide/installation' },
      { text: '快速上手', link: '/guide/getting-started' },
      { text: '核心概念', link: '/guide/introduction' },
      {
        text: '核心功能',
        items: [
          { text: '历史回放', link: '/features/playback' },
          { text: '模拟盘交易', link: '/features/sim-trading' },
          { text: '程序化交易', link: '/features/auto-trading' },
          { text: '多账户交易', link: '/features/multi-account' },
          { text: '操盘手训练', link: '/features/training' }
        ]
      },
      { text: 'FAQ', link: '/appendix/faq' },
      { text: '选项速查', link: '/appendix/reference' }
    ],

    sidebar: [
      {
        text: '入门',
        items: [
          { text: '安装与启动', link: '/guide/installation' },
          { text: '平台简介与核心概念', link: '/guide/introduction' },
          { text: '快速上手', link: '/guide/getting-started' }
        ]
      },
      {
        text: '核心功能',
        items: [
          { text: '历史回放', link: '/features/playback' },
          { text: '模拟盘交易', link: '/features/sim-trading' },
          { text: '程序化交易', link: '/features/auto-trading' },
          { text: '多账户交易', link: '/features/multi-account' },
          { text: '操盘手训练', link: '/features/training' }
        ]
      },
      {
        text: '附录',
        items: [
          { text: '常见问题（FAQ）', link: '/appendix/faq' },
          { text: '选项速查表', link: '/appendix/reference' },
        ]
      }
    ],

    outline: {
      level: [2, 3],
      label: '本页目录'
    },

    docFooter: {
      prev: '上一页',
      next: '下一页'
    },

    lastUpdatedText: '最后更新',
    darkModeSwitchLabel: '外观',
    sidebarMenuLabel: '菜单',
    returnToTopLabel: '返回顶部',

    search: {
      provider: 'local',
      options: {
        locales: {
          root: {
            translations: {
              button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
              modal: {
                noResultsText: '无法找到相关结果',
                resetButtonTitle: '清除查询条件',
                footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' }
              }
            }
          }
        }
      }
    },

    footer: {
      message: 'Northstar 盈富量化交易平台',
      copyright: '广州市盈富量化信息技术有限公司 版权所有'
    }
  }
})
