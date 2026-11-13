// Capture current UI with isolated, explicitly fictional data. No backend writes.
const fs = require('fs')
const path = require('path')
const WebSocket = require('ws')
const root = path.resolve('md/工地巡检平台操作手册')
async function main () {
  const version = await (await fetch('http://127.0.0.1:9232/json/version')).json()
  const browser = new WebSocket(version.webSocketDebuggerUrl)
  await new Promise(resolve => browser.once('open', resolve))
  const browserCall = (id, method, params = {}) => new Promise((resolve, reject) => {
    const receive = raw => { const e = JSON.parse(raw); if (e.id === id) { browser.off('message', receive); e.error ? reject(e.error) : resolve(e.result) } }
    browser.on('message', receive); browser.send(JSON.stringify({ id, method, params }))
  })
  const context = await browserCall(1, 'Target.createBrowserContext')
  const created = await browserCall(2, 'Target.createTarget', { url: 'about:blank', browserContextId: context.browserContextId })
  const target = (await (await fetch('http://127.0.0.1:9232/json')).json()).find(t => t.id === created.targetId)
  browser.close()
  fs.mkdirSync(root, { recursive: true }); fs.writeFileSync(path.join(root, '.browser.json'), JSON.stringify(target))
  const ws = new WebSocket(target.webSocketDebuggerUrl); await new Promise(resolve => ws.once('open', resolve))
  let id = 0; const pending = new Map()
  const send = (method, params = {}) => new Promise((resolve, reject) => { const n = ++id; pending.set(n, { resolve, reject }); ws.send(JSON.stringify({ id: n, method, params })) })
  const area = (id, name, lng, lat) => ({ id, name, version: 1, enabled: true, archived: false, category: '基础施工', notes: '操作手册示例工作面', area_sqm: 27000, boundary: [{ lng, lat }, { lng: lng + .002, lat }, { lng: lng + .002, lat: lat + .0015 }, { lng, lat: lat + .0015 }] })
  const areas = [area('a1', '01号风机基础工作面', 120.419, 36.079), area('a2', '02号吊装工作面', 120.424, 36.081)]
  const pts = [[120.4193, 36.0793], [120.4205, 36.0793], [120.4205, 36.0802], [120.4193, 36.0802]].map(([lng, lat], i) => ({ lng, lat, index: i + 1, height: 130, photo_suffixes: ['航点' + (i + 1)] }))
  const route = { wayline_id: 'r1', name: '01号工作面双光巡检航线', version: 1, area_version: 1, source: 'upload', status: 'VALID', message: '区域边界校验通过', height_mode: 'WGS84', points: pts, file_sign: 'manual-example' }
  const overall = { ...route, overall_id: 'o1', name: '固定机巢两工作面总体航线', dock_sn: 'dock-fixed', points: [...pts, ...pts.slice(0, 2).map((p, i) => ({ ...p, lng: p.lng + .005, lat: p.lat + .002, index: i + 5, photo_suffixes: ['航点' + (i + 5)] }))], bindings: [{ area_id: 'a1', area_name: areas[0].name, area_version: 1, waypoints: [1, 2, 3, 4] }, { area_id: 'a2', area_name: areas[1].name, area_version: 1, waypoints: [5, 6] }] }
  const plans = [{ plan_id: 'p1', name: '01号工作面日常巡检', area_id: 'a1', file_id: 'r1', dock_sn: 'dock-fixed', task_type: 0, status: 'READY', message: '可下发', archived: 0 }]
  const codes = ['missing_helmet', 'phone_use', 'smoking', 'person_fallen', 'missing_safety_clothing', 'person_count', 'fire_smoke', 'adverse_weather', 'animal_intrusion', 'barrier_crossing', 'person_under_lifting_load', 'missing_hook_latch']
  const names = ['未佩戴安全帽', '违规玩手机', '工作场所吸烟', '人员倒地', '未穿安全服', '人员数量统计', '火灾烟雾', '恶劣天气', '动物入侵', '人员违规跨栏', '吊物下方站人', '防脱钩缺失']
  const policy = { visible_checks: codes, infrared: { enabled: true, max_temperature_c: 60, min_hotspot_pixels: 5 } }
  const photos = Array.from({ length: 6 }, (_, i) => ({ file_id: 'photo-' + i, name: `DJI_20261105_航点${i + 1}_${i === 2 ? 'T' : 'V'}.JPG`, area_id: i < 4 ? 'a1' : 'a2', area_name: areas[i < 4 ? 0 : 1].name, archived: true, type: i === 2 ? 'infrared' : 'visible', waypoint_index: i + 1, checks: i === 2 ? [] : ['person_count', 'missing_helmet'], policy_source: 'area', status: 'SUCCEEDED', image_url: '/example', result: { checks: i === 2 ? [] : [{ code: 'person_count', status: 'success', count: i < 4 ? 3 : 2 }, { code: 'missing_helmet', status: 'success', boxes: i === 0 ? [{ rect: [300, 110, 55, 90], score: .92 }] : [] }] } }))
  photos[2].infrared_temperature_enabled = true
  photos[2].infrared_temperature = { status: 'success', max_temperature_c: 72.5, min_temperature_c: 20, average_temperature_c: 35, threshold_c: 60, min_hotspot_pixels: 5, hotspot_count: 1, abnormal: true, hotspots: [{ rect: [300, 110, 55, 90], pixels: 30, max_temperature_c: 72.5 }], message: '发现温度异常区域（示例）' }
  const batch = { batch_id: 'batch-example-20261105', status: 'SUCCEEDED', finished: 6, total: 6, created_at: 1793859600000, report_ready: true, photos, areas: [{ area_id: 'a1', status: 'success', count: 4 }, { area_id: 'a2', status: 'success', count: 2 }] }
  const detail = { job_id: 'offline-manual-example', plan_type: 6, name: '固定机巢两工作面巡检 · 操作示例', received_count: 6, expected_count: 6, media_complete: true, analysis_configured: true, analysis: batch, photos, overall_route: overall, unresolved_count: 0 }
  const origin = { deployment: 'fixed-example', workspace: 'example-workspace', job_id: 'fixed-job-example' }
  const transfers = { deployment: 'mobile-example', inbox: '/dji/java/data/construction-transfer/example-workspace/inbox', exports: '/dji/java/data/construction-transfer/example-workspace/exports', tasks: [{ job_id: detail.job_id, name: detail.name, dock_sn: '固定机巢01（示例）', execute_time: 1793859600000, imported_at: 1793863200000, photo_count: 6, origin }] }
  ws.on('message', async raw => {
    const e = JSON.parse(raw)
    if (e.id) { const c = pending.get(e.id); if (c) { pending.delete(e.id); e.error ? c.reject(e.error) : c.resolve(e.result) } return }
    if (e.method !== 'Fetch.requestPaused') return
    const { request, requestId } = e.params; const url = new URL(request.url); const p = url.pathname
    const fulfill = (body, type = 'application/json') => send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: type }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }], body: Buffer.from(body).toString('base64') })
    try {
      if (p === '/config.js') { await fulfill('window.CURRENT_CONFIG={baseURL:"http://127.0.0.1:8097/"};', 'application/javascript'); return }
      if (!p.includes('/api/') && !p.includes('/manage/') && !p.includes('/wayline/') && !p.includes('/control/')) { await send('Fetch.continueRequest', { requestId }); return }
      let data = { list: [], pagination: { total: 0, page: 1, page_size: 10 } }
      if (p.includes('/media/') || p.includes('/photos/')) {
        const thermal = p.includes('photo-2'); const annotated = p.endsWith('/result')
        const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="450"><defs><linearGradient id="heat"><stop stop-color="#19386f"/><stop offset=".5" stop-color="#dfaf36"/><stop offset="1" stop-color="#d84324"/></linearGradient></defs><rect width="800" height="450" fill="${thermal ? 'url(#heat)' : '#6e807e'}"/><path d="M0 380L800 260V450H0" fill="#b0a084"/><path d="M100 300V120H500V300M100 200H500M200 120V300M350 120V300" stroke="#d8d3c8" fill="none" stroke-width="14"/><text x="28" y="42" fill="white" font-size="22">操作示例图 · 非现场检测照片</text>${annotated ? '<rect x="300" y="110" width="55" height="90" fill="none" stroke="#ffff66" stroke-width="4"/>' : ''}</svg>`
        await fulfill(svg, 'image/svg+xml'); return
      }
      if (p.endsWith('/analysis-config')) {
        data = { revision: 1, catalog: codes.map((code, i) => ({ code, name: names[i] })), default_policy: policy, area_id: 'a1', area_name: areas[0].name, name: route.name, file_sign: 'example', route_version: 1, area: { override: policy, effective: policy, inherited: policy, inherited_source: 'default', source: 'area' }, points: pts.map(point => ({ ...point, area_id: 'a1', area_name: areas[0].name, override: policy, effective: policy, inherited: policy, inherited_source: 'area', source: 'waypoint' })) }
      } else if (p.endsWith('/coverage')) data = { dock_sn: p.split('/').at(-2), radius_m: 2000 }
      else if (p.includes('parking-points')) data = [{ id: 'park1', name: '北侧道路驻车点（示例）', lng: 120.421, lat: 36.0825, radius_m: 2000, version: 1 }]
      else if (p.includes('devices')) data = { list: [{ device_sn: 'dock-fixed', nickname: '固定机巢01（示例）' }, { device_sn: 'dock-mobile', nickname: '车载机巢02（示例）' }], pagination: { total: 2 } }
      else if (p.endsWith('/transfers') || p.endsWith('/transfers/scan')) data = transfers
      else if (p.endsWith('/export')) data = { name: 'inspection-example.zip', path: transfers.exports + '/inspection-example.zip' }
      else if (p.endsWith('/analysis/check')) data = { connected: true, remote: 'ready', thermal: 'ready', message: '任务所需分析能力已就绪（操作示例）' }
      else if (p.includes('/tasks/')) data = p.endsWith('/batches') ? [batch] : p.includes('/batches/') ? batch : detail
      else if (p.endsWith('/areas')) data = areas
      else if (/\/areas\/a\d$/.test(p)) data = areas.find(a => p.endsWith(a.id))
      else if (p.endsWith('/overall-routes')) data = [overall]
      else if (p.endsWith('/overall-routes/inspect')) data = overall
      else if (/\/overall-routes\/o1$/.test(p)) data = overall
      else if (p.endsWith('/plans')) data = p.includes('overall') ? [{ ...plans[0], name: '两工作面联合巡检计划', overall_id: 'o1', overall_route: overall, bindings: overall.bindings }] : plans
      else if (p.endsWith('/waylines')) data = [route]
      else if (p.endsWith('/waylines/r1')) data = route
      await fulfill(JSON.stringify({ code: 0, data }))
    } catch (err) { console.error(err) }
  })
  await send('Page.enable'); await send('Runtime.enable')
  await send('Fetch.enable', { patterns: [{ urlPattern: '*/config.js' }, { urlPattern: '*', resourceType: 'XHR' }, { urlPattern: '*', resourceType: 'Fetch' }] })
  // sessionStorage token not needed: this is a documentation-only UI, all API calls intercepted.
  await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','manual-example');" })
  await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1000, deviceScaleFactor: 1, mobile: false })
  await send('Page.navigate', { url: 'http://127.0.0.1:8097' + (process.argv[2] || '/variableMgt/constructionAreas') })
  console.log('Documentation preview ready. All application API calls use example responses.')
  setTimeout(() => ws.close(), 3600000)
}
main().catch(e => { console.error(e); process.exitCode = 1 })
