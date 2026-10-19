const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const source = fs.readFileSync('src/components/variableMgt/solarMgt/solarAccessoryMgt/accessoryModel.ts', 'utf8')
const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const mod = { exports: {} }
new Function('module', 'exports', 'require', output)(mod, mod.exports, require)
const { validateAccessory, moveWaypoint, reorderWaypoints } = mod.exports
const point = { name: '正面', longitude: 120, latitude: 36, height: 55, heading: 180, pitch: -30, focal_length: 24, action: 'take_photo', image_format: 'visible', enabled: true, sort_order: 1 }
const device = { orthophoto_id: 'photo', name: 'INV-01', device_type: 'inverter', longitude: 120, latitude: 36, height: 50, enabled: true, waypoints: [point] }
assert.equal(validateAccessory(device), '')
for (const value of [null, undefined, NaN, Infinity, '', 181]) {
  assert.ok(validateAccessory({ ...device, longitude: value }))
}
assert.ok(validateAccessory({ ...device, waypoints: [{ ...point, focal_length: 0 }] }))
assert.ok(validateAccessory({ ...device, waypoints: [{ ...point, pitch: -91 }] }))
assert.ok(validateAccessory({ ...device, name: ' ' }))
const points = [{ ...point, name: 'A' }, { ...point, name: 'B' }, { ...point, name: 'C' }]
moveWaypoint(points, 2, -1)
assert.deepEqual(points.map(p => p.name), ['A', 'C', 'B'])
reorderWaypoints(points)
assert.deepEqual(points.map(p => p.sort_order), [1, 2, 3])
moveWaypoint(points, 0, -1)
assert.deepEqual(points.map(p => p.name), ['A', 'C', 'B'])
console.log('solar accessory model tests passed')
