// 补拍4：交易面板（选账户）、账户绩效
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

await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
await page.locator('button:has-text("登")').first().click();
await page.waitForURL('**/welcome**');
await page.waitForTimeout(3000);
for (const btn of await page.locator('.el-alert__closebtn').all()) await btn.click().catch(() => {});

await page.goto(`${BASE}/#/trade/priceboard`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);

// 折叠把手：.trade-bar 的第一个子元素（绝对定位在面板上方）
const handle = page.locator('.trade-bar > *').first();
console.log('把手 box:', JSON.stringify(await handle.boundingBox()));
await handle.click();
await page.waitForTimeout(1200);
console.log('面板展开:', (await page.locator('.trade-bar.open').count()) > 0);

// 选账户
await page.locator('#optAccountId').click({ force: true });
await page.waitForTimeout(700);
console.log('账户选项:', JSON.stringify(await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').allTextContents()));
await page.locator('.el-select-dropdown:visible .el-select-dropdown__item:has-text("模拟账户")').first().click();
await page.waitForTimeout(2500);
await shot('sim-trade-panel');

// 账户绩效
const perf = page.locator('span[title="账户绩效"] .button');
console.log('绩效按钮数:', await perf.count());
await perf.first().click();
await page.waitForTimeout(3000);
await shot('sim-account-performance');

await browser.close();
console.log('补拍4完成');
