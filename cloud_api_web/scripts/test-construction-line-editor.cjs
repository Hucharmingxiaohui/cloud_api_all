const fs = require('node:fs')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
const filename = 'src/components/wayline/createWayline.vue'
const source = fs.readFileSync(filename, 'utf8')
const aircraftContext = { lineState: {} }
vm.runInNewContext(source.slice(source.indexOf('function configureConstructionAircraft ('), source.indexOf('function init ()')), aircraftContext)
const aircraftRoute = { missionConfig: { droneInfo: { droneEnumValue: 77, droneSubEnumValue: 0 }, payloadInfo: { payloadEnumValue: 66 } } }
aircraftContext.configureConstructionAircraft(aircraftRoute)
assert.equal(aircraftRoute.missionConfig.droneInfo.droneEnumValue, 100)
assert.equal(aircraftRoute.missionConfig.droneInfo.droneSubEnumValue, 1)
assert.equal(aircraftRoute.missionConfig.payloadInfo.payloadEnumValue, 99)
assert.equal(aircraftContext.lineState.droneEnumValue, 100)
const { descriptor, errors } = parse(source, { filename })
assert.deepEqual(errors, [])
const script = compileScript(descriptor, { id: 'line-editor' })
assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'line-editor', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
function load (file) {
  const mod = { exports: {} }
  const path = require('node:path')
  new Function('module', 'exports', 'require', ts.transpileModule(fs.readFileSync(file, 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS } }).outputText)(mod, mod.exports, name => name.startsWith('.') ? load(path.resolve(path.dirname(file), name + '.ts')) : require(name))
  return mod.exports
}

const { terrainRoute } = load('src/utils/terrain-route.ts')
const grid = { width: 3, height: 2, west: 0, north: 25, dx: 0.001, dy: -0.001, noData: -9999, values: [2000, 2250, 2020, 2000, 2250, 2020] }
const coords = [{ lng: 0, lat: 25 }, { lng: 0.002, lat: 25 }]
const result = terrainRoute(coords, 100, grid)
assert.deepEqual(result.heights, [2100, 2120])
assert.ok(result.minimum < 0, 'interior ridge detected')
assert.ok(result.warnings.length)
assert.throws(() => terrainRoute([{ lng: 102, lat: 25 }, coords[1]], 100, grid))
assert.throws(() => terrainRoute(coords, NaN, grid))
assert.throws(() => terrainRoute(coords, 0, grid))
const missing = terrainRoute(coords, 100, { ...grid, values: [2000, -9999, 2020, 2000, -9999, 2020] })
assert.ok(missing.unknown > 0)
const fn = source.slice(source.indexOf('async function calculateTerrain'), source.indexOf('async function applyLineHeight'))
const state = { loadElevation: async () => grid, terrainRoute, terrainReview: { value: '' }, ElMessageBox: { confirm: async () => {} } }
vm.runInNewContext(fn, state)
const route = () => ({ folder: {}, elements: coords.map(p => ({ resource: { content: { geometry: { coordinates: [p.lng, p.lat] } } }, Placemark: { useGlobalHeight: 1 } })) })
async function checks () {
  const r = route()
  assert.equal(await state.calculateTerrain(r, 100), true)
  assert.deepEqual(r.elements.map(p => p.Placemark.height), [2100, 2120])
  assert.ok(r.elements.every(p => p.Placemark.useGlobalHeight === 0))
  assert.equal(r.folder.executeHeightMode, 'WGS84')
  state.ElMessageBox.confirm = async () => { throw Error('cancel') }
  const cancelled = route()
  assert.equal(await state.calculateTerrain(cancelled, 100), false)
  assert.equal(cancelled.elements[0].Placemark.height, undefined)
  console.log('PASS terrain editor: compile, independent heights, ridge/NoData detection, invalid inputs, actual application and warning cancellation')
}
checks().catch(e => { console.error(e); process.exitCode = 1 })
