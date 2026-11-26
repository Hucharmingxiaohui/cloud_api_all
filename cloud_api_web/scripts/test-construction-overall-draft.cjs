const fs = require('node:fs')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ts = require('typescript')
const source = fs.readFileSync('src/components/construction/ConstructionOverallRoutes.vue', 'utf8')
const ctx = {
  configSn: { value: '' }, docks: { value: [] }, radius: { value: null },
  areas: { value: [{ id: 'line:L', enabled: true }, { id: 'A', enabled: true }, { id: 'old', enabled: false }] },
  selected: { value: { dock_sn: '', bindings: [], points: [] } }, sn: { value: '' },
  computed: fn => ({ get value () { return fn() } }),
  targetCovered: () => true, pointsWithinCoverage: () => true
}
vm.runInNewContext(ts.transpileModule(source.slice(source.indexOf('const coveredAreas ='), source.indexOf('const preview =')) + '\nglobalThis.targets=coveredAreas;globalThis.ready=currentCovered', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, ctx)
assert.equal(ctx.targets.value.length, 2)
assert.equal(ctx.ready.value, false)
ctx.configSn.value = 'dock'; ctx.sn.value = 'dock'; ctx.docks.value = [{ sn: 'dock' }]; ctx.radius.value = 1000
assert.equal(ctx.ready.value, false, 'draft cannot execute even with selected dock')
ctx.selected.value.dock_sn = 'dock'
assert.equal(ctx.ready.value, true)
ctx.configSn.value = ''; assert.equal(ctx.ready.value, false)
const draft = { overall_id: 'draft', dock_sn: '', wayline_id: 'file', file_sign: 'sign', version: 3, bindings: [{ area_id: 'line:L', waypoints: [1], segments: { 1: 'A1-A2' } }] }
Object.assign(ctx, {
  epoch: 1, rows: { value: [draft] }, selected: { value: draft }, editing: { value: false }, plans: {}, libraryVisible: {},
  inspectConstructionOverallRoute: async (sn) => ({ dock_sn: sn, wayline_id: 'file', file_sign: 'sign' }),
  setDraft: value => { ctx.result = value }, ElMessage: { warning () {} }
})
const fn = source.slice(source.indexOf('async function inspectDraft'), source.indexOf('async function uploadKmz'))
vm.runInNewContext(ts.transpileModule(fn, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, ctx)
async function run () {
  await ctx.inspectDraft('file', 1)
  assert.equal(ctx.result.version, 3, 'offline edit uses original version')
  ctx.configSn.value = 'dock'
  await ctx.inspectDraft('file', 1)
  assert.equal(ctx.result.version, 0, 'first dock configuration is new')
  assert.equal(ctx.result.dock_sn, 'dock')
  assert.equal(ctx.result.bindings[0].segments[1], 'A1-A2', 'span binding preserved')
  assert.equal(draft.dock_sn, '', 'source draft retained')
  console.log('PASS overall draft: offline targets, execution gates, optimistic version and span-preserving dock configuration')
}
run().catch(error => { console.error(error); process.exitCode = 1 })
