const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const WebSocket = require('ws')
async function main () {
  const cdp = process.env.CONSTRUCTION_CDP_URL || 'http://127.0.0.1:9231'
  const base = process.env.CONSTRUCTION_PREVIEW_URL || 'http://127.0.0.1:8097'
  const target = await (await fetch(`${cdp}/json/new?about:blank`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise(resolve => ws.once('open', resolve))
  let id = 0; const calls = new Map(); const errors = []; const saved = []; const pending = new Map()
  const area = { id: 'area-browser', name: '一号施工区', version: 1, enabled: true, archived: false, category: '基础施工', notes: '', boundary: [{ lng: 120, lat: 36 }, { lng: 120.002, lat: 36 }, { lng: 120.002, lat: 36.002 }, { lng: 120, lat: 36.002 }] }
  const points = [{ lng: 120.002, lat: 36.002, height: 90 }, { lng: 120.0015, lat: 36.0015, height: 90 }]
  const routes = [{ wayline_id: 'shared-route', name: '已关联航线', status: 'VALID', message: '区域边界校验通过', source: 'upload', area_version: 1, points, height_mode: 'WGS84' }]
  function send (method, params = {}) {
    return new Promise((resolve, reject) => {
      const key = ++id; const timer = setTimeout(() => { calls.delete(key); reject(Error('CDP timeout: ' + method)) }, 20000)
      calls.set(key, { resolve: value => { clearTimeout(timer); resolve(value) }, reject: error => { clearTimeout(timer); reject(error) } })
      ws.send(JSON.stringify({ id: key, method, params }))
    })
  }
  ws.on('message', raw => {
    const e = JSON.parse(raw)
    if (e.method === 'Network.requestWillBeSent') pending.set(e.params.requestId, e.params.request.url)
    if (e.method === 'Network.loadingFinished' || e.method === 'Network.loadingFailed') pending.delete(e.params.requestId)
    if (e.id) { const c = calls.get(e.id); if (c) { calls.delete(e.id); e.error ? c.reject(e.error) : c.resolve(e.result) } }
    if (e.method === 'Runtime.exceptionThrown') errors.push(e.params.exceptionDetails.exception?.description || e.params.exceptionDetails.text)
    if (e.method === 'Fetch.requestPaused') {
      if (e.params.resourceType === 'Image') {
        send('Fetch.fulfillRequest', { requestId: e.params.requestId, responseCode: 204 }).catch(console.error)
        return
      }
      const request = e.params.request; const url = new URL(request.url); let data = { list: [] }
      if (url.pathname.includes('/api/construction/areas')) {
        if (url.pathname.endsWith('/waylines/upload') || (url.pathname.endsWith('/waylines') && request.method === 'POST')) {
          if (request.postData && request.headers['Content-Type']?.includes('application/json')) saved.push(JSON.parse(request.postData))
          const route = { ...routes[0], wayline_id: 'new-route-' + routes.length, name: '新建巡检航线', source: request.url.endsWith('upload') ? 'upload' : 'manual' }
          routes.push(route); data = route
        } else if (url.pathname.endsWith('/waylines/bind') && request.method === 'POST') {
          const body = JSON.parse(request.postData)
          assert.equal(body.wayline_id, 'library-route')
          assert.equal(body.expected_version, 1)
          data = { ...routes[0], wayline_id: body.wayline_id, name: '航线库原文件', source: 'existing' }
          routes.push(data)
        } else if (url.pathname.endsWith('/waylines')) data = routes
        else if (url.pathname.includes('/waylines/')) data = routes.find(r => url.pathname.endsWith(r.wayline_id))
        else if (url.pathname.endsWith('/area-browser')) data = area
        else if (url.pathname.endsWith('/area-other')) data = { ...area, id: 'area-other', name: '二号施工区' }
        else data = [area, { ...area, id: 'area-other', name: '二号施工区' }]
        if (url.pathname.includes('area-other/waylines')) data = []
      } else if (url.pathname.includes('/wayline/api/')) data = { list: [{ id: 'library-route', name: '航线库原文件' }], pagination: { total: 1 } }
      send('Fetch.fulfillRequest', { requestId: e.params.requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code: 0, data })).toString('base64') }).catch(console.error)
    }
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let n = 0; n < 100; n++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 200)) } throw Error('Timeout: ' + expression) }
  const button = label => evaluate(`[...document.querySelectorAll('button')].find(b=>b.textContent.replace(/\\s/g,'')===${JSON.stringify(label)}).click()`)
  try {
    await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable'); await send('Network.setCacheDisabled', { cacheDisabled: true })
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','construction-browser');localStorage.setItem('wayline','generic-draft-preserved')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/construction/areas*' }, { urlPattern: '*/wayline/api/*' }, { urlPattern: '*devices*', resourceType: 'XHR' }, { urlPattern: '*/api/*.png', resourceType: 'Image' }] })
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: base + '/variableMgt/constructionAreas?area=area-browser' })
    await wait("!!document.querySelector('.route-select')")
    await evaluate("document.querySelector('.route-select').click()")
    await wait("document.querySelectorAll('.route-point').length===2")
    const image = await send('Page.captureScreenshot', { format: 'png' })
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-waylines.png'), Buffer.from(image.data, 'base64'))
    await evaluate("[...document.querySelectorAll('.area-card')].find(e=>e.textContent.includes('二号施工区')).click()")
    await wait("document.querySelectorAll('.route-point').length===0 && document.querySelector('.area-waylines')?.textContent.includes('暂无航线')")
    await evaluate("[...document.querySelectorAll('.area-card')].find(e=>e.textContent.includes('一号施工区')).click()")
    await wait("!!document.querySelector('.route-select')")
    await button('二维新建')
    await wait("document.querySelector('.construction-editor-banner')?.textContent.includes('一号施工区') && !!document.querySelector('#g-container .amap-maps')")
    await evaluate("(() => { const app=document.querySelector('#demo-app').__vue_app__; const p=app._context.provides; window.__routeStore=Reflect.ownKeys(p).map(k=>p[k]).find(v=>v?.state?.deviceState&&v.commit); window.__routeMap=app.config.globalProperties.$map })()")
    await wait('window.__routeStore.state.Points[0].elements.length===0')
    await button('自动生成S型航线')
    await wait("!!document.querySelector('.sweep-form')")
    await button('计算航线')
    await wait("!!document.querySelector('.sweep-summary')")
    await button('应用到编辑器')
    await wait('window.__routeStore.state.Points[0].elements.length>2')
    const generated = await evaluate('JSON.parse(JSON.stringify(window.__routeStore.state.Points[0]))')
    const count = generated.elements.length
    assert.ok(count < 2000)
    assert.equal(generated.folder.globalHeight, 100)
    assert.equal(generated.folder.globalWaypointHeadingParam.waypointHeadingMode, 'followWayline')
    for (const p of generated.elements) {
      assert.equal(p.Placemark.gimbalPitchAngle, -90)
      assert.equal(p.Placemark.actionGroup.actionList[0].actionActuatorFunc, 'gimbalRotate')
      assert.equal(p.Placemark.actionGroup.actionList[0].actionActuatorFuncParam.gimbalRotate.gimbalPitchRotateAngle, -90)
    }
    assert.equal(JSON.parse(await evaluate("localStorage.getItem('construction_wayline_draft_area-browser')")).elements.length, count)
    await button('自动生成S型航线'); await button('计算航线'); await button('应用到编辑器')
    await wait("!!document.querySelector('.el-message-box')")
    await evaluate("[...document.querySelectorAll('.el-message-box button')].find(b=>b.textContent.includes('取消')).click()")
    assert.equal(await evaluate('window.__routeStore.state.Points[0].elements[0].id'), generated.elements[0].id)
    await evaluate("[...document.querySelectorAll('.el-dialog__footer button')].find(b=>b.textContent.includes('取消')).click()")
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-sweep-preview.png'), Buffer.from(shot.data, 'base64'))
    await button('提交')
    await wait("location.pathname.includes('constructionAreas')")
    assert.equal(saved.length, 1)
    assert.equal(saved[0].wayline.folder.placeMarks.length, count)
    for (const p of saved[0].wayline.folder.placeMarks) {
      assert.equal(p.actionGroup.actionTrigger.actionTriggerType, 'reachPoint')
      const photo = p.actionGroup.actionList.find(a => a.actionActuatorFunc === 'takePhoto')
      if (photo) { assert.equal(photo.actionActuatorFuncParam.takePhoto.fileSuffix, '航点'); assert.equal(photo.actionActuatorFuncParam.takePhoto.payloadLensIndex, 'wide') }
    }
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-sweep-wayline.json'), JSON.stringify(saved[0]))
    assert.equal(await evaluate("localStorage.getItem('wayline')"), 'generic-draft-preserved')
    assert.deepEqual(errors, [])
    console.log('Sweep browser passed: editor entry, generation, settings, replacement cancellation, draft, submit payload and original draft isolation.')
  } catch (error) {
    const screenshot = await send('Page.captureScreenshot', { format: 'png' })
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-editor-check.png'), Buffer.from(screenshot.data, 'base64'))
    console.error(await evaluate('({url:location.href,text:document.body.innerText.slice(-1000)})'), errors, [...pending.values()].filter(url => !url.endsWith('.png')))
    throw error
  } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
