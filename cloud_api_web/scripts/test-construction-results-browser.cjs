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
  let id = 0; const calls = new Map(); const errors = []; const requests = []
  function send (method, params = {}) { return new Promise((resolve, reject) => { const key = ++id; const timer = setTimeout(() => reject(Error(method + ' timed out')), 20000); calls.set(key, { resolve: result => { clearTimeout(timer); resolve(result) }, reject }); ws.send(JSON.stringify({ id: key, method, params })) }) }
  const photos = Array.from({ length: 10 }, (_, i) => ({ file_id: `photo-${i}`, name: `DJI_20261103_施工检查_航点${i + 1}_V.JPG`, area_id: 'area-1', type: 'visible', waypoint_index: i + 1, checks: ['person_count', 'missing_helmet'], status: i === 1 ? 'PARTIAL' : 'SUCCEEDED', policy_source: 'area', archived: true, image_url: i === 1 ? undefined : '/evidence', result: { checks: [{ code: 'person_count', status: 'success', count: i }, { code: 'missing_helmet', status: i === 1 ? 'unavailable' : 'success', boxes: i === 1 ? undefined : [{ x: 20, y: 20 }], message: i === 1 ? '模型暂不可用' : '请核对图像中的检测目标' }] } }))
  photos[2] = { ...photos[2], type: 'infrared', checks: [], infrared_temperature_enabled: true, infrared_temperature: { status: 'success', max_temperature_c: 72.5, min_temperature_c: 20, average_temperature_c: 35, threshold_c: 60, min_hotspot_pixels: 5, hotspot_count: 1, abnormal: true, hotspots: [{ rect: [10, 20, 5, 6], pixels: 30, max_temperature_c: 72.5 }], message: '发现温度异常区域' }, result: { checks: [] } }
  const batch = { batch_id: 'batch-preview', status: 'SUCCEEDED', finished: 10, total: 10, created_at: Date.now(), report_ready: true, photos, areas: [{ area_id: 'area-1', status: 'success', count: 6 }] }
  const detail = { job_id: 'offline-review', name: '风电场施工巡检 · 分析结果预览', received_count: 10, expected_count: 10, media_complete: true, analysis_configured: true, analysis: batch, photos, overall_route: { overall_id: 'overall-1', bindings: [{ area_id: 'area-1', area_name: '一号风机基础工作面', waypoints: [1, 2, 3] }, { area_id: 'area-2', area_name: '二号施工工作面', waypoints: [4] }] } }
  ws.on('message', raw => {
    const event = JSON.parse(raw)
    if (event.id) { const call = calls.get(event.id); if (call) { calls.delete(event.id); event.error ? call.reject(event.error) : call.resolve(event.result) } }
    if (event.method === 'Network.responseReceived' && event.params.response.status >= 400) errors.push(event.params.response.status + ' ' + event.params.response.url)
    if (event.method === 'Network.loadingFailed') errors.push(event.params.errorText)
    if (event.method === 'Runtime.exceptionThrown') errors.push(event.params.exceptionDetails.exception?.description || event.params.exceptionDetails.text)
    if (event.method !== 'Fetch.requestPaused') return
    const { request, requestId } = event.params; requests.push(request)
    if (new URL(request.url).pathname === '/config.js') { send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/javascript' }], body: Buffer.from('window.CURRENT_CONFIG = { baseURL: ' + JSON.stringify(base + '/') + ' };').toString('base64') }).catch(console.error); return }
    const url = new URL(request.url); const isImage = url.pathname.includes('/photos/') || url.pathname.includes('/media/')
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="450"><rect width="800" height="450" fill="#536976"/><path d="M0 350L800 150V450H0" fill="#a49982"/><path d="M130 320V120H470V320M130 180H470M220 120V320M350 120V320" stroke="#cfcdc1" fill="none" stroke-width="12"/><text x="25" y="40" fill="white" font-size="23">TEST IMAGE</text>${url.pathname.endsWith('/result') ? '<rect x="510" y="210" width="90" height="120" stroke="#f7b959" stroke-width="4" fill="none"/><text x="510" y="200" fill="#fff" font-size="20">Detection</text>' : ''}</svg>`
    const data = url.pathname.endsWith('/analysis/check') ? { connected: false, remote: 'ready', thermal: 'failed', message: '本地SDK未就绪：请检查construction.thermal.enabled、executable及执行权限' } : url.pathname.endsWith('/batches') ? [batch] : url.pathname.includes('/batches/') ? batch : detail
    send('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: isImage ? 'image/svg+xml' : 'application/json' }, { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: '*' }, { name: 'Access-Control-Allow-Methods', value: '*' }, { name: 'Access-Control-Allow-Private-Network', value: 'true' }], body: Buffer.from(isImage ? svg : JSON.stringify({ code: 0, data })).toString('base64') }).catch(console.error)
  })
  async function evaluate (expression) { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  async function wait (expression) { for (let n = 0; n < 250; n++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 150)) } throw Error('Timeout: ' + expression) }
  try {
    await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable'); await send('Network.setCacheDisabled', { cacheDisabled: true })
    await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('workspace_id','test-review')" })
    await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/construction/tasks/*' }, { urlPattern: '*/config.js' }] })
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    await send('Page.navigate', { url: base + '/task/taskResult?job_id=offline-review' })
    await wait("!!document.querySelector('#tab-analysis')")
    await evaluate("document.querySelector('#tab-analysis').click()")
    await wait("document.querySelectorAll('.evidence-image img').length===2")
    assert.ok(await evaluate("(() => { const el=document.querySelector('.ctr-shell'); return el.scrollHeight>el.clientHeight && getComputedStyle(el).overflowY==='auto' && el.getBoundingClientRect().bottom<=innerHeight+1 })()"), 'result page owns a bounded vertical scroll container')
    await evaluate("document.querySelector('.ctr-shell').scrollTop=99999")
    await wait("document.querySelector('.ctr-shell').scrollTop>0 && document.querySelector('.control-disclosure').getBoundingClientRect().bottom<=innerHeight")
    await evaluate("document.querySelector('.ctr-shell').scrollTop=0")
    assert.ok(await evaluate("document.querySelector('.evidence-pair').getBoundingClientRect().bottom <= innerHeight"), 'evidence fits the first desktop viewport without scrolling')
    await send('Emulation.setDeviceMetricsOverride', { width: 1366, height: 768, deviceScaleFactor: 1, mobile: false })
    await wait("document.querySelector('.evidence-pair').getBoundingClientRect().bottom <= innerHeight")
    await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false })
    assert.ok(await evaluate("document.querySelector('.check-grid').textContent.includes('0人')"), 'zero photo count remains zero')
    assert.equal(await evaluate("document.querySelectorAll('.review-list>button').length"), 8, 'paginated photos')
    await evaluate("document.querySelectorAll('.review-list>button')[1].click()")
    await wait("document.querySelector('.check-grid')?.textContent.includes('模型暂不可用')")
    assert.ok(await evaluate("[...document.querySelectorAll('.evidence-empty')].some(e => e.textContent.includes('尚无标注图'))"))
    await evaluate("document.querySelectorAll('.review-list>button')[2].click()")
    await wait("document.querySelector('.thermal-summary')?.textContent.includes('72.5')")
    assert.ok(await evaluate("document.querySelector('.evidence-pair').textContent.includes('本地测温热力图')"), 'local thermal output is labelled separately from algorithm image')
    await evaluate("document.querySelector('.review-tools .el-select').click()")
    await wait("[...document.querySelectorAll('.el-select-dropdown__item')].some(e=>e.textContent==='执行异常 / 不可判断')")
    await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='执行异常 / 不可判断').click()")
    await wait("document.querySelectorAll('.review-list>button').length===1")
    await evaluate("document.querySelector('.review-tools .el-select').click()")
    await evaluate("[...document.querySelectorAll('.el-select-dropdown__item')].find(e=>e.textContent==='全部照片').click()")
    await evaluate("document.querySelector('.review-list>button').click()")
    await wait("document.querySelectorAll('.evidence-image img').length===2")
    fs.writeFileSync(path.join(os.tmpdir(), 'construction-analysis-review.png'), Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'))
    await evaluate("[...document.querySelectorAll('.photo-heading button')].find(b=>b.textContent.includes('放大对比')).click()")
    await wait("[...document.querySelectorAll('.el-dialog')].some(e=>e.textContent.includes('本批次照片证据')&&e.getBoundingClientRect().height>0)")
    await evaluate("[...document.querySelectorAll('.el-dialog')].find(e=>e.textContent.includes('本批次照片证据')).querySelector('.el-dialog__headerbtn').click()")
    await evaluate("[...document.querySelectorAll('.region-choice')].find(e=>e.textContent.includes('二号施工工作面')).click()")
    await wait("document.querySelector('.review')?.textContent.includes('当前区域暂无逐图结果')")
    await evaluate("[...document.querySelectorAll('.region-choice')].find(e=>e.textContent.includes('全部照片')).click()")
    await send('Emulation.setDeviceMetricsOverride', { width: 768, height: 1024, deviceScaleFactor: 1, mobile: false })
    await wait("document.querySelectorAll('.review-list>button').length===8")
    assert.ok(await evaluate("document.querySelector('.review').scrollWidth <= document.querySelector('.review').clientWidth + 1"), 'review fits narrow screen')
    await evaluate("document.querySelector('#tab-report').click()")
    await wait("document.querySelector('.report-panel').textContent.includes('已生成')")
    assert.equal(requests.filter(r => r.method === 'POST').length, 0, 'viewing results does not submit analyses or reports')
    await evaluate("document.querySelector('#tab-analysis').click();document.querySelector('.control-disclosure').open=true")
    await evaluate("[...document.querySelectorAll('.analysis-actions button')].find(b=>b.textContent.includes('检查分析能力')).click()")
    await wait("document.querySelector('.capability-status')?.textContent.includes('本地SDK未就绪')")
    assert.ok(await evaluate("document.querySelector('.capability-status').textContent.includes('可见光分析服务：已就绪') && document.querySelector('.capability-status').textContent.includes('本地红外 SDK：检查未通过')"), 'remote and thermal readiness shown separately')
    assert.deepEqual(errors, [])
    console.log('Result UI passed: evidence comparison, zero count, unavailable model, missing image, pagination, filtering, area switch, report tab, responsive layout; mocked services only.')
  } catch (error) { console.error(errors, await evaluate('document.body.innerText.slice(0,3500)'), requests.map(r => r.url)); throw error } finally { await send('Page.close').catch(() => {}); ws.close() }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
