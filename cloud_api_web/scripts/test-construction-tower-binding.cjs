const fs = require('node:fs')
const assert = require('node:assert/strict')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
const source = fs.readFileSync('src/components/construction/ConstructionOverallRoutes.vue', 'utf8')
const script = source.split('<script setup lang="ts">')[1].split('</script>')[0]
const ast = ts.createSourceFile('routes.ts', script, ts.ScriptTarget.Latest, true)
const functions = ast.statements.filter(n => ts.isFunctionDeclaration(n) && ['boundLine', 'towerOptions', 'save'].includes(n.name?.text)).map(n => n.getText(ast)).join('\n')
const code = ts.transpileModule(functions, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
const assignments = { value: { 1: 'line:L' } }
const areas = { value: [{ id: 'line:L', line: { tower_nos: ['A1', 'A2'] } }] }
const draft = { value: { points: [{ index: 1 }] } }
const busy = { value: false }; const error = { value: '' }; const segments = { value: { 1: 'A1-A2' } }
const actual = new Function('assignments', 'areas', 'draft', 'busy', 'error', 'segments', code + '; return { towerOptions, save }')(assignments, areas, draft, busy, error, segments)
async function main () {
  assert.deepEqual(actual.towerOptions(1), ['A1', 'A2'])
  await actual.save(); assert.ok(error.value.includes('1')); assert.equal(busy.value, false)
  error.value = ''; segments.value = {}; await actual.save(); assert.ok(error.value)
  assignments.value[1] = 'area'; assert.deepEqual(actual.towerOptions(1), [])
  const resultsSource = fs.readFileSync('src/components/construction/ConstructionTaskResult.vue', 'utf8').split('<script setup lang="ts">')[1].split('</script>')[0]
  const resultAst = ts.createSourceFile('results.ts', resultsSource, ts.ScriptTarget.Latest, true)
  const grouping = resultAst.statements.find(n => ts.isVariableStatement(n) && n.declarationList.declarations.some(d => d.name.getText(resultAst) === 'areaGroups')).getText(resultAst)
  const overall = { value: { bindings: [{ area_id: 'line:L', waypoints: [1, 2, 3], segments: { 1: 'A1', 2: 'A1', 3: 'A2' } }] } }
  const detail = { value: { photos: [{ file_id: 'one', area_id: 'line:L', segment: 'A1' }, { file_id: 'two', area_id: 'line:L', segment: 'A2' }] } }
  const group = new Function('computed', 'overall', 'detail', ts.transpileModule(grouping, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText + ';return areaGroups')(fn => fn, overall, detail)
  const groups = group()
  assert.deepEqual(groups[0].waypoints, [1, 2]); assert.deepEqual(groups[1].waypoints, [3])
  assert.equal(groups[0].photos[0].file_id, 'one'); assert.equal(groups[1].photos[0].file_id, 'two')
  overall.value.bindings[0].segments = { 1: 'A1-A2', 2: 'A1-A2', 3: 'A2-A3' }
  detail.value.photos[0].segment = 'A1-A2'; detail.value.photos[1].segment = 'A2-A3'
  assert.equal(group()[0].photos[0].file_id, 'one', 'historical span attribution preserved')
  for (const filename of ['src/components/construction/ConstructionOverallRoutes.vue', 'src/components/construction/ConstructionTaskResult.vue']) {
    const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename }); assert.deepEqual(errors, [])
    const compiled = compileScript(descriptor, { id: 'tower' })
    assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'tower', compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
  }
  console.log('PASS tower UI: single tower options, explicit selection, old span blocked, area unchanged, binding/results component compilation')
}
main().catch(e => { console.error(e); process.exitCode = 1 })
