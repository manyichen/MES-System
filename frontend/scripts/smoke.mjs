/**
 * Playwright 前端端到端冒烟测试。
 * 外部依赖：已运行的 MES 服务、Microsoft Edge、测试账号 admin/123456；
 * 覆盖桌面 1440x900 与手机 390x844，检查登录布局、角色首页、系统管理表格和浏览器控制台错误。
 */
import assert from 'node:assert/strict'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const output = path.resolve(root, '../backend/target')
// URL 和浏览器路径可由环境变量覆盖，默认使用本机 Edge 与专用测试端口。
const baseUrl = process.env.MES_SMOKE_URL || 'http://127.0.0.1:18082'
const executablePath = process.env.EDGE_PATH
  || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'

const browser = await chromium.launch({ executablePath, headless: true })

/** 完成真实登录并等待角色首页出现，证明前端、认证接口和路由跳转链路可用。 */
async function login(page) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
  await page.selectOption('select', 'admin')
  await page.fill('input[type="password"]', '123456')
  await page.click('button[type="submit"]')
  await page.waitForURL(url => !url.pathname.endsWith('/login'))
  await page.locator('.dashboard-page').waitFor()
}

/** 在指定视口运行布局和业务冒烟断言，并保存整页截图到 backend/target。 */
async function run(viewport, suffix) {
  const page = await browser.newPage({ viewport })
  const errors = []
  // 收集 console.error 和未捕获页面异常，最终统一断言为空。
  page.on('console', message => {
    if (message.type() === 'error') errors.push(message.text())
  })
  page.on('pageerror', error => errors.push(error.message))

  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
  const loginButton = await page.locator('.login-panel button[type="submit"]').boundingBox()
  // 在浏览器上下文读取真实几何尺寸，防止手机端按钮或表单溢出视口。
  const loginLayout = await page.evaluate(() => {
    const panel = document.querySelector('.login-panel')
    const form = panel.querySelector('form')
    const button = form.querySelector('button[type="submit"]')
    return {
      viewport: document.documentElement.clientWidth,
      bodyScrollWidth: document.body.scrollWidth,
      panel: panel.getBoundingClientRect().toJSON(),
      panelPadding: getComputedStyle(panel).padding,
      form: form.getBoundingClientRect().toJSON(),
      formWidth: getComputedStyle(form).width,
      button: button.getBoundingClientRect().toJSON()
    }
  })
  assert(loginButton, 'login button must be visible')
  assert(loginButton.x >= 0 && loginButton.x + loginButton.width <= viewport.width,
    `login button overflows ${viewport.width}px viewport: ${JSON.stringify(loginLayout)}`)

  await login(page)
  await page.locator('.metric-block').first().waitFor({ timeout: 10_000 })
  await page.screenshot({ path: path.join(output, `vue-dashboard-${suffix}.png`), fullPage: true })
  assert((await page.locator('.metric-block').count()) > 0, 'role dashboard metrics must render')

  if (suffix === 'desktop') {
    await page.goto(`${baseUrl}/module/access`, { waitUntil: 'networkidle' })
    await page.locator('.data-section').waitFor()
    assert((await page.locator('tbody tr').count()) > 0, 'access module table must render')
  }
  assert.deepEqual(errors, [], `browser console errors: ${errors.join('; ')}`)
  await page.close()
}

// 无论断言成功或失败都关闭浏览器进程，避免 CI/本机残留 Edge。
try {
  await run({ width: 1440, height: 900 }, 'desktop')
  await run({ width: 390, height: 844 }, 'mobile')
  console.log('Vue smoke test passed: desktop and 390px mobile')
} finally {
  await browser.close()
}
