const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')

let saved = null
let failure = 0
const clone = x => JSON.parse(JSON.stringify(x))
const source = ts.transpileModule(fs.readFileSync('src/components/cloudRenderer/cloudRendererConfig.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
function browser (enabled = true) {
  const storage = new Map([['x-auth-token', 'web-token'], ['cloud-renderer-config', JSON.stringify({ enabled: true })]])
  const config = { baseURL: 'http://platform', cloudRenderer: { enabled } }
  const sandbox = {
    exports: {}, console, AbortController,
    window: { location: { origin: 'http://web' }, setTimeout, clearTimeout },
    localStorage: { getItem: key => storage.get(key) || null },
    require: name => name === 'vue' ? { ref: value => ({ value }) } : { CURRENT_CONFIG: config },
    fetch: async (url, options) => {
      assert.equal(url, 'http://platform/api/platform/cloud-renderer-config')
      assert.equal(options.headers['x-auth-token'], 'web-token')
      if (failure) return { ok: false, status: failure }
      if (options.method === 'PUT') {
        const body = JSON.parse(options.body)
        if (body.revision !== (saved?.revision || 0)) return { ok: false, status: 409 }
        saved = { configured: true, revision: body.revision + 1, config: body.config }
      }
      return { ok: true, json: async () => ({ code: 0, data: clone(saved || { configured: false, revision: 0 }) }) }
    }
  }
  vm.runInNewContext(source, sandbox)
  return { api: sandbox.exports, storage }
}
async function main () {
  const a = browser()
  assert.equal(a.api.isCloudRendererEnabled(), false, 'no renderer before server config is read')
  await a.api.loadCloudRendererConfig()
  assert.equal(saved.config.enabled, true, 'first visit seeds deployment default')
  await a.api.saveCloudRendererConfig({ ...a.api.getCloudRendererConfig(), enabled: false })
  assert.equal(saved.config.enabled, false)
  a.storage.clear()
  await a.api.loadCloudRendererConfig()
  assert.equal(a.api.isCloudRendererEnabled(), false)
  a.storage.set('x-auth-token', 'web-token')
  await a.api.loadCloudRendererConfig()
  assert.equal(a.api.isCloudRendererEnabled(), false, 'logout/login retains disabled state')
  const b = browser(true)
  await b.api.loadCloudRendererConfig()
  assert.equal(b.api.isCloudRendererEnabled(), false, 'new browser and stale local override cannot reopen')
  const oldRevision = b.api.getCloudRendererConfigRevision()
  await a.api.saveCloudRendererConfig({ ...a.api.getCloudRendererConfig(), enabled: true })
  await assert.rejects(b.api.saveCloudRendererConfig(b.api.getCloudRendererConfig(), oldRevision), /其他页面/)
  await b.api.loadCloudRendererConfig()
  assert.equal(b.api.isCloudRendererEnabled(), true)
  const c = browser(false)
  await c.api.loadCloudRendererConfig()
  assert.equal(c.api.isCloudRendererEnabled(), true, 'changed config.js never overrides saved platform setting')
  await c.api.resetCloudRendererConfig()
  assert.equal(saved.config.enabled, false, 'reset is persisted centrally')
  failure = 500
  await assert.rejects(a.api.loadCloudRendererConfig())
  assert.equal(a.api.isCloudRendererEnabled(), false, 'read failure disables rendering rather than falling back to true')
  failure = 0
  await a.api.loadCloudRendererConfig()
  failure = 403
  await assert.rejects(a.api.saveCloudRendererConfig({ ...a.api.getCloudRendererConfig(), enabled: true }))
  assert.equal(saved.config.enabled, false, 'failed save does not change server config')
  assert.equal(a.api.getCloudRendererConfig().enabled, false, 'failed save does not change active config')
  console.log('Cloud renderer frontend checks passed: initialization, logout/login, new browser, config.js precedence, conflicts, reset and failure handling.')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
