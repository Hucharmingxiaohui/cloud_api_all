const fs = require('fs')
const path = require('path')
const WebSocket = require('ws')
const root = path.resolve('md/工地巡检平台操作手册')
async function main () {
  fs.mkdirSync(path.join(root, 'images'), { recursive: true })
  const cdp = 'http://127.0.0.1:9232'
  let target
  if (process.argv[2] === 'open') {
    target = await (await fetch(cdp + '/json/new?about:blank', { method: 'PUT' })).json()
    fs.writeFileSync(path.join(root, '.browser.json'), JSON.stringify(target))
  } else target = JSON.parse(fs.readFileSync(path.join(root, '.browser.json')))
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise(resolve => ws.once('open', resolve))
  let id = 0
  const calls = new Map()
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const key = ++id
    const timer = setTimeout(() => reject(Error(method + ' timeout')), 20000)
    calls.set(key, r => { clearTimeout(timer); r.error ? reject(Error(JSON.stringify(r.error))) : resolve(r.result) })
    ws.send(JSON.stringify({ id: key, method, params }))
  })
  ws.on('message', raw => { const r = JSON.parse(raw); if (r.id && calls.has(r.id)) { calls.get(r.id)(r); calls.delete(r.id) } })
  try {
    if (process.argv[2] === 'dialog') { console.log(await send('Page.handleJavaScriptDialog', { accept: true })); return }
    await send('Page.enable'); await send('Runtime.enable')
    if (process.argv[2] === 'open') {
      await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1000, deviceScaleFactor: 1, mobile: false })
      await send('Page.navigate', { url: 'http://127.0.0.1:8097/variableMgt/constructionAreas' })
      await new Promise(resolve => setTimeout(resolve, 5000))
    }
    if (process.argv[2] === 'eval') {
      const r = await send('Runtime.evaluate', { expression: process.argv[3], returnByValue: true, awaitPromise: true }); console.log(JSON.stringify(r))
    } else if (process.argv[2] === 'shot') {
      await new Promise(resolve => setTimeout(resolve, 1200))
      const result = await send('Page.captureScreenshot', { format: 'png' })
      const file = path.join(root, 'images', process.argv[3] + '.png'); fs.writeFileSync(file, Buffer.from(result.data, 'base64')); console.log(file)
    } else console.log((await send('Runtime.evaluate', { expression: 'document.body.innerText', returnByValue: true })).result.value)
  } finally { ws.close() }
}
main().catch(e => { console.error(e); process.exitCode = 1 })
