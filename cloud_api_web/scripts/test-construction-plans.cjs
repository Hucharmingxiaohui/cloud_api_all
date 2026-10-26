const assert = require('node:assert/strict')
const fs = require('node:fs')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
for (const name of ['ConstructionPlans', 'ConstructionPlanForm', 'ConstructionAreaMap']) {
  const filename = `src/components/construction/${name}.vue`
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  const result = compileTemplate({ source: descriptor.template.content, filename, id: filename, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(result.errors, [])
}
console.log('Construction plan Vue components compiled successfully.')
