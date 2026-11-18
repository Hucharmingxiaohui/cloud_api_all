const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const context = { window: {} }
vm.runInNewContext(fs.readFileSync('public/config.js', 'utf8'), context)
const config = JSON.parse(JSON.stringify(context.window.CURRENT_CONFIG.flightAirspace))
// 原文按点号录入，检查换行拆开的秒数及经纬度顺序。
const dms = [
  [24, 49, 55.48098, 103, 38, 23.75061], [24, 49, 55.50725, 103, 39, 55.38957],
  [24, 49, 31.28028, 103, 41, 4.63498], [24, 46, 32.54443, 103, 40, 40.40183],
  [24, 41, 27.44851, 103, 37, 59.99604], [24, 40, 15.03786, 103, 36, 27.76588],
  [24, 43, 41.73189, 103, 32, 17.69641], [24, 44, 44.82244, 103, 30, 29.08802],
  [24, 45, 8.83453, 103, 30, 32.53592], [24, 46, 41.06587, 103, 31, 21.40586],
  [24, 46, 57.39442, 103, 31, 48.12698], [24, 47, 46.91843, 103, 33, 54.92217],
  [24, 46, 49.83227, 103, 35, 41.86935], [24, 48, 21.91059, 103, 37, 2.31454]
]
assert.equal(config.points.length, 14)
dms.forEach((p, i) => {
  assert.ok(Math.abs(config.points[i][0] - (p[3] + p[4] / 60 + p[5] / 3600)) < 1e-9)
  assert.ok(Math.abs(config.points[i][1] - (p[0] + p[1] / 60 + p[2] / 3600)) < 1e-9)
})
function element () { return { style: {}, children: [], append (...children) { this.children.push(...children) }, appendChild (child) { this.children.push(child) }, addEventListener () {}, removeEventListener () {}, remove () { this.removed = true } } }
const document = { createElement: element, createTextNode: text => text }
const mod = { exports: {} }
let conversions = 0
const js = ts.transpileModule(fs.readFileSync('src/utils/flight-airspace.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
vm.compileFunction(js, ['require', 'exports', 'document'])(() => ({ wgs84togcj02: (lng, lat) => { conversions++; return [lng + 0.001, lat - 0.001] } }), mod.exports, document)
class Overlay { constructor (options) { this.options = options } }
const AMap = { Polygon: Overlay, Marker: Overlay, Pixel: Overlay }
const active = new Set(); let fitCount = 0
const map = { add: objects => objects.forEach(o => active.add(o)), remove: objects => objects.forEach(o => active.delete(o)), setFitView: () => { fitCount++ } }
const container = element()
const clear = mod.exports.addFlightAirspace(AMap, map, container, config)
assert.equal(conversions, 14, 'convert WGS84 exactly once per point')
assert.equal(active.size, 15)
assert.equal(fitCount, 0, 'preserve initial dock view')
const panel = container.children[0]; const checkbox = panel.children[0].children[0]
checkbox.checked = false; checkbox.onchange(); assert.equal(active.size, 0)
panel.children[1].onclick(); assert.equal(active.size, 15); assert.equal(fitCount, 1)
clear(); assert.equal(active.size, 0); assert.equal(panel.removed, true)
mod.exports.addFlightAirspace(AMap, map, container, { ...config, enabled: false })()
assert.equal(active.size, 0)
console.log('Airspace checks passed: 14 source DMS points, coordinate order/conversion, visibility, fit and cleanup.')
