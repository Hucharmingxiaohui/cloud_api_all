import { getDeviceTopo } from '/@/api/manage'
import { getWaylineFiles } from '/@/api/wayline'

export interface PlanDisplayRefs {
  waylineNames: Record<string, string>
  deviceNames: Record<string, string>
}

function collectDevices (items: any[], names: Record<string, string>) {
  items.forEach(item => {
    if (!item) return
    const sn = String(item.device_sn || '')
    if (sn) names[sn] = item.nickname || item.device_name || sn
    if (Array.isArray(item.children)) collectDevices(item.children, names)
  })
}

export async function loadPlanDisplayRefs (workspaceId: string): Promise<PlanDisplayRefs> {
  const refs: PlanDisplayRefs = { waylineNames: {}, deviceNames: {} }
  const [waylines, devices] = await Promise.all([
    getWaylineFiles(workspaceId, { page: 1, page_size: 10000, order_by: 'update_time desc' }).catch(() => null),
    getDeviceTopo(workspaceId).catch(() => null)
  ])

  waylines?.data?.list?.forEach((wayline: any) => {
    if (wayline?.id) refs.waylineNames[String(wayline.id)] = wayline.name || String(wayline.id)
  })
  collectDevices(devices?.data?.list || devices?.data || [], refs.deviceNames)
  return refs
}

export function getPlanWaylineName (plan: any, refs: PlanDisplayRefs) {
  return refs.waylineNames[String(plan?.file_id)] || plan?.file_id || '-'
}

export function getPlanDeviceName (plan: any, refs: PlanDisplayRefs) {
  return refs.deviceNames[String(plan?.dock_sn)] || plan?.dock_sn || '-'
}

export function filterPlanRows (rows: any[], waylineName: string, deviceSn: string, refs: PlanDisplayRefs) {
  const waylineKeyword = waylineName.trim().toLowerCase()
  return rows.filter(row => {
    const routeName = getPlanWaylineName(row, refs).toLowerCase()
    const routeMatched = !waylineKeyword || routeName.includes(waylineKeyword)
    const deviceMatched = !deviceSn || String(row.dock_sn) === deviceSn
    return routeMatched && deviceMatched
  })
}

export function paginatePlanRows (rows: any[], page: number, pageSize: number) {
  const start = (page - 1) * pageSize
  return rows.slice(start, start + pageSize)
}

export function buildDeviceOptions (deviceNames: Record<string, string>) {
  return [{ sn: '', name: '全部' }, ...Object.entries(deviceNames).map(([sn, name]) => ({ sn, name }))]
}
