const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
function element () {
  return {
    style: {},
    children: [],
    handlers: {},
    clientWidth: 100,
    clientHeight: 100,
    append (...items) { this.children.push(...items) },
    appendChild (item) { this.children.push(item) },
    setAttribute () {},
    addEventListener (name, fn) { this.handlers[name] = fn },
    removeEventListener (name) { delete this.handlers[name] },
    contains (item) { return this === item || this.children.some(child => child.contains(item)) },
    getBoundingClientRect () { return { left: 0, top: 0, width: 100, height: 100 } },
    remove () { this.removed = true }
  }
}
const document = { createElement: element }; const window = element()
const exported = {}
let conversions = 0
const js = ts.transpileModule(fs.readFileSync('src/utils/map-distance.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
vm.compileFunction(js, ['require', 'exports', 'document', 'window'])(() => ({ gcj02towgs84: (lng, lat) => { conversions++; return [lng, lat] } }), exported, document, window)
assert.equal(exported.horizontalDistance([103, 24], [103, 24]), 0)
assert.ok(Math.abs(exported.horizontalDistance([0, 0], [0, 1]) - 111195.08) < 0.1)
assert.ok(Math.abs(exported.horizontalDistance([0, 1], [0, 0]) - 111195.08) < 0.1)
const active = new Set()
const map = { add: o => active.add(o), remove: items => items.forEach(o => active.delete(o)), containerToLngLat: pixel => ({ lng: 103 + pixel.x / 1000, lat: 24 + pixel.y / 1000 }) }
class Overlay { constructor (options) { this.options = options } }
class Pixel { constructor (x, y) { this.x = x; this.y = y } }
const container = element(); let blocked = true
const dispose = exported.addMapDistance({ Marker: Overlay, Polyline: Overlay, Pixel }, map, container, () => blocked)
const panel = container.children[0]; const [start, clear, status] = panel.children
const click = (x, y) => { let consumed = false; container.handlers.click({ target: container, clientX: x, clientY: y, stopImmediatePropagation () { consumed = true } }); return consumed }
start.onclick(); assert.match(status.textContent, /先结束编辑/); assert.equal(click(0, 0), false)
blocked = false; start.onclick(); assert.equal(click(0, 0), true); assert.equal(active.size, 1)
assert.equal(click(10, 0), true); assert.equal(active.size, 3); assert.equal(conversions, 2); assert.match(status.textContent, /千米/)
assert.equal(click(20, 0), false, 'selection finishes after exactly two points')
start.onclick(); assert.equal(active.size, 0); click(0, 0); click(0, 0); assert.match(status.textContent, /0.0 米/)
clear.onclick(); assert.equal(active.size, 0)
start.onclick(); click(1, 1); window.handlers.keydown({ key: 'Escape', stopPropagation () {} }); assert.equal(active.size, 0)
dispose(); assert.equal(panel.removed, true); assert.equal(Object.keys(container.handlers).length, 0); assert.equal(Object.keys(window.handlers).length, 0)
console.log('Map distance checks passed: distances, editing lock, two-point capture, units, repeat/clear/Escape and disposal.')
