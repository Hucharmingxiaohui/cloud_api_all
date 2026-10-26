const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
const geometrySource = fs.readFileSync('src/components/construction/constructionWaylineGeometry.ts', 'utf8')
const compiled = ts.transpileModule(geometrySource, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const exportsObject = {}
require('node:vm').runInNewContext(compiled, { exports: exportsObject })
const { routeBoundaryError } = exportsObject
const p = (lng, lat) => ({ lng, lat })
const polygon = [[120, 36], [120.01, 36], [120.01, 36.01], [120.006, 36.01], [120.006, 36.004], [120.004, 36.004], [120.004, 36.01], [120, 36.01]].map(([x, y]) => p(x, y))
assert.equal(routeBoundaryError([p(120.002, 36.002), p(120.008, 36.002)], polygon), '')
assert.match(routeBoundaryError([p(120.002, 36.008), p(120.008, 36.008)], polygon), /航段/)
assert.match(routeBoundaryError([p(120.002, 36.002), p(121, 37)], polygon), /航点 2/)
assert.equal(routeBoundaryError([polygon[0], polygon[1]], polygon), '')
assert.match(routeBoundaryError([p(NaN, 36)], polygon), /航点 1/)
for (const file of ['construction/ConstructionAreaWaylines.vue', 'construction/ConstructionAreas.vue', 'construction/ConstructionAreaMap.vue', 'g-map/SetWaylinePoints.vue', 'wayline/createWayline.vue']) {
  const filename = 'src/components/' + file
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  const result = compileTemplate({ source: descriptor.template.content, filename, id: filename, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(result.errors, [], filename)
}
console.log('Construction wayline geometry and five Vue component compilation checks passed.')
