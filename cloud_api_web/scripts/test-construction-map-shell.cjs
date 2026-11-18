const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { compileFunction } = require('node:vm')
const { parse, compileScript } = require('@vue/compiler-sfc')
const base = 'src/components/construction/'
const source = name => fs.readFileSync(`${base}${name}.vue`, 'utf8')
function evaluate (code, mocks, args = {}) {
  const js = ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true } }).outputText
  const mod = { exports: {} }
  compileFunction(js, ['require', 'module', 'exports', ...Object.keys(args)])(mocks, mod, mod.exports, ...Object.values(args))
  return mod.exports
}
const area = { id: 'area-1', name: 'Area', boundary: [], enabled: true, version: 1 }
const line = { line_id: 'line-1', name: 'Line', points: [{ lng: 1, lat: 1 }, { lng: 2, lat: 2 }], color: '#FF9F43', enabled: true, archived: false, type: '道路', version: 1, length_m: 10 }
const calls = []
const api = {
  listConstructionAreas: async filters => { calls.push(['areas', filters]); return filters?.keyword ? [] : [area] },
  listConstructionLines: async filters => { calls.push(['lines', filters]); return filters?.keyword ? [] : [line] },
  getConstructionArea: async () => area,
  getConstructionLine: async () => line,
  createConstructionLine: async body => ({ ...line, ...body }),
  updateConstructionLine: async (id, body) => ({ ...line, ...body, version: body.expected_version + 1 }),
  archiveConstructionLine: async () => ({ ...line, archived: true }),
  constructionLineHistory: async () => [line]
}
let rejectDiscard = false
const element = { ElMessage: { error () {}, info () {}, success () {} }, ElMessageBox: { confirm: async () => { if (rejectDiscard) throw new Error('cancel') } } }
const windowMock = { addEventListener () {}, removeEventListener () {} }
async function contracts () {
  const emitted = []; let exposed
  const setup = parse(source('ConstructionLines')).descriptor.scriptSetup.content
  const controller = evaluate(setup + '\nmodule.exports = { load, newLine, draft, editing, drawing, keyword, viewHistory, save, cancel, archive, history, historyRows };', name => name === 'vue' ? { ...vue, onMounted () {}, onBeforeUnmount () {} } : name === 'vue-router' ? { useRouter: () => ({ push () {} }) } : name === 'element-plus' ? element : name.includes('/api/') ? api : name.includes('constructionLineModel') ? { validateLine: () => '', lineLengthMeters: () => 1 } : {}, {
    defineProps: () => ({ active: true, ledgerTarget: {}, propertiesTarget: {} }),
    defineEmits: () => (event, value) => emitted.push([event, value]),
    defineExpose: value => { exposed = value },
    window: windowMock
  })
  assert.ok(exposed.mapState, 'line controller exposes reactive mapState')
  assert.equal(typeof exposed.onSelectLine, 'function')
  assert.equal(typeof exposed.updatePoints, 'function')
  assert.equal(typeof exposed.setDrawing, 'function')
  assert.equal(typeof exposed.deactivate, 'function')
  await controller.load(); await vue.nextTick()
  assert.deepEqual(exposed.mapState.value.lines, [line])
  controller.keyword.value = 'no match'; await controller.load(); await vue.nextTick()
  assert.deepEqual(exposed.mapState.value.lines, [line], 'map records are independent of ledger filters')
  await exposed.onSelectLine(line.line_id); await vue.nextTick()
  assert.equal(exposed.mapState.value.selectedLineId, line.line_id)
  await controller.newLine(); exposed.updatePoints(line.points); exposed.setDrawing(true); await vue.nextTick()
  assert.deepEqual(exposed.mapState.value.linePoints, line.points)
  assert.equal(exposed.mapState.value.lineEditable, true)
  rejectDiscard = true; assert.equal(await exposed.confirmDiscard(), false)
  rejectDiscard = false; assert.equal(await exposed.confirmDiscard(), true)
  exposed.deactivate(); await vue.nextTick()
  assert.equal(controller.editing.value, false)
  assert.equal(controller.drawing.value, false)
  assert.equal(controller.draft.value, null, 'accepted discard removes an unsaved new line')
  await controller.newLine(); controller.draft.value.name = 'Created'; exposed.updatePoints(line.points)
  await controller.save(); assert.equal(controller.draft.value.name, 'Created'); assert.equal(controller.editing.value, false)
  await exposed.onSelectLine(line.line_id); exposed.deactivate()
  assert.equal(controller.draft.value.line_id, line.line_id, 'clean selection survives deactivation')
  await controller.history(line); assert.deepEqual(controller.historyRows.value, [line])
  controller.viewHistory(line); await vue.nextTick()
  assert.equal(exposed.mapState.value.lineEditable, false, 'historical geometry remains read-only')
  assert.deepEqual(exposed.mapState.value.linePoints, line.points)
  const last = emitted.filter(([event]) => event === 'map-state').pop()
  assert.deepEqual(last[1], exposed.mapState.value, 'emitted state matches exposed state contract')
  await controller.newLine(); await controller.cancel(); assert.equal(controller.draft.value, null)
  await controller.archive(line)
}
function node (type, text = '') { return { type, text, children: [], parent: null, props: {}, style: {}, tagName: type.toUpperCase(), addEventListener () {}, removeEventListener () {} } }
const renderer = vue.createRenderer({
  createElement: type => node(type),
  createText: text => node('text', text),
  createComment: text => node('comment', text),
  setText: (n, text) => { n.text = text },
  setElementText: (n, text) => { n.text = text },
  parentNode: n => n.parent,
  nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null,
  patchProp: (n, key, prev, value) => { n.props[key] = value },
  insert (n, parent, anchor) { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = parent; const i = parent.children.indexOf(anchor); parent.children.splice(i < 0 ? parent.children.length : i, 0, n) },
  remove (n) { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = null }
})
async function persistentShell () {
  assert.equal((source('ConstructionAreas').match(/<ConstructionAreaMap\b/g) || []).length, 1)
  assert.equal((source('ConstructionLines').match(/<ConstructionAreaMap\b/g) || []).length, 0, 'line panels never own a map')
  let mounted = 0; let unmounted = 0; let mapProps
  const MapStub = vue.defineComponent({ setup (props, { attrs }) { mounted++; mapProps = attrs; vue.onBeforeUnmount(() => unmounted++); return () => vue.h('map-stub') } })
  const empty = vue.defineComponent({ setup: () => () => null })
  const lineDescriptor = parse(source('ConstructionLines')).descriptor
  const lineComponent = evaluate(compileScript(lineDescriptor, { id: 'line-controller', inlineTemplate: true }).content, name => name === 'vue' ? vue : name === 'vue-router' ? { useRouter: () => ({ push () {} }) } : name === 'element-plus' ? element : name.includes('/api/') ? api : name.endsWith('.vue') ? empty : { validateLine: () => '', lineLengthMeters: () => 1 }, { window: windowMock }).default
  const descriptor = parse(source('ConstructionAreas')).descriptor
  const compiled = compileScript(descriptor, { id: 'persistent-shell', inlineTemplate: true })
  const component = evaluate(compiled.content, name => {
    if (name === 'vue') return vue
    if (name === 'element-plus') return element
    if (name === 'vue-router') return { useRoute: () => ({ query: {} }), onBeforeRouteLeave () {}, onBeforeRouteUpdate () {} }
    if (name.includes('/api/')) return api
    if (name.includes('useConstructionDocks')) return { useConstructionDocks: () => ({ docks: vue.ref([]), refresh () {} }) }
    if (name.includes('constructionModel')) return { validateBoundary: () => '', areaSquareMeters: () => 0 }
    if (name.includes('ConstructionAreaMap')) return MapStub
    if (name.includes('ConstructionLines')) return lineComponent
    return empty
  }, { window: windowMock }).default
  const app = renderer.createApp(component)
  const Button = vue.defineComponent({ setup: (props, { attrs, slots }) => () => vue.h('button', attrs, slots.default?.()) })
  app.component('el-button', Button); app.config.warnHandler = () => {}
  for (const tag of ['el-drawer', 'el-table', 'el-table-column']) app.component(tag, empty)
  const root = node('root'); app.mount(root)
  const flush = async () => { for (let i = 0; i < 8; i++) { await Promise.resolve(); await vue.nextTick() } }
  await flush()
  const find = (n, text) => n.type === 'button' && n.children.some(c => c.text.trim() === text) ? n : n.children.map(c => find(c, text)).find(Boolean)
  const map = () => { const walk = n => n.type === 'map-stub' ? n : n.children.map(walk).find(Boolean); return walk(root) }
  const original = map(); assert.ok(original)
  assert.deepEqual(mapProps.areas, [area]); assert.deepEqual(mapProps.lines, [line])
  await find(root, '线路').props.onClick(); await flush()
  mapProps.onSelectLine(line.line_id); await flush()
  assert.equal(mapProps['selected-line-id'], line.line_id)
  const selectedPoints = mapProps['line-points']
  await find(root, '区域').props.onClick(); await flush()
  await find(root, '线路').props.onClick(); await flush()
  assert.equal(mapProps['selected-line-id'], line.line_id, 'selection survives a round-trip ledger switch')
  assert.deepEqual(mapProps['line-points'], selectedPoints)
  await find(root, '＋ 新建线路').props.onClick(); await flush()
  mapProps['onUpdate:linePoints'](line.points); await flush()
  assert.equal(mapProps['line-editable'], true)
  rejectDiscard = true
  await find(root, '区域').props.onClick(); await flush()
  assert.equal(mapProps['line-editable'], true, 'rejected discard keeps line editing active')
  assert.equal(map(), original)
  rejectDiscard = false
  await find(root, '区域').props.onClick(); await flush()
  assert.equal(mapProps['line-editable'], false, 'accepted switch ends line editing')
  await find(root, '线路').props.onClick(); await flush()
  assert.equal(map(), original, 'ledger switches preserve the map host identity')
  assert.equal(mounted, 1, 'exactly one map mounted'); assert.equal(unmounted, 0, 'no ledger switch unmounts the map')
  app.unmount(); assert.equal(unmounted, 1)
}
contracts().then(persistentShell).then(() => console.log('Construction map shell: persistent map lifecycle and line map-state/handler contracts passed.')).catch(error => { console.error(error); process.exitCode = 1 })
