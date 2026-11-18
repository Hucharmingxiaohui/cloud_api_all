const fs = require('node:fs')
const assert = require('node:assert/strict')
const WebSocket = require('ws')
async function main () {
  const base = process.env.CONVERSION_PREVIEW || 'http://127.0.0.1:8099'
  const probe = '/.construction-backend/vendor-conversion-probe.html'
  assert.ok((await fetch(base)).ok, 'Vite must be running')
  const html = `<!doctype html><html><head><meta charset="utf-8"></head><body><div id="app"></div><script type="module">
import {createApp, h, ref, nextTick} from 'vue';
import ElementPlus from 'element-plus'; import 'element-plus/dist/index.css';
import Dialog from '/src/components/wayline/WaylineConversionDialog.vue';
import {parseVendorRoute,buildVendorKmz,packVendorKmz} from '/src/components/wayline/vendorJsonKmz.ts';
import {loadEgm96} from '/src/utils/egm96.ts';
import {parseKmzBlobToCloudDraft} from '/src/components/cloudRenderer/kmzBrowserParser.ts';
import JSZip from 'jszip';
window.tools={parseVendorRoute,buildVendorKmz,packVendorKmz,loadEgm96,parseKmzBlobToCloudDraft,JSZip,nextTick};
const visible=ref(true);window.visible=visible;window.captured=[];
const original=URL.createObjectURL;URL.createObjectURL=b=>{window.captured.push(b);return original(b)};
createApp({setup:()=>()=>h(Dialog,{modelValue:visible.value,'onUpdate:modelValue':v=>visible.value=v})}).use(ElementPlus).mount('#app');window.ready=true;
</script></body></html>`
  fs.writeFileSync('.construction-backend/vendor-conversion-probe.html', html)
  const target = await (await fetch('http://127.0.0.1:9233/json/new?about:blank', { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { ws.once('open', resolve); ws.once('error', reject) })
  let sequence = 0; const calls = new Map()
  ws.on('message', raw => { const e = JSON.parse(raw); if (e.id) { const p = calls.get(e.id); if (!p) return; calls.delete(e.id); clearTimeout(p.timer); if (e.error) p.reject(Error(JSON.stringify(e.error))); else p.resolve(e.result) } })
  function send (method, params = {}) { return new Promise((resolve, reject) => { const id = ++sequence; const timer = setTimeout(() => { calls.delete(id); reject(Error(`CDP timeout ${method}`)) }, 30000); calls.set(id, { resolve, reject, timer }); ws.send(JSON.stringify({ id, method, params })) }) }
  async function evaluate (expression) { const wrapped = /await /.test(expression) && !expression.startsWith('(async') ? `(async()=>{${expression}})()` : expression; const r = await send('Runtime.evaluate', { expression: wrapped, awaitPromise: true, returnByValue: true }); if (r.exceptionDetails) throw Error(JSON.stringify(r.exceptionDetails)); return r.result.value }
  try {
    await send('Page.enable'); await send('Page.navigate', { url: base + probe })
    const ready = await evaluate(`new Promise((resolve,reject)=>{const until=Date.now()+20000;const timer=setInterval(()=>{if(window.ready){clearInterval(timer);resolve(true)}else if(Date.now()>until){clearInterval(timer);reject(Error('probe startup timeout'))}},100)})`)
    assert.ok(ready)
    const point = { lng: 123.7942356560604, lat: 41.89016229514482, height: 132.36, dh: 132.36, waypointSpeed: 3, aircraftYaw: 10, gimbalPitch: -45, turnMode: 0, yawPitchArray: [] }
    const source = { taskname: '浏览器测试<&', finishedAction: 1, flightPathMode: 0, yawMode: 0, gimbalMode: 2, powerline: { keyPoint: [] }, points: [point, { ...point, lng: 123.79425, yawPitchArray: [{ FocalLengthRatio: 1, cameraType: 41, captureCount: 1, aircraftYaw: 12, gimbalPitch: -30, keyID: 32, keyName: '设备<&' }] }] }
    await evaluate(`window.source=${JSON.stringify(source)};window.chooseFile=async(data,name='route.json')=>{const transfer=new DataTransfer();transfer.items.add(new File([JSON.stringify(data)],name,{type:'application/json'}));const input=document.querySelector('input[type=file]');input.files=transfer.files;input.dispatchEvent(new Event('change',{bubbles:true}));await new Promise(r=>setTimeout(r,200));};window.button=t=>[...document.querySelectorAll('button')].find(b=>b.textContent.trim()===t);await window.chooseFile(window.source)`)
    assert.ok(await evaluate(`document.body.textContent.includes('浏览器测试<&')`))
    assert.ok(await evaluate(`button('转换').disabled && button('下载 KMZ').disabled`))
    // Select via the rendered Element Plus UI rather than mutating component internals.
    for (const [i, label] of [[0, '广角可见光（倍率 1）'], [1, '返航']]) {
      await evaluate(`document.querySelectorAll('.el-select')[${i}].click(); await tools.nextTick()`)
      await evaluate(`const items=[...document.querySelectorAll('.el-select-dropdown__item')];const option=items.find(e=>e.textContent.trim()===${JSON.stringify(label)});if(!option)throw Error('option missing');option.click();await tools.nextTick()`)
    }
    await evaluate(`document.querySelector('.el-checkbox input').click();await tools.nextTick();button('转换').click()`)
    await evaluate(`new Promise((resolve,reject)=>{const until=Date.now()+20000;const t=setInterval(()=>{if(!button('下载 KMZ').disabled){clearInterval(t);resolve(true)}else if(Date.now()>until){clearInterval(t);reject(Error(document.body.textContent))}},100)})`)
    await evaluate(`button('下载 KMZ').click();button('下载转换报告').click();await tools.nextTick()`)
    assert.equal(await evaluate('window.captured.length'), 2)
    const checked = await evaluate(`(async()=>{const zip=await tools.JSZip.loadAsync(await captured[0].arrayBuffer());const names=['wpmz/template.kml','wpmz/waylines.wpml'];for(const name of names){const doc=new DOMParser().parseFromString(await zip.file(name).async('string'),'text/xml');if(doc.querySelector('parsererror'))throw Error('invalid XML '+name)}const draft=await tools.parseKmzBlobToCloudDraft(captured[0],{waylineId:'test',routeName:'test'});window.draft=draft;return {keys:Object.keys(draft),report:JSON.parse(await captured[1].text()),draft:JSON.parse(JSON.stringify(draft))}})()`)
    assert.equal(checked.report.photos.length, 1)
    assert.equal(checked.report.points.length, 2)
    assert.equal(checked.draft.waypoints[0].capture_mode, 'none')
    assert.equal(checked.draft.waypoints[1].camera_params.pitch, -30)
    assert.equal(checked.draft.waypoints[1].headingTrue, 12)
    if (process.env.VENDOR_ROUTE_ROOT) {
      const path = require('node:path')
      for (const [name, count, shots] of [
        ['友爱航线/3.航线规划/已完成航线/220kVI母II母电压互感器.json', 16, 14],
        ['友爱航线/3.航线规划/已完成航线/辽友一线航线.json', 30, 28],
        ['李石寨航线/航线规划/精细巡检/李石寨220kV厂区航线.json', 438, 436]
      ]) {
        const source = JSON.parse(fs.readFileSync(path.join(process.env.VENDOR_ROUTE_ROOT, name), 'utf8'))
        const actual = await evaluate(`(async()=>{
          const source=${JSON.stringify(source)};const r=tools.parseVendorRoute(source);const model=await tools.loadEgm96();const built=tools.buildVendorKmz(r,{lens:'wide',finishAction:'goHome',takeoffHeight:30,rthHeight:100,confirmed:true},model);
          const parse=xml=>{const d=new DOMParser().parseFromString(xml,'text/xml');if(d.querySelector('parsererror'))throw Error('invalid XML');return d};
          const doc=parse(built.wpml),template=parse(built.template);const nodes=(d,n)=>[...d.getElementsByTagNameNS('*',n)];const val=(d,n)=>nodes(d,n)[0]?.textContent;
          const marks=nodes(doc,'Placemark'),tmarks=nodes(template,'Placemark');let shotCount=0;
          for(let i=0;i<marks.length;i++){const p=source.points[i],m=marks[i],t=tmarks[i];
            if(val(m,'coordinates')!==p.lng+','+p.lat||Number(val(m,'executeHeight'))!==p.height||Number(val(m,'waypointSpeed'))!==p.waypointSpeed)throw Error('point mismatch '+i);
            if(Math.abs(Number(val(t,'ellipsoidHeight'))-Number(val(t,'height'))-model.undulation(p.lng,p.lat))>1e-6)throw Error('height mismatch '+i);
            const acts=nodes(m,'action');const photos=acts.filter(a=>val(a,'actionActuatorFunc')==='takePhoto');shotCount+=photos.length;if(photos.length!==p.yawPitchArray.length)throw Error('photo mismatch '+i);
            for(let j=0;j<p.yawPitchArray.length;j++){const a=p.yawPitchArray[j];const offset=j*3;if(Number(val(acts[offset],'aircraftHeading'))!==a.aircraftYaw||Number(val(acts[offset+1],'gimbalPitchRotateAngle'))!==a.gimbalPitch)throw Error('pose mismatch '+i)}
          }
          const draft=await tools.parseKmzBlobToCloudDraft(new Blob([await tools.packVendorKmz(built)]),{waylineId:'sample'});
          for(let i=0;i<draft.waypoints.length;i++){const p=source.points[i],v=draft.waypoints[i],a=p.yawPitchArray[0];if(v.height!==p.height||v.longitude!==p.lng||v.latitude!==p.lat||v.camera_params.pitch!==Math.min(0,a?a.gimbalPitch:p.gimbalPitch))throw Error('draft mismatch '+i)}
          return {points:marks.length,shots:shotCount,draft:draft.waypoints.length}
        })()`)
        assert.deepEqual(actual, { points: count, shots, draft: count })
        console.log('XML and browser readback PASS:', name)
      }
    }
    console.log('Readback draft:', JSON.stringify(checked.draft).slice(0, 1800))
    await evaluate(`await chooseFile({points:[]},'invalid.json')`)
    assert.ok(await evaluate(`button('下载 KMZ').disabled && document.body.textContent.includes('执行航点')`))
    await evaluate(`visible.value=false;await tools.nextTick();visible.value=true;await tools.nextTick()`)
    assert.ok(await evaluate(`button('转换').disabled && !document.body.textContent.includes('invalid.json')`))
    console.log('PASS: browser file selection, explicit UI selections/confirmation, conversion, downloads, XML parsing, cloud draft readback, invalid-file cleanup and reopen.')
  } finally {
    await send('Page.close').catch(() => {})
    ws.close()
    fs.unlinkSync('.construction-backend/vendor-conversion-probe.html')
  }
}
main().catch(e => { console.error(e); process.exitCode = 1 })
