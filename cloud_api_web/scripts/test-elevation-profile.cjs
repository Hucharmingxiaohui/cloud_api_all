const assert = require('node:assert/strict')
const fs = require('node:fs'); const vm = require('node:vm'); const ts = require('typescript')
const exported = {}
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/utils/elevation-profile.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, { exports: exported })
const { sampleElevation, buildElevationProfile, finiteHeight } = exported
const grid = { width: 3, height: 2, west: 103, north: 25, dx: 0.001, dy: -0.001, noData: -9999, values: [2000, 2200, 2000, 2000, 2200, 2000] }
assert.equal(sampleElevation(grid, 103, 25), 2000)
assert.ok(Math.abs(sampleElevation(grid, 103.0005, 24.9995) - 2100) < 1e-6)
assert.equal(sampleElevation(grid, 104, 25), null)
assert.equal(sampleElevation({ ...grid, values: [-9999, 2200, 2000, 2000, 2200, 2000] }, 103.0005, 24.9995), null)
assert.equal(finiteHeight(null), null); assert.equal(finiteHeight(''), null); assert.equal(finiteHeight(false), null); assert.equal(finiteHeight(0), 0)
const points = [{ lng: 103, lat: 24.9995, height: 2129.7882 }, { lng: 103.002, lat: 24.9995, height: 2129.7882 }]
const absolute = buildElevationProfile(points, 'WGS84', null, grid)
assert.ok(absolute.samples.length > 3)
assert.ok(absolute.samples.some(p => p.clearance < 0), 'interior hill detected even when both waypoints are above ground')
assert.ok(absolute.samples[0].clearance > 100)
assert.ok(absolute.samples.every(p => p.flight === 2129.7882))
const relative = points.map(p => ({ ...p, height: 100 }))
assert.ok(buildElevationProfile(relative, 'relativeToStartPoint', '', grid).samples.every(p => p.flight === null && p.clearance === null))
assert.ok(buildElevationProfile(relative, 'relativeToStartPoint', 2029.7882, grid).samples.every(p => Math.abs(p.flight - 2129.7882) < 1e-6))
assert.ok(buildElevationProfile(relative, 'unknown', 2029, grid).samples.every(p => p.flight === null))
assert.ok(buildElevationProfile(points.map(p => ({ ...p, lng: p.lng + 1 })), 'WGS84', null, grid).samples.every(p => p.clearance === null))
assert.ok(buildElevationProfile(points.map(p => ({ ...p, height: null })), 'WGS84', null, grid).samples.every(p => p.flight === null))
async function main () {
  const { fromArrayBuffer } = await import('geotiff')
  const bytes = fs.readFileSync('public/terrain/shilin-ellipsoid.tif')
  const image = await (await fromArrayBuffer(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength))).getImage()
  assert.equal(image.getGeoKeys().GeographicTypeGeoKey, 4979)
  assert.equal(image.getGeoKeys().GTRasterTypeGeoKey, 2)
  const dir = image.getFileDirectory(); const values = (await image.readRasters({ samples: [0] }))[0]
  const actual = { width: image.getWidth(), height: image.getHeight(), west: dir.ModelTiepoint[3], north: dir.ModelTiepoint[4], dx: dir.ModelPixelScale[0], dy: -dir.ModelPixelScale[1], noData: Number(image.getGDALNoData()), values }
  assert.equal(actual.width, 829); assert.equal(actual.height, 723)
  const h = sampleElevation(actual, 103.54643128618687, 24.75737802924538)
  assert.ok(h > 1700 && h < 2400)
  console.log(`Elevation checks passed: bilinear, missing/bounds, interior hill, absolute/relative modes, real Shilin GeoTIFF; dock ground sample ${h.toFixed(2)} m (DSM, not OSD).`)
}
main().catch(e => { console.error(e); process.exitCode = 1 })
