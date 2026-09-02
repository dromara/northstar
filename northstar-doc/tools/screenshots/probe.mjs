// 探测脚本：登录演示站，输出各页面概况，为正式截图做准备
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';

const BASE = 'https://northstar.quantit.tech:8443';
const OUT = fileURLToPath(new URL('./out/', import.meta.url));

const browser = await chromium.launch({ channel: 'msedge' });
const ctx = await browser.newContext({
  ignoreHTTPSErrors: true,
  viewport: { width: 1600, height: 900 },
  deviceScaleFactor: 2
});
const page = await ctx.newPage();
page.setDefaultTimeout(20000);

// 打开登录页
await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
await page.screenshot({ path: `${OUT}/probe-login.png` });
console.log('登录页 title:', await page.title());

// 登录
await page.fill('input[placeholder*="账号"], input[placeholder*="帐号"]', 'admin').catch(() => {});
const userInput = page.locator('input').first();
console.log('账号框值:', await userInput.inputValue().catch(() => 'N/A'));
const pwdInput = page.locator('input[type="password"]').first();
await pwdInput.fill('123456');
await page.keyboard.press('Enter');
await page.waitForTimeout(4000);
console.log('登录后 URL:', page.url());
await page.screenshot({ path: `${OUT}/probe-after-login.png` });

// 网关管理
await page.goto(`${BASE}/#/gateway/all`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
await page.screenshot({ path: `${OUT}/probe-gateway.png` });
const cards = await page.locator('.el-card, [class*=card]').allTextContents().catch(() => []);
console.log('网关页文本片段:', JSON.stringify(cards).slice(0, 800));

// 模组管理
await page.goto(`${BASE}/#/module/mgmt`, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);
await page.screenshot({ path: `${OUT}/probe-module.png` });
const rows = await page.locator('tr').allTextContents().catch(() => []);
console.log('模组页行数:', rows.length, JSON.stringify(rows).slice(0, 800));

await browser.close();
console.log('探测完成');
