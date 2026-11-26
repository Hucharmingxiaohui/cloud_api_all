const fs = require('node:fs')
const assert = require('node:assert/strict')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
const mod = { exports: {} }
new Function('module', 'exports', ts.transpileModule(fs.readFileSync('src/components/construction/constructionCapture.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(mod, mod.exports)
const { constructionCaptureActions, addMissingConstructionCapture } = mod.exports
const a = constructionCaptureActions('wide,ir'); const b = constructionCaptureActions()
assert.equal(a[0].actionActuatorFuncParam.takePhoto.payloadLensIndex, 'wide,ir')
assert.ok(a[0].actionActuatorFuncParam.takePhoto.fileSuffix)
assert.notEqual(a[0], b[0])
assert.throws(() => constructionCaptureActions('invalid'))
const rotation = { actionActuatorFunc: 'gimbalRotate', actionActuatorFuncParam: { gimbalRotate: { gimbalPitchRotateAngle: -37 } } }
const points = [{ Placemark: { actionGroup: {} } }, { Placemark: { actionGroup: { actionList: [rotation] } } }, { Placemark: { actionGroup: { actionList: b } } }, { Placemark: { actionGroup: { actionList: [{ actionActuatorFunc: 'orientedShoot' }] } } }]
assert.equal(addMissingConstructionCapture(points, 'ir'), 2)
assert.equal(points[1].Placemark.actionGroup.actionList[0], rotation)
assert.equal(points[2].Placemark.actionGroup.actionList, b)
assert.equal(b[0].actionActuatorFuncParam.takePhoto.payloadLensIndex, 'wide')
assert.equal(addMissingConstructionCapture(points, 'ir'), 0)
for (const filename of ['src/components/g-map/SetWaylinePoints.vue', 'src/components/wayline/createWayline.vue']) {
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename }); assert.deepEqual(errors, [])
  const compiled = compileScript(descriptor, { id: 'capture' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'capture', compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
}
console.log('PASS capture: lens/suffix parameters, independent actions, explicit repair, action preservation, idempotency and both Vue components')
