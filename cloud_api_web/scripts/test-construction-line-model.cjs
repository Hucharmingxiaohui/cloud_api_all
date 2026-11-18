const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const source = fs.readFileSync('src/components/construction/constructionLineModel.ts', 'utf8')
const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const mod = { exports: {} }
new Function('module', 'exports', 'require', output)(mod, mod.exports, require)
const { validateLine, lineLengthMeters } = mod.exports
const p = (lng, lat) => ({ lng, lat })
assert.equal(validateLine([p(120, 30), p(120.001, 30)]), '')
assert.ok(lineLengthMeters([p(120, 30), p(120.001, 30)]) > 90)
assert.match(validateLine([p(120, 30)]), /两个/)
assert.match(validateLine([p(120, 30), p(120, 30)]), /相邻/)
assert.match(validateLine([p(181, 30), p(120, 30)]), /坐标/)
console.log('Construction line model checks passed.')
