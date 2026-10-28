const assert = require('node:assert/strict')
const WebSocket = require('ws')
async function main () {
  const cdp = process.env.CONSTRUCTION_CDP_URL || 'http://127.0.0.1:9231'
  const base = process.env.CONSTRUCTION_PREVIEW_URL || 'http://127.0.0.1:8098'
  const target = await (await fetch(`${cdp}/json/new?about:blank`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise(resolve => ws.once('open', resolve))
  let seq = 0; const pending = new Map(); const errors = []; const unexpected = []
  let scans = 0; let exports = 0; let imported = false
  const id = 'offline-00000000-0000-0000-0000-000000000001'
  const origin = { deployment: '11111111-1111-1111-1111-111111111111', workspace: 'fixed-workspace', job_id: 'fixed-job' }
  const task = { job_id: id, name: '固定机巢工作面巡检', dock_sn: 'fixed-dock', photo_count: 1, execute_time: 1793000000000, imported_at: 1793000001000, origin }
  const result = { job_id: id, name: task.name, plan_type: 6, transfer_source: origin, expected_count: 1, received_count: 1, media_complete: true, analysis_configured: false, analysis: {}, photos: [{ file_id: 'image', name: 'DJI_WP1.jpg', area_id: 'area', area_name: '固定工作面', archived: true }], overall_route: { single_area: true, points: [], bindings: [{ area_id: 'area', area_name: '固定工作面', waypoints: [] }] } }
  function send (method, params = {}) {
    return new Promise((resolve, reject) => {
      const n = ++seq; const timer = setTimeout(() => { pending.delete(n); reject(Error(method + ' timeout')) }, 20000)
      pending.set(n, { resolve: value => { clearTimeout(timer); resolve(value) }, reject }); ws.send(JSON.stringify({ id: n, method, params }))
    })
  }
  ws.on('message', raw => {
    const event = JSON.parse(raw)
    if (event.id) { const call = pending.get(event.id); if (call) { pending.delete(event.id); event.error ? call.reject(event.error) : call.resolve(event.result) } return }
    if (event.method === 'Runtime.exceptionThrown') errors.push(event.params.exceptionDetails.exception?.description || event.params.exceptionDetails.text)
    if (event.method !== 'Fetch.requestPaused') return
    const { request, requestId } = event.params; const p = new URL(request.url).pathname
    let data = {}; let code = 0
    if (request.method !== 'OPTIONS') {
      if (p.endsWith('/transfers') || p.endsWith('/transfers/scan')) {
        if (p.endsWith('/scan')) { imported = true; scans++ }
        data = { deployment: 'mobile', inbox: '/data/transfers/workspace/inbox', exports: '/data/transfers/workspace/exports', tasks: imported ? [task] : [], receipts: p.endsWith('/scan') ? [{ file: 'fixed.zip', status: scans === 1 ? 'IMPORTED' : 'SKIPPED', message: scans === 1 ? '已接收' : '该任务已导入' }, { file: 'broken.zip', status: 'FAILED', message: '原图损坏或拷贝不完整' }] : [] }
      } else if (p.endsWith('/jobs')) data = { list: [], pagination: { total: 0, page: 1, page_size: 10 } }
      else if (p.endsWith(`/tasks/${id}`)) data = result
      else if (p.endsWith(`/tasks/${id}/export`)) { exports++; data = { name: 'fixed.zip', path: '/data/transfers/workspace/exports/fixed.zip' } } else if (p.endsWith(`/tasks/${id}/media/image`)) {
        send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'image/svg+xml' }, { name: 'Access-Control-Allow-Origin', value: '*' }], body: Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="50" height="40"><rect width="50" height="40" fill="blue"/></svg>').toString('base64') }).catch(console.error); return
      } else if (p.includes('/api/construction/') || p.includes('getPlanType')) { unexpected.push(p); code = 404 } else data = { list: [], pagination: { total: 0, page: 1 } }
    }
    send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code, data })).toString('base64') }).catch(console.error)
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let i = 0; i < 120; i++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 150)) } throw Error('Timeout: ' + expression) }
  const button = text => evaluate(`[...document.querySelectorAll('button')].find(b=>b.textContent.replace(/\\s/g,'')===${JSON.stringify(text)}&&!b.disabled).click()`)
  try {
    await send('Page.enable'); await send('Runtime.enable')
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','w')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*', resourceType: 'XHR' }, { urlPattern: '*', resourceType: 'Fetch' }] })
    await send('Page.navigate', { url: base + '/task' })
    await wait("document.querySelector('.transfer-panel')?.textContent.includes('暂无离线接收任务')")
    assert.equal(scans, 0, 'opening list does not import unfinished operator copies')
    await button('刷新并接收')
    await wait("document.querySelector('.transfer-panel').textContent.includes('固定机巢工作面巡检')")
    assert.equal(await evaluate("document.querySelector('.transfer-receipts').textContent.includes('原图损坏')"), true)
    await button('刷新并接收')
    await wait("document.querySelector('.transfer-receipts').textContent.includes('该任务已导入')")
    assert.equal(await evaluate("document.querySelectorAll('.transfer-panel .el-table__body tr').length"), 1)
    await button('查看任务结果')
    await wait("document.querySelector('.construction-shell')?.textContent.includes('fixed-job')")
    assert.equal(await evaluate("document.querySelector('.construction-shell').textContent.includes('固定工作面')"), true)
    await button('导出离线任务包')
    await wait("document.querySelector('.construction-shell').textContent.includes('/data/transfers/workspace/exports/fixed.zip')")
    assert.equal(exports, 1)
    assert.deepEqual(unexpected, [], 'imported result never uses flight-plan type endpoint')
    assert.deepEqual(errors, [])
    console.log('Offline transfer browser passed: inbox instructions, scan receipts/errors, repeat, imported result routing and export. Mock APIs only.')
  } catch (e) { console.error(await evaluate('document.body.innerText.slice(-2500)'), errors, unexpected); throw e } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(e => { console.error(e); process.exitCode = 1 })
