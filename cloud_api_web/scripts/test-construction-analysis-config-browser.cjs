const assert = require('node:assert/strict')
const WebSocket = require('ws')
async function main () {
  const cdp = process.env.CONSTRUCTION_CDP_URL || 'http://127.0.0.1:9231'
  const base = process.env.CONSTRUCTION_PREVIEW_URL || 'http://127.0.0.1:8098'
  const target = await (await fetch(`${cdp}/json/new?about:blank`, { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl); await new Promise(resolve => ws.once('open', resolve))
  let sequence = 0; const pending = new Map(); const errors = []; const writes = []
  const codes = ['missing_helmet', 'phone_use', 'smoking', 'person_fallen', 'missing_safety_clothing', 'person_count', 'fire_smoke', 'adverse_weather', 'animal_intrusion', 'barrier_crossing', 'person_under_lifting_load', 'missing_hook_latch']
  const names = ['未佩戴安全帽', '违规玩手机', '工作场所吸烟', '人员倒地', '未穿安全服', '人员数量统计', '火灾烟雾', '恶劣天气', '动物入侵', '人员违规跨栏', '吊物下方站人', '防脱钩缺失']
  let defaultPolicy = { visible_checks: codes, infrared: { enabled: false, max_temperature_c: null, min_hotspot_pixels: 1 } }; let areaPolicy = null; const overrides = {}; let revision = 0
  let parking = []; let parkingId = 0
  const area = { id: 'a', name: '配置测试工作面', version: 1, enabled: true, archived: false, category: '施工', boundary: [{ lng: 120, lat: 36 }, { lng: 120.001, lat: 36 }, { lng: 120.001, lat: 36.001 }], notes: '' }
  const route = { wayline_id: 'file', name: '测试区域航线', status: 'VALID', source: 'existing', message: '可执行', area_version: 1, height_mode: 'WGS84', points: area.boundary }
  function view (url) {
    const result = { revision, catalog: codes.map((code, i) => ({ code, name: names[i] })), default_policy: defaultPolicy }
    if (url.searchParams.has('area_id')) Object.assign(result, { area_id: 'a', area_name: area.name, area: { override: areaPolicy, effective: areaPolicy || defaultPolicy, inherited: defaultPolicy, inherited_source: 'default', source: areaPolicy ? 'area' : 'default' } })
    if (url.searchParams.has('wayline_id')) Object.assign(result, { file_sign: 'signature', route_version: 1, name: route.name, points: [1, 2].map(index => ({ index, area_id: 'a', area_name: area.name, photo_suffixes: ['航点' + index], override: overrides[index] || null, effective: overrides[index] || areaPolicy || defaultPolicy, inherited: areaPolicy || defaultPolicy, inherited_source: areaPolicy ? 'area' : 'default', source: overrides[index] ? 'waypoint' : areaPolicy ? 'area' : 'default' })) })
    return result
  }
  function send (method, params = {}) { return new Promise((resolve, reject) => { const id = ++sequence; const timer = setTimeout(() => reject(Error(method + ' timed out')), 20000); pending.set(id, { resolve: v => { clearTimeout(timer); resolve(v) }, reject }); ws.send(JSON.stringify({ id, method, params })) }) }
  ws.on('message', raw => {
    const e = JSON.parse(raw); if (e.id) { const call = pending.get(e.id); if (call) { pending.delete(e.id); e.error ? call.reject(e.error) : call.resolve(e.result) } return }
    if (e.method === 'Network.loadingFailed' && e.params.type !== 'Image') errors.push(e.params.errorText + ' ' + e.params.type)
    if (e.method === 'Runtime.exceptionThrown') errors.push(e.params.exceptionDetails.exception?.description || e.params.exceptionDetails.text)
    if (e.method !== 'Fetch.requestPaused') return
    const { request, requestId } = e.params; const url = new URL(request.url); const p = url.pathname; let data = {}; let code = 0
    if (request.method !== 'OPTIONS') {
      if (p.endsWith('/analysis-config')) {
        if (request.method === 'PUT') {
          const body = JSON.parse(request.postData); assert.equal(body.expected_revision, revision); writes.push(body)
          if (body.target === 'default') defaultPolicy = body.policy
          else if (body.target === 'area') areaPolicy = body.policy
          else { assert.equal(body.file_sign, 'signature'); overrides[body.waypoint_index] = body.policy }
          revision++
        }
        data = view(url)
      } else if (p.includes('/parking-points')) {
        if (request.method === 'POST') { const row = { ...JSON.parse(request.postData), id: 'p' + ++parkingId, version: 1 }; parking.push(row); data = row } else if (request.method === 'PUT') { const body = JSON.parse(request.postData); const row = parking.find(p => p.id === url.pathname.split('/').pop()); assert.equal(body.version, row.version); Object.assign(row, body, { version: row.version + 1 }); data = row } else if (request.method === 'DELETE') { parking = parking.filter(p => p.id !== url.pathname.split('/').pop()); data = null } else data = parking
      } else if (p.includes('/devices')) data = { list: [{ device_sn: 'dock-test', nickname: '车载机巢' }] }
      else if (p.endsWith('/areas')) data = [area]
      else if (p.endsWith('/areas/a')) data = area
      else if (p.endsWith('/areas/a/waylines')) data = [route]
      else if (p.endsWith('/areas/a/waylines/file')) data = route
      else if (p.includes('/api/construction/')) { code = 404; errors.push('unexpected construction operation: ' + p) } else data = { list: [], pagination: { total: 0, page: 1 } }
    }
    send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }], body: Buffer.from(JSON.stringify({ code, data })).toString('base64') }).catch(console.error)
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let i = 0; i < 400; i++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 120)) } throw Error('Timeout: ' + expression) }
  async function button (name) { const q = `[...document.querySelectorAll('button')].find(e=>e.textContent.replace(/\\s/g,'')===${JSON.stringify(name)}&&!e.disabled&&e.getClientRects().length)`; await wait(`!!(${q})`); await evaluate(`${q}.click()`) }
  async function radio (name) { await evaluate(`[...document.querySelectorAll('.analysis-config .el-radio-button')].find(e=>e.textContent===${JSON.stringify(name)}).click()`) }
  async function close () { await button('关闭'); await wait("!document.querySelector('.analysis-config')") }
  async function number (index, value) { await evaluate(`(()=>{const input=document.querySelectorAll('.thermal-fields input')[${index}];input.value='${value}';input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));input.blur()})()`) }
  try {
    await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable'); await send('Network.setCacheDisabled', { cacheDisabled: true }); await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','w')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/construction/*' }, { urlPattern: '*devices*', resourceType: 'XHR' }] })
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: base + '/variableMgt/constructionAreas' })
    await button('默认检测配置'); await wait("document.querySelectorAll('.checks .el-checkbox').length===12")
    await evaluate("document.querySelector('.config-editor .el-switch').click()")
    await wait("!!document.querySelector('.thermal-fields')")
    assert.equal(await evaluate("[...document.querySelectorAll('button')].find(b=>b.textContent==='保存检测配置').disabled"), true, 'infrared requires explicit threshold')
    await number(0, 80); await button('保存检测配置'); await wait("document.querySelector('.saved-message')?.textContent.includes('已保存')")
    assert.equal(defaultPolicy.infrared.max_temperature_c, 80); assert.equal(defaultPolicy.visible_checks.length, 12); await close()
    await wait("!!document.querySelector('.area-card')"); await evaluate("document.querySelector('.area-card').click()")
    await button('工作面检测配置'); await wait("!!document.querySelector('.config-editor .el-radio-group')")
    assert.equal(await evaluate("document.querySelector('.checks input').disabled"), true, 'inherited settings read only')
    await radio('独立配置'); await button('全不选'); await evaluate("document.querySelector('.config-editor .el-switch').click()")
    await button('保存检测配置'); await wait("document.querySelector('.saved-message')?.textContent.includes('已保存')")
    assert.deepEqual(areaPolicy.visible_checks, []); assert.equal(areaPolicy.infrared.enabled, false); await close()
    await wait("!!document.querySelector('.route-select')"); await evaluate("document.querySelector('.area-waylines .route-card p').click()")
    await wait("document.querySelectorAll('.map-container .route-point').length===3")
    assert.equal(await evaluate("document.querySelector('.wayline-preview-bar').textContent.includes('测试区域航线')"), true)
    await button('定位航线')
    await button('航点检测配置'); await wait("document.querySelectorAll('.point-list button').length===2")
    assert.equal(await evaluate("document.querySelector('.config-source').textContent.includes('工作面配置')"), true)
    await radio('独立配置'); await evaluate("[...document.querySelectorAll('.checks .el-checkbox')].find(e=>e.textContent==='火灾烟雾').click();document.querySelector('.config-editor .el-switch').click()")
    await number(0, 60); await button('保存检测配置'); await wait("document.querySelector('.saved-message')?.textContent.includes('已保存')")
    assert.deepEqual(overrides[1].visible_checks, ['fire_smoke']); assert.equal(overrides[1].infrared.max_temperature_c, 60)
    await evaluate("document.querySelectorAll('.point-list button')[1].click()")
    await wait("document.querySelector('.config-editor h3').textContent.includes('航点 2')")
    assert.equal(await evaluate("document.querySelector('.config-source').textContent.includes('可见光 0 项 · 红外关闭')"), true)
    await evaluate("document.querySelectorAll('.point-list button')[0].click()")
    await radio('继承工作面 / 默认配置')
    assert.equal(await evaluate("document.querySelector('.config-source').textContent.includes('可见光 0 项 · 红外关闭')"), true, 'restore inheritance previews parent not old override')
    await button('保存检测配置'); await wait("document.querySelector('.saved-message')?.textContent.includes('已保存')"); assert.equal(overrides[1], null); await close()
    assert.deepEqual(writes.map(w => w.target), ['default', 'area', 'waypoint', 'waypoint']); assert.deepEqual(errors, [])
    await button('＋标定驻车点')
    await evaluate("(()=>{const e=document.querySelector('.parking-form input');e.value='北侧驻车点';e.dispatchEvent(new Event('input',{bubbles:true}))})()")
    const pos = await evaluate("(()=>{const r=document.querySelector('.map-container').getBoundingClientRect();return {x:r.x+r.width*.6,y:r.y+r.height*.6}})()")
    await send('Input.dispatchMouseEvent', { type: 'mouseMoved', ...pos }); await send('Input.dispatchMouseEvent', { type: 'mousePressed', button: 'left', clickCount: 1, ...pos }); await send('Input.dispatchMouseEvent', { type: 'mouseReleased', button: 'left', clickCount: 1, ...pos })
    await button('保存驻车点'); await wait("!!document.querySelector('.parking-details') && !!document.querySelector('.parking-marker.selected')")
    assert.equal(parking.length, 1); assert.ok(Number.isFinite(parking[0].lng)); assert.equal(parking[0].radius_m, 2000)
    await evaluate("(()=>{const p=document.querySelector('#demo-app').__vue_app__._context.provides;const s=Reflect.ownKeys(p).map(k=>p[k]).find(v=>v?.state?.deviceState&&v.commit);s.commit('SET_DOCK_INFO',{sn:'dock-test',host:{mode_code:0,longitude:120,latitude:36}})})()")
    await wait("!!document.querySelector('.parking-distance-label')")
    assert.equal(await evaluate("document.querySelector('.parking-distance-label').textContent.includes('直线距离')"), true)
    assert.equal(await evaluate("document.querySelector('.parking-areas').textContent.includes('完全覆盖')"), true)
    await evaluate("document.querySelector('.parking-heading select').value='';document.querySelector('.parking-heading select').dispatchEvent(new Event('change',{bubbles:true}))")
    await wait("!document.querySelector('.parking-marker.selected')")
    await new Promise(resolve => setTimeout(resolve, 500)) // Wait for the map resize after the details row closes.
    const pin = await evaluate("(()=>{const r=document.querySelector('.parking-pin').getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()")
    await send('Input.dispatchMouseEvent', { type: 'mouseMoved', ...pin }); await send('Input.dispatchMouseEvent', { type: 'mousePressed', button: 'left', clickCount: 1, ...pin }); await send('Input.dispatchMouseEvent', { type: 'mouseReleased', button: 'left', clickCount: 1, ...pin })
    await wait("!!document.querySelector('.parking-marker.selected')")
    await evaluate("[...document.querySelectorAll('.parking-details button')].find(b=>b.textContent==='编辑').click()")
    await button('5km'); await button('保存驻车点'); await wait("document.querySelector('.parking-details')?.textContent.includes('5000m')")
    assert.equal(parking[0].version, 2)
    await evaluate("[...document.querySelectorAll('.parking-heading button')].find(b=>b.textContent==='刷新').click()")
    await wait("!!document.querySelector('.parking-marker.selected')")
    await button('删除')
    await button('确定'); await wait("!document.querySelector('.parking-marker')"); assert.equal(parking.length, 0)
    assert.deepEqual(errors, [])
    console.log('Parking browser passed: map placement, persistence/refresh, coverage, dock distance, selection pulse, editing and delete.')
    console.log('Configuration browser passed: default/workface/waypoint entries, inheritance, explicit empty, infrared threshold required, independent visible/IR and restore inheritance. No analysis calls.')
  } catch (e) { const shot = await send('Page.captureScreenshot', { format: 'png' }); require('node:fs').writeFileSync(require('node:path').join(require('node:os').tmpdir(), 'construction-parking-review.png'), Buffer.from(shot.data, 'base64')); console.error(await evaluate('document.body.innerText.slice(-2500)'), errors); throw e } finally { ws.close(); await fetch(`${cdp}/json/close/${target.id}`) }
}
main().catch(e => { console.error(e); process.exitCode = 1 })
