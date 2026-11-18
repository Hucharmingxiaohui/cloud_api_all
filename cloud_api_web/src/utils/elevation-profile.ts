/** Pure WGS84 calculations. Missing terrain/height is always unknown, never zero. */
export interface ElevationGrid { width: number; height: number; west: number; north: number; dx: number; dy: number; noData: number; values: ArrayLike<number> }
export interface HeightPoint { lng: number; lat: number; height: unknown }
export interface ProfileSample { lng: number; lat: number; distance: number; terrain: number | null; flight: number | null; clearance: number | null; segment: number }
export function finiteHeight (value: unknown): number | null {
  if (typeof value !== 'number' && typeof value !== 'string') return null
  if (String(value).trim() === '') return null
  const n = Number(value); return Number.isFinite(n) ? n : null
}
export function sampleElevation (g: ElevationGrid, lng: number, lat: number): number | null {
  const x = (lng - g.west) / g.dx; const y = (lat - g.north) / g.dy
  if (!Number.isFinite(x) || !Number.isFinite(y) || x < 0 || y < 0 || x > g.width - 1 || y > g.height - 1) return null
  const x0 = Math.floor(x); const y0 = Math.floor(y); const x1 = Math.min(x0 + 1, g.width - 1); const y1 = Math.min(y0 + 1, g.height - 1)
  const a = x - x0; const b = y - y0
  const nodes = [[x0, y0, (1 - a) * (1 - b)], [x1, y0, a * (1 - b)], [x0, y1, (1 - a) * b], [x1, y1, a * b]]
  let result = 0
  for (const [col, row, weight] of nodes) {
    if (!weight) continue
    const n = g.values[row * g.width + col]
    if (!Number.isFinite(n) || n === g.noData) return null
    result += n * weight
  }
  return result
}
export function profileDistance (a: HeightPoint, b: HeightPoint) {
  const r = Math.PI / 180
  const h = Math.sin((b.lat - a.lat) * r / 2) ** 2 + Math.cos(a.lat * r) * Math.cos(b.lat * r) * Math.sin((b.lng - a.lng) * r / 2) ** 2
  return 12742017.6 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))))
}
export function buildElevationProfile (points: HeightPoint[], mode: string | undefined, base: unknown, grid: ElevationGrid) {
  if (points.length < 2 || points.length > 2000 || points.some(p => !Number.isFinite(p.lng) || !Number.isFinite(p.lat) || Math.abs(p.lng) > 180 || Math.abs(p.lat) > 90)) throw Error('航线坐标无效或航点数量超限')
  const lengths = points.slice(1).map((p, i) => profileDistance(points[i], p))
  const total = lengths.reduce((a, b) => a + b, 0)
  // Preserve every waypoint, and bound extra interior samples on long routes.
  const spacing = Math.max(25, total / Math.max(1, 6000 - points.length))
  const origin = finiteHeight(base)
  const absolute = (p: HeightPoint) => {
    const height = finiteHeight(p.height)
    if (height === null) return null
    if (mode === 'WGS84') return height
    if (mode === 'relativeToStartPoint' && origin !== null) return height + origin
    return null
  }
  const samples: ProfileSample[] = []; let distance = 0
  lengths.forEach((length, i) => {
    const a = points[i]; const b = points[i + 1]; const steps = Math.max(1, Math.ceil(length / spacing))
    const ha = absolute(a); const hb = absolute(b)
    for (let j = i === 0 ? 0 : 1; j <= steps; j++) {
      const f = j / steps; const lng = a.lng + (b.lng - a.lng) * f; const lat = a.lat + (b.lat - a.lat) * f
      const flight = ha === null || hb === null ? null : ha + (hb - ha) * f
      const terrain = sampleElevation(grid, lng, lat)
      samples.push({ lng, lat, distance: distance + length * f, terrain, flight, clearance: terrain === null || flight === null ? null : flight - terrain, segment: i + 1 })
    }
    distance += length
  })
  return { samples, spacing, total }
}
