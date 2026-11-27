const fs = require('node:fs')
const assert = require('node:assert/strict')
const ts = require('typescript')
const vue = require('@vue/compiler-sfc')
const filename = 'src/components/g-map/MobileDockCalibration.vue'
const source = fs.readFileSync(filename, 'utf8')
const { descriptor, errors } = vue.parse(source, { filename })
assert.deepEqual(errors, [])
const compiled = vue.compileScript(descriptor, { id: 'calibration' })
assert.deepEqual(vue.compileTemplate({ source: descriptor.template.content, filename, id: 'calibration', compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
const ast = ts.createSourceFile('calibration.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const body = ast.statements.filter(n => !ts.isImportDeclaration(n)).map(n => n.getText(ast)).join('\n')
const js = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
const props = { sn: 'one' }
let changed, unmount, dialog
const pending = []
let posts = 0
const request = { get: path => new Promise(resolve => pending.push({ path, resolve })), post: async () => { posts++; return { data: { code: 0, data: { ready: false, message: 'pending' } } } } }
const ui = new Function('ref', 'watch', 'onBeforeUnmount', 'defineProps', 'request', 'Modal', 'setTimeout', 'clearTimeout', js + ';return {info,error,start,refresh}')(
  value => ({ value }), (getter, callback) => { changed = callback; callback() }, callback => { unmount = callback }, () => props, request,
  { confirm: options => { dialog = options; return { destroy () {} } } }, () => 1, () => {})
async function flush () { await Promise.resolve(); await Promise.resolve(); await Promise.resolve() }
async function main () {
  assert.equal(pending[0].path, '/api/mobile-docks/one/calibration')
  props.sn = 'two'; changed()
  pending[0].resolve({ data: { code: 0, data: { ready: true, message: 'old' } } }); await flush()
  assert.equal(ui.info.value, null, 'ignore stale response from previous dock')
  pending[1].resolve({ data: { code: 0, data: { ready: true, message: 'current' } } }); await flush()
  assert.equal(ui.info.value.message, 'current')
  ui.start(); assert.equal(posts, 0, 'opening confirmation never sends a command')
  await dialog.onOk(); assert.equal(posts, 1)
  assert.equal(ui.info.value.ready, false)
  ui.refresh(); unmount()
  pending[2].resolve({ data: { code: 0, data: { ready: true, message: 'late' } } }); await flush()
  assert.equal(ui.info.value.message, 'pending', 'ignore responses after unmount')
  console.log('PASS mobile calibration UI compilation, device switching, explicit confirmation and unmount response isolation')
}
main().catch(e => { console.error(e); process.exitCode = 1 })
