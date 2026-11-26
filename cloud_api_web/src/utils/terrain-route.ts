import { sampleElevation, profileDistance } from './elevation-profile'
import type { ElevationGrid } from './elevation-profile'

/** Fixed WGS84 heights. This does not perform onboard terrain following. */
export function terrainRoute (points: Array<{ lng: number; lat: number }>, clearance: number, grid: ElevationGrid) {
  if (!Number.isFinite(clearance) || clearance < 1 || clearance > 1500) throw Error('离地高度需为1–1500米')
  if (points.length < 2 || points.length > 2000) throw Error('请设置2–2000个航点')
  const heights = points.map((p, i) => {
    if (!Number.isFinite(p.lng) || !Number.isFinite(p.lat) || Math.abs(p.lng) > 180 || Math.abs(p.lat) > 90) throw Error(`航点${i + 1}坐标无效`)
    const ground = sampleElevation(grid, p.lng, p.lat)
    if (ground === null) throw Error(`航点${i + 1}缺少地表高程，无法生成航线`)
    return ground + clearance
  })
  let minimum = Infinity; let maximum = -Infinity; let unknown = 0; let count = 0; let worstSegment = 1
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i]; const b = points[i + 1]
    const steps = Math.max(1, Math.ceil(profileDistance({ ...a, height: 0 }, { ...b, height: 0 }) / 10))
    count += steps + 1
    if (count > 100000) throw Error('航线过长，请分段规划后复核高程')
    for (let j = 0; j <= steps; j++) {
      const f = j / steps
      const terrain = sampleElevation(grid, a.lng + (b.lng - a.lng) * f, a.lat + (b.lat - a.lat) * f)
      if (terrain === null) { unknown++; continue }
      const gap = heights[i] + (heights[i + 1] - heights[i]) * f - terrain
      if (gap < minimum) { minimum = gap; worstSegment = i + 1 }
      maximum = Math.max(maximum, gap)
    }
  }
  const warnings: string[] = []
  if (unknown) warnings.push(`${unknown}个航段采样点缺少高程，无法确认净空`)
  if (minimum < Math.max(30, clearance - 0.5)) warnings.push(`第${worstSegment}段最低采样净空${minimum.toFixed(1)}米，低于设定离地高度或30米提示值`)
  if (maximum >= 300) warnings.push(`最大采样离地高度${maximum.toFixed(1)}米，达到或超过300米`)
  return { heights, warnings, minimum, maximum, unknown }
}
