import fs from 'node:fs'
import path from 'node:path'
import newman from 'newman'
import { writeDefenseReport } from './defense-report.mjs'

const directory = import.meta.dirname
const reportDirectory = path.join(directory, 'reports')
fs.mkdirSync(reportDirectory, { recursive: true })

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
  if (error) {
    console.error(error)
    process.exitCode = 1
    return
  }
  const result = writeDefenseReport(summary, reportDirectory)
  console.log(`Defense report: ${path.join(reportDirectory, 'defense-test-report.html')}`)
  const failedAssertions = summary.run.stats.assertions.failed
  const requestErrors = summary.run.failures.filter(failure => !failure.error?.test).length
  if (failedAssertions || requestErrors || result.junit.failures || result.junit.errors) {
    console.error(`Postman test failed: ${failedAssertions} assertions failed, ${requestErrors} request errors`)
    process.exitCode = 1
  }
})
