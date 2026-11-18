const assert = require('node:assert/strict')
const fs = require('node:fs'); const path = require('node:path'); const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
function load (filename, mock) {
  const mod = { exports: {} }
  const js = ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS } }).outputText
  new Function('module', 'exports', 'require', js)(mod, mod.exports, mock || require)
  return mod.exports
}
const root = 'src/components/construction/'
const geometry = load(root + 'dockCoverageGeometry.ts')
const { lineTarget, targetCovered } = load(root + 'constructionPatrolTarget.ts', () => geometry)
const line = { line_id: 'same-id', name: 'test', points: [{ lng: 120, lat: 36 }, { lng: 120.001, lat: 36 }], enabled: true, archived: false, version: 1 }
const target = lineTarget(line)
assert.equal(target.id, 'line:same-id')
assert.deepEqual(target.boundary, [], 'line is never rendered as a polygon')
assert.equal(targetCovered({ lng: 120, lat: 36 }, 500, target), true, 'two-point line supported')
assert.equal(targetCovered({ lng: 121, lat: 36 }, 500, target), false)
const area = { id: 'same-id', boundary: [...line.points, { lng: 120, lat: 36.001 }] }
assert.equal(targetCovered(line.points[0], 500, area), geometry.areaCoverage(line.points[0], 500, area.boundary) === 'full', 'legacy polygon rule unchanged')
assert.equal(area.id, 'same-id')
const requests = []
const request = Object.fromEntries(['get', 'post', 'put', 'delete'].map(method => [method, async (url, body) => { requests.push({ method, url, body }); return { data: { code: 0, data: {} } } }]))
const api = load('src/api/construction.ts', () => ({ default: request, __esModule: true }))
async function checks () {
  for (const id of ['same-id', 'line:same-id']) {
    const base = id.startsWith('line:') ? '/api/construction/lines/same-id' : '/api/construction/areas/same-id'
    await api.listConstructionWaylines(id); assert.equal(requests.at(-1).url, base + '/waylines')
    await api.bindConstructionWayline(id, 3, 'file'); assert.equal(requests.at(-1).url, base + '/waylines/bind'); assert.equal(requests.at(-1).body.expected_version, 3)
    await api.listConstructionPlans(id); assert.equal(requests.at(-1).url, base + '/plans')
    await api.createConstructionPlan(id, { name: 'test' }); assert.equal(requests.at(-1).url, base + '/plans')
  }
  for (const name of ['ConstructionLines', 'ConstructionLineWaylines', 'ConstructionAreas', 'ConstructionPlans', 'ConstructionOverallRoutes', 'ConstructionPlanForm']) {
    const filename = path.join(root, name + '.vue')
    const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename }); assert.equal(errors.length, 0)
    const compiled = compileScript(descriptor, { id: name })
    const template = compileTemplate({ source: descriptor.template.content, filename, id: name, compilerOptions: { bindingMetadata: compiled.bindings } })
    assert.deepEqual(template.errors, [], name)
  }
  console.log('Line patrol frontend: namespaced endpoints, legacy area URLs/coverage, line coverage and six Vue component compilations passed.')
}
checks().catch(e => { console.error(e); process.exitCode = 1 })
