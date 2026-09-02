// 补拍脚本：修复 capture.mjs 中失败的截图
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';

const BASE = 'https://northstar.quantit.tech:8443';
const OUT = fileURLToPath(new URL('../../docs/public/images/', import.meta.url));

const browser = await chromium.launch({ channel: 'msedge' });
const ctx = await browser.newContext({
  ignoreHTTPSErrors: true,
  viewport: { width: 1600, height: 900 },
  deviceScaleFactor: 2
});
const page = await ctx.newPage();
page.setDefaultTimeout(12000);

async function shot(name) {
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${OUT}${name}.png` });
  console.log('✅', name);
}
async function closeBanners() {
  for (const btn of await page.locator('.el-alert__closebtn').all()) await btn.click().catch(() => {});
  await page.waitForTimeout(300);
}
async function closeDialogs() {
  for (let i = 0; i < 4; i++) {
    await page.keyboard.press('Escape');
    await page.waitForTimeout(300);
    const cancel = page.locator('.el-dialog:visible button:has-text("取消")').last();
    if (await cancel.count()) await cancel.click().catch(() => {});
    await page.waitForTimeout(300);
  }
}
async function tryStep(desc, fn) {
  try {
    await fn();
  } catch (e) {
    console.log('⚠️ 跳过[' + desc + ']:', e.message.split('\n')[0].slice(0, 100));
    await closeDialogs();
  }
}
// 选择 el-select 选项
async function pickSelect(scope, labelText, optionText) {
  const item = scope.locator(`.el-form-item:visible:has-text("${labelText}")`).first();
  await item.locator('.el-select').click();
  await page.waitForTimeout(600);
  await page
    .locator(`.el-select-dropdown:visible .el-select-dropdown__item:has-text("${optionText}")`)
    .first()
    .click();
  await page.waitForTimeout(500);
}

// 登录
await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
await page.locator('button:has-text("登")').first().click();
await page.waitForURL('**/welcome**');
await page.waitForTimeout(3000);
await closeBanners();

// ---------- 1. PLAYBACK 网关配置弹窗 ----------
await page.goto(`${BASE}/#/gateway/all`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
await tryStep('网关配置弹窗', async () => {
  await page.locator('button:has-text("新建")').first().click();
  await page.locator('.el-dialog:visible').first().waitFor();
  await page.waitForTimeout(800);
  const dlg = page.locator('.el-dialog:visible').first();
  await pickSelect(dlg, '网关用途', '行情');
  await pickSelect(dlg, '网关渠道', 'PLAYBACK');
  await dlg.locator('button:has-text("网关配置")').click();
  await page.waitForTimeout(1200);
  await shot('playback-gateway-settings');
  await closeDialogs();
});

// ---------- 2. 回放控制面板 ----------
await tryStep('回放控制面板', async () => {
  const card = page.locator('.list-card-item_detail:has-text("PLAYBACK")').first();
  await card.locator('.el-dropdown').first().click();
  await page.waitForTimeout(700);
  await page
    .locator('.el-dropdown-menu:visible .el-dropdown-menu__item:has-text("回放")')
    .first()
    .click();
  await page.waitForTimeout(1500);
  await shot('playback-controller');
  await closeDialogs();
});

// ---------- 3. 出入金弹窗 ----------
await tryStep('出入金弹窗', async () => {
  const card = page.locator('.list-card-item_detail:has-text("模拟账户")').first();
  await card.locator('.el-dropdown').first().click();
  await page.waitForTimeout(700);
  await page
    .locator('.el-dropdown-menu:visible .el-dropdown-menu__item:has-text("出入金")')
    .first()
    .click();
  await page.waitForTimeout(1500);
  await shot('sim-deposit');
  await closeDialogs();
});

// ---------- 4. 多图同列：回放行情 ----------
await page.goto(`${BASE}/#/trade/multicharts`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);
await closeBanners();
await tryStep('多图同列回放', async () => {
  await pickSelect(page, '行情网关', 'PLAYBACK');
  // 品种
  await page.locator('.el-form-item:visible:has-text("品种") .el-select').first().click();
  await page.waitForTimeout(800);
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click();
  await page.waitForTimeout(400);
  // 周期
  await pickSelect(page, '周期', '5m');
  await page.locator('button:has-text("刷新")').first().click();
  await page.waitForTimeout(4000);
  await shot('playback-multicharts');
});

// ---------- 5. 交易面板（选模拟账户） ----------
await tryStep('交易面板', async () => {
  await page.locator('.trade-bar > *').first().click();
  await page.waitForTimeout(1200);
  const accSelect = page.locator('#optAccountId');
  await accSelect.click();
  await page.waitForTimeout(700);
  await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item:has-text("模拟账户")')
    .first()
    .click();
  await page.waitForTimeout(2000);
  await shot('sim-trade-panel');
});

// ---------- 6. 账户绩效 ----------
await tryStep('账户绩效', async () => {
  await page.locator('span[title="账户绩效"] .button, span[title="账户绩效"] i').first().click();
  await page.waitForTimeout(2500);
  await shot('sim-account-performance');
  await closeDialogs();
});

await browser.close();
console.log('补拍完成');
