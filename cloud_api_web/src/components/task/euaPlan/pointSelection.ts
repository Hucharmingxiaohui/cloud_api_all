import type { EuaPointGroup } from '/@/api/cq-eua-plan'

export function routeKey (group: { subCode: string; routeId: string }) {
  return JSON.stringify([group.subCode, group.routeId])
}

function parseCodes (pointCodes: unknown): string[] | null {
  try {
    const codes = typeof pointCodes === 'string' ? JSON.parse(pointCodes) : null
    return Array.isArray(codes) && codes.length > 0 && codes.every(code => typeof code === 'string' && code.trim()) && new Set(codes).size === codes.length ? codes : null
  } catch {
    return null
  }
}

export function hasPlanPoints (plan: { pointCodes?: unknown; requiresPointSelection?: boolean }) {
  return plan.requiresPointSelection !== true && parseCodes(plan.pointCodes) !== null
}

export function selectionError (groups: EuaPointGroup[], ids: number[]) {
  if (!ids.length) return '请选择点位'
  const selectedGroups = groups.filter(group => group.points.some(point => ids.includes(point.id)))
  const points = selectedGroups.flatMap(group => group.points).filter(point => ids.includes(point.id))
  if (new Set(points.map(point => point.id)).size !== new Set(ids).size) return '所选点位已失效，请重新选择'
  if (selectedGroups.length !== 1) return '只能选择同一场站、同一EUA航线下的点位'
  return ''
}

export function buildPointTree (groups: EuaPointGroup[], ids: number[]) {
  const active = groups.find(group => group.points.some(point => ids.includes(point.id)))
  return groups.map(group => {
    const key = routeKey(group)
    const disabled = !!active && routeKey(active) !== key
    return {
      key,
      label: `${group.routeName}（${group.subCode} / ${group.routeId}）`,
      disabled,
      children: group.points.map(point => ({
        key: `point:${point.id}`,
        pointId: point.id,
        label: `${point.pointName || point.pointCode}（${point.pointCode}）`,
        disabled
      }))
    }
  })
}

export function restoreSelection (groups: EuaPointGroup[], plan: any): { ids: number[]; warning: string } {
  const codes = parseCodes(plan.pointCodes)
  if (!codes) return { ids: [], warning: '旧计划或点位快照无效，请重新选择点位并保存后执行' }
  const group = groups.find(group => group.subCode === plan.subCode && group.routeId === plan.routeId && group.routeName === plan.routeName)
  const stale = { ids: [], warning: '计划点位已缺失、重绑或与原快照不一致，请重新选择全部点位后保存' }
  if (!group) return stale
  const points = group.points.filter(point => codes.includes(point.pointCode))
  if (points.length !== codes.length || new Set(points.map(point => point.pointCode)).size !== codes.length) return stale
  const ids = points.map(point => point.id)
  if (Array.isArray(plan.pointIds) && (plan.pointIds.length !== ids.length || plan.pointIds.some((id: number) => !ids.includes(id)))) return stale
  return { ids, warning: '' }
}
