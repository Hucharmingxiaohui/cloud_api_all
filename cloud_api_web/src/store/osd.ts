import type { RootStateType } from './index'

export function isOsdOffline (status: unknown): boolean {
  return status === false || status === 0
}

// OSD 是增量上报；空值不覆盖已上报值，0/false 和显式空数组仍是有效更新。
export function mergeOsd<T> (previous: T, incoming: T): T {
  const result = { ...previous } as any
  Object.entries(incoming || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    if (typeof value === 'object' && !Array.isArray(value)) {
      const old = result[key]
      const changedDevice = 'device_sn' in value && value.device_sn && value.device_sn !== old?.device_sn
      result[key] = mergeOsd(changedDevice ? {} : old || {}, value)
    } else {
      result[key] = value
    }
  })
  return result
}

// 只按当前选择设备取快照，不使用最后一包消息的 SN/类型决定面板内容。
export function selectControlOsd (state: RootStateType['deviceState'], selected: RootStateType['osdVisible']) {
  const gatewaySn = selected.gateway_sn
  const dock = state.dockInfo[gatewaySn]
  const subDevice = dock?.basic_osd?.sub_device
  const droneSn = selected.is_dock ? subDevice?.device_sn : selected.sn
  return {
    gateway: state.gatewayInfo[gatewaySn],
    dock,
    device: selected.is_dock && isOsdOffline(subDevice?.device_online_status)
      ? undefined
      : state.deviceInfo[droneSn || '']
  }
}
