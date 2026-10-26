const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
function compile (source) { return ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText }
const mod = { exports: {} }
// eslint-disable-next-line no-new-func
new Function('module', 'exports', compile(fs.readFileSync('src/components/construction/constructionTaskType.ts', 'utf8')))(mod, mod.exports)
const { isConstructionTask } = mod.exports
assert.ok(isConstructionTask({ plan_type: 6 }))
assert.ok(isConstructionTask({ plan_type: '6' }))
for (const type of [0, 1, 2, 3, 4, 5, null, undefined]) assert.equal(isConstructionTask({ plan_type: type }), false)
const sfc = fs.readFileSync('src/components/task/TaskPanel.vue', 'utf8')
const source = sfc.split('<script setup lang="ts">')[1].split('</script>')[0]
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const names = ['anaysisTaskResult', 'checkAnaysisStaus', 'checkSingleTaskAnalysisStatus']
const declarations = ast.statements.filter(node => ts.isFunctionDeclaration(node) && names.includes(node.name?.text)).map(node => node.getText(ast)).join('\n')
let starts = 0; let queries = 0
const pending = { value: new Set() }; const states = { value: new Map() }
// Execute the actual TaskPanel handlers with stubbed I/O, including timer-driven retries.
// eslint-disable-next-line no-new-func
const handlers = new Function('isConstructionTask', 'startTaskAnasisyApi', 'isAnalyzedApi', 'analyzingTasks', 'taskAnalysisStatus', compile(declarations) + '\nreturn { anaysisTaskResult, checkAnaysisStaus, checkSingleTaskAnalysisStatus }')(
  isConstructionTask, async () => { starts++; return { data: 0 } }, async () => { queries++; return { data: 0 } }, pending, states)
async function main () {
  const construction = { job_id: 'construction', plan_type: 6 }
  pending.value.add(construction.job_id)
  for (const handler of Object.values(handlers)) await handler(construction)
  assert.equal(starts, 0, 'construction cannot enter legacy automatic analysis')
  assert.equal(queries, 0, 'construction has its own status endpoint')
  assert.equal(pending.value.has(construction.job_id), false)
  for (const type of [0, 1, 3, 4, 5]) await handlers.checkSingleTaskAnalysisStatus({ job_id: 'legacy-' + type, plan_type: type })
  assert.equal(starts, 5, 'legacy polling behavior preserved')
  assert.equal(queries, 5)
  console.log('Construction task integration passed: numeric/string type handling, no automatic construction analysis, legacy behavior preserved.')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
