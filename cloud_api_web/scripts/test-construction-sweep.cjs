const assert = require('node:assert/strict')
const fs = require('node:fs'); const path = require('node:path'); const ts = require('typescript'); const vm = require('node:vm')
const cache = {}
function load (file) {
  file = path.resolve(file); if (cache[file]) return cache[file]
  const exports = {}; cache[file] = exports
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, { exports, require: name => load(path.resolve(path.dirname(file), name + '.ts')) })
  return exports
}
const { generateSweep, resolveSweepAbsoluteHeight } = load('src/components/construction/constructionSweep.ts')
const { resolveWaypointHeadings } = load('src/components/construction/constructionHeading.ts')
const { routeBoundaryError } = load('src/components/construction/constructionWaylineGeometry.ts')
const poly = xy => xy.map(([x, y]) => ({ lng: 120 + x / 90000, lat: 36 + y / 111000 }))
const rectangle = poly([[0, 0], [120, 0], [120, 100], [0, 100]])
const concave = poly([[0, 0], [150, 0], [150, 120], [110, 120], [110, 30], [40, 30], [40, 120], [0, 120]])
for (const boundary of [rectangle, concave, [...concave].reverse(), poly([[0, 0], [130, 0], [20, 110]])]) {
  for (const direction of [0, 30, 90, 135, 179]) {
    const result = generateSweep(boundary, { direction, rowSpacing: 20, pointSpacing: 15 })
    assert.equal(routeBoundaryError(result.points, boundary), '')
    assert.ok(result.points.length > 2 && result.points.length <= 2000)
    assert.ok(result.points.some(p => p.capture)); assert.ok(result.distance > 0)
  }
}
assert.ok(generateSweep(concave, { direction: 90, rowSpacing: 20, pointSpacing: 20 }).points.some(p => !p.capture), 'concave connectors stay inside and do not take photos')
const result = generateSweep(rectangle, { direction: 0, rowSpacing: 20, pointSpacing: 20 })
assert.ok(result.points[0].lat < result.points[1].lat, 'first sweep heads north')
assert.equal(resolveSweepAbsoluteHeight(100, 30), 130, 'dock ellipsoid height plus configured relative height becomes WGS84 route height')
assert.equal(resolveSweepAbsoluteHeight(-25.5, 30), 4.5, 'negative WGS84 dock ellipsoid heights remain valid')
assert.throws(() => resolveSweepAbsoluteHeight(undefined, 30), /椭球高/, 'missing dock ellipsoid height blocks route generation')
assert.throws(() => resolveSweepAbsoluteHeight(1490, 20), /允许范围/, 'out-of-range absolute route height is blocked')
const headings = resolveWaypointHeadings([
  { lng: 120, lat: 36, placemark: { waypointHeadingParam: { waypointHeadingMode: 'followWayline' } } },
  { lng: 120, lat: 36.001, placemark: { waypointHeadingParam: { waypointHeadingMode: 'followWayline' } } },
  { lng: 120.001, lat: 36.001, placemark: { waypointHeadingParam: { waypointHeadingMode: 'smoothTransition', waypointHeadingAngle: 225 } } }
])
assert.ok(Math.abs(headings[0]) < 0.1, 'follow-wayline heading points north')
assert.ok(Math.abs(headings[1] - 90) < 0.1, 'follow-wayline heading points east')
assert.equal(headings[2], 225, 'custom heading uses its configured angle')
assert.ok(result.points.some((p, i) => i && p.lat < result.points[i - 1].lat), 'alternating reverse pass')
assert.throws(() => generateSweep(rectangle, { direction: 180, rowSpacing: 20, pointSpacing: 20 }))
assert.throws(() => generateSweep(rectangle, { direction: 0, rowSpacing: NaN, pointSpacing: 20 }))
assert.throws(() => generateSweep(poly([[0, 0], [1000, 0], [1000, 1000], [0, 1000]]), { direction: 0, rowSpacing: 1, pointSpacing: 1 }))
console.log('Sweep geometry passed: rectangle, triangle, concavity, winding, rotated sweeps, internal transfers, alternating rows and limits.')
