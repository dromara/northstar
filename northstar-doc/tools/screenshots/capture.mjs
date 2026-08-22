// 正式截图脚本：登录演示站，截取文档所需全部界面
// 注意：只读操作，不保存/不提交任何表单
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';

const BASE = 'https://northstar.quantit.tech:8443';
const OUT = fileURLToPath(new URL('../../docs/public/images/', import.meta.url));

const results = [];
async function shot(page, name, note) {
  const file = `${OUT}${name}.png`;
  await page.waitForTimeout(600);
  await page.screenshot({ path: file });
  results.push([name, note]);
  console.log('✅', name, '-', note);
}

async function tryStep(desc, fn) {
  try {
    await fn();
  } catch (e) {
    console.log('⚠️ 跳过[' + desc + ']:', e.message.split('\n')[0].slice(0, 120));
  }
}

const browser = await chromium.launch({ channel: 'msedge' });
const ctx = await browser.newContext({
  ignoreHTTPSErrors: true,
  viewport: { width: 1600, height: 900 },
  deviceScaleFactor: 2
});
const page = await ctx.newPage();
page.setDefaultTimeout(15000);

// ---------- 1. 登录页 ----------
await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
await shot(page, 'getting-started-login', '登录页');

// ---------- 2. 登录 ----------
await tryStep('登录', async () => {
  const pwd = page.locator('input[type="password"]').first();
  if (!(await pwd.inputValue())) await pwd.fill('123456');
  await page.locator('button:has-text("登"), button:has-text("登录")').first().click();
  await page.waitForURL('**/welcome**', { timeout: 15000 });
});

// 关闭提示横幅与消息
await page.waitForTimeout(3500);
await tryStep('关闭横幅', async () => {
  for (const btn of await page.locator('.el-alert__closebtn').all()) await btn.click().catch(() => {});
  await page.keyboard.press('Escape');
  await page.waitForTimeout(800);
});

// ---------- 3. 首页 ----------
await shot(page, 'getting-started-welcome', '首页（三张折叠卡片）');
await tryStep('展开首页卡片', async () => {
  for (const h of await page.locator('.el-collapse-item__header').all()) {
    await h.click().catch(() => {});
    await page.waitForTimeout(300);
  }
  await page.waitForTimeout(800);
  await shot(page, 'getting-started-welcome-expanded', '首页卡片展开');
});

// ---------- 4. 网关管理 ----------
await page.goto(`${BASE}/#/gateway/all`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
await shot(page, 'introduction-gateway-mgmt', '网关管理页');

// 新建网关弹窗
await tryStep('新建网关弹窗', async () => {
  await page.locator('button:has-text("新建")').first().click();
  await page.locator('.el-dialog').first().waitFor();
  await page.waitForTimeout(800);
  await shot(page, 'playback-gateway-create', '新建网关弹窗');
});

// 选择 PLAYBACK 渠道并打开网关配置
await tryStep('网关配置弹窗', async () => {
  const dlg = page.locator('.el-dialog').first();
  // 网关用途选"行情"
  await dlg.locator('.el-radio:has-text("行情"), label:has-text("行情")').first().click().catch(() => {});
  // 渠道下拉
  await dlg.locator('.el-select').last().click();
  await page.waitForTimeout(500);
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item:has-text("PLAYBACK")').first().click();
  await page.waitForTimeout(500);
  // 网关配置按钮
  await dlg.locator('button:has-text("网关配置")').click();
  await page.waitForTimeout(1000);
  await shot(page, 'playback-gateway-settings', 'PLAYBACK 网关配置弹窗');
  // 关闭所有弹窗
  await page.keyboard.press('Escape');
  await page.waitForTimeout(300);
  await page.locator('.el-dialog button:has-text("取消")').last().click().catch(() => {});
  await page.keyboard.press('Escape');
  await page.waitForTimeout(500);
});

// ---------- 5. 回放控制面板（PLAYBACK 卡片 → 更多 → 回放） ----------
await tryStep('回放控制面板', async () => {
  const card = page.locator('.el-card:has-text("PLAYBACK")').first();
  await card.locator('.el-dropdown').first().click();
  await page.waitForTimeout(500);
  await page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item:has-text("回放")').first().click();
  await page.waitForTimeout(1200);
  await shot(page, 'playback-controller', '历史回放控制面板');
  await page.keyboard.press('Escape');
  await page.locator('.el-dialog__headerbtn:visible').first().click().catch(() => {});
  await page.waitForTimeout(500);
});

// ---------- 6. 多图同列（回放） ----------
await page.goto(`${BASE}/#/trade/multicharts`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);
await tryStep('多图同列选回放', async () => {
  // 数据类型下拉（第2个 el-form-item）
  const items = page.locator('.el-form-item');
  await items.nth(1).locator('.el-select').click();
  await page.waitForTimeout(500);
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item:has-text("回放")').first().click();
  await page.waitForTimeout(500);
  await page.locator('button:has-text("刷新")').first().click();
  await page.waitForTimeout(3500);
  await shot(page, 'playback-multicharts', '多图同列-回放行情');
});

// ---------- 7. 交易面板（展开） ----------
await tryStep('展开交易面板', async () => {
  await page.locator('.trade-bar > *').first().click();
  await page.waitForTimeout(1200);
  await shot(page, 'training-multicharts-panel', '多图同列+交易面板（训练界面）');
});

// ---------- 8. 交易面板选账户 ----------
await tryStep('交易面板', async () => {
  await page.locator('#optAccountId').click();
  await page.waitForTimeout(500);
  const opt = page.locator('.el-select-dropdown:visible .el-select-dropdown__item:has-text("模拟账户")').first();
  if (await opt.count()) await opt.click();
  await page.waitForTimeout(1500);
  await shot(page, 'sim-trade-panel', '底部交易面板');
});

// ---------- 9. 账户绩效 ----------
await tryStep('账户绩效', async () => {
  await page.locator('span[title*="账户绩效"] .button, span[title*="绩效"] i').first().click();
  await page.waitForTimeout(2000);
  await shot(page, 'sim-account-performance', '账户绩效弹窗');
  await page.keyboard.press('Escape');
  await page.locator('.el-dialog__headerbtn:visible').first().click().catch(() => {});
  await page.waitForTimeout(400);
});

// ---------- 10. 出入金（网关管理 → 模拟账户卡片） ----------
await page.goto(`${BASE}/#/gateway/all`, { waitUntil: 'networkidle' });
await page.waitForTimeout(1500);
await tryStep('出入金弹窗', async () => {
  const card = page.locator('.el-card:has-text("模拟账户")').first();
  await card.locator('.el-dropdown').first().click();
  await page.waitForTimeout(500);
  await page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item:has-text("出入金")').first().click();
  await page.waitForTimeout(1200);
  await shot(page, 'sim-deposit', '模拟账户出入金弹窗');
  await page.keyboard.press('Escape');
  await page.locator('.el-dialog__headerbtn:visible').first().click().catch(() => {});
});

// ---------- 11. 模组管理 ----------
await page.goto(`${BASE}/#/module/mgmt`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);
await shot(page, 'auto-module-mgmt', '模组管理页');

// 新建模组弹窗
await tryStep('新建模组弹窗', async () => {
  await page.locator('button:has-text("新建")').first().click();
  await page.locator('.el-dialog').first().waitFor();
  await page.waitForTimeout(1000);
  await shot(page, 'auto-module-create', '新建模组弹窗');
  await page.locator('.el-dialog button:has-text("取消")').first().click().catch(() => {});
  await page.keyboard.press('Escape');
  await page.waitForTimeout(400);
});

// 模组运行时状态
await tryStep('模组运行时', async () => {
  const row = page.locator('tr:has-text("回测")').first();
  await row.locator('.el-dropdown, button:has-text("操作"), .el-button').last().click();
  await page.waitForTimeout(500);
  await page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item:has-text("运行时状态")').first().click();
  await page.waitForTimeout(3500);
  await shot(page, 'auto-module-runtime', '模组运行时状态页');
  await page.goBack();
  await page.waitForTimeout(1500);
});

// ---------- 12. 策略探索 ----------
await page.goto(`${BASE}/#/module/strategy-explorer`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);
await shot(page, 'auto-strategy-explorer', '策略探索页');

// ---------- 13. 活跃行情报价 ----------
await page.goto(`${BASE}/#/trade/priceboard`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);
await shot(page, 'multi-priceboard', '活跃行情报价页');

// ---------- 14. 合约配置 ----------
await page.goto(`${BASE}/#/contract-config/all`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
await shot(page, 'multi-contract-config', '合约配置页');

await browser.close();
console.log('\n===== 截图清单 =====');
results.forEach(([n, d]) => console.log(`${n}.png  <- ${d}`));
