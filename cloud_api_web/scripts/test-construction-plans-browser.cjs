const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const WebSocket = require('ws')
async function main () {
  const cdp = process.env.CONSTRUCTION_CDP_URL || 'http://127.0.0.1:9231'
  const base = process.env.CONSTRUCTION_PREVIEW_URL || 'http://127.0.0.1:8098'
  const target = await (await fetch(`${cdp}/json/new?about:blank`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise(resolve => ws.once('open', resolve))
  let sequence = 0; const calls = new Map(); const errors = []; const saved = []; const dispatched = []
  let coverageRadius = 2000; let coverageSaves = 0
  const area = { id: 'a1', name: '一号施工区', version: 1, enabled: true, archived: false, category: '基础施工', boundary: [{ lng: 120, lat: 36 }, { lng: 120.01, lat: 36 }, { lng: 120.01, lat: 36.01 }, { lng: 120, lat: 36.01 }] }
  const route = { wayline_id: 'r1', name: '一号区域航线', status: 'VALID', message: '校验通过', points: [{ lng: 120.002, lat: 36.002, height: 90 }, { lng: 120.008, lat: 36.008, height: 90 }] }
  const plans = [{ plan_id: 'p1', id: 1, name: '已有区域计划', area_id: 'a1', file_id: 'r1', dock_sn: 'dock1', task_type: 0, status: 'READY', message: '可下发', archived: 0 }]
  function send (method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = ++sequence; const timer = setTimeout(() => { calls.delete(id); reject(Error(method + ' timeout')) }, 15000)
      calls.set(id, { resolve: r => { clearTimeout(timer); resolve(r) }, reject: e => { clearTimeout(timer); reject(e) } })
      ws.send(JSON.stringify({ id, method, params }))
    })
  }
  ws.on('message', raw => {
    const e = JSON.parse(raw)
    if (e.id) { const c = calls.get(e.id); if (c) { calls.delete(e.id); e.error ? c.reject(e.error) : c.resolve(e.result) } }
    if (e.method === 'Runtime.exceptionThrown') errors.push(e.params.exceptionDetails.exception?.description || e.params.exceptionDetails.text)
    if (e.method !== 'Fetch.requestPaused') return
    const { request, requestId, resourceType } = e.params
    if (resourceType === 'Image') { send('Fetch.fulfillRequest', { requestId, responseCode: 204 }).catch(console.error); return }
    const url = new URL(request.url); const p = url.pathname; let data = {}; let code = 0; let message = ''
    if (request.method !== 'OPTIONS') {
      if (p.endsWith('/docks/dock1/coverage')) {
        if (request.method === 'PUT') { coverageRadius = JSON.parse(request.postData).radius_m; coverageSaves++ }
        data = { dock_sn: 'dock1', radius_m: coverageRadius }
      } else if (p.includes('devices')) data = { list: [{ device_sn: 'dock1', nickname: '一号机巢' }], pagination: { total: 1 } }
      else if (p.endsWith('/areas')) data = [area, { ...area, id: 'a2', name: '二号施工区' }]
      else if (p.endsWith('/a2/plans') || p.endsWith('/a2/waylines')) data = []
      else if (p.endsWith('/a1/plans') && request.method === 'GET') data = plans
      else if (p.endsWith('/a1/waylines')) data = [route, { ...route, wayline_id: 'stale', name: '失效航线', status: 'STALE' }]
      else if (p.endsWith('/waylines/r1')) data = route
      else if (p.endsWith('/a1/plans') && request.method === 'POST') {
        const body = JSON.parse(request.postData); saved.push(body)
        assert.equal(body.file_id, 'r1'); assert.equal(body.expected_version, 1)
        data = { ...plans[0], ...body, id: 2, plan_id: 'p2' }; plans.push(data)
      } else if (p.endsWith('/dispatch')) {
        dispatched.push(JSON.parse(request.postData)); data = { status: 'SUCCEEDED', job_id: 'job1', message: '已提交' }
        plans[0].last_dispatch = { status: 'SUCCEEDED', job_id: 'job1' }
      } else if (p.endsWith('/archive')) { plans.splice(0, 1) } else { code = 404; message = 'Unexpected mock URL: ' + p }
    }
    send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code, data, message })).toString('base64') }).catch(console.error)
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let i = 0; i < 100; i++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 150)) } throw Error('Timeout: ' + expression) }
  const button = text => evaluate(`[...document.querySelectorAll('button')].find(b=>b.textContent.replace(/\\s/g,'')===${JSON.stringify(text)}&&!b.disabled).click()`)
  try {
    await send('Page.enable'); await send('Runtime.enable')
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','w')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/construction/*' }, { urlPattern: '*devices*', resourceType: 'XHR' }, { urlPattern: '*/api/*.png', resourceType: 'Image' }] })
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: base + '/taskManage/construction/plans?area=a1' })
    await wait("document.querySelector('.plan-list')?.textContent.includes('已有区域计划')")
    await evaluate("document.querySelector('.el-table__body tbody tr').click()")
    await wait("document.querySelectorAll('.route-point').length===2")
    await button('刷新')
    await wait("document.querySelectorAll('.route-point').length===2 && !!document.querySelector('.el-table__body tr.current-row')")
    await evaluate("document.querySelectorAll('.plan-area-card')[1].click()")
    await wait("document.querySelector('.plan-list')?.textContent.includes('二号施工区') && document.querySelectorAll('.route-point').length===0")
    await evaluate("document.querySelectorAll('.plan-area-card')[0].click()")
    await wait("document.querySelector('.plan-list')?.textContent.includes('已有区域计划')")
    await button('＋新建计划')
    await wait("!!document.querySelector('input[data-testid=plan-name], [data-testid=plan-name] input')")
    await evaluate("(()=>{const el=document.querySelector('input[data-testid=plan-name], [data-testid=plan-name] input');el.value='新增工地计划';el.dispatchEvent(new Event('input',{bubbles:true}))})()")
    await evaluate("document.querySelector('.el-dialog .el-select').click()")
    await wait("[...document.querySelectorAll('.el-select-dropdown__item')].some(e=>e.textContent==='一号区域航线')")
    assert.equal(await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='失效航线').classList.contains('is-disabled')"), true)
    await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='一号区域航线').click()")
    await evaluate("document.querySelectorAll('.el-dialog .el-select')[1].click()")
    await wait("[...document.querySelectorAll('.el-select-dropdown__item')].some(e=>e.textContent==='一号机巢')")
    await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='一号机巢').click()")
    await button('保存计划')
    await wait("!document.querySelector('.el-dialog') && document.querySelector('.plan-list').textContent.includes('新增工地计划')")
    assert.equal(saved.length, 1); assert.equal(dispatched.length, 0, 'save never dispatches')
    await button('下发任务'); await wait("!!document.querySelector('.el-message-box')")
    assert.equal(dispatched.length, 0, 'confirmation required')
    assert.equal(await evaluate("[...document.querySelectorAll('.plan-area-card')].every(b=>b.disabled)"), true, 'area cannot switch during dispatch confirmation')
    await button('确认下发'); await wait("document.querySelector('.plan-list').textContent.includes('最近下发：已提交')")
    assert.equal(dispatched.length, 1); assert.ok(dispatched[0].request_id)
    // Inject the same reactive telemetry store used by production WebSocket handling.
    await evaluate("(()=>{const app=document.querySelector('#demo-app').__vue_app__;const store=Reflect.ownKeys(app._context.provides).map(k=>app._context.provides[k]).find(v=>v?.state?.deviceState&&v.commit);store.state.deviceState.dockInfo.dock1={basic_osd:{longitude:120.005,latitude:36.005}}})()")
    await wait("document.querySelectorAll('.construction-dock-marker').length===1")
    await evaluate("document.querySelector('.el-table__body tbody tr').click()")
    await wait("document.querySelectorAll('.route-point').length===2 && !!document.querySelector('.active-dock')")
    const firstMarker = await evaluate("(()=>{const r=document.querySelector('.construction-dock-marker .dock-pin').getBoundingClientRect();return {x:r.left+r.width/2,y:r.top+r.height/2}})()")
    await send('Input.dispatchMouseEvent', { type: 'mouseMoved', ...firstMarker })
    await send('Input.dispatchMouseEvent', { type: 'mousePressed', button: 'left', clickCount: 1, ...firstMarker })
    await send('Input.dispatchMouseEvent', { type: 'mouseReleased', button: 'left', clickCount: 1, ...firstMarker })
    await wait("document.querySelector('.coverage-toolbar')?.textContent.includes('已保存 2000m')")
    await button('5km')
    await wait("document.querySelector('.coverage-toolbar').textContent.includes('预览未保存')")
    await button('保存范围')
    await wait("document.querySelector('.coverage-toolbar').textContent.includes('已保存 5000m')")
    assert.equal(coverageRadius, 5000); assert.equal(coverageSaves, 1)
    await wait("document.querySelectorAll('.coverage-area.full').length===2")
    await button('100m')
    await wait("document.querySelectorAll('.coverage-area.partial').length===2 && document.querySelector('.coverage-areas').textContent.includes('按预览半径')")
    assert.equal(coverageSaves, 1, 'coverage preview does not save configuration')
    await button('5km')
    await evaluate("(()=>{const el=document.querySelector('.coverage-toolbar input');el.value='5001';el.dispatchEvent(new Event('input',{bubbles:true}))})()")
    assert.equal(await evaluate("[...document.querySelectorAll('.coverage-toolbar button')].find(b=>b.textContent==='保存范围').disabled"), true)
    await evaluate("(()=>{const el=document.querySelector('.dock-actions select');el.value='';el.dispatchEvent(new Event('change',{bubbles:true}))})()")
    await wait("!document.querySelector('.coverage-toolbar')")
    await new Promise(resolve => setTimeout(resolve, 400))
    const marker = await evaluate("(()=>{const r=document.querySelector('.construction-dock-marker .dock-pin').getBoundingClientRect();return {x:r.left+r.width/2,y:r.top+r.height/2}})()")
    await send('Input.dispatchMouseEvent', { type: 'mouseMoved', ...marker })
    await send('Input.dispatchMouseEvent', { type: 'mousePressed', button: 'left', clickCount: 1, ...marker })
    await send('Input.dispatchMouseEvent', { type: 'mouseReleased', button: 'left', clickCount: 1, ...marker })
    await wait("document.querySelector('.coverage-toolbar')?.textContent.includes('已保存 5000m')")
    await evaluate("[...document.querySelectorAll('.coverage-area')].find(b=>b.textContent.includes('二号施工区')).click()")
    await wait("document.querySelector('.plan-list').textContent.includes('二号施工区') && !!document.querySelector('.coverage-areas')")
    const screenshot = await send('Page.captureScreenshot', { format: 'png' })
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-plans-browser.png'), Buffer.from(screenshot.data, 'base64'))
    assert.deepEqual(errors, [])
    console.log('Construction plans browser passed: existing plan flow, telemetry, dock click coverage, radius preview/save/reload, over-limit disabled. Mock APIs only.')
  } catch (error) { console.error(await evaluate('document.body.innerText.slice(-1800)'), errors); throw error } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
