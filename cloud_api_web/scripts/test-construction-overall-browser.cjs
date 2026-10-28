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
  let uploads = 0; let failInspect = false
  let needsPreparation = false; let preparations = 0; let assignmentsSaved = 0
  let resultFixture; let archived = false; let manyPhotos = false; let counted = false
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
    if (request.method !== 'OPTIONS' && p.includes('/tasks/job/media/')) {
      const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="960" height="600"><rect width="960" height="600" fill="#284c67"/><path d="M0 500L250 200L550 400L730 150L960 400V600H0" fill="#477b88"/><text x="50" y="80" fill="white" font-size="30">Inspection test image</text></svg>'
      send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'image/svg+xml' }, { name: 'Access-Control-Allow-Origin', value: '*' }], body: Buffer.from(svg).toString('base64') }).catch(console.error); return
    }
    if (request.method !== 'OPTIONS') {
      if (p.includes('/devices')) data = { list: [{ device_sn: 'dock', nickname: '测试机巢' }], pagination: { total: 1 } }
      else if (p.endsWith('/areas')) data = areas
      else if (p.endsWith('/docks/dock/coverage')) data = { dock_sn: 'dock', radius_m: 2000 }
      else if (p.endsWith('/waylines/file/upload')) { assert.ok(p.startsWith('/importNoValiKmz/'), 'reuse the actual route manager upload endpoint'); uploads++; data = { wayline_id: 'file', name: route.name } } else if (p.endsWith('/overall-routes/prepare')) { const body = JSON.parse(request.postData); assert.equal(body.wayline_id, 'file'); assert.equal(body.file_sign, 'sign'); preparations++; data = { ...route, wayline_id: 'execution', source_wayline_id: 'file', source_sign: 'sign', file_sign: 'prepared', needs_preparation: false } } else if (p.endsWith('/overall-routes/inspect')) { data = { ...route, needs_preparation: needsPreparation }; if (failInspect) { code = 400; failInspect = false } } else if (p.endsWith('/overall-routes') && request.method === 'GET') data = overall ? [overall] : []
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
      else if (p.endsWith('/tasks/job/photo-associations')) {
        const body = JSON.parse(request.postData); assert.equal(body.expected_revision, assignmentsSaved); assert.deepEqual(body.file_ids, ['4'])
        if (body.excluded) assert.ok(body.reason.length > 0); else assert.equal(body.area_id, 'b')
        assignmentsSaved++
        data = { ...resultFixture, association_revision: assignmentsSaved, unresolved_count: 0, analysis_stale: true, photos: resultFixture.photos.map(p => p.file_id === '4' ? { ...p, association: body.excluded ? 'EXCLUDED' : 'MANUAL', area_id: body.excluded ? undefined : 'b', area_name: body.excluded ? undefined : '二区', excluded: body.excluded, association_message: body.excluded ? body.reason : '人工确认工作面' } : p) }; resultFixture = data
      } else if (p.endsWith('/tasks/job/archive')) { archived = true; data = { ...resultFixture, photos: resultFixture.photos.map(p => ({ ...p, archived: true })) } } else if (p.endsWith('/tasks/job')) data = { job_id: 'job', name: '总体巡检', plan_type: 6, expected_count: 4, received_count: 4, media_complete: true, analysis_configured: false, analysis: {}, overall_route: overall, photos: [{ file_id: '1', name: 'DJI_V_航点1.jpg', area_id: 'a', area_name: '一区', waypoint_index: 1, archived: false }, { file_id: '2', name: 'DJI_T_航点2.jpg', area_id: 'a', area_name: '一区', waypoint_index: 2, archived: false }, { file_id: '3', name: 'DJI_V_航点3.jpg', area_id: 'b', area_name: '二区', waypoint_index: 3, archived: false }, { file_id: '4', name: 'unknown.jpg', association: 'UNMATCHED', archived: false }] }
      else { unexpected.push(p); code = 404 }
    }
    if (request.method !== 'OPTIONS' && p.endsWith('/tasks/job')) {
      if (manyPhotos) data.photos = Array.from({ length: 27 }, (_, i) => ({ file_id: String(i + 1), name: `photo-${i + 1}.jpg`, area_id: 'a', area_name: '一区', archived }))
      else data.photos = data.photos.map(p => ({ ...p, archived }))
      if (counted) data.analysis = { status: 'SUCCEEDED', batch_id: 'batch', result: { areas: [{ area_id: 'a', status: 'success', count: 0 }, { area_id: 'b', status: 'unassessable', count: null }] } }
      resultFixture = data
    }
    send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code, data, message: code ? 'Unexpected mock URL: ' + p : '' })).toString('base64') }).catch(console.error)
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let i = 0; i < 120; i++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 150)) } throw Error('Timeout: ' + expression) }
  const button = async text => {
    const selector = `[...document.querySelectorAll('button')].find(b=>b.textContent.replace(/\\s/g,'')===${JSON.stringify(text)}&&!b.disabled&&b.getClientRects().length)`
    await wait(`!!(${selector})`)
    return evaluate(`${selector}.click()`)
  }
  const moveDock = lng => evaluate(`(()=>{const app=document.querySelector('#demo-app').__vue_app__;const store=Reflect.ownKeys(app._context.provides).map(k=>app._context.provides[k]).find(v=>v?.state?.deviceState&&v.commit);store.state.deviceState.dockInfo.dock={basic_osd:{longitude:${lng},latitude:36.001}}})()`)
  try {
    await send('Page.enable'); await send('Runtime.enable')
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','w')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/construction/*' }, { urlPattern: '*devices*', resourceType: 'XHR' }, { urlPattern: '*/workspaces/w/waylines*' }, { urlPattern: '*/files/getPlanType*' }, { urlPattern: '*/api/*.png', resourceType: 'Image' }] })
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: base + '/taskManage/construction/overall' })
    await wait("!!document.querySelector('.overall-page')")
    assert.equal(await evaluate("document.querySelector('.upload-overall').disabled"), true, 'upload requires a selected dock')
    await moveDock(120.003)
    await wait("document.querySelectorAll('.construction-dock-marker').length===1")
    await evaluate("(()=>{const el=document.querySelector('.dock-actions select');el.value='dock';el.dispatchEvent(new Event('change',{bubbles:true}))})()")
    await wait("document.querySelectorAll('.area-card').length===2 && ![...document.querySelectorAll('button')].find(b=>b.textContent==='从航线库添加').disabled")
    const selectFile = name => evaluate(`(()=>{const el=document.querySelector('input[type=file]');const dt=new DataTransfer();dt.items.add(new File(['mock-kmz'],${JSON.stringify(name)}));el.files=dt.files;el.dispatchEvent(new Event('change',{bubbles:true}))})()`)
    await selectFile('invalid.txt')
    await wait("document.querySelector('.overall-page').textContent.includes('请选择非空的 KMZ')")
    assert.equal(uploads, 0)
    failInspect = true
    await selectFile('overall.KMZ')
    await wait("document.querySelector('.overall-page').textContent.includes('文件已保存到航线库，但未进入绑定')")
    assert.equal(uploads, 1)
    await button('重新解析')
    await wait("document.querySelectorAll('.bindings-panel .el-checkbox').length===4")
    assert.equal(uploads, 1, 'retry parsing does not upload a duplicate')
    assert.equal(dispatches, 0, 'upload and parsing never dispatch flight')
    await button('取消编辑'); await wait("!!document.querySelector('.el-message-box')"); await button('确定')
    await wait("!document.querySelector('.bindings-panel')")
    needsPreparation = true
    await button('从航线库添加'); await wait("document.querySelector('.el-dialog')?.textContent.includes('总体测试航线')")
    await button('选择并解析'); await wait("!!document.querySelector('.execution-notice')")
    assert.equal(preparations, 1, 'missing identifiers prepare execution route automatically')
    assert.equal(dispatches, 0)
    await button('取消编辑'); await wait("!!document.querySelector('.el-message-box')"); await button('确定')
    await wait("!document.querySelector('.bindings-panel')")
    needsPreparation = false
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
    // 新版结果页：按区域绑定快照分组的照片卡片网格
    await wait("document.querySelectorAll('.construction-shell .photo-card').length===4")
    await evaluate("[...document.querySelectorAll('.region-choice')].find(e=>e.textContent.includes('一区')).click()")
    await wait("document.querySelectorAll('.photo-card').length===2")
    const groupCards = name => `document.querySelectorAll('.photo-group').length && [...document.querySelectorAll('.photo-group')].find(g=>g.querySelector('h3').textContent.includes('${name}'))?.querySelectorAll('.photo-card').length`
    assert.equal(await evaluate(groupCards('一区')), 2, 'area group shows its two photos')
    await evaluate("[...document.querySelectorAll('.region-choice')].find(e=>e.textContent.includes('二区')).click()")
    await wait("document.querySelectorAll('.photo-card').length===1")
    assert.equal(await evaluate(groupCards('二区')), 1, 'second area group shows its photo')
    await evaluate("[...document.querySelectorAll('.region-choice')].find(e=>e.textContent.includes('未匹配')).click()")
    await wait("document.querySelector('.photo-group.warn')")
    assert.equal(await evaluate(groupCards('未匹配 / 未绑定')), 1, 'unmatched group shows unknown photo')
    assert.equal(await evaluate("[...document.querySelectorAll('.photo-group')].some(g=>g.className.includes('warn'))"), true, 'unmatched group carries warning style')
    assert.equal(await evaluate("document.querySelector('.construction-shell').textContent.includes('unknown.jpg')"), true)
    assert.equal(await evaluate("document.querySelectorAll('.stat-card').length"), 4, 'result overview has four status cards')
    await evaluate("document.querySelector('.region-choice').click()")
    await evaluate("[...document.querySelectorAll('.el-tabs__item')].find(e=>e.textContent.includes('分析与结果')).click()")
    await wait("document.querySelector('.analysis-rows') && document.querySelector('.analysis-rows').textContent.includes('未检查')")
    assert.equal(await evaluate("document.querySelector('.construction-shell').textContent.includes('未分析不代表无隐患')"), true, 'empty analysis is explicit')
    await evaluate("[...document.querySelectorAll('.el-tabs__item')].find(e=>e.textContent.includes('巡检报告')).click()")
    await wait("document.querySelector('.report-panel')")
    assert.equal(await evaluate("document.querySelector('.report-panel').textContent.includes('不构成最终巡检结论')"), true, 'report preview does not invent conclusions')
    await evaluate("[...document.querySelectorAll('.report-chapter')].find(e=>e.textContent.includes('一区')).querySelector('button').click()")
    await wait("document.querySelectorAll('.photo-card').length===2")
    assert.equal(await evaluate("document.querySelector('.region-choice.selected').textContent.includes('一区')"), true, 'report opens selected area photos')
    await evaluate("[...document.querySelectorAll('.region-choice')].find(e=>e.textContent.includes('未匹配')).click()")
    await wait("document.querySelectorAll('.photo-card').length===1")
    assert.equal(await evaluate("document.querySelector('.photo-card').textContent.includes('unknown.jpg')"), true, 'unmatched filter isolates unknown photo')
    await evaluate("document.querySelector('.region-choice').click()")
    await wait("document.querySelectorAll('.photo-card').length===4")
    await evaluate("(()=>{const input=document.querySelector('.gallery-actions input');input.value='航点2';input.dispatchEvent(new Event('input',{bubbles:true}))})()")
    await wait("document.querySelectorAll('.photo-card').length===1")
    assert.equal(await evaluate("document.querySelector('.photo-card').disabled"), true, 'unarchived photo cannot preview')
    await evaluate("(()=>{const input=document.querySelector('.gallery-actions input');input.value='不存在';input.dispatchEvent(new Event('input',{bubbles:true}))})()")
    await wait("document.querySelectorAll('.photo-card').length===0")
    await button('清除筛选')
    await wait("document.querySelectorAll('.photo-card').length===4")
    await button('保存/补齐本地原图')
    await wait("document.querySelectorAll('.photo-card.archived').length===4")
    await evaluate("document.querySelector('.photo-card').click()")
    await wait("!!document.querySelector('.viewer-canvas img')")
    assert.equal(await evaluate("document.querySelector('.viewer-info').textContent.includes('航点 1')"), true)
    await button('下一张→')
    await wait("document.querySelector('.viewer-info').textContent.includes('航点 2')")
    await button('放大2×')
    await wait("document.querySelector('.viewer-canvas img').style.width==='200%'")
    await evaluate("document.querySelector('.construction-photo-dialog .el-dialog__headerbtn').click()")
    await wait("!document.querySelector('.el-overlay-dialog') || document.querySelector('.el-overlay').style.display==='none'")
    manyPhotos = true
    await button('刷新结果')
    await wait("document.querySelectorAll('.photo-card').length===24")
    await evaluate("document.querySelector('.el-pagination .btn-next').click()")
    await wait("document.querySelectorAll('.photo-card').length===3")
    manyPhotos = false; counted = true
    await button('刷新结果')
    await wait("document.querySelectorAll('.photo-card').length===4")
    await evaluate("[...document.querySelectorAll('.el-tabs__item')].find(e=>e.textContent.includes('分析与结果')).click()")
    await wait("document.querySelectorAll('.people-grid article').length===2")
    assert.equal(await evaluate("document.querySelector('.people-grid article strong').textContent"), '0人', 'successful zero count is displayed')
    assert.equal(await evaluate("document.querySelectorAll('.people-grid article strong')[1].textContent"), '—人', 'unassessable count is not displayed as zero')
    await evaluate("[...document.querySelectorAll('.region-choice')].find(e=>e.textContent.includes('二区')).click()")
    await wait("document.querySelectorAll('.people-grid article').length===1")
    assert.equal(await evaluate("document.querySelector('.people-grid article').textContent.includes('无法判断')"), true)
    await evaluate("document.querySelector('.region-choice').click();[...document.querySelectorAll('.el-tabs__item')].find(e=>e.textContent.includes('本地照片')).click()")
    await button('确认照片归属')
    await wait("document.querySelectorAll('.association-form .el-table__body tr').length===1")
    await evaluate("document.querySelector('.association-form .el-table__body .el-checkbox input').click();document.querySelector('.association-form .el-select').click()")
    await wait("[...document.querySelectorAll('.el-select-dropdown__item')].some(e=>e.textContent==='二区')")
    await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='二区').click()")
    await button('保存确认')
    await wait("document.querySelector('.association-toolbar').textContent.includes('0 张照片待确认')")
    assert.equal(assignmentsSaved, 1)
    assert.equal(await evaluate("document.querySelector('.construction-shell').textContent.includes('归属变更待重分析')"), true)
    await button('确认照片归属')
    await wait("document.querySelectorAll('.association-form .el-table__body tr').length===4")
    await evaluate("[...document.querySelectorAll('.association-form .el-table__body tr')].find(e=>e.textContent.includes('unknown.jpg')).querySelector('.el-checkbox input').click();[...document.querySelectorAll('.association-form .el-radio')].find(e=>e.textContent.includes('排除分析')).click()")
    await wait("!!document.querySelector('.association-form input[maxlength]')")
    assert.equal(await evaluate("[...document.querySelectorAll('button')].find(e=>e.textContent==='保存确认').disabled"), true, 'exclusion requires reason')
    await evaluate("(()=>{const el=document.querySelector('.association-form input[maxlength]');el.value='转场测试照片';el.dispatchEvent(new Event('input',{bubbles:true}))})()")
    await button('保存确认')
    await wait("document.querySelector('.association-toolbar').textContent.includes('已排除 1 张')")
    assert.equal(assignmentsSaved, 2)
    await send('Emulation.setDeviceMetricsOverride', { width: 768, height: 1024, deviceScaleFactor: 1, mobile: false })
    await wait('innerWidth===768')
    assert.equal(await evaluate('document.documentElement.scrollWidth <= window.innerWidth + 2'), true, 'result page fits tablet width')
    await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1080, deviceScaleFactor: 1, mobile: false })
    await wait('innerWidth===1600')
    await send('Page.bringToFront')
    const resultShot = await send('Page.captureScreenshot', { format: 'png' })
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-result-page.png'), Buffer.from(resultShot.data, 'base64'))
    console.log('Result page screenshot saved')
    assert.deepEqual(errors, []); assert.deepEqual(unexpected, [])
    console.log('Overall browser passed: shared route/plan workflow, area photo cards, analysis and report boundary, tablet layout. Mock APIs only.')
  } catch (e) { console.error(await evaluate('document.body.innerText.slice(-2200)'), errors, unexpected); throw e } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(e => { console.error(e); process.exitCode = 1 })
