import type { ConstructionPoint } from '/@/api/construction'
import { routeBoundaryError } from './constructionWaylineGeometry'
import { groundDistance } from './dockCoverageGeometry'
export interface SweepOptions { direction: number; rowSpacing: number; pointSpacing: number }
export interface SweepPoint extends ConstructionPoint { capture: boolean }

export function resolveSweepAbsoluteHeight (dockEllipsoidHeight: unknown, relativeHeight: unknown): number {
  const dockHeight = Number(dockEllipsoidHeight)
  const relative = Number(relativeHeight)
  if (!Number.isFinite(dockHeight)) throw Error('当前机巢OSD缺少有效WGS84椭球高，请等待遥测更新后再生成航线')
  if (!Number.isFinite(relative) || relative < 1 || relative > 1500) throw Error('相对机巢航高须为 1–1500 米')
  const absolute = dockHeight + relative
  if (!Number.isFinite(absolute) || absolute < 2 || absolute > 1500) throw Error('机巢椭球高加相对航高超出 WGS84 航高允许范围（2–1500 米）')
  return absolute
}

// Local metre coordinates are used only for planning. Stored coordinates remain WGS84.
export function generateSweep (boundary: ConstructionPoint[], options: SweepOptions) {
  const polygon = boundary.map(p => ({ ...p }))
  if (polygon.length > 1 && groundDistance(polygon[0], polygon[polygon.length - 1]) < 0.001) polygon.pop()
  if (polygon.length < 3 || polygon.length > 200 || polygon.some(p => !Number.isFinite(p.lng) || !Number.isFinite(p.lat) || Math.abs(p.lng) > 180 || Math.abs(p.lat) > 85)) throw Error('区域边界无效或过于复杂（最多200个顶点）')
  if (!Number.isFinite(options.direction) || options.direction < 0 || options.direction >= 180 || ![options.rowSpacing, options.pointSpacing].every(n => Number.isFinite(n) && n >= 1 && n <= 1000)) throw Error('方向须为0至179度，间距须为1至1000米')
  const origin = polygon[0]; const rad = Math.PI / 180; const scale = 6371008.8 * rad; const cosLat = Math.cos(origin.lat * rad)
  const sin = Math.sin(options.direction * rad); const cos = Math.cos(options.direction * rad)
  const local = polygon.map(p => { const x = (p.lng - origin.lng) * scale * cosLat; const y = (p.lat - origin.lat) * scale; return { u: x * sin + y * cos, v: x * cos - y * sin } })
  const world = (u: number, v: number): ConstructionPoint => ({ lng: origin.lng + (u * sin + v * cos) / (scale * cosLat), lat: origin.lat + (u * cos - v * sin) / scale })
  const min = Math.min(...local.map(p => p.v)); const max = Math.max(...local.map(p => p.v)); const width = max - min
  if (width < 0.05 || Math.max(...polygon.map(p => groundDistance(origin, p))) > 20000) throw Error('区域过窄或跨度超过20公里，请调整区域')
  const count = Math.ceil(width / options.rowSpacing)
  if (count > 1000) throw Error('航线过密，请增大行间距')
  const rows = Array.from({ length: count }, (_, i) => min + width * (i + 0.5) / count)
  // Ensure narrow concave branches receive a pass, even when narrower than the spacing.
  const levels = [...new Set(local.map(p => p.v))].sort((a, b) => a - b)
  for (let i = 1; i < levels.length; i++) if (levels[i] - levels[i - 1] > 0.05 && !rows.some(v => v > levels[i - 1] && v < levels[i])) rows.push((levels[i - 1] + levels[i]) / 2)
  rows.sort((a, b) => a - b)
  const points: SweepPoint[] = []
  const visible = (a: ConstructionPoint, b: ConstructionPoint) => !routeBoundaryError([a, b], polygon)
  const edgeCache = new Map<string, boolean>()
  function connect (start: ConstructionPoint, end: ConstructionPoint): ConstructionPoint[] {
    if (visible(start, end)) return []
    const nodes = [start, end, ...polygon]; const dist = nodes.map(() => Infinity); const prev = nodes.map(() => -1); const done = new Set<number>(); dist[0] = 0
    for (let step = 0; step < nodes.length; step++) {
      let u = -1; for (let i = 0; i < nodes.length; i++) if (!done.has(i) && (u === -1 || dist[i] < dist[u])) u = i
      if (u === -1 || !Number.isFinite(dist[u])) break
      if (u === 1) { const result: ConstructionPoint[] = []; for (let k = prev[1]; k > 0; k = prev[k]) result.unshift(nodes[k]); return result }
      done.add(u)
      for (let v = 1; v < nodes.length; v++) {
        if (done.has(v)) continue
        const key = `${Math.min(u, v)}:${Math.max(u, v)}`
        let ok = u > 1 && v > 1 ? edgeCache.get(key) : undefined
        if (ok === undefined) { ok = visible(nodes[u], nodes[v]); if (u > 1 && v > 1) edgeCache.set(key, ok) }
        const cost = dist[u] + groundDistance(nodes[u], nodes[v])
        if (ok && cost < dist[v]) { dist[v] = cost; prev[v] = u }
      }
    }
    throw Error('无法在区域内连接扫描航段，请调整区域或方向')
  }
  function add (p: ConstructionPoint, capture: boolean) {
    if (points.length && groundDistance(points[points.length - 1], p) < 0.001) { points[points.length - 1].capture ||= capture; return }
    points.push({ ...p, capture }); if (points.length > 2000) throw Error('生成航点超过2000个，请增大间距或缩小区域')
  }
  rows.forEach((v, row) => {
    const hits: number[] = []
    for (let i = 0; i < local.length; i++) { const a = local[i]; const b = local[(i + 1) % local.length]; if ((a.v > v) !== (b.v > v)) hits.push(a.u + (b.u - a.u) * (v - a.v) / (b.v - a.v)) }
    hits.sort((a, b) => a - b)
    const spans: Array<[number, number]> = []
    for (let i = 0; i + 1 < hits.length; i += 2) if (hits[i + 1] - hits[i] > 0.05) spans.push([hits[i] + 0.01, hits[i + 1] - 0.01])
    if (row % 2) spans.reverse()
    spans.forEach(span => {
      const [a, b] = row % 2 ? [span[1], span[0]] : span
      const start = world(a, v)
      if (points.length) connect(points[points.length - 1], start).forEach(p => add(p, false))
      const steps = Math.max(1, Math.ceil(Math.abs(b - a) / options.pointSpacing))
      if (steps > 2000) throw Error('拍照点过密，请增大拍照点间距')
      for (let i = 0; i <= steps; i++) add(world(a + (b - a) * i / steps, v), true)
    })
  })
  if (points.length < 2) throw Error('区域不足以生成有效航线')
  const issue = routeBoundaryError(points, polygon); if (issue) throw Error(issue)
  return { points, rows: rows.length, distance: points.slice(1).reduce((total, p, i) => total + groundDistance(points[i], p), 0) }
}
