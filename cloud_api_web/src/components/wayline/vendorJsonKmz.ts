import JSZip from 'jszip'
import type { Egm96Model } from '../../utils/egm96'

interface Photo { yaw: number; pitch: number; cameraType: number; keyID: number; name: string }
interface VendorPoint { lng: number; lat: number; height: number; speed: number; yaw: number; pitch: number; photos: Photo[] }
export interface VendorRoute { name: string; points: VendorPoint[]; photoCount: number; cameraTypes: number[]; sourceFinish: number; warnings: string[] }
export interface ConversionOptions { lens: string; finishAction: string; takeoffHeight: number; rthHeight: number; confirmed: boolean }
interface ConversionResult { template: string; wpml: string; report: string; name: string }
export const MAX_VENDOR_BYTES = 20 * 1024 * 1024
function object (value: unknown, label: string): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error(`${label}格式无效`)
  return value as Record<string, unknown>
}
function number (value: unknown, label: string, min: number, max: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < min || value > max) throw new Error(`${label}必须为 ${min} 至 ${max} 的有限数值`)
  return value
}
function text (value: unknown, label: string, max: number): string {
  if (typeof value !== 'string' || !value.trim() || value.length > max || Array.from(value).some(c => { const n = c.charCodeAt(0); return (n < 32 && n !== 9 && n !== 10 && n !== 13) || n === 65534 || n === 65535 })) throw new Error(`${label}缺失、过长或含非法字符`)
  return value.trim()
}
function keys (value: Record<string, unknown>, allowed: string[], label: string) {
  const unknown = Object.keys(value).filter(key => !allowed.includes(key))
  if (unknown.length) throw new Error(`${label}含未支持字段：${unknown.join('、')}，不能保证执行语义一致`)
}
export function parseVendorRoute (input: unknown): VendorRoute {
  const data = object(input, 'JSON')
  if (!Array.isArray(data.points) || data.points.length < 2) throw new Error('该文件没有至少两个执行航点；设备标定点/工程辅助文件不能直接转换为航线')
  if (data.points.length > 2000) throw new Error('本期最多支持 2000 个执行航点，需另行评估设备上限与续航')
  if (!data.powerline || !Array.isArray(object(data.powerline, '设备工程').keyPoint)) throw new Error('未识别的厂家航线结构：缺少 powerline.keyPoint')
  for (const [field, expected] of Object.entries({ flightPathMode: 0, yawMode: 0, gimbalMode: 2 })) {
    if (data[field] !== expected) throw new Error(`不支持源 ${field}=${String(data[field])}，需核对厂家语义`)
  }
  if (data.finishedAction !== 1) throw new Error('不支持该源结束动作枚举，请先核对厂家格式')
  if (data.elecFenceBox !== undefined && (!Array.isArray(data.elecFenceBox) || data.elecFenceBox.length)) throw new Error('本期不支持转换源电子围栏')
  const power = object(data.powerline, '设备工程')
  for (const field of ['safeBox', 'boundaryPts']) if (power[field] !== undefined && (!Array.isArray(power[field]) || (power[field] as unknown[]).length)) throw new Error(`本期不支持转换 ${field} 安全几何`)
  let photoCount = 0
  let unnamedCount = 0
  const cameraTypes = new Set<number>()
  const points = data.points.map((raw, index): VendorPoint => {
    const p = object(raw, `航点 ${index + 1}`)
    keys(p, ['SIMainWayPointType', 'aircraftYaw', 'cornerRadiusInMeters', 'dh', 'gimbalPitch', 'height', 'isStart', 'keyID', 'lat', 'lng', 'phaseSequence', 'towerName', 'turnMode', 'waypointSpeed', 'yawPitchArray'], `航点 ${index + 1}`)
    if (p.turnMode !== 0 || (p.SIMainWayPointType !== undefined && p.SIMainWayPointType !== '')) throw new Error(`航点 ${index + 1} 的转弯/特殊航点模式尚未支持`)
    const height = number(p.height, `航点 ${index + 1} 椭球高`, -1500, 10000)
    if (p.dh !== undefined && (typeof p.dh !== 'number' || !Number.isFinite(p.dh) || Math.abs(p.dh - height) > 1e-6)) throw new Error(`航点 ${index + 1} 的 height 与 dh 不一致`)
    if (!Array.isArray(p.yawPitchArray) || p.yawPitchArray.length > 10) throw new Error(`航点 ${index + 1} 拍摄动作格式无效或超过 10 组`)
    const photos = p.yawPitchArray.map((raw, actionIndex): Photo => {
      const a = object(raw, `航点 ${index + 1} 拍摄 ${actionIndex + 1}`)
      keys(a, ['FocalLengthRatio', 'aircraftYaw', 'cameraType', 'captureCount', 'gimbalPitch', 'keyID', 'keyName'], '拍摄配置')
      if (a.FocalLengthRatio !== 1 || a.captureCount !== 1) throw new Error('本期仅支持倍率 1、单张拍摄；不会静默丢弃变焦或连拍配置')
      if (a.cameraType !== 41 && a.cameraType !== 44) throw new Error(`未识别的源相机类型 ${String(a.cameraType)}`)
      cameraTypes.add(a.cameraType)
      const keyID = number(a.keyID, '拍摄目标 ID', 0, Number.MAX_SAFE_INTEGER)
      if (!Number.isInteger(keyID)) throw new Error('拍摄目标 ID 必须为整数')
      if (typeof a.keyName !== 'string') throw new Error('设备目标名称字段必须为字符串')
      const name = a.keyName.trim() ? text(a.keyName, '设备目标名称', 500) : ''
      if (!name) unnamedCount++
      return { yaw: number(a.aircraftYaw, '拍摄机头角度', -180, 180), pitch: number(a.gimbalPitch, 'M4TD 拍摄俯仰角', -90, 30), cameraType: a.cameraType, keyID, name }
    })
    photoCount += photos.length
    return { lng: number(p.lng, '经度', -180, 180), lat: number(p.lat, '纬度', -90, 90), height, speed: number(p.waypointSpeed, '航点速度', 0.1, 15), yaw: number(p.aircraftYaw, '机头角度', -180, 180), pitch: number(p.gimbalPitch, 'M4TD 云台俯仰角', -90, 30), photos }
  })
  if (photoCount > 4000) throw new Error('拍摄数量超过本期 4000 次限制')
  if (cameraTypes.size > 1) throw new Error('源航线混用多个相机类型，本期不能统一替换为一个镜头，请先明确逐动作镜头映射')
  return {
    name: text(data.taskname, '航线名称', 100),
    points,
    photoCount,
    cameraTypes: [...cameraTypes],
    sourceFinish: 1,
    warnings: [
      ...(unnamedCount ? [`源文件有 ${unnamedCount} 次拍摄未命名目标，保留原始目标 ID 和空名称，详见对应报告。`] : []),
      ...(points.some(p => p.pitch > 0 || p.photos.some(a => a.pitch > 0)) ? ['本航线含正俯仰仰拍，KMZ 保留原角度；现有云渲染编辑器会将其截为 0°，请勿经该编辑器重存转换结果。'] : []),
      '源高度按 WGS84 椭球高解释，尚未经厂家或现场基准点确认；EGM96 换算不验证此假设。',
      '源相机 41/44 不是 DJI 负载枚举；使用你确认的 M4TD 镜头，倍率 1 不转换为未知焦距。',
      '源结束/转弯枚举未获厂家定义：输出使用所选结束动作、逐点停稳模式和显式姿态动作，需现场复核。',
      '未验证障碍物净距、设备位置精度、续航或实际执行能力；转换成功不代表可以直接飞行。'
    ]
  }
}

const xml = (value: string | number) => String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;')
const tag = (name: string, value: string | number) => `<wpml:${name}>${xml(value)}</wpml:${name}>`
const wrap = (name: string, body: string) => `<wpml:${name}>${body}</wpml:${name}>`
const doc = (body: string) => `<?xml version="1.0" encoding="UTF-8"?><kml xmlns="http://www.opengis.net/kml/2.2" xmlns:wpml="http://www.dji.com/wpmz/1.0.6"><Document>${body}</Document></kml>`
function heading (yaw: number) {
  return wrap('waypointHeadingParam', tag('waypointHeadingMode', 'smoothTransition') + tag('waypointHeadingAngle', yaw) + tag('waypointPoiPoint', '0,0,0') + tag('waypointHeadingPathMode', 'followBadArc') + tag('waypointHeadingAngleEnable', 1))
}
function gimbal (pitch: number) {
  return tag('payloadPositionIndex', 0) + tag('gimbalHeadingYawBase', 'aircraft') + tag('gimbalRotateMode', 'absoluteAngle') + tag('gimbalPitchRotateEnable', 1) + tag('gimbalPitchRotateAngle', pitch) + tag('gimbalRollRotateEnable', 0) + tag('gimbalRollRotateAngle', 0) + tag('gimbalYawRotateEnable', 0) + tag('gimbalYawRotateAngle', 0) + tag('gimbalRotateTimeEnable', 0) + tag('gimbalRotateTime', 0)
}
export function buildVendorKmz (route: VendorRoute, options: ConversionOptions, model: Egm96Model): ConversionResult {
  if (!options.confirmed) throw new Error('请先确认源高度、镜头映射与停点动作转换假设')
  if (!['wide', 'ir', 'wide,ir'].includes(options.lens)) throw new Error('请选择已确认的目标镜头（广角、红外或可见光＋红外）')
  if (!['goHome', 'noAction', 'autoLand', 'gotoFirstWaypoint'].includes(options.finishAction)) throw new Error('请选择任务结束动作')
  number(options.takeoffHeight, '安全起飞高度', 2, 1500)
  number(options.rthHeight, '全局返航高度', 2, 1500)
  const mission = wrap('missionConfig', tag('flyToWaylineMode', 'safely') + tag('finishAction', options.finishAction) + tag('exitOnRCLost', 'executeLostAction') + tag('executeRCLostAction', 'goBack') + tag('takeOffSecurityHeight', options.takeoffHeight) + tag('globalTransitionalSpeed', 3) + tag('globalRTHHeight', options.rthHeight) + wrap('droneInfo', tag('droneEnumValue', 100) + tag('droneSubEnumValue', 1)) + wrap('payloadInfo', tag('payloadEnumValue', 99) + tag('payloadSubEnumValue', 0) + tag('payloadPositionIndex', 0)))
  let actionID = 0
  const mapping: object[] = []
  const templates: string[] = []; const executions: string[] = []
  route.points.forEach((p, index) => {
    const n = model.undulation(p.lng, p.lat)
    if (!Number.isFinite(n) || Math.abs(n) > 200) throw new Error(`航点 ${index + 1} 高程换算无效`)
    const actions: string[] = []
    function action (func: string, param: string) { actions.push(wrap('action', tag('actionId', actionID++) + tag('actionActuatorFunc', func) + wrap('actionActuatorFuncParam', param))) }
    const orient = (yaw: number, pitch: number) => {
      action('rotateYaw', tag('aircraftHeading', yaw) + tag('aircraftPathMode', 'clockwise'))
      action('gimbalRotate', gimbal(pitch))
    }
    if (!p.photos.length) orient(p.yaw, p.pitch)
    p.photos.forEach((photo, shot) => {
      orient(photo.yaw, photo.pitch)
      const suffix = `V${String(index + 1).padStart(4, '0')}S${String(shot + 1).padStart(2, '0')}`
      action('takePhoto', tag('payloadPositionIndex', 0) + tag('fileSuffix', suffix) + tag('payloadLensIndex', options.lens) + tag('useGlobalPayloadLensIndex', 0))
      mapping.push({ waypoint: index + 1, photo: shot + 1, suffix, targetID: photo.keyID, targetName: photo.name, sourceCameraType: photo.cameraType, lens: options.lens, yaw: photo.yaw, pitch: photo.pitch })
    })
    const group = wrap('actionGroup', tag('actionGroupId', index) + tag('actionGroupStartIndex', index) + tag('actionGroupEndIndex', index) + tag('actionGroupMode', 'sequence') + wrap('actionTrigger', tag('actionTriggerType', 'reachPoint')) + actions.join(''))
    const common = `<Point><coordinates>${p.lng},${p.lat}</coordinates></Point>` + tag('index', index) + tag('waypointSpeed', p.speed) + heading(p.yaw) + wrap('waypointTurnParam', tag('waypointTurnMode', 'toPointAndStopWithDiscontinuityCurvature') + tag('waypointTurnDampingDist', 0)) + tag('useStraightLine', 1)
    templates.push(`<Placemark>${common}${tag('ellipsoidHeight', p.height)}${tag('height', p.height - n)}${tag('useGlobalHeight', 0)}${tag('useGlobalSpeed', 0)}${tag('useGlobalHeadingParam', 0)}${tag('useGlobalTurnParam', 0)}${tag('gimbalPitchAngle', p.pitch)}${group}</Placemark>`)
    executions.push(`<Placemark>${common}${tag('executeHeight', p.height)}${group}</Placemark>`)
  })
  const globalHeading = wrap('globalWaypointHeadingParam', tag('waypointHeadingMode', 'smoothTransition') + tag('waypointHeadingAngle', route.points[0].yaw) + tag('waypointPoiPoint', '0,0,0') + tag('waypointHeadingPathMode', 'followBadArc'))
  const template = doc(`<name>${xml(route.name)}</name>${tag('author', '平台本地厂家JSON转换')}${mission}<Folder>${tag('templateType', 'waypoint')}${tag('templateId', 0)}${wrap('waylineCoordinateSysParam', tag('coordinateMode', 'WGS84') + tag('heightMode', 'EGM96'))}${tag('autoFlightSpeed', 3)}${tag('globalHeight', route.points[0].height - model.undulation(route.points[0].lng, route.points[0].lat))}${tag('gimbalPitchMode', 'usePointSetting')}${globalHeading}${tag('globalWaypointTurnMode', 'toPointAndStopWithDiscontinuityCurvature')}${tag('globalUseStraightLine', 1)}${wrap('payloadParam', tag('payloadPositionIndex', 0) + tag('imageFormat', options.lens))}${templates.join('')}</Folder>`)
  const wpml = doc(`${mission}<Folder>${tag('templateId', 0)}${tag('executeHeightMode', 'WGS84')}${tag('waylineId', 0)}${tag('autoFlightSpeed', 3)}${executions.join('')}</Folder>`)
  const report = JSON.stringify({ name: route.name, target: 'DJI Dock 3 / Matrice 4TD', sourceHeightAssumption: 'WGS84 ellipsoid metres; unverified', templateHeight: 'EGM96 H=h-N (NGA 15-minute model, bilinear)', options, warnings: route.warnings, sourceFinish: route.sourceFinish, conversion: 'Explicit sequential stop/orientation/photo actions; not flight certified', points: route.points.map((p, i) => ({ index: i + 1, lng: p.lng, lat: p.lat, ellipsoidHeight: p.height, egm96Height: p.height - model.undulation(p.lng, p.lat), speed: p.speed })), photos: mapping }, null, 2)
  const reportData = JSON.parse(report)
  reportData.expectedImageCount = route.photoCount * options.lens.split(',').length
  return { template, wpml, report: JSON.stringify(reportData, null, 2), name: route.name }
}
export async function packVendorKmz (result: ConversionResult): Promise<Uint8Array> {
  const zip = new JSZip()
  zip.file('wpmz/template.kml', result.template)
  zip.file('wpmz/waylines.wpml', result.wpml)
  return zip.generateAsync({ type: 'uint8array', compression: 'DEFLATE' })
}
