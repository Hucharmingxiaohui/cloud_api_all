const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')

const source = fs.readFileSync('src/router/index.ts', 'utf8')
const output = ts.transpileModule(source.replace('import.meta.env.BASE_URL', "'/'"), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
let routes
const mod = { exports: {} }
const router = { beforeEach () {}, afterEach () {}, onError () {} }
const stubs = {
  'vue-router': { createRouter: options => { routes = options.routes; return router }, createWebHistory: () => ({}) },
  '/@/types/index': { ERouterName: new Proxy({}, { get: (_, name) => name }) },
  vue: { ref: value => ({ value }) }
}
// eslint-disable-next-line no-new-func
new Function('module', 'exports', 'require', output)(mod, mod.exports, name => {
  assert.ok(stubs[name], `Unexpected router dependency: ${name}`)
  return stubs[name]
})
const flatten = list => list.flatMap(route => [route, ...flatten(route.children || [])])
const all = flatten(routes)
const taskManage = all.find(route => route.path === '/taskManage')
assert.ok(taskManage)
const entries = taskManage.children.filter(route => route.meta?.label === '工地巡检计划' && route.meta.showInMenu)
assert.equal(entries.length, 1, 'task management has one construction menu entry')
assert.equal(all.filter(route => route.meta?.label === '工地巡检' && route.meta.showInMenu).length, 0)
const entry = entries[0]
assert.equal(entry.path, '/taskManage/construction/plans')
assert.ok(String(entry.component).includes('ConstructionPlans.vue'), 'plans entry mounts the real area-scoped plans page')
const variables = all.find(route => route.path === '/variableMgt')
const areasRoute = variables.children.find(route => route.meta?.label === '施工区域' && route.component)
assert.ok(areasRoute)
assert.ok(String(areasRoute.component).includes('ConstructionAreas.vue'), 'areas page mounts the real construction area page')
assert.equal(all.find(route => route.path === '/taskManage/construction/tasks').component, undefined, 'no second task list')
assert.deepEqual(all.find(route => route.path === '/construction/tasks').redirect({ query: { task: 'demo-123' } }), { path: '/task/taskResult', query: { demo_task: 'demo-123' } })
assert.deepEqual(all.find(route => route.path === '/construction/plans').redirect({ query: { area: 'area-1', create: '1' } }), { path: entry.path, query: { area: 'area-1', create: '1' } })
assert.ok(String(all.find(route => route.path === '/task/taskResult').component).includes('TaskResultEntry.vue'))
assert.ok(all.find(route => route.name === 'constructionPreview')?.component)
console.log('Construction platform routes passed')
