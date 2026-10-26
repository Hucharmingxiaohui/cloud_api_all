/* Run against a local preview server and an isolated Chromium/Edge CDP profile.
 * CONSTRUCTION_CDP_URL and CONSTRUCTION_PREVIEW_URL may override the defaults.
 * Only the isolated browser's construction-demo-v1:local key is changed.
 */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const os = require('node:os')
const WebSocket = require('ws')
const cdp = process.env.CONSTRUCTION_CDP_URL || 'http://127.0.0.1:9228'
const base = process.env.CONSTRUCTION_PREVIEW_URL || 'http://127.0.0.1:8097'
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))

async function main () {
  const target = await (await fetch(`${cdp}/json/new?about:blank`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { ws.once('open', resolve); ws.once('error', reject) })
  let nextId = 0
  const pending = new Map()
  const errors = []
  const apiCalls = []
  let configured = false
  ws.on('message', raw => {
    const event = JSON.parse(raw)
    if (event.method === 'Fetch.requestPaused') {
      const { requestId, request } = event.params
      apiCalls.push(request.method + ' ' + request.url)
      let data
      if (request.url.includes('/getPlanType')) data = 6
      else if (request.url.endsWith('/analysis/check')) data = { connected: true }
      else if (request.url.endsWith('/analysis/refresh')) data = { batch_id: 'batch-browser', status: 'SUCCEEDED', result: { detections: [] } }
      else if (request.url.endsWith('/analysis')) data = { batch_id: 'batch-browser', status: 'QUEUED' }
      else data = { job_id: 'browser-construction', name: '工地真实入口接口样例', plan_type: 6, expected_count: 1, received_count: 1, media_complete: true, analysis_configured: configured, photos: [], analysis: {} }
      send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code: 0, data })).toString('base64') }).catch(e => errors.push(e.message))
    }
    if (event.id && pending.has(event.id)) {
      const p = pending.get(event.id); pending.delete(event.id); clearTimeout(p.timer)
      if (event.error) p.reject(new Error(JSON.stringify(event.error)))
      else p.resolve(event.result)
    }
    if (event.method === 'Runtime.exceptionThrown') errors.push(event.params.exceptionDetails.text + ' ' + (event.params.exceptionDetails.exception?.description || ''))
    if (event.method === 'Runtime.consoleAPICalled' && event.params.type === 'error') {
      const text = event.params.args.map(a => a.value || a.description || '').join(' ')
      if (/Vue Global Error|App Error Captured|Unhandled Promise/.test(text)) errors.push(text)
    }
  })
  function send (method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = ++nextId
      const timer = setTimeout(() => { pending.delete(id); reject(new Error(`CDP timeout: ${method}`)) }, 30000)
      pending.set(id, { resolve, reject, timer }); ws.send(JSON.stringify({ id, method, params }))
    })
  }
  async function evaluate (expression) {
    const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text)
    return result.result.value
  }
  async function waitFor (expression) {
    for (let i = 0; i < 120; i++) { if (await evaluate(expression)) return; await sleep(500) }
    throw new Error(`UI timeout: ${expression}`)
  }
  async function click (text) {
    await evaluate(`(() => {const e=[...document.querySelectorAll('button, [role="tab"]')].find(e=>e.textContent.trim()===${JSON.stringify(text)});if(!e)throw Error('Missing button/tab: '+${JSON.stringify(text)});e.click()})()`)
    await sleep(250)
  }
  async function input (selector, value) {
    await evaluate(`(() => {const e=document.querySelector(${JSON.stringify(selector)});if(!e)throw Error('Missing input');e.value=${JSON.stringify(value)};e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}))})()`)
  }
  async function screenshot (name) {
    await send('Page.bringToFront')
    const result = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false, fromSurface: true })
    const file = path.join(os.tmpdir(), `construction-${name}.png`)
    fs.writeFileSync(file, Buffer.from(result.data, 'base64')); console.log(`Screenshot: ${file}`)
  }
  try {
    await send('Runtime.enable'); await send('Page.enable')
    await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1100, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: `${base}/construction-preview/areas` })
    await waitFor("!!document.querySelector('.area-card')")
    await evaluate('localStorage.removeItem("construction-demo-v1:local")')
    await send('Page.reload')
    await waitFor("document.querySelectorAll('.area-card').length === 3")
    await screenshot('areas')
    await click('＋ 新建区域')
    await input('.area-properties input[maxlength="40"]', '浏览器测试区域')
    await click('▭ 矩形区域')
    await evaluate('(() => {const svg=document.querySelector(".construction-map svg");for(const [x,y] of [[280,250],[420,340]]){const p=svg.createSVGPoint();p.x=x;p.y=y;const c=p.matrixTransform(svg.getScreenCTM());svg.dispatchEvent(new MouseEvent("click",{clientX:c.x,clientY:c.y,bubbles:true}))}})()')
    await click('保存区域')
    await waitFor("document.querySelectorAll('.area-card').length === 4")
    assert.equal(await evaluate("JSON.parse(localStorage.getItem('construction-demo-v1:local')).areas.at(-1).boundary.length"), 4)
    await click('编辑区域')
    await click('⬡ 多边形')
    await evaluate('(() => {const svg=document.querySelector(".construction-map svg");for(const [x,y] of [[280,250],[420,240],[400,360]]){const p=svg.createSVGPoint();p.x=x;p.y=y;const c=p.matrixTransform(svg.getScreenCTM());svg.dispatchEvent(new MouseEvent("click",{clientX:c.x,clientY:c.y,bubbles:true}))}})()')
    await click('完成绘制')
    await click('保存区域')
    assert.equal(await evaluate("JSON.parse(localStorage.getItem('construction-demo-v1:local')).areas.at(-1).boundary.length"), 3)
    assert.equal(await evaluate("JSON.parse(localStorage.getItem('construction-demo-v1:local')).areas.at(-1).version"), 2)
    await click('编辑区域')
    await input('.area-properties input[maxlength="40"]', '未保存的修改')
    await evaluate("document.querySelectorAll('.construction-nav .el-tabs__item')[1].click()")
    await waitFor("!!document.querySelector('.el-message-box')")
    await click('继续编辑')
    assert.equal(await evaluate("location.pathname.endsWith('/areas')"), true)
    await click('取消')
    await click('放弃修改')
    await click('编辑区域')
    await click('⬡ 多边形')
    await evaluate('document.querySelector(".property-actions button").click()')
    await waitFor('!!document.querySelector(".el-message-box")')
    await click('放弃修改')
    assert.equal(await evaluate('!!document.querySelector(".construction-map svg.drawing")'), false, 'cancel editing must exit drawing mode')
    await click('创建计划')
    await waitFor("!!document.querySelector('.plan-form')")
    await input('.plan-form input[maxlength="60"]', '浏览器测试计划')
    await screenshot('plan')
    await click('保存演示计划')
    await waitFor("!document.querySelector('.plan-form')")
    await evaluate("[...document.querySelectorAll('.el-table__body tr')].find(r=>r.textContent.includes('浏览器测试计划')).querySelectorAll('button')[1].click()")
    await waitFor("!!document.querySelector('.task-overview')")
    assert.equal(await evaluate("document.querySelector('.task-overview').textContent.includes('未下发')"), true)
    await click('区域与参数')
    await waitFor("!!document.querySelector('.snapshot-layout')")
    await screenshot('task')
    const sampleId = await evaluate("JSON.parse(localStorage.getItem('construction-demo-v1:local')).tasks.find(t=>t.status==='sample').id")
    await send('Page.navigate', { url: `${base}/construction-preview/tasks?task=${encodeURIComponent(sampleId)}` })
    await waitFor("document.querySelectorAll('.photo-card').length === 3")
    await screenshot('photos')
    const doc = await send('DOM.getDocument')
    const fileInput = await send('DOM.querySelector', { nodeId: doc.root.nodeId, selector: '.photo-import input[type="file"]' })
    await send('DOM.setFileInputFiles', { nodeId: fileInput.nodeId, files: [path.join(os.tmpdir(), 'construction-areas.png')] })
    await waitFor("document.querySelectorAll('.photo-card').length === 4")
    await evaluate("document.querySelector('.photo-card').click()")
    await waitFor("!!document.querySelector('.construction-photo-dialog img')")
    await evaluate("document.querySelector('.el-dialog__headerbtn').click()")
    await click('分析与复核')
    await waitFor("!!document.querySelector('.issue-example')")
    await click('巡检报告')
    await waitFor("!!document.querySelector('.report-paper')")
    await send('Emulation.setDeviceMetricsOverride', { width: 768, height: 1024, deviceScaleFactor: 1, mobile: false })
    await sleep(300)
    assert.equal(await evaluate('document.documentElement.scrollWidth <= window.innerWidth + 2'), true, 'tablet must not overflow horizontally')
    await screenshot('tablet')
    await send('Page.reload')
    await waitFor("!!document.querySelector('.task-overview')")
    assert.equal(await evaluate("document.querySelectorAll('.photo-card').length"), 3, 'local file previews must not pretend to persist after reload')
    assert.equal(await evaluate("JSON.parse(localStorage.getItem('construction-demo-v1:local')).plans.length"), 2)
    await click('返回工地计划')
    await click('恢复示例')
    await waitFor("!!document.querySelector('.el-message-box')")
    await click('取消')
    assert.equal(await evaluate("JSON.parse(localStorage.getItem('construction-demo-v1:local')).plans.length"), 2, 'cancel reset preserves browser data')
    await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1100, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: `${base}/construction/plans` })
    await waitFor("location.pathname === '/taskManage/construction/plans' && !!document.querySelector('.construction-shell .el-table')")
    assert.equal(await evaluate("!!document.querySelector('.construction-nav .el-tabs__item')"), false, 'platform pages use their own menu entries')
    await screenshot('platform-plans')
    await evaluate("document.querySelector('.el-table__body tr').querySelectorAll('button')[1].click()")
    await waitFor("location.pathname === '/task/taskResult' && !!document.querySelector('.task-overview')")
    const beforeReload = await evaluate('performance.timeOrigin')
    await send('Page.reload')
    await waitFor(`performance.timeOrigin !== ${beforeReload} && location.pathname === '/task/taskResult' && !!document.querySelector('.task-overview')`)
    assert.equal(await evaluate("document.querySelectorAll('.tasks-page .el-table').length"), 0, 'result entry must not create a second task list')
    await send('Fetch.enable', { patterns: [{ urlPattern: '*getPlanType*' }, { urlPattern: '*/api/construction/tasks/*' }] })
    await send('Page.navigate', { url: `${base}/task/taskResult?job_id=browser-construction` })
    await waitFor("document.querySelector('.construction-shell')?.textContent.includes('工地真实入口接口样例')")
    await click('分析与结果')
    await waitFor("document.querySelector('.construction-shell')?.textContent.includes('工地算法接口尚未配置')")
    assert.equal(await evaluate("[...document.querySelectorAll('button')].find(e=>e.textContent.trim()==='提交分析').disabled"), true)
    assert.equal(apiCalls.some(call => call.startsWith('POST ')), false, 'opening results never submits analysis')
    configured = true
    await click('刷新本地结果')
    await click('检查分析主机')
    await waitFor("document.querySelector('.construction-shell').textContent.includes('可连接')")
    await click('提交分析')
    await waitFor("!!document.querySelector('.el-message-box')")
    await evaluate("document.querySelector('.el-message-box .el-button--primary').click()")
    await waitFor("document.querySelector('.construction-shell').textContent.includes('排队中')")
    assert.equal(apiCalls.filter(call => call.startsWith('POST ') && call.endsWith('/analysis')).length, 1)
    await click('查询分析进度 / 取回结果')
    await waitFor("!!document.querySelector('.analysis-json')")
    assert.equal(apiCalls.filter(call => call.startsWith('POST ') && call.endsWith('/analysis')).length, 1, 'refresh never starts a second batch')
    await screenshot('manual-analysis')
    assert.deepEqual(errors, [], 'no uncaught runtime or Vue errors')
    console.log('Browser checks passed: rectangle/polygon drawing, versions, persistence, unsaved guard, plan, task snapshot, local photo selection, report and tablet layout.')
  } finally {
    ws.close()
  }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
