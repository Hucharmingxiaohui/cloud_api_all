import type { ConstructionPoint } from '/@/api/construction'

export function lineLengthMeters (points: ConstructionPoint[]): number {
  let length = 0
  for (let i = 1; i < points.length; i++) {
    const a = points[i - 1]; const b = points[i]
    const rad = Math.PI / 180
    const h = Math.sin((b.lat - a.lat) * rad / 2) ** 2 + Math.cos(a.lat * rad) * Math.cos(b.lat * rad) * Math.sin((b.lng - a.lng) * rad / 2) ** 2
    length += 6371008.8 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))))
  }
  return length
}

export function validateLine (points: ConstructionPoint[]): string {
  if (!points || points.length < 2) return '线路至少需要两个折点'
  if (points.length > 200) return '线路最多支持200个折点'
  for (let i = 0; i < points.length; i++) {
    const p = points[i]
    if (!p || !Number.isFinite(p.lng) || !Number.isFinite(p.lat) || Math.abs(p.lng) > 180 || Math.abs(p.lat) > 90) return '线路坐标无效'
    if (i && p.lng === points[i - 1].lng && p.lat === points[i - 1].lat) return '线路相邻折点不能重合'
  }
  return lineLengthMeters(points) > 0 ? '' : '线路长度必须大于零'
}
