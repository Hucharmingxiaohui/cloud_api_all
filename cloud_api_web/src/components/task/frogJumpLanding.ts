import type { LandingCandidate, LandingRecommendation } from '/@/api/frogJump'

function knownNumber (value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function onlineState (value: unknown): boolean | null {
  return typeof value === 'boolean' ? value : null
}

export function normalizeRecommendation (data: any, takeoffDockSn: string): LandingRecommendation {
  const candidates: LandingCandidate[] = (Array.isArray(data?.candidates) ? data.candidates : []).map((item: any) => {
    const distance = item.distance_meters ?? item.distanceMeters
    const online = onlineState(item.online)
    return {
      dock_sn: item.dock_sn ?? item.dockSn ?? '',
      name: item.name || item.dock_sn || item.dockSn || '',
      distance_meters: knownNumber(distance) && distance >= 0 ? distance : null,
      online,
      selectable: item.selectable === true && online === true,
      reason: item.reason || (online === false ? '机场离线' : online === null ? '在线状态未知' : item.selectable !== true ? '当前不可选择' : ''),
      recommended: item.recommended === true,
    }
  }).filter((item: LandingCandidate) => item.dock_sn && item.dock_sn !== takeoffDockSn)
    .sort((a: LandingCandidate, b: LandingCandidate) => (a.distance_meters ?? Infinity) - (b.distance_meters ?? Infinity))
  const recommendedSn = data?.recommended_dock_sn ?? data?.recommendedDockSn ?? ''
  const recommended = candidates.find(item => item.dock_sn === recommendedSn && item.selectable && item.distance_meters !== null)
  const endpoint = data?.endpoint
  return {
    endpoint: endpoint && knownNumber(endpoint.longitude) && knownNumber(endpoint.latitude)
      ? { longitude: endpoint.longitude, latitude: endpoint.latitude, wayline_id: endpoint.wayline_id ?? endpoint.waylineId, waypoint_index: endpoint.waypoint_index ?? endpoint.waypointIndex }
      : null,
    candidates,
    recommended_dock_sn: recommended?.dock_sn || '',
    message: data?.message || '',
  }
}

export function bindingDevicesToCandidates (devices: any[], takeoffDockSn: string): LandingCandidate[] {
  return devices.filter(item => item.device_sn && item.device_sn !== takeoffDockSn).map(item => {
    const online = onlineState(item.online_status ?? item.onlineStatus ?? item.online)
    return {
      dock_sn: item.device_sn,
      name: item.nickname || item.device_name || item.device_sn,
      distance_meters: null,
      online,
      selectable: online !== false,
      reason: online === false ? '机场离线' : online === null ? '在线状态未知，需由任务准备阶段校验' : '需由任务准备阶段校验',
      recommended: false,
    }
  })
}

export function formatLandingDistance (distance: number | null): string {
  if (distance === null || !knownNumber(distance) || distance < 0) return '无法计算'
  return distance >= 1000 ? `${(distance / 1000).toFixed(2)} 公里` : `${Math.round(distance)} 米`
}
