import type { ConstructionArea, ConstructionData, ConstructionDock, ConstructionParams, ConstructionPlan, ConstructionPoint, ConstructionTask } from '../../types/construction'

export const defaultParams: ConstructionParams = { altitude: 50, speed: 5, pitch: -60, interval: 3, lens: 'dual' }
export const demoDocks: ConstructionDock[] = [
  { id: 'demo-mobile', name: '车载机巢 · 演示', mode: 'mobile', position: { lng: 121.3650, lat: 37.5188 } },
  { id: 'demo-fixed', name: '固定机巢 · 演示', mode: 'fixed', position: { lng: 121.3715, lat: 37.5218 } }
]
export function clone<T> (value: T): T { return JSON.parse(JSON.stringify(value)) }
export function newId (prefix: string) { return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}` }
export function areaSquareMeters (points: ConstructionPoint[]) {
  if (points.length < 3) return 0
  const origin = points[0]
  const xScale = 111320 * Math.cos(origin.lat * Math.PI / 180)
  const p = points.map(p => ({ x: (p.lng - origin.lng) * xScale, y: (p.lat - origin.lat) * 110540 }))
  return Math.abs(p.reduce((sum, a, i) => { const b = p[(i + 1) % p.length]; return sum + a.x * b.y - b.x * a.y }, 0)) / 2
}
function orientation (a: ConstructionPoint, b: ConstructionPoint, c: ConstructionPoint) {
  return (b.lng - a.lng) * (c.lat - a.lat) - (b.lat - a.lat) * (c.lng - a.lng)
}
function intersects (a: ConstructionPoint, b: ConstructionPoint, c: ConstructionPoint, d: ConstructionPoint) {
  const on = (p: ConstructionPoint, q: ConstructionPoint, r: ConstructionPoint) => Math.abs(orientation(p, q, r)) < 1e-12 && r.lng >= Math.min(p.lng, q.lng) && r.lng <= Math.max(p.lng, q.lng) && r.lat >= Math.min(p.lat, q.lat) && r.lat <= Math.max(p.lat, q.lat)
  return (orientation(a, b, c) * orientation(a, b, d) < 0 && orientation(c, d, a) * orientation(c, d, b) < 0) || on(a, b, c) || on(a, b, d) || on(c, d, a) || on(c, d, b)
}
export function validateBoundary (points: ConstructionPoint[]) {
  if (!Array.isArray(points) || points.length < 3) return '请绘制至少包含三个顶点的区域'
  if (points.length > 100) return '区域最多支持100个顶点'
  if (points.some(p => !p || !Number.isFinite(p.lng) || !Number.isFinite(p.lat) || Math.abs(p.lng) > 180 || Math.abs(p.lat) > 85)) return '区域坐标无效，请检查经纬度'
  if (new Set(points.map(p => `${p.lng},${p.lat}`)).size !== points.length) return '区域包含重复顶点'
  for (let i = 0; i < points.length; i++) {
    for (let j = i + 1; j < points.length; j++) {
      if (j === i + 1 || (i === 0 && j === points.length - 1)) continue
      if (intersects(points[i], points[(i + 1) % points.length], points[j], points[(j + 1) % points.length])) return '区域边界不能交叉或重叠'
    }
  }
  if (areaSquareMeters(points) < 10) return '区域面积过小或顶点共线，请重新绘制'
  return ''
}
export function validateParams (p: ConstructionParams) {
  if (!p) return '巡检参数缺失'
  if (!Number.isFinite(p.altitude) || p.altitude < 10 || p.altitude > 120) return '演示航高范围为10–120米'
  if (!Number.isFinite(p.speed) || p.speed < 1 || p.speed > 15) return '飞行速度范围为1–15米/秒'
  if (!Number.isFinite(p.pitch) || p.pitch < -90 || p.pitch > 0) return '云台俯仰范围为-90–0度'
  if (!Number.isFinite(p.interval) || p.interval < 2 || p.interval > 30) return '拍照间隔范围为2–30秒'
  if (!['dual', 'visible', 'infrared'].includes(p.lens)) return '请选择拍摄模式'
  return ''
}
export function validateArea (area: ConstructionArea) {
  if (!area.name.trim() || area.name.trim().length > 40) return '请输入1–40字的区域名称'
  return validateBoundary(area.boundary) || validateParams(area.params)
}
export function validatePlan (plan: ConstructionPlan, areas: ConstructionArea[], now = Date.now()) {
  const area = areas.find(a => a.id === plan.areaId)
  if (!plan.name.trim() || plan.name.trim().length > 60) return '请输入1–60字的计划名称'
  if (!area || !area.enabled) return '请选择一个启用的施工区域'
  if (area.version !== plan.areaVersion) return '区域已更新，请重新选择区域并确认参数'
  if (!demoDocks.some(d => d.id === plan.dockId)) return '请选择执行机巢'
  if (!['generated', 'imported'].includes(plan.source)) return '请选择航线来源'
  if (plan.source === 'imported' && !/\.kmz$/i.test(plan.fileName)) return '请选择KMZ文件（仅记录文件名，不解析或上传）'
  if (!['manual', 'scheduled'].includes(plan.schedule)) return '请选择执行方式'
  if (plan.schedule === 'scheduled' && (!Number.isFinite(Date.parse(plan.scheduledAt)) || Date.parse(plan.scheduledAt) <= now)) return '请选择未来的计划时间'
  return validateParams(plan.params)
}
export function makePreviewTask (plan: ConstructionPlan, areas: ConstructionArea[]): ConstructionTask {
  const error = validatePlan(plan, areas)
  if (error) throw new Error(error)
  return { id: newId('task'), name: plan.name, planId: plan.id, area: clone(areas.find(a => a.id === plan.areaId)!), dock: clone(demoDocks.find(d => d.id === plan.dockId)!), params: clone(plan.params), createdAt: new Date().toISOString(), status: 'preview' }
}
export function seedData (): ConstructionData {
  const time = '2026-10-20T08:30:00+08:00'
  const areas: ConstructionArea[] = [
    { id: 'area-foundation', name: '01号风机 · 基础施工区', category: '基础施工', enabled: true, notes: '关注基坑周边作业人员与现场防护。', version: 1, boundary: [{ lng: 121.3638, lat: 37.5200 }, { lng: 121.3662, lat: 37.5203 }, { lng: 121.3665, lat: 37.5187 }, { lng: 121.3640, lat: 37.5185 }], params: clone(defaultParams), updatedAt: time },
    { id: 'area-lifting', name: '02号风机 · 吊装作业区', category: '吊装作业', enabled: true, notes: '演示区域，请按实际施工边界重新绘制。', version: 1, boundary: [{ lng: 121.3680, lat: 37.5225 }, { lng: 121.3707, lat: 37.5225 }, { lng: 121.3707, lat: 37.5208 }, { lng: 121.3680, lat: 37.5208 }], params: { ...defaultParams, altitude: 60 }, updatedAt: time },
    { id: 'area-storage', name: '施工材料堆放区', category: '材料堆场', enabled: false, notes: '暂停巡检的示例区域。', version: 1, boundary: [{ lng: 121.3671, lat: 37.5184 }, { lng: 121.3696, lat: 37.5184 }, { lng: 121.3696, lat: 37.5173 }, { lng: 121.3671, lat: 37.5173 }], params: clone(defaultParams), updatedAt: time }
  ]
  const plan: ConstructionPlan = { id: 'plan-sample', name: '基础施工区日常巡视', areaId: areas[0].id, areaVersion: 1, dockId: demoDocks[0].id, params: clone(defaultParams), source: 'generated', fileName: '', schedule: 'manual', scheduledAt: '', createdAt: time }
  return { version: 1, areas, plans: [plan], tasks: [{ id: 'task-sample', name: '施工现场巡检 · 结果示例', planId: plan.id, area: clone(areas[0]), dock: clone(demoDocks[0]), params: clone(defaultParams), createdAt: time, status: 'sample' }] }
}
export function parseSavedData (raw: string): ConstructionData {
  const d = JSON.parse(raw)
  if (!d || d.version !== 1 || !Array.isArray(d.areas) || !Array.isArray(d.plans) || !Array.isArray(d.tasks)) throw new Error('演示数据格式不兼容')
  const validArea = (a: any) => a && typeof a.id === 'string' && typeof a.name === 'string' && typeof a.category === 'string' && typeof a.notes === 'string' && typeof a.enabled === 'boolean' && Number.isInteger(a.version) && a.version > 0 && typeof a.updatedAt === 'string' && !validateArea(a)
  if (!d.areas.every(validArea) || new Set(d.areas.map((a: ConstructionArea) => a.id)).size !== d.areas.length) throw new Error('区域演示数据无效')
  if (!d.plans.every((p: any) => p && typeof p.id === 'string' && typeof p.name === 'string' && typeof p.fileName === 'string' && typeof p.scheduledAt === 'string' && typeof p.createdAt === 'string' && typeof p.areaId === 'string' && Number.isInteger(p.areaVersion) && demoDocks.some(d => d.id === p.dockId) && ['manual', 'scheduled'].includes(p.schedule) && ['generated', 'imported'].includes(p.source) && !validateParams(p.params))) throw new Error('计划演示数据无效')
  if (!d.tasks.every((t: any) => t && typeof t.id === 'string' && typeof t.name === 'string' && typeof t.createdAt === 'string' && ['preview', 'sample'].includes(t.status) && validArea(t.area) && t.dock && demoDocks.some(d => d.id === t.dock.id) && typeof t.dock.name === 'string' && !validateParams(t.params))) throw new Error('任务演示数据无效')
  return d
}
