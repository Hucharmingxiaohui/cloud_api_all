import { wgs84togcj02 } from '/@/vendors/coordtransform'
import type { FlightAirspaceConfig } from '/@/types/runtime-config'

/** 展示图层独立于施工区域、绘制工具和航线校验。返回销毁函数。 */
export function addFlightAirspace (AMap: any, map: any, container: HTMLElement | null, config?: FlightAirspaceConfig) {
  if (!config?.enabled || !container || config.coordinateSystem !== 'WGS84' || !Array.isArray(config.points) || config.points.length < 3 ||
    config.points.some(p => !Array.isArray(p) || p.length !== 2 || !p.every(Number.isFinite) || Math.abs(p[0]) > 180 || Math.abs(p[1]) > 90)) return () => {}
  const path = config.points.map(p => wgs84togcj02(p[0], p[1]))
  const polygon = new AMap.Polygon({ path, strokeColor: '#ffd166', strokeWeight: 3, strokeStyle: 'dashed', fillColor: '#ffd166', fillOpacity: 0.035, zIndex: 105, bubble: true })
  const markers = path.map((position, i) => new AMap.Marker({
    position,
    zIndex: 106,
    bubble: true,
    offset: new AMap.Pixel(-10, -10),
    title: `空域边界点${i + 1} · WGS84 ${config.points[i].join(', ')}`,
    content: `<span style="display:block;width:20px;height:20px;text-align:center;line-height:20px;background:#543d14;color:#ffe3a1;border:1px solid #ffd166;border-radius:50%;font-size:11px;pointer-events:none">${i + 1}</span>`
  }))
  const overlays = [polygon, ...markers]
  map.add(overlays)
  const panel = document.createElement('div')
  panel.style.cssText = 'position:absolute;left:12px;bottom:35px;z-index:200;padding:8px 10px;border:1px solid #ffd166;border-radius:5px;background:rgba(7,30,54,.94);color:#ffe3a1;font-size:12px;display:flex;gap:10px;align-items:center;max-width:calc(100% - 24px);flex-wrap:wrap;'
  const label = document.createElement('label')
  const checkbox = document.createElement('input'); checkbox.type = 'checkbox'; checkbox.checked = true
  label.append(checkbox, document.createTextNode(` ${config.name || '作业空域'} · ${config.heightLabel || ''}`))
  const fit = document.createElement('button'); fit.type = 'button'; fit.textContent = '查看全域'
  fit.style.cssText = 'border:1px solid #ffd166;background:transparent;color:#ffe3a1;border-radius:3px;cursor:pointer;padding:2px 6px;'
  checkbox.onchange = () => { if (checkbox.checked) map.add(overlays); else map.remove(overlays) }
  fit.onclick = () => { if (!checkbox.checked) { checkbox.checked = true; map.add(overlays) }; map.setFitView([polygon], false, [65, 65, 80, 65]) }
  panel.append(label, fit)
  const stop = (event: Event) => event.stopPropagation()
  const events = ['click', 'dblclick', 'mousedown', 'pointerdown', 'touchstart', 'wheel']
  events.forEach(event => panel.addEventListener(event, stop))
  container.appendChild(panel)
  return () => { checkbox.onchange = null; fit.onclick = null; events.forEach(event => panel.removeEventListener(event, stop)); panel.remove(); map.remove(overlays) }
}
