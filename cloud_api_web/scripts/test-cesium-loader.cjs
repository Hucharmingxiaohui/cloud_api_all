const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const path = require('node:path')
const source = fs.readFileSync('src/utils/cesium-loader.ts', 'utf8').replace('import.meta.env.BASE_URL', "'/'")
const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
function setup (ua, streams = true) {
  const scripts = []
  const scope = { navigator: { userAgent: ua }, setTimeout, clearTimeout }
  if (streams) for (const key of ['TransformStream', 'ReadableStream', 'WritableStream']) scope[key] = function () {}
  const fallback = { default: { name: 'Unavailable' } }
  const mod = { exports: {} }
  const document = { createElement: () => ({ remove () { this.removed = true } }), head: { appendChild: script => scripts.push(script) } }
  new Function('module', 'exports', 'require', 'window', 'document', 'console', code)(mod, mod.exports, name => {
    assert.equal(name, '../components/cesium/CesiumUnavailable.vue')
    return fallback
  }, scope, document, { error () {} })
  return { ...mod.exports, scope, scripts, fallback }
}
async function main () {
  for (const [ua, streams] of [['Firefox/79.0', false], ['Firefox/79.0', true], ['Firefox/128.0', false]]) {
    const app = setup(ua, streams)
    let imports = 0
    const result = await app.loadCesiumPage(async () => { imports++; return {} })
    assert.equal(result.default, app.fallback.default)
    assert.equal(imports, 0, 'unsupported browser must not import Cesium-dependent modules')
    assert.equal(app.scripts.length, 0, 'unsupported browser must not request Cesium')
  }
  const app = setup('Firefox/128.0')
  let imports = 0
  const route = app.loadCesiumPage(async () => { imports++; assert.ok(app.scope.Cesium); return 'page' })
  const concurrent = app.ensureCesium()
  assert.equal(imports, 0, 'wait for script before evaluating dependent module')
  assert.equal(app.scripts.length, 1)
  assert.equal(app.scripts[0].src, '/cesium/Cesium.js')
  app.scope.Cesium = {}
  app.scripts[0].onload()
  await concurrent
  assert.equal(await route, 'page')
  await app.ensureCesium()
  assert.equal(app.scripts.length, 1, 'reuse initialized runtime')
  const failed = setup('Chrome/120.0')
  const first = failed.loadCesiumPage(async () => { throw Error('must not import') })
  failed.scripts[0].onerror()
  assert.equal((await first).default, failed.fallback.default)
  assert.equal(failed.scripts[0].removed, true)
  const retry = failed.ensureCesium()
  failed.scope.Cesium = {}
  failed.scripts[1].onload()
  await retry
  console.log('PASS Cesium gate: Firefox79/no-streams skip scripts and imports; modern browser ordered/deduplicated loading; failure fallback and retry')
  if (process.argv.includes('--dist')) {
    const html = fs.readFileSync('dist/index.html', 'utf8').replace(/<!--[\s\S]*?-->/g, '')
    assert.ok(!/<script[^>]+src=["'][^"']*\/cesium\/Cesium\.js/.test(html), 'HTML must not eagerly load Cesium')
    const roots = [...html.matchAll(/(?:src|href)="([^"]+\.js)"/g)].map(m => path.resolve('dist', '.' + m[1]))
    const seen = new Set()
    function visit (file) {
      if (seen.has(file)) return
      seen.add(file)
      const ast = ts.createSourceFile(file, fs.readFileSync(file, 'utf8'), ts.ScriptTarget.Latest, true)
      function check (node) {
        assert.ok(!(ts.isPropertyAccessExpression(node) && ts.isIdentifier(node.expression) && node.expression.text === 'Cesium'), `Eager Cesium access: ${file}`)
        ts.forEachChild(node, check)
      }
      check(ast)
      for (const node of ast.statements) {
        if ((ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) && node.moduleSpecifier && ts.isStringLiteral(node.moduleSpecifier)) {
          visit(path.resolve(path.dirname(file), node.moduleSpecifier.text))
        }
      }
    }
    assert.ok(roots.length > 0)
    roots.forEach(visit)
    console.log(`PASS production startup graph: ${seen.size} modules, no Cesium global access`)
  }
}
main().catch(error => { console.error(error); process.exitCode = 1 })
