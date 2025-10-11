// voxelNav.js (WGS84 原点 + 调试日志版)
import * as Cesium from 'cesium'

/* ================== 调试开关 ================== */
let DEBUG = true
export function setVoxelDebug (flag) { DEBUG = !!flag }
const dlog = (...a) => { if (DEBUG) console.log('[voxelNav]', ...a) }
const dwarn = (...a) => { if (DEBUG) console.warn('[voxelNav]', ...a) }
const dgroup = (label) => { if (DEBUG) console.group(label) }
const dgroupEnd = () => { if (DEBUG) console.groupEnd() }

/* ============ 体素加载：origin 为 WGS84 [lon,lat,h] ============ */
export async function loadVoxel (outDir) {
  const h = await (await fetch(`${outDir}/header.json`)).json()
  const buf = await (await fetch(`${outDir}/occupancy.bin`)).arrayBuffer()
  const occ = new Uint8Array(buf)

  const [lon0, lat0, h0] = h.anchorLLH
  const anchorECEF = Cesium.Cartesian3.fromDegrees(lon0, lat0, h0)
  const enuFrame44 = Cesium.Transforms.eastNorthUpToFixedFrame(anchorECEF)
  const enu2ecef33 = Cesium.Matrix4.getMatrix3(enuFrame44, new Cesium.Matrix3())
  const ecef2enu33 = Cesium.Matrix3.transpose(enu2ecef33, new Cesium.Matrix3())

  const header = {
    anchorLLH: { lon: lon0, lat: lat0, h: h0 },
    anchorECEF,
    enu2ecef33,
    ecef2enu33,
    gridOriginENU: new Cesium.Cartesian3(
      h.gridOriginENU[0],
      h.gridOriginENU[1],
      h.gridOriginENU[2]
    ),
    voxel: Number(h.voxelSize),
    dims: h.dims
  }

  return { header, occ }
}

/* ================== 基础工具 ================== */
function linIdx (i, j, k, dims) {
  const [nx, ny, nz] = dims
  return i * (ny * nz) + j * nz + k
}
function inBounds (i, j, k, dims) {
  const [nx, ny, nz] = dims
  return i >= 0 && j >= 0 && k >= 0 && i < nx && j < ny && k < nz
}

/* ============ 坐标换算（WGS84 航点 ⇄ 体素） ============ */
export function wgs84ToVoxelIndex (lon, lat, height, header) {
  // 1) WGS84 → ECEF
  const pECEF = Cesium.Cartesian3.fromDegrees(lon, lat, height)

  // 2) ECEF → ENU（相对 anchor）
  const deltaECEF = Cesium.Cartesian3.subtract(
    pECEF,
    header.anchorECEF,
    new Cesium.Cartesian3()
  )
  const enu = Cesium.Matrix3.multiplyByVector(
    header.ecef2enu33,
    deltaECEF,
    new Cesium.Cartesian3()
  )

  // 3) 扣掉 gridOriginENU → 相对体素网格
  const rel = Cesium.Cartesian3.subtract(
    enu,
    header.gridOriginENU,
    new Cesium.Cartesian3()
  )

  // 4) 落格
  const v = header.voxel
  const ix = Math.floor(rel.x / v)
  const iy = Math.floor(rel.y / v)
  const iz = Math.floor(rel.z / v)

  return [ix, iy, iz]
}

export function gridToWGS84Center (i, j, k, header) {
  const v = header.voxel
  // ENU 坐标 = gridOriginENU + index*voxel
  const enu = new Cesium.Cartesian3(
    header.gridOriginENU.x + (i + 0.5) * v,
    header.gridOriginENU.y + (j + 0.5) * v,
    header.gridOriginENU.z + (k + 0.5) * v
  )

  // ENU → ECEF → WGS84
  const dECEF = Cesium.Matrix3.multiplyByVector(
    header.enu2ecef33,
    enu,
    new Cesium.Cartesian3()
  )
  const pECEF = Cesium.Cartesian3.add(
    header.anchorECEF,
    dECEF,
    new Cesium.Cartesian3()
  )
  const carto = Cesium.Cartographic.fromCartesian(pECEF)

  return {
    longitude: Cesium.Math.toDegrees(carto.longitude),
    latitude: Cesium.Math.toDegrees(carto.latitude),
    height: carto.height
  }
}

/* ============ 3D Bresenham 直线采样 ============ */
export function * bresenham3D (a, b) {
  let [x0, y0, z0] = a.map(Math.trunc)
  const [x1, y1, z1] = b.map(Math.trunc)
  const dx = Math.abs(x1 - x0); const dy = Math.abs(y1 - y0); const dz = Math.abs(z1 - z0)
  const xs = x1 > x0 ? 1 : -1; const ys = y1 > y0 ? 1 : -1; const zs = z1 > z0 ? 1 : -1

  if (dx >= dy && dx >= dz) {
    let p1 = 2 * dy - dx; let p2 = 2 * dz - dx
    for (let i = 0; i <= dx; i++) {
      yield [x0, y0, z0]
      if (p1 >= 0) { y0 += ys; p1 -= 2 * dx }
      if (p2 >= 0) { z0 += zs; p2 -= 2 * dx }
      p1 += 2 * dy; p2 += 2 * dz; x0 += xs
    }
  } else if (dy >= dx && dy >= dz) {
    let p1 = 2 * dx - dy; let p2 = 2 * dz - dy
    for (let i = 0; i <= dy; i++) {
      yield [x0, y0, z0]
      if (p1 >= 0) { x0 += xs; p1 -= 2 * dy }
      if (p2 >= 0) { z0 += zs; p2 -= 2 * dy }
      p1 += 2 * dx; p2 += 2 * dz; y0 += ys
    }
  } else {
    let p1 = 2 * dy - dz; let p2 = 2 * dx - dz
    for (let i = 0; i <= dz; i++) {
      yield [x0, y0, z0]
      if (p1 >= 0) { y0 += ys; p1 -= 2 * dz }
      if (p2 >= 0) { x0 += xs; p2 -= 2 * dz }
      p1 += 2 * dy; p2 += 2 * dx; z0 += zs
    }
  }
}

export function segmentCollides (header, occ, aIdx, bIdx) {
  dgroup('segmentCollides()')
  dlog('A idx =', aIdx, 'B idx =', bIdx, 'dims =', header.dims)
  let steps = 0; let outOfBounds = 0; let hits = 0; let firstHit = null; let firstInBounds = null; let lastInBounds = null

  for (const [i, j, k] of bresenham3D(aIdx, bIdx)) {
    steps++
    if (!inBounds(i, j, k, header.dims)) { outOfBounds++; continue }
    if (!firstInBounds) firstInBounds = [i, j, k]
    lastInBounds = [i, j, k]
    const v = occ[linIdx(i, j, k, header.dims)]
    if (v !== 0) { hits++; if (!firstHit) firstHit = [i, j, k] }
  }

  dlog('samples =', steps, 'inBounds =', steps - outOfBounds, 'outOfBounds =', outOfBounds)
  if (firstInBounds) dlog('first inBounds idx =', firstInBounds)
  if (lastInBounds) dlog('last  inBounds idx =', lastInBounds)
  if (firstHit) {
    dlog('HIT at idx =', firstHit)
    const w = gridToWGS84Center(firstHit[0], firstHit[1], firstHit[2], header)
    dlog('HIT center WGS84 ≈', w)
  } else {
    dlog('no voxel hit on line')
  }
  dgroupEnd()

  return hits > 0
}

/* ============ 局部 A*（在[a,b]附近盒子里搜） ============ */
function neighbors26 (i, j, k) {
  const out = []
  for (let dx = -1; dx <= 1; dx++) {
    for (let dy = -1; dy <= 1; dy++) {
      for (let dz = -1; dz <= 1; dz++) {
        if (dx === 0 && dy === 0 && dz === 0) continue
        out.push([i + dx, j + dy, k + dz, Math.hypot(dx, dy, dz)])
      }
    }
  }
  return out
}

export function astarLocal (header, occ, aIdx, bIdx, marginVox = 10) {
  const [nx, ny, nz] = header.dims
  const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v))

  const ix0 = clamp(Math.min(aIdx[0], bIdx[0]) - marginVox, 0, nx - 1)
  const iy0 = clamp(Math.min(aIdx[1], bIdx[1]) - marginVox, 0, ny - 1)
  const iz0 = clamp(Math.min(aIdx[2], bIdx[2]) - marginVox, 0, nz - 1)
  const ix1 = clamp(Math.max(aIdx[0], bIdx[0]) + marginVox, 0, nx - 1)
  const iy1 = clamp(Math.max(aIdx[1], bIdx[1]) + marginVox, 0, ny - 1)
  const iz1 = clamp(Math.max(aIdx[2], bIdx[2]) + marginVox, 0, nz - 1)

  const sx = ix1 - ix0 + 1; const sy = iy1 - iy0 + 1; const sz = iz1 - iz0 + 1
  const subSize = sx * sy * sz
  const subIdx = (i, j, k) => (i - ix0) * (sy * sz) + (j - iy0) * sz + (k - iz0)
  const subIn = (i, j, k) => (i >= ix0 && j >= iy0 && k >= iz0 && i <= ix1 && j <= iy1 && k <= iz1)

  const isOcc = (i, j, k) => !subIn(i, j, k) || occ[linIdx(i, j, k, header.dims)] !== 0

  dgroup('astarLocal()')
  dlog('local box idx =', { ix0, iy0, iz0, ix1, iy1, iz1, size: [sx, sy, sz], subSize })
  dlog('start idx =', aIdx, 'occupied? =', isOcc(aIdx[0], aIdx[1], aIdx[2]), 'inBox =', subIn(aIdx[0], aIdx[1], aIdx[2]))
  dlog('goal  idx =', bIdx, 'occupied? =', isOcc(bIdx[0], bIdx[1], bIdx[2]), 'inBox =', subIn(bIdx[0], bIdx[1], bIdx[2]))

  if (isOcc(aIdx[0], aIdx[1], aIdx[2]) || isOcc(bIdx[0], bIdx[1], bIdx[2])) {
    dwarn('start/goal occupied or out-of-box, abort A*')
    dgroupEnd()
    return null
  }

  const g = new Float32Array(subSize); g.fill(Infinity)
  const visited = new Uint8Array(subSize)
  const came = new Int32Array(subSize * 3); came.fill(-1)
  const B = subIdx(bIdx[0], bIdx[1], bIdx[2])
  const h = (i, j, k) => Math.hypot(i - bIdx[0], j - bIdx[1], k - bIdx[2])

  const heap = []
  const push = (f, gi, i, j, k) => heap.push([f, gi, i, j, k])
  const pop = () => { let m = 0; for (let t = 1; t < heap.length; t++) if (heap[t][0] < heap[m][0]) m = t; const v = heap[m]; heap.splice(m, 1); return v }

  const A = subIdx(aIdx[0], aIdx[1], aIdx[2])
  g[A] = 0; push(h(aIdx[0], aIdx[1], aIdx[2]), 0, aIdx[0], aIdx[1], aIdx[2])

  let expansions = 0
  while (heap.length) {
    const [, gi, i, j, k] = pop()
    expansions++
    const L = subIdx(i, j, k)
    if (visited[L]) continue
    visited[L] = 1

    if (L === B) {
      const path = [[i, j, k]]
      let ci = came[L * 3]; let cj = came[L * 3 + 1]; let ck = came[L * 3 + 2]
      while (ci !== -1) {
        path.push([ci, cj, ck])
        const L2 = subIdx(ci, cj, ck)
        ci = came[L2 * 3]; cj = came[L2 * 3 + 1]; ck = came[L2 * 3 + 2]
      }
      path.reverse()
      dlog('A* success: expansions =', expansions, 'path voxels =', path.length)
      dgroupEnd()
      return path
    }

    for (let dx = -1; dx <= 1; dx++) {
      for (let dy = -1; dy <= 1; dy++) {
        for (let dz = -1; dz <= 1; dz++) {
          if (dx === 0 && dy === 0 && dz === 0) continue
          const ni = i + dx; const nj = j + dy; const nk = k + dz
          if (!subIn(ni, nj, nk)) continue
          if (isOcc(ni, nj, nk)) continue
          const L2 = subIdx(ni, nj, nk)
          const step = Math.hypot(dx, dy, dz)
          const cand = gi + step
          if (cand < g[L2]) {
            g[L2] = cand
            came[L2 * 3] = i; came[L2 * 3 + 1] = j; came[L2 * 3 + 2] = k
            const f = cand + h(ni, nj, nk)
            push(f, cand, ni, nj, nk)
          }
        }
      }
    }
  }
  dwarn('A* failed: no path')
  dgroupEnd()
  return null
}

/* ============ 首次拐点（返回 WGS84） ============ */
export function firstBendWaypoint (pathIdx, header) {
  if (!pathIdx || pathIdx.length < 3) return null
  const dir = (a, b) => [b[0] - a[0], b[1] - a[1], b[2] - a[2]]
  const d0 = dir(pathIdx[0], pathIdx[1]).toString()
  for (let t = 1; t < pathIdx.length - 1; t++) {
    const d1 = dir(pathIdx[t], pathIdx[t + 1]).toString()
    if (d1 !== d0) {
      const [i, j, k] = pathIdx[t]
      const llh = gridToWGS84Center(i, j, k, header)
      dlog('first bend at idx =', [i, j, k], 'WGS84 ≈', llh)
      return llh
    }
  }
  dlog('path straight or length < 3 → no bend')
  return null
}

/* ============ 高层：检查并插入避障点（输入/输出 WGS84） ============ */
export function checkAndInsertAvoidanceBetweenTwoPoints ({ header, occ, ptA, ptB, marginVox = 10 }) {
  dgroup('check segment A->B')
  dlog('A (WGS84) =', ptA)
  dlog('B (WGS84) =', ptB)

  const aIdx = wgs84ToVoxelIndex(ptA.longitude, ptA.latitude, ptA.height, header)
  const bIdx = wgs84ToVoxelIndex(ptB.longitude, ptB.latitude, ptB.height, header)

  // 先给出越界提示
  const aIn = inBounds(aIdx[0], aIdx[1], aIdx[2], header.dims)
  const bIn = inBounds(bIdx[0], bIdx[1], bIdx[2], header.dims)
  if (!aIn || !bIn) {
    dwarn('start or goal voxel index out of bounds:', { aIdx, aIn, bIdx, bIn, dims: header.dims })
  }

  const collided = segmentCollides(header, occ, aIdx, bIdx)
  if (!collided) {
    dlog('NO collision, keep straight.')
    dgroupEnd()
    return { collided: false, avoidLLH: null }
  }

  const path = astarLocal(header, occ, aIdx, bIdx, marginVox)
  if (!path) {
    dwarn('collision detected BUT no local path found.')
    dgroupEnd()
    return { collided: true, avoidLLH: null, noPath: true }
  }

  const avoidLLH = firstBendWaypoint(path, header)
  if (!avoidLLH) {
    dlog('path found but no bend → not inserting extra waypoint.')
    dgroupEnd()
    return { collided: true, avoidLLH: null, degenerate: true }
  }

  dlog('INSERT avoid waypoint =', avoidLLH)
  dgroupEnd()
  return { collided: true, avoidLLH }
}
