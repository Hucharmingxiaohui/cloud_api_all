const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const os = require('node:os')
const WebSocket = require('ws')
async function main () {
  const cdp = process.env.CONSTRUCTION_CDP_URL || 'http://127.0.0.1:9231'
  const base = process.env.CONSTRUCTION_PREVIEW_URL || 'http://127.0.0.1:8098'
  const target = await (await fetch(`${cdp}/json/new?about:blank`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise(resolve => ws.once('open', resolve))
  let seq = 0; const calls = new Map(); const errors = []; const unexpected = []
  const areas = ['a', 'b'].map((id, i) => ({ id, name: i ? '二区' : '一区', enabled: true, archived: false, version: 1, category: '施工', boundary: [{ lng: 120 + i * 0.004, lat: 36 }, { lng: 120.003 + i * 0.004, lat: 36 }, { lng: 120.003 + i * 0.004, lat: 36.003 }, { lng: 120 + i * 0.004, lat: 36.003 }] }))
  const route = { wayline_id: 'file', name: '总体测试航线', dock_sn: 'dock', file_sign: 'sign', height_mode: 'WGS84', points: [1, 2, 3, 4].map((index) => ({ index, lng: 120 + index * 0.0015, lat: 36.001, height: 100, photo_suffixes: index === 4 ? [] : ['航点' + index] })) }
  let overall = null; let plan = null; let dispatches = 0
  function send (method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = ++seq; const timer = setTimeout(() => { calls.delete(id); reject(Error(method + ' timeout')) }, 15000)
      calls.set(id, { resolve: value => { clearTimeout(timer); resolve(value) }, reject }); ws.send(JSON.stringify({ id, method, params }))
    })
  }
  ws.on('message', raw => {
    const event = JSON.parse(raw)
    if (event.id) { const c = calls.get(event.id); if (c) { calls.delete(event.id); event.error ? c.reject(event.error) : c.resolve(event.result) } }
    if (event.method === 'Runtime.exceptionThrown') errors.push(event.params.exceptionDetails.exception?.description || event.params.exceptionDetails.text)
    if (event.method !== 'Fetch.requestPaused') return
    const { request, requestId, resourceType } = event.params
    if (resourceType === 'Image') { send('Fetch.fulfillRequest', { requestId, responseCode: 204 }).catch(console.error); return }
    const p = new URL(request.url).pathname; let data = {}; let code = 0
    if (request.method !== 'OPTIONS') {
      if (p.includes('/devices')) data = { list: [{ device_sn: 'dock', nickname: '测试机巢' }], pagination: { total: 1 } }
      else if (p.endsWith('/areas')) data = areas
      else if (p.endsWith('/docks/dock/coverage')) data = { dock_sn: 'dock', radius_m: 2000 }
      else if (p.endsWith('/overall-routes/inspect')) data = route
      else if (p.endsWith('/overall-routes') && request.method === 'GET') data = overall ? [overall] : []
      else if (p.endsWith('/overall-routes') && request.method === 'POST') {
        const body = JSON.parse(request.postData)
        assert.equal(body.wayline_id, 'file'); assert.equal(body.dock_sn, 'dock'); assert.equal(body.file_sign, 'sign')
        assert.deepEqual(body.bindings.map(b => [b.area_id, b.waypoints]), [['a', [1, 2]], ['b', [3]]])
        overall = { ...route, overall_id: 'overall', version: 1, status: 'VALID', message: '绑定已保存', bindings: body.bindings.map(b => ({ ...b, area_name: areas.find(a => a.id === b.area_id).name })) }; data = overall
      } else if (p.endsWith('/overall-routes/overall/plans') && request.method === 'POST') {
        const body = JSON.parse(request.postData); assert.equal(body.file_id, 'file'); assert.equal(body.dock_sn, 'dock'); assert.equal(body.expected_version, 1)
        plan = { ...body, plan_id: 'plan', overall_id: 'overall', bindings: overall.bindings, status: 'READY', message: '可下发', archived: false }; data = plan
      } else if (p.endsWith('/overall-routes/overall/plans')) data = plan ? [plan] : []
      else if (p.endsWith('/plans/plan/dispatch')) { dispatches++; data = { status: 'SUCCEEDED', message: '已提交', job_id: 'job' }; plan.message = '已提交'; plan.status = 'BLOCKED' } else if (p.endsWith('/waylines')) data = { list: [{ id: 'file', name: route.name }], pagination: { total: 1 } }
      else if (p.endsWith('/files/getPlanType')) data = 6
      else if (p.endsWith('/tasks/job')) data = { job_id: 'job', name: '总体巡检', plan_type: 6, expected_count: 4, received_count: 4, media_complete: true, analysis_configured: false, analysis: {}, overall_route: overall, photos: [{ file_id: '1', name: 'DJI_V_航点1.jpg', area_id: 'a', area_name: '一区', waypoint_index: 1, archived: false }, { file_id: '2', name: 'DJI_T_航点2.jpg', area_id: 'a', area_name: '一区', waypoint_index: 2, archived: false }, { file_id: '3', name: 'DJI_V_航点3.jpg', area_id: 'b', area_name: '二区', waypoint_index: 3, archived: false }, { file_id: '4', name: 'unknown.jpg', association: 'UNMATCHED', archived: false }] }
      else { unexpected.push(p); code = 404 }
    }
    send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code, data, message: code ? 'Unexpected mock URL: ' + p : '' })).toString('base64') }).catch(console.error)
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let i = 0; i < 120; i++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 150)) } throw Error('Timeout: ' + expression) }
  const button = text => evaluate(`[...document.querySelectorAll('button')].find(b=>b.textContent.replace(/\\s/g,'')===${JSON.stringify(text)}&&!b.disabled).click()`)
  const moveDock = lng => evaluate(`(()=>{const app=document.querySelector('#demo-app').__vue_app__;const store=Reflect.ownKeys(app._context.provides).map(k=>app._context.provides[k]).find(v=>v?.state?.deviceState&&v.commit);store.state.deviceState.dockInfo.dock={basic_osd:{longitude:${lng},latitude:36.001}}})()`)
  try {
    await send('Page.enable'); await send('Runtime.enable')
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','w')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/construction/*' }, { urlPattern: '*devices*', resourceType: 'XHR' }, { urlPattern: '*/workspaces/w/waylines*' }, { urlPattern: '*/files/getPlanType*' }, { urlPattern: '*/api/*.png', resourceType: 'Image' }] })
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: base + '/taskManage/construction/overall' })
    await wait("!!document.querySelector('.overall-page')")
    await moveDock(120.003)
    await wait("document.querySelectorAll('.construction-dock-marker').length===1")
    await evaluate("(()=>{const el=document.querySelector('.dock-actions select');el.value='dock';el.dispatchEvent(new Event('change',{bubbles:true}))})()")
    await wait("document.querySelectorAll('.area-card').length===2 && ![...document.querySelectorAll('button')].find(b=>b.textContent==='从航线库添加').disabled")
    await button('从航线库添加'); await wait("document.querySelector('.el-dialog')?.textContent.includes('总体测试航线')")
    await button('选择并解析'); await wait("document.querySelectorAll('.bindings-panel .el-checkbox').length===4")
    assert.equal(await evaluate("document.querySelector('.dock-actions select').disabled"), true, 'dock locked during binding edit')
    assert.equal(await evaluate("document.querySelectorAll('.bindings-panel .el-checkbox input')[3].disabled"), true, 'transit point without suffix cannot bind')
    await evaluate("document.querySelectorAll('.area-card')[0].click();document.querySelectorAll('.bindings-panel .el-checkbox input')[0].click();document.querySelectorAll('.bindings-panel .el-checkbox input')[1].click()")
    await button('绑定选中航点')
    await evaluate("document.querySelectorAll('.area-card')[1].click();document.querySelectorAll('.bindings-panel .el-checkbox input')[2].click()")
    await button('绑定选中航点'); await button('保存绑定')
    await wait("!!document.querySelector('.plans-panel') && document.querySelectorAll('.route-card').length===1")
    assert.equal(dispatches, 0, 'binding does not create flight')
    await moveDock(121)
    await wait("[...document.querySelectorAll('button')].find(b=>b.textContent==='新建总体计划').disabled")
    await moveDock(120.003)
    await wait("![...document.querySelectorAll('button')].find(b=>b.textContent==='新建总体计划').disabled")
    await button('新建总体计划'); await wait("!!document.querySelector('[data-testid=plan-name] input, input[data-testid=plan-name]')")
    await evaluate("(()=>{const el=document.querySelector('[data-testid=plan-name] input, input[data-testid=plan-name]');el.value='多区域测试计划';el.dispatchEvent(new Event('input',{bubbles:true}))})()")
    await button('保存计划'); await wait("document.querySelector('.plans-panel').textContent.includes('多区域测试计划')")
    assert.equal(dispatches, 0, 'saving plan does not dispatch')
    await button('下发任务'); await wait("!!document.querySelector('.el-message-box')")
    assert.equal(dispatches, 0, 'dispatch awaits explicit confirmation')
    await button('确认下发'); await wait("document.querySelector('.plans-panel').textContent.includes('已提交')")
    assert.equal(dispatches, 1, 'single flight for both areas')
    const screenshot = await send('Page.captureScreenshot', { format: 'png' }); fs.writeFileSync(path.join(os.tmpdir(), 'construction-overall-browser.png'), Buffer.from(screenshot.data, 'base64'))
    await button('编辑/重新校验绑定')
    await wait("document.querySelectorAll('.bindings-panel .el-checkbox').length===4")
    await button('取消编辑'); await wait("!!document.querySelector('.el-message-box')"); await button('确定')
    await wait("!!document.querySelector('.plans-panel')")
    route.file_sign = 'changed'
    await button('编辑/重新校验绑定'); await wait("document.querySelectorAll('.bindings-panel .el-checkbox').length===4")
    assert.equal(await evaluate("[...document.querySelectorAll('button')].find(b=>b.textContent==='保存绑定').disabled"), true, 'changed file clears stale point bindings')
    await button('取消编辑'); await wait("!!document.querySelector('.el-message-box')"); await button('确定')
    await send('Page.navigate', { url: base + '/task/taskResult?job_id=job' })
    await wait("document.querySelectorAll('.construction-shell .el-table__body tbody tr').length===4")
    await evaluate("document.querySelector('.construction-shell .el-select').click()")
    await wait("[...document.querySelectorAll('.el-select-dropdown__item')].some(e=>e.textContent==='一区（2）')")
    await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='一区（2）').click()")
    await wait("document.querySelectorAll('.construction-shell .el-table__body tbody tr').length===2")
    await evaluate("document.querySelector('.construction-shell .el-select').click()")
    await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='未匹配 / 未绑定（1）').click()")
    await wait("document.querySelectorAll('.construction-shell .el-table__body tbody tr').length===1 && document.querySelector('.construction-shell').textContent.includes('unknown.jpg')")
    assert.deepEqual(errors, []); assert.deepEqual(unexpected, [])
    console.log('Overall browser passed: shared library, multi-region/point binding, transit exclusion, moving dock, one plan/job, dispatch confirmation, changed-file rebinding and regional/unmatched photo filters. Mock APIs only.')
  } catch (e) { console.error(await evaluate('document.body.innerText.slice(-2200)'), errors, unexpected); throw e } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(e => { console.error(e); process.exitCode = 1 })
