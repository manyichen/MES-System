/**
 * Newman 命令行执行入口。
 * 读取标准 Postman collection/environment，输出 CLI、JUnit XML、JSON 摘要和答辩 HTML 报告；
 * 任一断言、请求错误或后端 JUnit 失败都会设置非零退出码，便于 CI/答辩前自动验收。
 */
import fs from 'node:fs'
import path from 'node:path'
import newman from 'newman'
import { writeDefenseReport } from './defense-report.mjs'

// 报告目录由脚本位置推导，不依赖调用命令的当前工作目录。
const directory = import.meta.dirname
const reportDirectory = path.join(directory, 'reports')
fs.mkdirSync(reportDirectory, { recursive: true })

// 5 秒单请求超时，10 ms 间隔避免本地服务瞬间承受无意义突发压力。
newman.run({
  collection: path.join(directory, 'MES-Full-API.postman_collection.json'),
  environment: path.join(directory, 'MES-Local.postman_environment.json'),
  reporters: ['cli', 'junit'],
  reporter: {
    junit: { export: path.join(reportDirectory, 'postman-junit.xml') }
  },
  timeoutRequest: 5000,
  delayRequest: 10,
  color: 'on'
}, (error, summary) => {
  // Newman 运行级错误与断言失败分别处理，两者都应使 npm test 失败。
  if (error) {
    console.error(error)
    process.exitCode = 1
    return
  }
  // 将 Newman summary 与 Maven Surefire XML 汇总为适合答辩展示的脱敏报告。
  const result = writeDefenseReport(summary, reportDirectory)
  console.log(`Defense report: ${path.join(reportDirectory, 'defense-test-report.html')}`)
  const failedAssertions = summary.run.stats.assertions.failed
  const requestErrors = summary.run.failures.filter(failure => !failure.error?.test).length
  if (failedAssertions || requestErrors || result.junit.failures || result.junit.errors) {
    console.error(`Postman test failed: ${failedAssertions} assertions failed, ${requestErrors} request errors`)
    process.exitCode = 1
  }
})
