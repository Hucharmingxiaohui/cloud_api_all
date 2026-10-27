import type { ConstructionPoint } from '/@/api/construction'

export function pointsWithinCoverage (center: ConstructionPoint, radius: number, points: ConstructionPoint[]): boolean {
  if (!Number.isInteger(radius) || radius < 100 || radius > 5000 || !points.length) return false
  const rad = Math.PI / 180
  return points.every(p => {
    const h = Math.sin((p.lat - center.lat) * rad / 2) ** 2 + Math.cos(center.lat * rad) * Math.cos(p.lat * rad) * Math.sin((p.lng - center.lng) * rad / 2) ** 2
    return Number.isFinite(h) && 2 * 6371008.8 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h)))) <= radius + 0.01
  })
}

// Local azimuthal equidistant projection: distances from the dock remain in metres.
export function areaCoverage (center: ConstructionPoint, radius: number, boundary: ConstructionPoint[]): 'full' | 'partial' | null {
  if (!Number.isFinite(radius) || radius < 100 || radius > 5000 || boundary.length < 3) return null
  const rad = Math.PI / 180; const lat = center.lat * rad
  const points = boundary.map(p => {
    const y = p.lat * rad; const dx = (p.lng - center.lng) * rad
    const h = Math.sin((y - lat) / 2) ** 2 + Math.cos(lat) * Math.cos(y) * Math.sin(dx / 2) ** 2
    const distance = 2 * 6371008.8 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))))
    const bearing = Math.atan2(Math.sin(dx) * Math.cos(y), Math.cos(lat) * Math.sin(y) - Math.sin(lat) * Math.cos(y) * Math.cos(dx))
    return { x: distance * Math.sin(bearing), y: distance * Math.cos(bearing), distance }
  })
  if (points.some(p => !Number.isFinite(p.distance))) return null
  if (points.every(p => p.distance <= radius + 0.01)) return 'full'
  if (points.some(p => p.distance <= radius + 0.01)) return 'partial'
  let containsCenter = false
  for (let i = 0; i < points.length; i++) {
    const a = points[i]; const b = points[(i + 1) % points.length]
    const dx = b.x - a.x; const dy = b.y - a.y; const length = dx * dx + dy * dy
    const t = length ? Math.max(0, Math.min(1, -(a.x * dx + a.y * dy) / length)) : 0
    if (Math.hypot(a.x + t * dx, a.y + t * dy) <= radius + 0.01) return 'partial'
    if ((a.y > 0) !== (b.y > 0) && a.x + (b.x - a.x) * -a.y / (b.y - a.y) > 0) containsCenter = !containsCenter
  }
  return containsCenter ? 'partial' : null
}

// Calculate metre-based destinations on WGS84 coordinates, then let the map convert each vertex.
export function coverageBoundary (center: ConstructionPoint, radius: number): ConstructionPoint[] {
  if (!Number.isFinite(radius) || radius < 100 || radius > 5000 || !Number.isInteger(radius)) return []
  const rad = Math.PI / 180
  const lat = center.lat * rad; const lng = center.lng * rad; const distance = radius / 6371008.8
  return Array.from({ length: 96 }, (_, i) => {
    const bearing = 2 * Math.PI * i / 96
    const y = Math.asin(Math.sin(lat) * Math.cos(distance) + Math.cos(lat) * Math.sin(distance) * Math.cos(bearing))
    const x = lng + Math.atan2(Math.sin(bearing) * Math.sin(distance) * Math.cos(lat), Math.cos(distance) - Math.sin(lat) * Math.sin(y))
    return { lng: (x / rad + 540) % 360 - 180, lat: y / rad }
  })
}
