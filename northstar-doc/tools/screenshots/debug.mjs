// 调试：查看网关卡片下拉、多图同列表单、交易面板账户选择器的 DOM
import { chromium } from 'playwright';

const BASE = 'https://northstar.quantit.tech:8443';
const browser = await chromium.launch({ channel: 'msedge' });
const ctx = await browser.newContext({ ignoreHTTPSErrors: true, viewport: { width: 1600, height: 900 } });
const page = await ctx.newPage();
page.setDefaultTimeout(10000);

await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
await page.locator('button:has-text("登")').first().click();
await page.waitForURL('**/welcome**');
await page.waitForTimeout(3000);

// 1. 网关卡片 DOM
await page.goto(`${BASE}/#/gateway/all`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
const cards = await page.locator('.el-card').count();
console.log('卡片数:', cards);
for (let i = 0; i < Math.min(cards, 5); i++) {
  const html = await page.locator('.el-card').nth(i).evaluate(el => {
    const dd = el.querySelector('[class*="dropdown"]');
    return { text: el.innerText.slice(0, 40), dropdown: dd ? dd.outerHTML.slice(0, 300) : null };
  });
  console.log(i, JSON.stringify(html, null, 1));
}

// 2. 多图同列表单项
await page.goto(`${BASE}/#/trade/multicharts`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
const items = await page.locator('.el-form-item').all();
console.log('表单项数:', items.length);
for (let i = 0; i < items.length; i++) {
  console.log(i, (await items[i].innerText()).slice(0, 50).replace(/\n/g, ' | '));
}

// 3. 打开 数据类型/行情网关 下拉看选项
const gwSelect = items[1].locator('.el-select');
await gwSelect.click();
await page.waitForTimeout(800);
const opts = await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').allTextContents();
console.log('第2个下拉选项:', JSON.stringify(opts));
await page.keyboard.press('Escape');

// 4. 交易面板
await page.locator('.trade-bar > *').first().click();
await page.waitForTimeout(1000);
const accCount = await page.locator('#optAccountId').count();
const accVisible = await page.locator('#optAccountId').first().isVisible().catch(() => 'err');
console.log('#optAccountId count:', accCount, 'visible:', accVisible);
const tradeSelects = await page.locator('.trade-bar .el-select').count();
console.log('.trade-bar .el-select count:', tradeSelects);

// 5. 绩效图标
const perf = await page.locator('span[title]').all();
for (const s of perf.slice(0, 10)) console.log('span title:', await s.getAttribute('title'));

await browser.close();
