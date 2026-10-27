const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const exported = {}
const source = fs.readFileSync('src/components/construction/dockCoverageGeometry.ts', 'utf8')
require('node:vm').runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, { exports: exported })
const center = { lng: 120, lat: 36 }
assert.equal(exported.pointsWithinCoverage(center, 2000, [center, { lng: 120.001, lat: 36 }]), true, 'two-point overall route')
assert.equal(exported.pointsWithinCoverage(center, 100, [center, { lng: 120.01, lat: 36 }]), false, 'outside endpoint')
assert.equal(exported.pointsWithinCoverage(center, 2000, [{ lng: NaN, lat: 36 }]), false)
for (const radius of [100, 2000, 5000]) {
  const points = exported.coverageBoundary(center, radius)
  assert.equal(points.length, 96)
  for (const p of points) {
    const rad = Math.PI / 180
    const h = Math.sin((p.lat - center.lat) * rad / 2) ** 2 + Math.cos(p.lat * rad) * Math.cos(center.lat * rad) * Math.sin((p.lng - center.lng) * rad / 2) ** 2
    assert.ok(Math.abs(2 * 6371008.8 * Math.asin(Math.sqrt(h)) - radius) < 0.001)
  }
}
for (const n of [0, 99, 5001, NaN, Infinity, 100.5]) assert.equal(exported.coverageBoundary(center, n).length, 0)
console.log('Dock coverage geometry passed: metre-based radius and invalid limits.')
const polygon = coords => coords.map(([lng, lat]) => ({ lng, lat }))
assert.equal(exported.areaCoverage(center, 2000, polygon([[119.999, 35.999], [120.001, 35.999], [120.001, 36.001], [119.999, 36.001]])), 'full')
assert.equal(exported.areaCoverage(center, 100, polygon([[119.99, 35.99], [120.01, 35.99], [120.01, 36.01], [119.99, 36.01]])), 'partial', 'circle wholly inside area')
assert.equal(exported.areaCoverage(center, 100, polygon([[119.99, 35.9999], [120.01, 35.9999], [120.01, 36.0001], [119.99, 36.0001]])), 'partial', 'crossing edges even with every vertex outside')
assert.equal(exported.areaCoverage(center, 100, polygon([[120, 36], [120.01, 36], [120.01, 36.01]])), 'partial')
assert.equal(exported.areaCoverage(center, 5000, polygon([[121, 37], [121.01, 37], [121, 37.01]])), null)
assert.equal(exported.areaCoverage(center, 100, []), null)
assert.equal(exported.areaCoverage(center, NaN, polygon([[120, 36], [120.01, 36], [120.01, 36.01]])), null)
console.log('Coverage area classification passed: full, partial, enclosing circle, crossing edges, disjoint and invalid input.')
