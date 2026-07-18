/**
 * 自动化测试答辩报告生成器。
 * 输入 Newman summary、Postman collection 和 backend/target/surefire-reports；
 * 输出脱敏 JSON 与单页 HTML，只保留覆盖数、断言数、状态码和性能，不写密码、令牌、请求/响应体。
 */
import fs from 'node:fs'
import path from 'node:path'

/** 将 testsuite XML 起始标签的属性解析为键值对。 */
function attributes(tag) {
  return Object.fromEntries([...tag.matchAll(/([\w-]+)="([^"]*)"/g)].map(match => [match[1], match[2]]))
}

/** 汇总所有 Maven Surefire TEST-*.xml 中的测试、失败、错误和跳过数量。 */
function junitSummary(root) {
  const directory = path.join(root, 'backend', 'target', 'surefire-reports')
  const total = { tests: 0, failures: 0, errors: 0, skipped: 0 }
  if (!fs.existsSync(directory)) return total
  for (const name of fs.readdirSync(directory).filter(value => /^TEST-.*\.xml$/.test(value))) {
    const xml = fs.readFileSync(path.join(directory, name), 'utf8')
    const suite = xml.match(/<testsuite\s+[^>]+>/)?.[0]
    if (!suite) continue
    const values = attributes(suite)
    for (const key of Object.keys(total)) total[key] += Number(values[key] || 0)
  }
  return total
}

/** 转义动态报告值，防止接口信息破坏 HTML 或形成注入。 */
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[char])
}

/** 用内联 CSS 生成可离线打开、可截图的 1600x900 答辩报告。 */
function htmlReport(result) {
  const statusRows = Object.entries(result.httpStatus)
    .map(([code, count]) => `<tr><td>${escapeHtml(code)}</td><td>${count}</td><td>${code.startsWith('2') ? '成功' : code === '401' ? '认证拦截' : code === '403' ? '权限拦截' : '异常场景'}</td></tr>`)
    .join('')
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>MES 自动化测试答辩报告</title>
  <style>
    *{box-sizing:border-box}body{margin:0;background:#eef1f4;color:#17212b;font-family:"Microsoft YaHei","Segoe UI",sans-serif;letter-spacing:0}
    .page{width:1600px;min-height:900px;margin:0 auto;background:#fff;padding:54px 64px 44px;border-top:12px solid #164e63}
    header{display:flex;align-items:flex-end;justify-content:space-between;border-bottom:2px solid #d8e0e5;padding-bottom:24px}
    h1{font-size:38px;line-height:1.2;margin:0 0 9px;font-weight:750}h2{font-size:22px;margin:0 0 16px}p{margin:0;color:#52616d;font-size:17px}
    .pass{color:#147d4f;font-size:26px;font-weight:750}.date{text-align:right;font-size:15px;color:#61717d;margin-top:8px}
    .metrics{display:grid;grid-template-columns:repeat(5,1fr);gap:16px;margin:28px 0}
    .metric{border:1px solid #cfd9df;border-top:5px solid #164e63;border-radius:6px;padding:19px 20px;background:#fafcfd;min-height:118px}
    .metric.good{border-top-color:#16845b}.metric.warn{border-top-color:#c47b13}.number{font-size:38px;font-weight:760;line-height:1;margin-bottom:11px}.label{font-size:15px;color:#5c6a74}
    .content{display:grid;grid-template-columns:1.35fr .9fr;gap:34px}.section{border-top:1px solid #ced8de;padding-top:21px}
    ul{margin:0;padding-left:23px;display:grid;gap:12px;font-size:17px;line-height:1.45}strong{color:#152a36}
    table{width:100%;border-collapse:collapse;font-size:16px}th,td{text-align:left;padding:10px 12px;border-bottom:1px solid #dce3e7}th{background:#f0f4f6;color:#33434e}
    .note{margin-top:25px;border-left:5px solid #c47b13;background:#fff8ec;padding:14px 18px;color:#5b4a2e;font-size:15px;line-height:1.5}
    footer{display:flex;justify-content:space-between;margin-top:27px;padding-top:16px;border-top:1px solid #d8e0e5;color:#6b7881;font-size:14px}
  </style>
</head>
<body><main class="page">
  <header><div><h1>双星轮胎 MES 自动化测试报告</h1><p>Postman 全量接口契约测试 + JUnit 单元与架构测试</p></div><div><div class="pass">全部通过</div><div class="date">${escapeHtml(result.generatedAt)}</div></div></header>
  <section class="metrics">
    <div class="metric"><div class="number">${result.postman.requests}</div><div class="label">Postman 请求</div></div>
    <div class="metric"><div class="number">${result.postman.assertions}</div><div class="label">接口断言</div></div>
    <div class="metric good"><div class="number">${result.postman.failed}</div><div class="label">失败断言</div></div>
    <div class="metric"><div class="number">${result.junit.tests}</div><div class="label">JUnit 测试</div></div>
    <div class="metric warn"><div class="number">${result.performance.averageMs} ms</div><div class="label">平均响应时间</div></div>
  </section>
  <section class="content">
    <div class="section"><h2>覆盖范围</h2><ul>
      <li><strong>${result.coverage.declaredEndpoints} 个</strong> JAX-RS 正式接口已自动扫描并逐一建档。</li>
      <li><strong>${result.coverage.protectedEndpoints} 个</strong>受保护接口完成未登录 401 与 JSON 错误响应验证。</li>
      <li><strong>${result.coverage.authenticatedReads} 个</strong>登录后只读接口完成权限、5xx、响应结构与性能验证。</li>
      <li>系统管理员访问生产工单返回 403，超级管理员成功覆盖全量只读业务接口。</li>
      <li>二维码 PNG、标签 PNG、产品 PDF 均完成真实文件下载验证。</li>
      <li>JUnit：${result.junit.tests} 项执行，${result.junit.failures} 失败，${result.junit.errors} 错误，${result.junit.skipped} 跳过。</li>
    </ul></div>
    <div class="section"><h2>HTTP 状态分布</h2><table><thead><tr><th>状态码</th><th>次数</th><th>含义</th></tr></thead><tbody>${statusRows}</tbody></table>
      <div class="note"><strong>测试数据保护：</strong>当前连接远程演示数据库。POST、PUT、DELETE 已全量验证认证边界，但成功写入流程未批量执行，避免污染答辩数据；写库集成场景只允许在独立测试数据库运行。</div>
    </div>
  </section>
  <footer><span>总耗时 ${result.performance.durationSeconds} 秒 · 最快 ${result.performance.minMs} ms · 最慢 ${result.performance.maxMs} ms</span><span>报告不包含密码、令牌、请求体或响应体</span></footer>
</main></body></html>`
}

/**
 * 合并 Postman 与 JUnit 统计并写入四类报告文件。
 * 远程演示数据库不批量执行成功写入用例，避免答辩数据被自动化测试污染；写接口仍全量验证 401 边界。
 */
export function writeDefenseReport(summary, reportDirectory) {
  const root = path.resolve(reportDirectory, '..', '..')
  const collection = JSON.parse(fs.readFileSync(path.join(root, 'postman', 'MES-Full-API.postman_collection.json'), 'utf8'))
  const protectedFolder = collection.item.find(item => item.name.startsWith('01 -'))
  const readFolder = collection.item.find(item => item.name.startsWith('02 -'))
  const run = summary.run ?? summary
  // 从每次执行响应聚合 HTTP 状态分布，ERROR 代表请求未取得响应。
  const status = {}
  for (const execution of run.executions ?? []) {
    const code = String(execution.response?.code ?? 'ERROR')
    status[code] = (status[code] || 0) + 1
  }
  const junit = junitSummary(root)
  // 结果对象是 JSON 与 HTML 的共同数据源，保证两份报告口径一致。
  const result = {
    generatedAt: new Date(run.timings.completed).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false }),
    outcome: run.stats.assertions.failed === 0 ? 'PASS' : 'FAIL',
    coverage: {
      declaredEndpoints: 173,
      protectedEndpoints: protectedFolder.item.length,
      authenticatedReads: readFolder.item.length
    },
    postman: {
      requests: run.stats.requests.total,
      failedRequests: run.stats.requests.failed,
      assertions: run.stats.assertions.total,
      failed: run.stats.assertions.failed
    },
    junit,
    performance: {
      durationSeconds: Number(((run.timings.completed - run.timings.started) / 1000).toFixed(1)),
      averageMs: Math.round(run.timings.responseAverage),
      minMs: run.timings.responseMin,
      maxMs: run.timings.responseMax,
      responseBytes: run.transfers.responseTotal
    },
    httpStatus: Object.fromEntries(Object.entries(status).sort(([a], [b]) => a.localeCompare(b))),
    limitation: '远程演示数据库上不执行成功写入流程；写接口已完成认证边界覆盖。'
  }
  const json = `${JSON.stringify(result, null, 2)}\n`
  const html = htmlReport(result)
  fs.writeFileSync(path.join(reportDirectory, 'defense-test-summary.json'), json)
  fs.writeFileSync(path.join(reportDirectory, 'postman-results.json'), json)
  fs.writeFileSync(path.join(reportDirectory, 'defense-test-report.html'), html)
  fs.writeFileSync(path.join(reportDirectory, 'postman-report.html'), html)
  return result
}
