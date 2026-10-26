import type { ConstructionPoint } from '/@/api/construction'
const EPS = 1e-10
const cross = (a: ConstructionPoint, b: ConstructionPoint) => a.lng * b.lat - a.lat * b.lng
const sub = (a: ConstructionPoint, b: ConstructionPoint) => ({ lng: a.lng - b.lng, lat: a.lat - b.lat })
export function insideArea (p: ConstructionPoint, polygon: ConstructionPoint[]): boolean {
  let inside = false
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[j]; const b = polygon[i]
    if (Math.abs(cross(sub(b, a), sub(p, a))) < EPS * Math.hypot(b.lng - a.lng, b.lat - a.lat) && p.lng >= Math.min(a.lng, b.lng) - EPS && p.lng <= Math.max(a.lng, b.lng) + EPS && p.lat >= Math.min(a.lat, b.lat) - EPS && p.lat <= Math.max(a.lat, b.lat) + EPS) return true
    if ((a.lat > p.lat) !== (b.lat > p.lat) && p.lng < (b.lng - a.lng) * (p.lat - a.lat) / (b.lat - a.lat) + a.lng) inside = !inside
  }
  return inside
}
export function areaLabelPosition (polygon: ConstructionPoint[]): ConstructionPoint {
  const lat = (Math.min(...polygon.map(p => p.lat)) + Math.max(...polygon.map(p => p.lat))) / 2
  const center = { lng: (Math.min(...polygon.map(p => p.lng)) + Math.max(...polygon.map(p => p.lng))) / 2, lat }
  if (insideArea(center, polygon)) return center
  // If the bounding-box center falls in a concavity, use an interior scanline segment.
  const crossings: number[] = []
  for (let i = 0; i < polygon.length; i++) {
    const a = polygon[i]; const b = polygon[(i + 1) % polygon.length]
    if ((a.lat > lat) !== (b.lat > lat)) crossings.push(a.lng + (b.lng - a.lng) * (lat - a.lat) / (b.lat - a.lat))
  }
  crossings.sort((a, b) => a - b)
  let width = -1; let result = polygon[0]
  for (let i = 0; i + 1 < crossings.length; i += 2) {
    if (crossings[i + 1] - crossings[i] > width) {
      width = crossings[i + 1] - crossings[i]; result = { lng: (crossings[i + 1] + crossings[i]) / 2, lat }
    }
  }
  return result
}
export function routeBoundaryError (points: ConstructionPoint[], polygon: ConstructionPoint[]): string {
  if (polygon.length < 3) return '区域边界尚未加载'
  for (let i = 0; i < points.length; i++) {
    const b = points[i]
    if (!Number.isFinite(b.lng) || !Number.isFinite(b.lat) || !insideArea(b, polygon)) return `航点 ${i + 1} 超出区域边界`
    if (!i) continue
    const a = points[i - 1]; const r = sub(b, a); const cuts = [0, 1]
    for (let j = 0; j < polygon.length; j++) {
      const c = polygon[j]; const s = sub(polygon[(j + 1) % polygon.length], c); const den = cross(r, s)
      if (Math.abs(den) < 1e-20) continue
      const t = cross(sub(c, a), s) / den; const u = cross(sub(c, a), r) / den
      if (t > 0 && t < 1 && u >= 0 && u <= 1) cuts.push(t)
    }
    cuts.sort((x, y) => x - y)
    for (let k = 1; k < cuts.length; k++) {
      const t = (cuts[k - 1] + cuts[k]) / 2
      if (!insideArea({ lng: a.lng + r.lng * t, lat: a.lat + r.lat * t }, polygon)) return `航段 ${i} → ${i + 1} 穿出区域边界`
    }
  }
  return ''
}
