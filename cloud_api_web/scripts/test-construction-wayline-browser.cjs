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
  const area = { id: 'area-browser', name: '一号施工区', version: 1, enabled: true, archived: false, category: '基础施工', notes: '', boundary: [{ lng: 120, lat: 36 }, { lng: 120.01, lat: 36 }, { lng: 120.01, lat: 36.01 }, { lng: 120, lat: 36.01 }] }
  const points = [{ lng: 120.002, lat: 36.002, height: 90 }, { lng: 120.008, lat: 36.008, height: 90 }]
  const routes = [{ wayline_id: 'shared-route', name: '已关联航线', status: 'VALID', message: '区域边界校验通过', source: 'upload', area_version: 1, points, height_mode: 'relativeToStartPoint' }]
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
    await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable')
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
    await evaluate("document.querySelector('.g-action-item').click()")
    async function clickPoint (lng, lat) {
      const point = await evaluate(`(async()=>{const c=await import('/src/vendors/coordtransform.js');const xy=c.wgs84togcj02(${lng},${lat});const p=window.__routeMap.lngLatToContainer(new AMap.LngLat(xy[0],xy[1]));const r=document.querySelector('#g-container').getBoundingClientRect();return {x:r.left+p.x,y:r.top+p.y}})()`)
      await send('Input.dispatchMouseEvent', { type: 'mouseMoved', ...point })
      await send('Input.dispatchMouseEvent', { type: 'mousePressed', button: 'left', clickCount: 1, ...point })
      await send('Input.dispatchMouseEvent', { type: 'mouseReleased', button: 'left', clickCount: 1, ...point })
    }
    await clickPoint(120.002, 36.002)
    await wait('window.__routeStore.state.Points[0].elements.length===1')
    await new Promise(resolve => setTimeout(resolve, 400))
    await evaluate("document.querySelector('.g-action-item').click()")
    await clickPoint(120.008, 36.008)
    await wait('window.__routeStore.state.Points[0].elements.length===2')
    // Drag the actual marker beyond the polygon and verify rollback.
    await evaluate("document.querySelectorAll('.g-action-item')[1].click()")
    const drag = await evaluate("(async()=>{const c=await import('/src/vendors/coordtransform.js');const e=window.__routeStore.state.Points[0].elements[0];const m=window.__routeStore.state.coverMap[e.id][0];const r=document.querySelector('#g-container').getBoundingClientRect();const a=window.__routeMap.lngLatToContainer(m.getPosition());const b=window.__routeMap.lngLatToContainer(new AMap.LngLat(...c.wgs84togcj02(120.0104,36.002)));return {a:{x:r.left+a.x,y:r.top+a.y-8},b:{x:r.left+b.x,y:r.top+b.y}}})()")
    await send('Input.dispatchMouseEvent', { type: 'mouseMoved', ...drag.a })
    await send('Input.dispatchMouseEvent', { type: 'mousePressed', button: 'left', buttons: 1, clickCount: 1, ...drag.a })
    for (let step = 1; step <= 5; step++) await send('Input.dispatchMouseEvent', { type: 'mouseMoved', button: 'left', buttons: 1, x: drag.a.x + (drag.b.x - drag.a.x) * step / 5, y: drag.a.y + (drag.b.y - drag.a.y) * step / 5 })
    await send('Input.dispatchMouseEvent', { type: 'mouseReleased', button: 'left', clickCount: 1, ...drag.b })
    assert.ok(await evaluate('window.__routeStore.state.Points[0].elements[0].resource.content.geometry.coordinates[0]<120.01'))
    await button('提交')
    await wait("location.pathname.includes('constructionAreas') && document.querySelectorAll('.route-point').length===2")
    assert.equal(saved.length, 1)
    assert.equal(saved[0].expected_version, 1)
    assert.equal(saved[0].wayline.folder.placeMarks.length, 2)
    assert.equal(await evaluate("localStorage.getItem('wayline')"), 'generic-draft-preserved')
    const root = await send('DOM.getDocument')
    const input = await send('DOM.querySelector', { nodeId: root.root.nodeId, selector: 'input[type=file]' })
    await send('DOM.setFileInputFiles', { nodeId: input.nodeId, files: [path.join(os.tmpdir(), 'construction-wayline-fixture.kmz')] })
    await wait("document.querySelectorAll('.route-card').length===3")
    await button('关联已有航线')
    await wait("document.querySelector('.el-dialog')?.textContent.includes('航线库原文件')")
    await button('校验并关联')
    await wait("document.querySelectorAll('.route-card').length===4 && document.querySelectorAll('.route-point').length===2")
    assert.equal(routes.filter(r => r.wayline_id === 'library-route').length, 1)
    assert.equal(saved.length, 1)
    assert.deepEqual(errors, [])
    console.log('Browser passed: region selection, preview, constrained editor, save+return, isolated draft, KMZ upload, existing library route binding and map preview.')
  } catch (error) {
    const screenshot = await send('Page.captureScreenshot', { format: 'png' })
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-editor-check.png'), Buffer.from(screenshot.data, 'base64'))
    console.error(await evaluate('({url:location.href,text:document.body.innerText.slice(-1000)})'), errors, [...pending.values()].filter(url => !url.endsWith('.png')))
    throw error
  } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
