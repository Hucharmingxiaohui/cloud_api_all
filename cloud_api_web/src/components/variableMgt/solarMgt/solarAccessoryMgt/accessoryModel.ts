import type { AccessoryWaypoint, SolarAccessory } from '/@/api/solar-accessory'

export const deviceTypes = [
  { value: 'inverter', label: '逆变器' },
  { value: 'combiner', label: '汇流箱' },
  { value: 'transformer', label: '箱变' },
  { value: 'cabinet', label: '配电柜' },
  { value: 'tower', label: '杆塔' },
  { value: 'weather_station', label: '气象站' },
  { value: 'other', label: '其他' }
]

export function reorderWaypoints (points: AccessoryWaypoint[]) {
  points.forEach((point, index) => { point.sort_order = index + 1 })
}

export function moveWaypoint (points: AccessoryWaypoint[], index: number, direction: number) {
  const target = index + direction
  if (target < 0 || target >= points.length) return
  const [point] = points.splice(index, 1)
  points.splice(target, 0, point)
  reorderWaypoints(points)
}

function inRange (value: unknown, min: number, max: number) {
  return typeof value === 'number' && Number.isFinite(value) && value >= min && value <= max
}

export function validateAccessory (device: SolarAccessory): string {
  if (!device.orthophoto_id) return '请选择正射图'
  if (!device.name.trim() || device.name.length > 100) return '设备名称不能为空且不能超过100字'
  if (!deviceTypes.some(type => type.value === device.device_type)) return '请选择设备类型'
  const positionValid = (point: { longitude: unknown; latitude: unknown; height: unknown }) =>
    inRange(point.longitude, -180, 180) && inRange(point.latitude, -90, 90) && inRange(point.height, -1000, 10000)
  if (!positionValid(device)) return '请填写有效的设备经纬度和绝对椭球高'
  if (device.waypoints.length > 200) return '单台设备最多支持200个航点'
  for (let i = 0; i < device.waypoints.length; i++) {
    const point = device.waypoints[i]
    if (!point.name.trim() || point.name.length > 100 || !positionValid(point)) return `航点${i + 1}名称或经纬高无效`
    if (!inRange(point.heading, -180, 360) || !inRange(point.pitch, -90, 30) || !inRange(point.focal_length, 0.1, 2000)) return `航点${i + 1}偏航、俯仰或焦距无效`
    if (!['take_photo', 'none'].includes(point.action) || !['visible', 'ir', 'both'].includes(point.image_format)) return `航点${i + 1}拍摄参数无效`
  }
  return ''
}
