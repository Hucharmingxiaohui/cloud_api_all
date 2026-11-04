import request from '/@/api/http/request'

export interface EuaReadQuery {
  current: number
  size: number
  unitCode?: string
  dockSn?: string
  droneSn?: string
  keyword?: string
  startTime?: string
  endTime?: string
  level?: number
  gatewayType?: string
  module?: number
}

export interface EuaMonitoringRow {
  id: number | string
  unit_code: string | null
  topic_dock_sn: string | null
  task_id: string | null
  dock_sn: string | null
  drone_sn: string | null
  drone_message_id: string | null
  dock_message_id: string | null
  drone_curr_time: string | null
  dock_curr_time: string | null
  drone_mode_code: number | null
  dock_mode_code: number | null
  flighttask_step_code: number | null
  drone_in_dock: boolean | null
  latitude: string | null
  longitude: string | null
  flight_height: string | null
  horizontal_speed: string | null
  vertical_speed: string | null
  home_distance: string | null
  flight_distance: string | null
  total_flight_time: string | null
  battery_level: string | null
  nest_voltage: string | null
  nest_temperature: string | null
  nest_humidity: string | null
  ambient_temperature: string | null
  wind_speed: string | null
  rainfall: string | null
  create_time: string | number | null
  update_time: string | number | null
}

export interface EuaHmsRow {
  id: number | string
  unit_code: string | null
  topic_dock_sn: string | null
  message_id: string | null
  alarm_code: string | null
  dock_sn: string | null
  drone_sn: string | null
  gateway_type: string | null
  imminent: boolean | null
  in_the_sky: boolean | null
  level: number | null
  module: number | null
  message: string | null
  level_label: string | null
  module_label: string | null
  create_time: string | number | null
  update_time: string | number | null
}

export interface EuaReadPage<T> { records: T[]; total: number; current: number; size: number }
export interface EuaReceptionStatus { enabled: boolean; server_time: string | number | null }

export async function getEuaMonitoringStatus (): Promise<EuaReceptionStatus> {
  return (await request.get('/api/cqDock/monitoring/status', { timeout: 15000 })).data
}

export async function getEuaMonitoringPage (params: EuaReadQuery): Promise<EuaReadPage<EuaMonitoringRow>> {
  return (await request.get('/api/cqDock/monitoring/page', { params, timeout: 15000 })).data
}

export async function getEuaHmsPage (params: EuaReadQuery): Promise<EuaReadPage<EuaHmsRow>> {
  return (await request.get('/api/cqDock/hms/page', { params, timeout: 15000 })).data
}
