const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { compileFunction } = require('node:vm')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
function loadTs (source, requireMock) {
  const mod = { exports: {} }
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
  compileFunction(js, ['require', 'module', 'exports'])(requireMock, mod, mod.exports)
  return mod.exports
}
const model = loadTs(fs.readFileSync('src/components/construction/constructionLineModel.ts', 'utf8'), require)
const p = (lng, lat) => ({ lng, lat })
assert.equal(model.validateLine([p(0, 0), p(1, 1), p(0, 1), p(1, 0), p(0, 0)]), '', 'crossings, closure and non-neighbour repeats allowed')
for (const points of [[p(NaN, 0), p(1, 0)], [p(0, Infinity), p(1, 0)], [p(0, 91), p(1, 0)], Array.from({ length: 201 }, (_, i) => p(i / 1000, 0))]) assert.ok(model.validateLine(points))
assert.ok(Math.abs(model.lineLengthMeters([p(0, 0), p(1, 0)]) - 111195.08) < 1)
for (const name of ['ConstructionLines', 'ConstructionLineLayer', 'ConstructionAreas', 'ConstructionAreaMap']) {
  const file = `src/components/construction/${name}.vue`
  const { descriptor, errors } = parse(fs.readFileSync(file, 'utf8'), { filename: file })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: name })
  const template = compileTemplate({ source: descriptor.template.content, filename: file, id: name, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
}
const events = []; const handlers = {}; const overlays = new Set(); const watchers = []
class Overlay {
  constructor (options) { this.options = options; this.handlers = {} }
  on (name, handler) { this.handlers[name] = handler }
  getPosition () { return { lng: 121, lat: 31 } }
}
const map = { add: value => (Array.isArray(value) ? value : [value]).forEach(o => overlays.add(o)), remove: value => (Array.isArray(value) ? value : [value]).forEach(o => overlays.delete(o)), on: (name, handler) => { handlers[name] = handler }, off: name => { delete handlers[name] }, getStatus: () => ({ doubleClickZoom: true }), setStatus: value => { map.status = value }, setFitView: () => { map.fits = (map.fits || 0) + 1 } }
const props = { map, lines: [], selectedId: '', points: [], editable: true, disabled: false }
const source = parse(fs.readFileSync('src/components/construction/ConstructionLineLayer.vue', 'utf8')).descriptor.scriptSetup.content
const js = ts.transpileModule(source + '\nreturn { begin, click, undo, finish, cancel, render, drawing, pending, stop };', { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
const mocks = name => name === 'vue' ? { ref: value => ({ value }), watch: (get, callback) => watchers.push(callback), onBeforeUnmount: () => {} } : name === 'element-plus' ? { ElMessage: { error: () => {}, warning: () => {} } } : name.includes('coordtransform') ? { wgs84togcj02: (lng, lat) => [lng, lat], gcj02towgs84: (lng, lat) => [lng, lat] } : model
const layer = compileFunction(js, ['require', 'exports', 'defineProps', 'defineEmits', 'AMap', 'document'])(mocks, {}, () => props, () => (name, value) => events.push([name, value]), { Polyline: Overlay, Marker: Overlay, Pixel: Overlay }, { createElement: () => ({ style: {} }) })
layer.begin(); assert.equal(layer.drawing.value, true); assert.equal(map.status.doubleClickZoom, false)
layer.click({ lnglat: p(120, 30) }); layer.click({ lnglat: p(120.001, 30) }); layer.undo(); assert.equal(layer.pending.value.length, 1)
layer.cancel(); assert.equal(events.filter(e => e[0] === 'update:points').length, 0, 'cancel never saves geometry'); assert.equal(handlers.click, undefined); assert.equal(map.status.doubleClickZoom, true)
layer.begin(); layer.click({ lnglat: p(120, 30) }); layer.click({ lnglat: p(120.001, 30) }); layer.finish()
assert.equal(events.find(e => e[0] === 'update:points')[1].length, 2)
props.points = [p(120, 30), p(120.001, 30)]; layer.render(true); assert.ok(map.fits > 0)
assert.equal([...overlays].filter(o => o.options.draggable).length, 2, 'node markers editable without plugin')
const fits = map.fits
props.editable = false; watchers[1](null, [props.lines, props.points, true])
assert.equal(map.fits, fits, 'ending editing for a ledger switch must not change map zoom/position')
props.points = []; layer.render(); assert.equal(overlays.size, 0, 'clear does not restore saved geometry')
layer.stop()
console.log('Construction lines: geometry, four Vue components, drawing/undo/cancel/finish, cleanup, locating and draggable node checks passed.')
