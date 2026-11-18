const assert = require('node:assert/strict'); const fs = require('node:fs'); const WebSocket = require('ws')
async function main () {
  fs.mkdirSync('.construction-backend/terrain-fix', { recursive: true })
  fs.writeFileSync('.construction-backend/terrain-fix/probe.html', "<html><head><meta charset=\"UTF-8\"><script>window.CURRENT_CONFIG={terrainElevation:{enabled:true,url:'/terrain/shilin-ellipsoid.tif'}};</script></head><body style=\"margin:0;background:#051831\"><div id=\"probe\"></div><script type=\"module\">\nimport { createApp, ref, h } from 'vue'\nimport Profile from '/src/components/construction/ConstructionElevationProfile.vue'\nconst mode=ref('WGS84');const points=ref([{lng:103.54643,lat:24.75738,height:2129.7882},{lng:103.55643,lat:24.76738,height:2129.7882}]);\nwindow.probe={mode,points};\ncreateApp({components:{Profile},setup(){return()=>h(Profile,{mode:mode.value,points:points.value})},unused:'<Profile :points=\"points\" :mode=\"mode\" />'}).mount('#probe');\n</script></body></html>\n")
  const target = await (await fetch('http://127.0.0.1:9231/json/new?about:blank', { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl); await new Promise(resolve => ws.once('open', resolve)); let id = 0; const calls = new Map()
  const send = (method, params = {}) => new Promise((resolve, reject) => { const key = ++id; calls.set(key, { resolve, reject }); ws.send(JSON.stringify({ id: key, method, params })) })
  ws.on('message', raw => { const e = JSON.parse(raw); if (e.method === 'Runtime.exceptionThrown')console.log(JSON.stringify(e.params)); if (e.method === 'Runtime.consoleAPICalled' && e.params.type === 'error')console.log(JSON.stringify(e.params.args)); if (e.id) { const p = calls.get(e.id); calls.delete(e.id); if (e.error)p.reject(e.error); else p.resolve(e.result) } })
  const evaluate = async expression => { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(r.exceptionDetails.exception?.description); return r.result.value }
  const wait = async expression => { for (let n = 0; n < 100; n++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 250)) } throw Error('Timed out: ' + expression) }
  try {
    await send('Runtime.enable'); await send('Page.enable'); await send('Page.navigate', { url: 'http://127.0.0.1:8098/.construction-backend/terrain-fix/probe.html' })
    await wait('!!document.querySelector(".elevation-review button")')
    await evaluate('document.querySelector(".elevation-review button").click()')
    await wait('!!document.querySelector("svg path")')
    let result = await evaluate('({text:document.body.innerText,paths:[...document.querySelectorAll("svg path")].map(p=>p.getAttribute("d"))})')
    assert.ok(result.paths.every(p => p.startsWith('M'))); assert.ok(result.text.includes('2129.8')); assert.ok(!result.text.includes('高程不可用'))
    await evaluate('window.probe.mode.value="relativeToStartPoint"')
    await wait('document.body.innerText.includes("原航线起飞点")')
    result = await evaluate('document.body.innerText'); assert.ok(result.includes('航线 未知')); assert.ok(result.includes('无法完成全部复核'))
    await evaluate('const input=document.querySelector("input[type=number]");input.value="2029.8";input.dispatchEvent(new Event("input",{bubbles:true}))')
    await wait('!document.body.innerText.includes("航线 未知")')
    await evaluate('window.probe.points.value=[{lng:120,lat:36,height:100},{lng:120.01,lat:36.01,height:100}]')
    await wait('document.body.innerText.includes("地表 未知")')
    assert.ok(await evaluate('document.body.innerText.includes("无法完成全部复核")'))
    await evaluate('window.probe.mode.value="WGS84";window.probe.points.value=[{lng:103.54643,lat:24.75738,height:2129.7882},{lng:103.55643,lat:24.76738,height:2129.7882}]')
    await wait('document.body.innerText.includes("2129.8")')
    const screenshot = await send('Page.captureScreenshot', { format: 'png' }); fs.writeFileSync('.construction-backend/terrain-fix/profile.png', Buffer.from(screenshot.data, 'base64'))
    console.log('Browser profile passed: real TIFF fetch/decode/render, >1500 height, missing relative base, manual base, outside coverage.')
  } finally { await send('Page.close'); ws.close() }
}
main().catch(e => { console.error(e); process.exit(1) })
