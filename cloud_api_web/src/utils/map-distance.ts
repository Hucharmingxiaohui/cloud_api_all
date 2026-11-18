import { gcj02towgs84 } from '/@/vendors/coordtransform'

/** 两点地表水平距离（米），不包含地形起伏或高度差。 */
export function horizontalDistance (a: number[], b: number[]) {
  const rad = Math.PI / 180
  const h = Math.sin((b[1] - a[1]) * rad / 2) ** 2 + Math.cos(a[1] * rad) * Math.cos(b[1] * rad) * Math.sin((b[0] - a[0]) * rad / 2) ** 2
  return 2 * 6371008.8 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))))
}
export function addMapDistance (AMap: any, map: any, container: HTMLElement | null, blocked = () => false) {
  if (!container) return () => {}
  let active = false
  let queryHeight = false; let epoch = 0
  let points: number[][] = []
  let overlays: any[] = []
  let cursor = ''
  const panel = document.createElement('div')
  panel.style.cssText = 'position:absolute;right:12px;bottom:35px;z-index:210;display:flex;gap:8px;align-items:center;flex-wrap:wrap;max-width:90%;padding:8px 10px;background:#071e36ee;border:1px solid #53d9ff;border-radius:5px;color:#dff8ff;font-size:12px;'
  const start = document.createElement('button'); start.type = 'button'; start.textContent = '测距'
  const clear = document.createElement('button'); clear.type = 'button'; clear.textContent = '清除'
  const status = document.createElement('span'); status.textContent = '两点直线距离'
  status.setAttribute('aria-live', 'polite')
  for (const button of [start, clear]) button.style.cssText = 'cursor:pointer;border:1px solid #53d9ff;border-radius:3px;background:transparent;color:#dff8ff;padding:3px 8px;'
  panel.append(start, clear, status)
  const elevation = document.createElement('button'); elevation.type = 'button'; elevation.textContent = '查椭球高'; elevation.style.cssText = start.style.cssText
  panel.append(elevation)
  container.appendChild(panel)
  function stop () { if (active) container!.style.cursor = cursor; active = false; start.textContent = points.length === 2 ? '重新测距' : '测距' }
  function reset () { epoch++; stop(); queryHeight = false; map.remove(overlays); overlays = []; points = []; start.textContent = '测距'; elevation.textContent = '查椭球高'; status.textContent = '两点直线距离' }
  elevation.onclick = () => {
    if (queryHeight) { reset(); return }
    if (blocked()) { status.textContent = '请先结束编辑或选点，再查高程'; return }
    reset(); queryHeight = true; cursor = container.style.cursor; container.style.cursor = 'crosshair'; active = true
    elevation.textContent = '取消查高程'; status.textContent = '点击地图查询地表椭球高（Esc取消）'
  }
  start.onclick = () => {
    if (active) { reset(); return }
    if (blocked()) { status.textContent = '请先结束编辑或选点，再测距'; return }
    reset(); cursor = container.style.cursor; container.style.cursor = 'crosshair'; active = true
    start.textContent = '取消测距'; status.textContent = '请点击起点（Esc取消）'
  }
  clear.onclick = reset
  const consume = (event: Event) => { event.stopImmediatePropagation() }
  function click (event: MouseEvent) {
    if (panel.contains(event.target as Node) || !active) return
    consume(event)
    if (blocked()) { reset(); return }
    const rect = container!.getBoundingClientRect()
    const p = map.containerToLngLat(new AMap.Pixel((event.clientX - rect.left) * container!.clientWidth / rect.width, (event.clientY - rect.top) * container!.clientHeight / rect.height))
    if (!p || !Number.isFinite(p.lng) || !Number.isFinite(p.lat)) return
    points.push([p.lng, p.lat])
    if (queryHeight) {
      const token = ++epoch; const coordinate = gcj02towgs84(p.lng, p.lat)
      const marker = new AMap.Marker({ position: [p.lng, p.lat], zIndex: 220, title: '地表高程查询' }); overlays.push(marker); map.add(marker)
      stop(); queryHeight = false; elevation.textContent = '查椭球高'; status.textContent = '正在读取高程…'
      Promise.all([import('./terrain-elevation'), import('./elevation-profile')]).then(async ([loader, sampler]) => {
        const grid = await loader.loadElevation(); const h = sampler.sampleElevation(grid, coordinate[0], coordinate[1])
        if (token === epoch) status.textContent = h === null ? '该点高程未知（覆盖外或 NoData）' : `地表椭球高：${h.toFixed(1)} m · WGS84 · 约30米地表模型，仅供复核`
      }).catch(error => { if (token === epoch) status.textContent = error instanceof Error ? error.message : '高程读取失败' })
      return
    }
    const marker = new AMap.Marker({ position: points[points.length - 1], zIndex: 220, offset: new AMap.Pixel(-12, -12), content: `<span style="display:block;width:24px;height:24px;line-height:24px;text-align:center;border-radius:50%;background:#087c98;color:white;pointer-events:none">${points.length === 1 ? '起' : '终'}</span>` })
    overlays.push(marker); map.add(marker)
    if (points.length === 1) { status.textContent = '请点击终点（Esc取消）'; return }
    const distance = horizontalDistance(gcj02towgs84(points[0][0], points[0][1]), gcj02towgs84(points[1][0], points[1][1]))
    const result = distance >= 1000 ? `${(distance / 1000).toFixed(3)} 千米` : `${distance.toFixed(1)} 米`
    const line = new AMap.Polyline({ path: points, strokeColor: '#53d9ff', strokeWeight: 3, zIndex: 219, bubble: true })
    overlays.push(line); map.add(line)
    status.textContent = `直线距离：${result}（水平距离）`
    stop()
  }
  const intercept = (event: Event) => { if (active && !panel.contains(event.target as Node)) consume(event) }
  const keys = (event: KeyboardEvent) => { if (event.key === 'Escape' && active) { reset(); event.stopPropagation() } }
  const events = ['pointerdown', 'pointerup', 'mousedown', 'mouseup', 'dblclick', 'touchstart', 'touchend']
  events.forEach(name => container.addEventListener(name, intercept, true))
  container.addEventListener('click', click, true)
  const panelEvents = ['click', 'dblclick', 'pointerdown', 'pointerup', 'mousedown', 'mouseup', 'touchstart', 'touchend', 'wheel']
  panelEvents.forEach(name => panel.addEventListener(name, consume))
  window.addEventListener('keydown', keys)
  return () => {
    reset(); start.onclick = null; clear.onclick = null; elevation.onclick = null
    events.forEach(name => container.removeEventListener(name, intercept, true))
    container.removeEventListener('click', click, true)
    panelEvents.forEach(name => panel.removeEventListener(name, consume))
    window.removeEventListener('keydown', keys); panel.remove()
  }
}
