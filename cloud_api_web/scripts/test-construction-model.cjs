const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const source = fs.readFileSync('src/components/construction/constructionModel.ts', 'utf8')
const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const mod = { exports: {} }
// Match the existing dependency-free TS test harness in this repository.
// eslint-disable-next-line no-new-func
new Function('module', 'exports', 'require', output)(mod, mod.exports, require)
const { seedData, validateArea, validateBoundary, validatePlan, validateParams, makePreviewTask, parseSavedData, areaSquareMeters } = mod.exports
const data = seedData()
assert.equal(validateArea(data.areas[0]), '')
assert.ok(areaSquareMeters(data.areas[0].boundary) > 10000)
const point = (lng, lat) => ({ lng, lat })
assert.ok(validateBoundary([point(0, 0), point(1, 1), point(0, 1), point(1, 0)]), 'crossing boundary rejected')
assert.ok(validateBoundary([point(0, 0), point(1, 1), point(2, 2)]), 'collinear boundary rejected')
assert.ok(validateBoundary([point(181, 0), point(180, 1), point(179, 0)]))
assert.ok(validateBoundary([point(0, 0), point(1, 0), point(1, 1), point(0, 0)]))
assert.equal(validateBoundary([point(0, 0), point(0.01, 0), point(0.01, 0.01), point(0.005, 0.005), point(0, 0.01)]), '', 'valid concave polygon allowed')
assert.ok(validateParams({ ...data.plans[0].params, speed: null }))
assert.ok(validateParams({ ...data.plans[0].params, altitude: NaN }))
assert.equal(validatePlan(data.plans[0], data.areas), '')
assert.ok(validatePlan({ ...data.plans[0], areaVersion: 0 }, data.areas))
assert.ok(validatePlan({ ...data.plans[0], areaId: data.areas[2].id }, data.areas))
assert.ok(validatePlan({ ...data.plans[0], source: 'imported', fileName: '' }, data.areas))
assert.ok(validatePlan({ ...data.plans[0], schedule: 'scheduled', scheduledAt: '2020-01-01' }, data.areas))
assert.throws(() => makePreviewTask({ ...data.plans[0], areaId: 'missing' }, data.areas))
const task = makePreviewTask(data.plans[0], data.areas)
data.areas[0].boundary[0].lng = 120
data.areas[0].name = 'Changed'
data.plans[0].params.speed = 12
assert.notEqual(task.area.name, 'Changed')
assert.notEqual(task.area.boundary[0].lng, 120)
assert.equal(task.params.speed, 5, 'task preserves immutable execution parameters')
assert.deepEqual(parseSavedData(JSON.stringify(seedData())), seedData())
assert.throws(() => parseSavedData('{'))
assert.throws(() => parseSavedData('{"version":2,"areas":[],"plans":[],"tasks":[]}'))
const broken = seedData(); broken.tasks[0].area = null
assert.throws(() => parseSavedData(JSON.stringify(broken)))
console.log('Construction model: geometry, validation, stale plans, snapshots and persistence checks passed.')
