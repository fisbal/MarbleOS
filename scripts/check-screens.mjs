// Opens MarbleOS on phones, tablets, PC and Mac sizes, in French and English, on the home,
// levels, shop and in-game screens, and fails when anything runs off the screen or is too small to tap.
// Usage: node scripts/check-screens.mjs [game dir] [screenshot dir]   (needs Playwright)
import { chromium } from 'playwright';
import { createServer } from 'node:http';
import { readFile, mkdir } from 'node:fs/promises';
import { join, extname, resolve } from 'node:path';

const dir = resolve(process.argv[2] || 'android/app/src/main/assets');
const shots = process.argv[3];
const sizes = {
  'small-phone': [320, 568], phone: [390, 844], 'phone-landscape': [844, 390],
  tablet: [820, 1180], 'tablet-landscape': [1180, 820], pc: [1440, 900],
};
const types = { '.html': 'text/html', '.js': 'text/javascript', '.woff2': 'font/woff2', '.png': 'image/png' };
const server = createServer(async (req, res) => {
  const path = join(dir, decodeURIComponent(req.url.split('?')[0]).replace(/\/$/, '/index.html'));
  try { res.writeHead(200, { 'Content-Type': types[extname(path)] || 'application/octet-stream' }); res.end(await readFile(path)); }
  catch { res.writeHead(404); res.end(); }
}).listen(0);
const base = `http://127.0.0.1:${server.address().port}/`;
if (shots) await mkdir(shots, { recursive: true });

const audit = () => {
  const out = [];
  const W = innerWidth, H = innerHeight;
  if (document.documentElement.scrollWidth > W + 1) out.push(`page scrolls sideways (${document.documentElement.scrollWidth} > ${W})`);
  for (const el of document.querySelectorAll('button, a, input, h1, h2, h3, p, span, canvas')) {
    const r = el.getBoundingClientRect();
    const cs = getComputedStyle(el);
    if (!r.width || !r.height || cs.visibility === 'hidden' || +cs.opacity === 0) continue;
    if (el.closest('[hidden], [aria-hidden="true"]')) continue;
    const label = (el.getAttribute('aria-label') || el.textContent || el.tagName).trim().replace(/\s+/g, ' ').slice(0, 40);
    // Only things on the visible screen layer: inside a scroll box, being below the fold is fine.
    let scroller = el.parentElement, inScroll = false;
    while (scroller) { const o = getComputedStyle(scroller).overflowY; if (o === 'auto' || o === 'scroll') { inScroll = true; break; } scroller = scroller.parentElement; }
    if (r.left < -1 || r.right > W + 1) out.push(`"${label}" runs off the side (${Math.round(r.left)}..${Math.round(r.right)} of ${W})`);
    if (!inScroll && el.tagName !== 'CANVAS' && (r.top < -1 || r.bottom > H + 1)) out.push(`"${label}" is below or above the screen and can't be scrolled to`);
    if (el.matches('button') && !el.disabled && (r.width < 32 || r.height < 32)) out.push(`button "${label}" is too small to tap (${Math.round(r.width)}x${Math.round(r.height)})`);
    if (el.matches('button')) {
      // The label's own text must stay inside the button (badges placed on the edge on purpose are fine).
      for (const node of el.querySelectorAll('*')) if (getComputedStyle(node).position === 'absolute') node.dataset.skip = '1';
      const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
      for (let n = walker.nextNode(); n; n = walker.nextNode()) {
        if (!n.textContent.trim() || n.parentElement.closest('[data-skip]')) continue;
        const range = document.createRange(); range.selectNodeContents(n);
        for (const tr of range.getClientRects()) if (tr.left < r.left - 1 || tr.right > r.right + 1) { out.push(`button "${label}" text is cut off`); break; }
      }
    }
  }
  return [...new Set(out)];
};

const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-webgl', '--ignore-gpu-blocklist'] });
const failures = [];
for (const [label, [width, height]] of Object.entries(sizes)) {
  for (const lang of ['fr', 'en']) {
    const touch = width < 1000;
    const ctx = await browser.newContext({ viewport: { width, height }, locale: lang, isMobile: touch, hasTouch: touch });
    await ctx.addInitScript(l => { try { localStorage.setItem('marbleos-lang', l); } catch (e) {} }, lang);
    const page = await ctx.newPage();
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    await page.goto(base, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1200);
    const screens = [['home', null], ['levels', '[data-act="levels"]'], ['shop', '[data-act="shop"]'], ['game', '[data-act="go"]']];
    for (const [name, sel] of screens) {
      if (sel) {
        // Each screen starts from a fresh home screen.
        if (name !== 'home') { await page.reload({ waitUntil: 'domcontentloaded' }); await page.waitForTimeout(1200); }
        const el = page.locator(sel).filter({ visible: true }).first();
        if (await el.count() === 0) { failures.push(`${label} ${lang}: no ${name} button on the home screen`); continue; }
        await el.evaluate(b => b.click());
        await page.waitForTimeout(name === 'game' ? 2500 : 900);
      }
      for (const i of await page.evaluate(audit)) failures.push(`${label} ${width}x${height} ${lang} ${name}: ${i}`);
      if (shots) await page.screenshot({ path: `${shots}/${label}-${lang}-${name}.png` });
    }
    for (const e of errors) failures.push(`${label} ${lang}: script error: ${e}`);
    await ctx.close();
  }
}
await browser.close();
server.close();
if (failures.length) { console.error(failures.join('\n')); process.exit(1); }
console.log(`All screens fit: ${Object.keys(sizes).length} sizes x 2 languages x 4 screens.`);
