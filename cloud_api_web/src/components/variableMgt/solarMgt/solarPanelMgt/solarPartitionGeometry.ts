export interface PixelPoint {
  x: number
  y: number
}

export interface DetectedPanelGeometry {
  id: string
  name: string
  points: PixelPoint[]
  center: PixelPoint
}

const EPSILON = 1e-8

function numberValue (value: unknown): number {
  if (value === null || value === undefined || value === '') return NaN
  return Number(value)
}

function pickNumber (source: Record<string, any>, camel: string, snake: string): number {
  return numberValue(source[camel] !== undefined ? source[camel] : source[snake])
}

function signedArea (points: PixelPoint[]): number {
  return points.reduce((sum, point, index) => {
    const next = points[(index + 1) % points.length]
    return sum + point.x * next.y - next.x * point.y
  }, 0) / 2
}

function sortClockwise (points: PixelPoint[]): PixelPoint[] {
  const center = getCenter(points)
  const sorted = [...points].sort((a, b) =>
    Math.atan2(a.y - center.y, a.x - center.x) - Math.atan2(b.y - center.y, b.x - center.x)
  )
  if (signedArea(sorted) < 0) sorted.reverse()
  const start = sorted.reduce((best, point, index) => {
    const current = sorted[best]
    return point.y < current.y || (point.y === current.y && point.x < current.x) ? index : best
  }, 0)
  return sorted.slice(start).concat(sorted.slice(0, start))
}

export function getCenter (points: PixelPoint[]): PixelPoint {
  const total = points.reduce((sum, point) => ({ x: sum.x + point.x, y: sum.y + point.y }), { x: 0, y: 0 })
  return { x: total.x / points.length, y: total.y / points.length }
}

export function normalizeDetectedPanel (source: Record<string, any>, index: number): DetectedPanelGeometry | null {
  const points = [1, 2, 3, 4].map(corner => ({
    x: pickNumber(source, `corner${corner}Col`, `corner${corner}_col`),
    y: pickNumber(source, `corner${corner}Row`, `corner${corner}_row`)
  }))
  if (points.some(point => !Number.isFinite(point.x) || !Number.isFinite(point.y))) return null
  const orderedPoints = sortClockwise(points)
  if (Math.abs(signedArea(orderedPoints)) < EPSILON) return null
  return {
    id: String(source.id ?? index),
    name: String(source.solarPanelName ?? source.solar_panel_name ?? `光伏板${index + 1}`),
    points: orderedPoints,
    center: getCenter(orderedPoints)
  }
}

function targetGroupSizes (itemCount: number, groupCount: number): number[] {
  const baseSize = Math.floor(itemCount / groupCount)
  const remainder = itemCount % groupCount
  return Array.from({ length: groupCount }, (_, index) => baseSize + (index < remainder ? 1 : 0))
}

function squaredDistance (a: PixelPoint, b: PixelPoint): number {
  const dx = a.x - b.x
  const dy = a.y - b.y
  return dx * dx + dy * dy
}

function groupCenter (panels: DetectedPanelGeometry[]): PixelPoint {
  return getCenter(panels.map(panel => panel.center))
}

function farthestPair (panels: DetectedPanelGeometry[]): [DetectedPanelGeometry, DetectedPanelGeometry] {
  const farthestFrom = (source: DetectedPanelGeometry) => {
    let farthest = panels[0]
    let maxDistance = -1
    panels.forEach(panel => {
      const distance = squaredDistance(source.center, panel.center)
      if (distance > maxDistance) {
        maxDistance = distance
        farthest = panel
      }
    })
    return farthest
  }
  const first = farthestFrom(panels[0])
  const second = farthestFrom(first)
  return [first, second]
}

// 在严格保持左右组容量的前提下，用双中心迭代寻找空间上最紧凑的切分。
function splitBalanced (
  panels: DetectedPanelGeometry[],
  leftItemCount: number
): [DetectedPanelGeometry[], DetectedPanelGeometry[]] {
  const [firstSeed, secondSeed] = farthestPair(panels)
  let leftCenter = firstSeed.center
  let rightCenter = secondSeed.center
  let left: DetectedPanelGeometry[] = []
  let right: DetectedPanelGeometry[] = []
  let previousSignature = ''

  for (let iteration = 0; iteration < 30; iteration++) {
    const ranked = [...panels].sort((a, b) => {
      const scoreA = squaredDistance(a.center, leftCenter) - squaredDistance(a.center, rightCenter)
      const scoreB = squaredDistance(b.center, leftCenter) - squaredDistance(b.center, rightCenter)
      if (Math.abs(scoreA - scoreB) > EPSILON) return scoreA - scoreB
      return a.id.localeCompare(b.id)
    })
    left = ranked.slice(0, leftItemCount)
    right = ranked.slice(leftItemCount)
    const signature = left.map(panel => panel.id).sort().join('|')
    if (signature === previousSignature) break
    previousSignature = signature
    leftCenter = groupCenter(left)
    rightCenter = groupCenter(right)
  }

  return [left, right]
}

function splitGroups (
  panels: DetectedPanelGeometry[],
  sizes: number[]
): DetectedPanelGeometry[][] {
  if (sizes.length === 1) return [panels]
  const leftGroupCount = Math.ceil(sizes.length / 2)
  const leftSizes = sizes.slice(0, leftGroupCount)
  const rightSizes = sizes.slice(leftGroupCount)
  const leftItemCount = leftSizes.reduce((sum, size) => sum + size, 0)
  const [leftPanels, rightPanels] = splitBalanced(panels, leftItemCount)
  return [
    ...splitGroups(leftPanels, leftSizes),
    ...splitGroups(rightPanels, rightSizes)
  ]
}

export function partitionPanelsBalanced (
  panels: DetectedPanelGeometry[],
  groupCount: number
): DetectedPanelGeometry[][] {
  if (!Number.isInteger(groupCount) || groupCount < 1 || groupCount > panels.length) return []
  return splitGroups(panels, targetGroupSizes(panels.length, groupCount))
}

function cross (origin: PixelPoint, a: PixelPoint, b: PixelPoint): number {
  return (a.x - origin.x) * (b.y - origin.y) - (a.y - origin.y) * (b.x - origin.x)
}

export function convexHull (points: PixelPoint[]): PixelPoint[] {
  const unique = Array.from(new Map(points.map(point => [`${point.x},${point.y}`, point])).values())
    .sort((a, b) => a.x - b.x || a.y - b.y)
  if (unique.length <= 2) return unique
  const lower: PixelPoint[] = []
  unique.forEach(point => {
    while (lower.length >= 2 && cross(lower[lower.length - 2], lower[lower.length - 1], point) <= 0) lower.pop()
    lower.push(point)
  })
  const upper: PixelPoint[] = []
  for (let index = unique.length - 1; index >= 0; index--) {
    const point = unique[index]
    while (upper.length >= 2 && cross(upper[upper.length - 2], upper[upper.length - 1], point) <= 0) upper.pop()
    upper.push(point)
  }
  lower.pop()
  upper.pop()
  return lower.concat(upper)
}

function axisAlignedRectangle (points: PixelPoint[]): PixelPoint[] {
  const minX = Math.min(...points.map(point => point.x))
  const maxX = Math.max(...points.map(point => point.x))
  const minY = Math.min(...points.map(point => point.y))
  const maxY = Math.max(...points.map(point => point.y))
  return sortClockwise([
    { x: minX, y: minY },
    { x: maxX, y: minY },
    { x: maxX, y: maxY },
    { x: minX, y: maxY }
  ])
}

export function minimumAreaRectangle (points: PixelPoint[]): PixelPoint[] {
  if (!points.length) return []
  const hull = convexHull(points)
  if (hull.length < 3) return axisAlignedRectangle(points)
  let bestArea = Infinity
  let bestRectangle: PixelPoint[] = []
  for (let index = 0; index < hull.length; index++) {
    const current = hull[index]
    const next = hull[(index + 1) % hull.length]
    const angle = Math.atan2(next.y - current.y, next.x - current.x)
    const cosine = Math.cos(angle)
    const sine = Math.sin(angle)
    let minX = Infinity
    let maxX = -Infinity
    let minY = Infinity
    let maxY = -Infinity
    hull.forEach(point => {
      const rotatedX = point.x * cosine + point.y * sine
      const rotatedY = -point.x * sine + point.y * cosine
      minX = Math.min(minX, rotatedX)
      maxX = Math.max(maxX, rotatedX)
      minY = Math.min(minY, rotatedY)
      maxY = Math.max(maxY, rotatedY)
    })
    const area = (maxX - minX) * (maxY - minY)
    if (area >= bestArea) continue
    bestArea = area
    bestRectangle = [
      { x: minX, y: minY },
      { x: maxX, y: minY },
      { x: maxX, y: maxY },
      { x: minX, y: maxY }
    ].map(point => ({
      x: point.x * cosine - point.y * sine,
      y: point.x * sine + point.y * cosine
    }))
  }
  return sortClockwise(bestRectangle)
}

export function getGroupRectangle (panels: DetectedPanelGeometry[]): PixelPoint[] {
  return minimumAreaRectangle(panels.flatMap(panel => panel.points))
}

export interface PanelPartitionRectangle {
  panels: DetectedPanelGeometry[]
  points: PixelPoint[]
}

export interface RectangleBounds {
  minX: number
  minY: number
  maxX: number
  maxY: number
}

interface ProjectionFrame {
  origin: PixelPoint
  axis: PixelPoint
  normal: PixelPoint
}

function dot (point: PixelPoint, axis: PixelPoint): number {
  return point.x * axis.x + point.y * axis.y
}

function getProjectionFrame (panels: DetectedPanelGeometry[]): ProjectionFrame | null {
  const rectangle = getGroupRectangle(panels)
  if (rectangle.length !== 4) return null
  const firstEdge = { x: rectangle[1].x - rectangle[0].x, y: rectangle[1].y - rectangle[0].y }
  const secondEdge = { x: rectangle[3].x - rectangle[0].x, y: rectangle[3].y - rectangle[0].y }
  const firstLength = Math.hypot(firstEdge.x, firstEdge.y)
  const secondLength = Math.hypot(secondEdge.x, secondEdge.y)
  const mainEdge = firstLength >= secondLength ? firstEdge : secondEdge
  const mainLength = Math.max(firstLength, secondLength)
  if (mainLength < EPSILON) return null
  const axis = { x: mainEdge.x / mainLength, y: mainEdge.y / mainLength }
  return {
    origin: rectangle[0],
    axis,
    normal: { x: -axis.y, y: axis.x }
  }
}

function fromProjection (frame: ProjectionFrame, along: number, across: number): PixelPoint {
  return {
    x: frame.origin.x + frame.axis.x * along + frame.normal.x * across,
    y: frame.origin.y + frame.axis.y * along + frame.normal.y * across
  }
}

interface PartitionCell {
  panels: DetectedPanelGeometry[]
  frame: ProjectionFrame
  uLo: number
  uHi: number
  vLo: number
  vHi: number
}

function cellToRectangle (cell: PartitionCell): PanelPartitionRectangle {
  const { frame } = cell
  return {
    panels: cell.panels,
    points: sortClockwise([
      fromProjection(frame, cell.uLo, cell.vLo),
      fromProjection(frame, cell.uHi, cell.vLo),
      fromProjection(frame, cell.uHi, cell.vHi),
      fromProjection(frame, cell.uLo, cell.vHi)
    ])
  }
}

function createPartitionCells (
  panels: DetectedPanelGeometry[],
  groupCount: number
): PartitionCell[] {
  // 每一层递归都用当前板集自己的最小面积矩形重建投影坐标系，
  // 保证切分方向与生成的矩形始终对齐这一层光伏板的实际排向，
  // 即使整片阵列由朝向/错位不同的多簇组成也不会出现歪斜矩形。
  const build = (items: DetectedPanelGeometry[], count: number): PartitionCell[] => {
    if (!items.length) return []
    const frame = getProjectionFrame(items)
    if (!frame) return []
    const project = (point: PixelPoint, direction: PixelPoint) => dot({
      x: point.x - frame.origin.x,
      y: point.y - frame.origin.y
    }, direction)
    const projectionRange = (direction: PixelPoint) => {
      const values = items.flatMap(panel => panel.points).map(point => project(point, direction))
      return { min: Math.min(...values), max: Math.max(...values) }
    }
    const rangeU = projectionRange(frame.axis)
    const rangeV = projectionRange(frame.normal)
    if (count <= 1) {
      return [{ panels: items, frame, uLo: rangeU.min, uHi: rangeU.max, vLo: rangeV.min, vHi: rangeV.max }]
    }

    const direction = rangeU.max - rangeU.min >= rangeV.max - rangeV.min ? frame.axis : frame.normal
    const sorted = [...items].sort((a, b) => {
      const difference = project(a.center, direction) - project(b.center, direction)
      return Math.abs(difference) > EPSILON ? difference : a.id.localeCompare(b.id)
    })
    const samePosition = (index: number) =>
      Math.abs(project(sorted[index - 1].center, direction) - project(sorted[index].center, direction)) <= EPSILON

    const sizes = targetGroupSizes(sorted.length, count)
    let cutIndex = sizes.slice(0, Math.ceil(count / 2)).reduce((sum, size) => sum + size, 0)
    // 切分点若落在同一排/列内部，就近移动到有空隙的位置，避免把一排板拆到两个区域。
    if (cutIndex < sorted.length && samePosition(cutIndex)) {
      let adjusted = -1
      for (let offset = 1; adjusted < 0 && (cutIndex - offset >= 1 || cutIndex + offset < sorted.length); offset++) {
        const before = cutIndex - offset
        const after = cutIndex + offset
        if (before >= 1 && !samePosition(before)) adjusted = before
        else if (after < sorted.length && !samePosition(after)) adjusted = after
      }
      if (adjusted > 0) cutIndex = adjusted
    }

    const leftGroups = Math.ceil(count / 2)
    return [
      ...build(sorted.slice(0, cutIndex), leftGroups),
      ...build(sorted.slice(cutIndex), count - leftGroups)
    ]
  }

  return build(panels, groupCount)
}

function polygonAxes (points: PixelPoint[]): PixelPoint[] {
  const axes: PixelPoint[] = []
  for (let index = 0; index < points.length; index++) {
    const current = points[index]
    const next = points[(index + 1) % points.length]
    const edge = { x: next.x - current.x, y: next.y - current.y }
    const length = Math.hypot(edge.x, edge.y)
    if (length < EPSILON) continue
    axes.push({ x: -edge.y / length, y: edge.x / length })
  }
  return axes
}

function polygonProjectionRange (points: PixelPoint[], axis: PixelPoint): [number, number] {
  let min = Infinity
  let max = -Infinity
  points.forEach(point => {
    const value = point.x * axis.x + point.y * axis.y
    min = Math.min(min, value)
    max = Math.max(max, value)
  })
  return [min, max]
}

function convexPolygonsOverlap (a: PixelPoint[], b: PixelPoint[]): boolean {
  const axes = [...polygonAxes(a), ...polygonAxes(b)]
  for (const axis of axes) {
    const [minA, maxA] = polygonProjectionRange(a, axis)
    const [minB, maxB] = polygonProjectionRange(b, axis)
    if (maxA <= minB || maxB <= minA) return false
  }
  return true
}

// 每个分区在自己的局部坐标系里四边外扩；若外扩后任意两区相交（SAT 检测），
// 整体退回不外扩，保证永远不会因留白而重叠。
function applyPartitionPadding (
  cells: PartitionCell[],
  padding: number
): PanelPartitionRectangle[] {
  const toRectangle = (cell: PartitionCell, distance: number) => cellToRectangle({
    ...cell,
    uLo: cell.uLo - distance,
    uHi: cell.uHi + distance,
    vLo: cell.vLo - distance,
    vHi: cell.vHi + distance
  })
  if (padding <= 0 || cells.length <= 1) return cells.map(cell => toRectangle(cell, Math.max(0, padding)))
  const padded = cells.map(cell => toRectangle(cell, padding))
  const hasOverlap = padded.some((rect, index) =>
    padded.some((other, otherIndex) =>
      otherIndex > index && convexPolygonsOverlap(rect.points, other.points)
    )
  )
  return hasOverlap ? cells.map(cell => toRectangle(cell, 0)) : padded
}

export function partitionPanelsIntoNonOverlappingRectangles (
  panels: DetectedPanelGeometry[],
  groupCount: number,
  padding: number,
  bounds?: RectangleBounds
): PanelPartitionRectangle[] {
  if (!Number.isInteger(groupCount) || groupCount < 1 || groupCount > panels.length) return []
  const create = (distance: number) => applyPartitionPadding(createPartitionCells(panels, groupCount), distance)
  if (!bounds || padding <= 0) return create(Math.max(0, padding))
  const unpadded = create(0)
  if (!unpadded.length || unpadded.some(partition => !isWithinBounds(partition.points, bounds))) return unpadded
  let low = 0
  let high = padding
  for (let iteration = 0; iteration < 24; iteration++) {
    const middle = (low + high) / 2
    if (create(middle).every(partition => isWithinBounds(partition.points, bounds))) low = middle
    else high = middle
  }
  return create(low)
}

function isWithinBounds (points: PixelPoint[], bounds: RectangleBounds): boolean {
  return points.every(point =>
    point.x >= bounds.minX - EPSILON &&
    point.x <= bounds.maxX + EPSILON &&
    point.y >= bounds.minY - EPSILON &&
    point.y <= bounds.maxY + EPSILON
  )
}

export function expandRectangle (points: PixelPoint[], padding: number, bounds?: RectangleBounds): PixelPoint[] {
  if (points.length !== 4 || padding <= 0) return points
  const edgeX = { x: points[1].x - points[0].x, y: points[1].y - points[0].y }
  const edgeY = { x: points[3].x - points[0].x, y: points[3].y - points[0].y }
  const edgeXLength = Math.hypot(edgeX.x, edgeX.y)
  const edgeYLength = Math.hypot(edgeY.x, edgeY.y)
  if (edgeXLength < EPSILON || edgeYLength < EPSILON) return points
  const unitX = { x: edgeX.x / edgeXLength, y: edgeX.y / edgeXLength }
  const unitY = { x: edgeY.x / edgeYLength, y: edgeY.y / edgeYLength }
  const expand = (distance: number) => [
    { x: points[0].x - unitX.x * distance - unitY.x * distance, y: points[0].y - unitX.y * distance - unitY.y * distance },
    { x: points[1].x + unitX.x * distance - unitY.x * distance, y: points[1].y + unitX.y * distance - unitY.y * distance },
    { x: points[2].x + unitX.x * distance + unitY.x * distance, y: points[2].y + unitX.y * distance + unitY.y * distance },
    { x: points[3].x - unitX.x * distance + unitY.x * distance, y: points[3].y - unitX.y * distance + unitY.y * distance }
  ]
  if (!bounds) return expand(padding)
  if (!isWithinBounds(points, bounds)) return points

  let low = 0
  let high = padding
  for (let iteration = 0; iteration < 24; iteration++) {
    const middle = (low + high) / 2
    if (isWithinBounds(expand(middle), bounds)) low = middle
    else high = middle
  }
  return expand(low)
}
