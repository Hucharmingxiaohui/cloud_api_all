// Requires an isolated Edge/Chromium CDP profile and the local Vite server.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const WebSocket = require('ws')
async function main () {
  const cdp = process.env.CONSTRUCTION_CDP_URL || 'http://127.0.0.1:9229'
  const base = process.env.CONSTRUCTION_PREVIEW_URL || 'http://127.0.0.1:8097'
  const target = await (await fetch(`${cdp}/json/new?about:blank`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise(resolve => ws.once('open', resolve))
  let id = 0; const calls = new Map(); const errors = []
  function send (method, params = {}) {
    return new Promise((resolve, reject) => {
      const key = ++id
      const timer = setTimeout(() => { calls.delete(key); reject(Error('CDP timeout: ' + method)) }, 15000)
      calls.set(key, { resolve: value => { clearTimeout(timer); resolve(value) }, reject: error => { clearTimeout(timer); reject(error) } })
      ws.send(JSON.stringify({ id: key, method, params }))
    })
  }
  ws.on('message', raw => {
    const event = JSON.parse(raw)
    if (event.id) { const pending = calls.get(event.id); if (pending) { calls.delete(event.id); event.error ? pending.reject(event.error) : pending.resolve(event.result) } }
    if (event.method === 'Runtime.exceptionThrown') errors.push(event.params.exceptionDetails.exception?.description || event.params.exceptionDetails.text)
    if (event.method === 'Fetch.requestPaused') {
      const url = event.params.request.url
      const data = url.includes('/construction/areas') ? [] : url.includes('/parking-points') ? [{ id: 'parking-layout', name: '布局检查驻车点', lng: 120.42, lat: 36.08, radius_m: 2000, version: 1 }] : url.includes('/coverage') ? { dock_sn: 'dock-layout-check', radius_m: 2000 } : { list: [{ device_sn: 'dock-layout-check', nickname: '现场机巢 01' }] }
      send('Fetch.fulfillRequest', { requestId: event.params.requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code: 0, data })).toString('base64') }).catch(console.error)
    }
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let n = 0; n < 100; n++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 300)) } throw Error('Timeout: ' + expression) }
  try {
    await send('Page.enable'); await send('Runtime.enable')
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','construction-layout-check')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/construction/*' }, { urlPattern: '*devices*', resourceType: 'XHR' }] })
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: base + '/variableMgt/constructionAreas' })
    await wait("!!document.querySelector('.map-container .amap-maps')")
    const size = await evaluate("({map:document.querySelector('.map-container').getBoundingClientRect().height, page:innerHeight, bottom:document.querySelector('.area-layout').getBoundingClientRect().bottom})")
    assert.ok(size.map > size.page * 0.65, JSON.stringify(size))
    assert.ok(size.bottom <= size.page, 'layout fits available height')
    await evaluate('(() => { const app=document.querySelector(\'#demo-app\').__vue_app__; const p=app._context.provides; const store=Reflect.ownKeys(p).map(k=>p[k]).find(v=>v?.state?.deviceState && v.commit); if(!store) throw Error(\'store not found\'); window.__layoutStore=store; store.commit(\'SET_DOCK_INFO\',{sn:\'dock-layout-check\',host:{mode_code:0,longitude:120.42,latitude:36.08}}) })()')
    await wait("!!document.querySelector('.construction-dock-marker')")
    await wait("document.querySelector('.dock-label')?.textContent === '现场机巢 01'")
    await evaluate("[...document.querySelectorAll('button')].find(b=>b.textContent==='定位机巢').click()")
    for (const [width, height] of [[1920, 1080], [1366, 768], [768, 1024]]) {
      await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false })
      await evaluate("new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))")
      const before = await evaluate("document.querySelector('.map-container').getBoundingClientRect().height")
      await evaluate("document.querySelector('.parameter-switches button').click()")
      await evaluate("(() => { const s=document.querySelector('.dock-actions select'); s.value='dock-layout-check'; s.dispatchEvent(new Event('change', { bubbles: true })) })()")
      await wait("!!document.querySelector('.coverage-toolbar input')")
      assert.ok(Math.abs(before - await evaluate("document.querySelector('.map-container').getBoundingClientRect().height")) <= 2, 'coverage panel must not shrink map')
      await evaluate("document.querySelectorAll('.parameter-switches button')[1].click()")
      await evaluate("document.querySelector('.parking-heading button').click()")
      await wait("!!document.querySelector('.picking-status')")
      assert.ok(Math.abs(before - await evaluate("document.querySelector('.map-container').getBoundingClientRect().height")) <= 2, 'parking editor must not shrink map')
      const mapPoint = await evaluate("(() => { const r=document.querySelector('.map-container').getBoundingClientRect(); return {x:r.left+r.width*.8,y:r.top+r.height*.7} })()")
      await send('Input.dispatchMouseEvent', { type: 'mousePressed', ...mapPoint, button: 'left', clickCount: 1 })
      await send('Input.dispatchMouseEvent', { type: 'mouseReleased', ...mapPoint, button: 'left', clickCount: 1 })
      await wait("!document.querySelector('.picking-status') && document.querySelector('.parking-form')?.textContent.includes('WGS84')")
      await evaluate("document.querySelector('.parking-form input').value = '保留草稿'; document.querySelector('.parking-form input').dispatchEvent(new Event('input', { bubbles: true }))")
      await evaluate("document.querySelector('.parameter-heading button').click(); document.querySelectorAll('.parameter-switches button')[1].click()")
      assert.equal(await evaluate("document.querySelector('.parking-form input').value"), '保留草稿', 'collapse retains parking draft')
      await evaluate("[...document.querySelectorAll('.parking-form button')].find(b=>b.textContent==='取消').click(); document.querySelector('.parameter-heading button').click()")
      assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth + 2'), 'no horizontal overflow')
    }
    await send('Emulation.setDeviceMetricsOverride', { width: 1366, height: 768, deviceScaleFactor: 1, mobile: false })
    await evaluate("document.querySelectorAll('.parameter-switches button')[1].click()")
    await evaluate("(() => { const s=document.querySelector('.parking-heading select'); s.value='parking-layout'; s.dispatchEvent(new Event('change', { bubbles: true })) })()")
    await wait("!!document.querySelector('.parking-details')")
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    const screenshot = path.join(os.tmpdir(), 'construction-area-layout.png')
    fs.writeFileSync(screenshot, Buffer.from(shot.data, 'base64')); console.log(screenshot)
    await evaluate("window.__layoutStore.commit('SET_DOCK_INFO',{sn:'dock-layout-check',host:{mode_code:0,longitude:0,latitude:0}})")
    await wait("!document.querySelector('.construction-dock-marker')")
    await send('Emulation.setDeviceMetricsOverride', { width: 768, height: 1024, deviceScaleFactor: 1, mobile: false })
    assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth + 2'))
    assert.deepEqual(errors, [])
    console.log('Area layout passed: 3 viewport sizes, unchanged map height, coverage and parking panels, real map picking, retained draft, named markers and no horizontal overflow.')
  } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
