// 补拍3：回放控制面板、交易面板（选账户）、账户绩效
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

await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
await page.locator('button:has-text("登")').first().click();
await page.waitForURL('**/welcome**');
await page.waitForTimeout(3000);
for (const btn of await page.locator('.el-alert__closebtn').all()) await btn.click().catch(() => {});

// ---------- 1. 回放控制面板（精确匹配 PLAYBACK 渠道卡片） ----------
await page.goto(`${BASE}/#/gateway/all`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
await tryStep('回放控制面板', async () => {
  const card = page.locator('.list-card-item_detail:has-text("网关渠道: PLAYBACK")').first();
  console.log('PLAYBACK卡片数:', await page.locator('.list-card-item_detail:has-text("网关渠道: PLAYBACK")').count());
  await card.locator('.el-dropdown').first().click({ timeout: 8000 });
  await page.waitForTimeout(800);
  const items = await page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item').allTextContents();
  console.log('下拉项:', JSON.stringify(items));
  await page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item:has-text("回放")').first().click();
  await page.waitForTimeout(1500);
  await shot('playback-controller');
  await closeDialogs();
});

// ---------- 2. 交易面板（选模拟账户） ----------
await page.goto(`${BASE}/#/trade/priceboard`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
await tryStep('交易面板', async () => {
  const bar = page.locator('.trade-bar');
  const box = await bar.boundingBox();
  console.log('trade-bar box:', JSON.stringify(box));
  await page.mouse.click(box.x + box.width / 2, box.y + 8); // 点击顶部中央折叠条
  await page.waitForTimeout(1200);
  const opened = await page.locator('.trade-bar.open').count();
  console.log('面板展开:', opened > 0);
  await page.locator('#optAccountId').click();
  await page.waitForTimeout(700);
  const accOpts = await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').allTextContents();
  console.log('账户选项:', JSON.stringify(accOpts));
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item:has-text("模拟账户")').first().click();
  await page.waitForTimeout(2000);
  await shot('sim-trade-panel');
});

// ---------- 3. 账户绩效 ----------
await tryStep('账户绩效', async () => {
  const btn = page.locator('span[title="账户绩效"]');
  console.log('绩效图标数:', await btn.count());
  await btn.first().locator('i, svg, .button').first().click();
  await page.waitForTimeout(2500);
  await shot('sim-account-performance');
  await closeDialogs();
});

await browser.close();
console.log('补拍3完成');
