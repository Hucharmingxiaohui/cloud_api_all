// 自动生成任务：区域几何计算与单机/多机推荐算法（纯函数，无框架依赖）

export const SINGLE_DRONE_AREA_LIMIT_MU = 200 // 单机单架次可巡面积上限（亩）
export const DRONE_SPEED_MPS = 10 // 巡检飞行速度（m/s）
export const EFFECTIVE_SWATH_M = 80 // 有效巡检带宽（m），航程 ≈ 面积 / 带宽
export const SORTIE_OVERHEAD_MIN = 10 // 每架次起降+换电固定耗时（分钟）

const MU_TO_SQM = 666.67
const EARTH_M_PER_DEG_LAT = 110540
const EARTH_M_PER_DEG_LNG = 111320

export interface AreaGeometry {
  id: string
  name: string
  centroidLng: number
  centroidLat: number
  areaMu: number
  areaM2: number
}

export interface DockPosition {
  sn: string
  nickname: string
  lng: number
  lat: number
}

export interface AutoTaskAssignment {
  sn: string
  nickname: string
  areaIds: string[]
  areaNames: string[]
  distanceM: number
}

export interface AutoTaskRecommendation {
  mode: 'single' | 'multi'
  droneCount: number
  totalAreaMu: number
  totalAreaM2: number
  estimatedMinutes: number
  degraded: boolean
  assignments: AutoTaskAssignment[]
}

// 由区域 4 个经纬度角点计算质心与面积（等距投影后鞋带公式）；任一角点缺失返回 null
// 兼容两种序列化命名：本地新包 camelCase（corner1Lng）/ 服务器 SNAKE_CASE（corner1_lng）
export function computeAreaGeometry (area: Record<string, any>): AreaGeometry | null {
  const pickCorner = (corner: number, axis: 'Lng' | 'Lat') => {
    const camel = area[`corner${corner}${axis}`]
    const snake = area[`corner${corner}_${axis.toLowerCase()}`]
    return Number(camel !== undefined ? camel : snake)
  }
  const lngs = [1, 2, 3, 4].map(corner => pickCorner(corner, 'Lng'))
  const lats = [1, 2, 3, 4].map(corner => pickCorner(corner, 'Lat'))
  if (lngs.some(value => !Number.isFinite(value)) || lats.some(value => !Number.isFinite(value))) {
    return null
  }
  const centroidLng = lngs.reduce((sum, value) => sum + value, 0) / 4
  const centroidLat = lats.reduce((sum, value) => sum + value, 0) / 4
  const metersPerDegLng = EARTH_M_PER_DEG_LNG * Math.cos(centroidLat * Math.PI / 180)
  const xs = lngs.map(lng => lng * metersPerDegLng)
  const ys = lats.map(lat => lat * EARTH_M_PER_DEG_LAT)
  let doubledArea = 0
  for (let index = 0; index < 4; index++) {
    const next = (index + 1) % 4
    doubledArea += xs[index] * ys[next] - xs[next] * ys[index]
  }
  const areaM2 = Math.abs(doubledArea) / 2
  return {
    id: String(area.id),
    name: String(area.solar_panel_area_name ?? area.solarPanelAreaName ?? area.id),
    centroidLng,
    centroidLat,
    areaM2,
    areaMu: areaM2 / MU_TO_SQM
  }
}

// equirectangular 近似距离（米）
export function distanceM (a: { lng: number; lat: number }, b: { lng: number; lat: number }): number {
  const metersPerDegLng = EARTH_M_PER_DEG_LNG * Math.cos(((a.lat + b.lat) / 2) * Math.PI / 180)
  const dx = (a.lng - b.lng) * metersPerDegLng
  const dy = (a.lat - b.lat) * EARTH_M_PER_DEG_LAT
  return Math.hypot(dx, dy)
}

// 推荐执行方式：按总面积阈值确定架数，取距整体质心最近的 N 个机巢，区域就近分配
export function recommendAutoTask (areas: AreaGeometry[], docks: DockPosition[]): AutoTaskRecommendation | null {
  if (!areas.length || !docks.length) return null
  const totalAreaM2 = areas.reduce((sum, area) => sum + area.areaM2, 0)
  const totalAreaMu = totalAreaM2 / MU_TO_SQM
  const centroid = {
    lng: areas.reduce((sum, area) => sum + area.centroidLng, 0) / areas.length,
    lat: areas.reduce((sum, area) => sum + area.centroidLat, 0) / areas.length
  }
  const rawCount = Math.ceil(totalAreaMu / SINGLE_DRONE_AREA_LIMIT_MU)
  // 架数不超过在线机巢数，也不超过区域数（保证每架至少分到一个区域）
  const droneCount = Math.min(Math.max(rawCount, 1), docks.length, areas.length)
  const degraded = rawCount > docks.length
  const chosenDocks = [...docks]
    .sort((a, b) => distanceM(centroid, a) - distanceM(centroid, b))
    .slice(0, droneCount)

  const assignments: AutoTaskAssignment[] = chosenDocks.map(dock => ({
    sn: dock.sn,
    nickname: dock.nickname,
    areaIds: [],
    areaNames: [],
    distanceM: 0
  }))

  // 就近分配：每个区域分给距离最近的机巢
  areas.forEach(area => {
    const position = { lng: area.centroidLng, lat: area.centroidLat }
    let bestIndex = 0
    let bestDistance = Infinity
    chosenDocks.forEach((dock, index) => {
      const distance = distanceM(position, dock)
      if (distance < bestDistance) {
        bestDistance = distance
        bestIndex = index
      }
    })
    assignments[bestIndex].areaIds.push(area.id)
    assignments[bestIndex].areaNames.push(area.name)
  })

  // 兜底：就近分配可能让个别机巢分不到区域，从区域最多的分组匀出距其机巢最远的一个
  assignments.forEach((assignment, index) => {
    while (assignment.areaIds.length === 0) {
      let donorIndex = -1
      assignments.forEach((candidate, candidateIndex) => {
        if (candidateIndex !== index && candidate.areaIds.length > (donorIndex < 0 ? 0 : assignments[donorIndex].areaIds.length)) {
          donorIndex = candidateIndex
        }
      })
      if (donorIndex < 0 || assignments[donorIndex].areaIds.length < 2) break
      const donor = assignments[donorIndex]
      const donorDock = chosenDocks[donorIndex]
      let farthestArea: AreaGeometry | null = null
      let farthestDistance = -1
      for (const area of areas) {
        if (!donor.areaIds.includes(area.id)) continue
        const distance = distanceM({ lng: area.centroidLng, lat: area.centroidLat }, donorDock)
        if (distance > farthestDistance) {
          farthestDistance = distance
          farthestArea = area
        }
      }
      if (!farthestArea) break
      const movedArea = farthestArea
      donor.areaIds = donor.areaIds.filter(id => id !== movedArea.id)
      donor.areaNames = donor.areaNames.filter(name => name !== movedArea.name)
      assignment.areaIds.push(movedArea.id)
      assignment.areaNames.push(movedArea.name)
    }
  })

  // 统一重算每个机巢到负责区域的最远距离（最差转场距离）
  assignments.forEach((assignment, index) => {
    const dock = chosenDocks[index]
    assignment.distanceM = areas
      .filter(area => assignment.areaIds.includes(area.id))
      .reduce((max, area) => Math.max(max, distanceM({ lng: area.centroidLng, lat: area.centroidLat }, dock)), 0)
  })

  const groupMinutes = assignments.map(assignment => {
    const groupAreaM2 = areas
      .filter(area => assignment.areaIds.includes(area.id))
      .reduce((sum, area) => sum + area.areaM2, 0)
    const routeLengthM = groupAreaM2 / EFFECTIVE_SWATH_M
    return SORTIE_OVERHEAD_MIN + routeLengthM / DRONE_SPEED_MPS / 60
  })
  const estimatedMinutes = Math.max(1, Math.ceil(Math.max(...groupMinutes)))

  return {
    mode: droneCount === 1 ? 'single' : 'multi',
    droneCount,
    totalAreaMu,
    totalAreaM2,
    estimatedMinutes,
    degraded,
    assignments
  }
}
