const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const JSZip = require('jszip')
async function main () {
  const geo = await import('geotiff')
  const cache = {}
  function load (file) {
    file = path.resolve(file)
    if (cache[file]) return cache[file]
    const exports = {}; cache[file] = exports
    const requireLocal = name => {
      if (name === 'geotiff') return geo
      if (name === 'jszip') return JSZip
      if (name.startsWith('.')) return load(path.resolve(path.dirname(file), name + '.ts'))
      return require(name)
    }
    const output = ts.transpileModule(fs.readFileSync(file, 'utf8').replace(/import\.meta\.env\.BASE_URL/g, "'/'"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true } }).outputText
    vm.runInNewContext(output, { exports, require: requireLocal, TextDecoder, TextEncoder, Uint8Array, Float32Array, ArrayBuffer, crypto: require('node:crypto').webcrypto, console })
    return exports
  }
  const egm = load('src/utils/egm96.ts')
  const bytes = fs.readFileSync('public/geoid/us_nga_egm96_15.tif')
  const model = await egm.decodeEgm96(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength))
  assert.ok(Math.abs(model.undulation(123.7942356560604, 41.89016229514482) - 11.7992) < 0.15)
  assert.ok(Math.abs(model.undulation(123.762957482014, 41.811599492326906) - 11.7791) < 0.15)
  assert.equal(model.undulation(-180, 20), model.undulation(180, 20))
  assert.ok(model.undulation(0, 0) > 0)
  assert.ok(model.undulation(80, 0) < 0)
  assert.equal(model.undulation(0, 90), model.undulation(120, 90))
  assert.equal(model.undulation(0, -90), model.undulation(120, -90))
  assert.throws(() => model.undulation(NaN, 0))
  assert.throws(() => model.undulation(0, 91))
  const corrupt = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength); new Uint8Array(corrupt)[0] ^= 1
  await assert.rejects(() => egm.decodeEgm96(corrupt), /校验/)
  const converter = load('src/components/wayline/vendorJsonKmz.ts')
  const point = { lng: 123.7942356560604, lat: 41.89016229514482, height: 132.36, dh: 132.36, waypointSpeed: 3, aircraftYaw: 10, gimbalPitch: -45, turnMode: 0, yawPitchArray: [] }
  const photo = { FocalLengthRatio: 1, cameraType: 41, captureCount: 1, aircraftYaw: 12, gimbalPitch: -30, keyID: 32, keyName: '设备<&"测试' }
  const source = () => ({ taskname: '测试<&航线', finishedAction: 1, flightPathMode: 0, yawMode: 0, gimbalMode: 2, points: [{ ...point }, { ...point, lng: 123.79425, yawPitchArray: [photo] }], powerline: { keyPoint: [{ ID: 32, partName: photo.keyName }] } })
  const options = { lens: 'wide', finishAction: 'goHome', takeoffHeight: 30, rthHeight: 100, confirmed: true }
  const route = converter.parseVendorRoute(source())
  assert.equal(route.photoCount, 1)
  assert.equal(route.points.length, 2)
  assert.throws(() => converter.parseVendorRoute({ points: [], powerline: { keyPoint: [{}] } }), /执行航点/)
  for (const patch of [{ lng: Infinity }, { lat: 91 }, { waypointSpeed: 0 }, { gimbalPitch: 31 }, { height: null }, { dh: 1 }, { dh: NaN }]) {
    const d = source(); Object.assign(d.points[0], patch); assert.throws(() => converter.parseVendorRoute(d))
  }
  for (const patch of [{ FocalLengthRatio: 2 }, { captureCount: 2 }, { cameraType: 999 }, { gimbalPitch: -91 }, { action: 'video' }]) {
    const d = source(); d.points[1].yawPitchArray = [{ ...photo, ...patch }]; assert.throws(() => converter.parseVendorRoute(d))
  }
  const multiple = source(); multiple.points[1].yawPitchArray = [photo, { ...photo, aircraftYaw: 20 }]
  assert.equal(converter.parseVendorRoute(multiple).photoCount, 2)
  const mixed = source(); mixed.points[1].yawPitchArray = [photo, { ...photo, cameraType: 44 }]
  assert.throws(() => converter.parseVendorRoute(mixed), /混用/)
  const unnamed = source(); unnamed.points[1].yawPitchArray = [{ ...photo, keyName: '' }]
  assert.equal(converter.parseVendorRoute(unnamed).photoCount, 1)
  assert.ok(converter.parseVendorRoute(unnamed).warnings.some(w => w.includes('未命名')))
  assert.throws(() => converter.buildVendorKmz(route, { ...options, confirmed: false }, model), /确认/)
  assert.throws(() => converter.buildVendorKmz(route, { ...options, lens: '' }, model), /镜头/)
  assert.throws(() => converter.buildVendorKmz(route, { ...options, finishAction: '' }, model), /结束/)
  assert.throws(() => converter.buildVendorKmz(route, options, { undulation: () => NaN }), /高程/)
  const negative = source(); negative.points.forEach(p => { p.height = -10; p.dh = -10 })
  assert.equal(converter.parseVendorRoute(negative).points[0].height, -10)
  const result = converter.buildVendorKmz(route, options, model)
  const dual = converter.buildVendorKmz(route, { ...options, lens: 'wide,ir' }, model)
  assert.ok(dual.template.includes('<wpml:imageFormat>wide,ir</wpml:imageFormat>'))
  for (const xml of [dual.template, dual.wpml]) {
    assert.ok(xml.includes('<wpml:payloadLensIndex>wide,ir</wpml:payloadLensIndex>'))
    assert.equal((xml.match(/<wpml:actionActuatorFunc>takePhoto</g) || []).length, 1)
  }
  assert.deepEqual(JSON.parse(dual.report).points, JSON.parse(result.report).points)
  const dualPhoto = JSON.parse(dual.report).photos[0]
  assert.equal(dualPhoto.lens, 'wide,ir')
  assert.equal(dualPhoto.yaw, photo.aircraftYaw)
  assert.equal(dualPhoto.pitch, photo.gimbalPitch)
  assert.equal(JSON.parse(dual.report).expectedImageCount, 2)
  for (const lens of ['wide,wide', 'ir,zoom', 'wide,ir,zoom']) assert.throws(() => converter.buildVendorKmz(route, { ...options, lens }, model), /镜头/)
  assert.ok(result.template.includes('&lt;&amp;'))
  assert.ok(result.wpml.includes('<wpml:droneEnumValue>100</wpml:droneEnumValue>'))
  assert.ok(result.wpml.includes('<wpml:droneSubEnumValue>1</wpml:droneSubEnumValue>'))
  assert.ok(result.wpml.includes('<wpml:payloadEnumValue>99</wpml:payloadEnumValue>'))
  assert.equal((result.wpml.match(/<wpml:actionActuatorFunc>takePhoto</g) || []).length, 1)
  assert.equal((result.wpml.match(/<Placemark>/g) || []).length, 2)
  assert.ok(result.wpml.includes('<wpml:executeHeightMode>WGS84</wpml:executeHeightMode>'))
  assert.ok(result.template.includes('<wpml:heightMode>EGM96</wpml:heightMode>'))
  const h = Number(result.template.match(/<wpml:height>([^<]+)/)[1])
  assert.ok(Math.abs(132.36 - h - model.undulation(point.lng, point.lat)) < 1e-6)
  const actions = [...result.wpml.matchAll(/<wpml:actionId>(\d+)/g)].map(m => m[1])
  assert.equal(new Set(actions).size, actions.length)
  assert.ok(result.wpml.indexOf('rotateYaw') < result.wpml.indexOf('takePhoto'))
  assert.ok(result.wpml.indexOf('gimbalRotate') < result.wpml.indexOf('takePhoto'))
  const zip = await JSZip.loadAsync(await converter.packVendorKmz(result))
  assert.equal(await zip.file('wpmz/template.kml').async('string'), result.template)
  assert.equal(await zip.file('wpmz/waylines.wpml').async('string'), result.wpml)
  const specimenRoot = process.env.VENDOR_ROUTE_ROOT
  if (specimenRoot) {
    const files = [
      ['友爱航线/3.航线规划/已完成航线/220kVI母II母电压互感器.json', 16, 14],
      ['友爱航线/3.航线规划/已完成航线/辽友一线航线.json', 30, 28],
      ['友爱航线/3.航线规划/已完成航线/辽友二线航线.json', 30, 28],
      ['李石寨航线/航线规划/精细巡检/李石寨220kV厂区航线.json', 438, 436]
    ]
    for (const [name, count, shots] of files) {
      const route = converter.parseVendorRoute(JSON.parse(fs.readFileSync(path.join(specimenRoot, name), 'utf8')))
      assert.equal(route.points.length, count); assert.equal(route.photoCount, shots)
      const kmz = converter.buildVendorKmz(route, options, model)
      assert.equal((kmz.wpml.match(/<Placemark>/g) || []).length, count)
      assert.equal((kmz.wpml.match(/<wpml:actionActuatorFunc>takePhoto</g) || []).length, shots)
      console.log(`Sample PASS: ${name} (${count}/${shots})`)
    }
  }
  console.log('PASS: EGM96 integrity/interpolation, vendor validation, height preservation, XML/action mapping and KMZ packaging.')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
