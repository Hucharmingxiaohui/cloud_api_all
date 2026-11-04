import type { EuaReadQuery, EuaReadPage, EuaMonitoringRow, EuaHmsRow } from '/@/api/eua-monitoring'

export type TabKey = 'drone' | 'dock' | 'hms'
export type EuaRow = Partial<EuaMonitoringRow & EuaHmsRow>
export interface Filters {
  unitCode: string; dockSn: string; droneSn: string; keyword: string
  range: string[] | null; level?: number; gatewayType?: string; module?: number
}
export interface Field { key: keyof EuaRow; label: string; unit?: string; kind?: 'code' | 'bool' | 'level' | 'module' | 'gateway' | 'time' }
export interface TabState {
  filters: Filters; applied: Filters; current: number; size: number; total: number
  records: EuaRow[]; loading: boolean; error: string; generation: number; loaded: boolean
}

export function emptyFilters (): Filters {
  return { unitCode: '', dockSn: '', droneSn: '', keyword: '', range: [], level: undefined, gatewayType: undefined, module: undefined }
}

export function createTabState (): TabState {
  return { filters: emptyFilters(), applied: emptyFilters(), current: 1, size: 20, total: 0, records: [], loading: false, error: '', generation: 0, loaded: false }
}

export function buildQuery (filters: Filters, current: number, size: number, tab: TabKey): EuaReadQuery {
  if (!Number.isSafeInteger(current) || current < 1 || !Number.isInteger(size) || size < 1 || size > 100) throw new Error('分页参数无效')
  const query: EuaReadQuery = { current, size }
  for (const key of ['unitCode', 'dockSn', 'droneSn', 'keyword'] as const) {
    const value = filters[key]?.trim()
    if (value && value.length > 256) throw new Error('查询内容不能超过256个字符')
    if (value) query[key] = value
  }
  if (filters.range?.length === 2) {
    if (filters.range[0] > filters.range[1]) throw new Error('开始时间不能晚于结束时间')
    // LocalDateTime binding uses ISO wall-clock text, without timezone conversion.
    query.startTime = filters.range[0].replace(' ', 'T')
    query.endTime = filters.range[1].replace(' ', 'T')
  }
  if (tab === 'hms') {
    if (filters.level !== undefined && filters.level !== null) query.level = filters.level
    if (filters.gatewayType) query.gatewayType = filters.gatewayType
    if (filters.module !== undefined && filters.module !== null) query.module = filters.module
  }
  return query
}

export function formatValue (value: unknown, unit = ''): string {
  if (value === null || value === undefined || value === '') return '—'
  return `${value}${unit ? ' ' + unit : ''}`
}

export function formatCode (value: unknown): string {
  return value === null || value === undefined || value === '' ? '—' : `代码 ${value}`
}

const labels: Record<string, Record<string, string>> = {
  level: { 0: '通知', 1: '提醒', 2: '警告' },
  module: { 0: '飞行任务', 1: '设备管理', 2: '媒体', 3: 'HMS' },
  gateway: { 1: '机巢', 2: '无人机' }
}

export function formatField (field: Field, value: unknown): string {
  if (value === null || value === undefined || value === '') return '—'
  if (field.kind === 'code') return formatCode(value)
  if (field.kind === 'bool') return value === true ? '是' : value === false ? '否' : formatCode(value)
  if (field.kind === 'level' || field.kind === 'module' || field.kind === 'gateway') {
    return labels[field.kind][String(value)] ?? formatCode(value)
  }
  if (field.kind === 'time' && typeof value === 'number') {
    const time = new Date(value)
    return Number.isNaN(time.getTime()) ? formatValue(value) : `${time.toLocaleString('zh-CN', { hour12: false })}（浏览器本地时间）`
  }
  return formatValue(value, field.unit)
}

export function requestError (error: any, context: string): string {
  const status = error?.response?.status ?? error?.code
  return `${context}失败${typeof status === 'number' ? `（${status}）` : '，请检查网络或后端服务'}，请重试`
}

export function invalidateTab (state: TabState): void {
  state.generation++
  state.loading = false
}

export async function loadTab (state: TabState, fetchPage: () => Promise<EuaReadPage<EuaRow>>): Promise<void> {
  const generation = ++state.generation
  state.loading = true
  state.error = ''
  try {
    const response = await fetchPage()
    if (generation !== state.generation) return
    if (!Array.isArray(response?.records) || !Number.isFinite(response?.total) || response.total < 0) throw new Error('Invalid page')
    state.records = response.records
    state.total = response.total
    state.loaded = true
  } catch (error) {
    if (generation === state.generation) {
      state.records = []
      state.total = 0
      state.loaded = false
      state.error = requestError(error, 'EUA数据查询')
    }
  } finally {
    if (generation === state.generation) state.loading = false
  }
}

const identity: Field[] = [
  { key: 'unit_code', label: '单位编码' }, { key: 'dock_sn', label: '机巢 SN' }, { key: 'drone_sn', label: '无人机 SN' }
]
const drone: Field[] = [
  { key: 'drone_mode_code', label: '无人机模式', kind: 'code' }, { key: 'battery_level', label: '电量', unit: '%' },
  { key: 'latitude', label: '纬度', unit: '°' }, { key: 'longitude', label: '经度', unit: '°' },
  { key: 'flight_height', label: '源高度（绝对高度）', unit: 'm' },
  { key: 'horizontal_speed', label: '水平速度', unit: 'm/s' }, { key: 'vertical_speed', label: '垂直速度', unit: 'm/s' },
  { key: 'home_distance', label: '距返航点距离', unit: 'm' }, { key: 'flight_distance', label: '累计飞行距离', unit: 'm' },
  { key: 'total_flight_time', label: '累计飞行时间', unit: 's' }, { key: 'drone_curr_time', label: '无人机源时间（原值）' }
]
const dock: Field[] = [
  { key: 'dock_mode_code', label: '机巢模式', kind: 'code' }, { key: 'drone_in_dock', label: '无人机在巢（源标记）', kind: 'bool' },
  { key: 'nest_voltage', label: '机巢电压', unit: 'V' }, { key: 'nest_temperature', label: '机巢温度', unit: '℃' },
  { key: 'nest_humidity', label: '机巢湿度', unit: '%' }, { key: 'ambient_temperature', label: '环境温度', unit: '℃' },
  { key: 'wind_speed', label: '风速', unit: 'm/s' }, { key: 'rainfall', label: '降雨量（存储原值）' },
  { key: 'dock_curr_time', label: '机巢源时间（原值）' }
]
const hms: Field[] = [
  { key: 'level', label: '告警级别', kind: 'level' }, { key: 'gateway_type', label: '告警来源', kind: 'gateway' },
  { key: 'module', label: '模块', kind: 'module' }, { key: 'alarm_code', label: '告警代码' },
  { key: 'message', label: '告警内容' }, { key: 'create_time', label: '接收时间', kind: 'time' }
]

export function tableFields (tab: TabKey): Field[] {
  return [...identity, ...(tab === 'hms' ? hms : tab === 'dock' ? dock.slice(0, 7) : drone.slice(0, 8)),
    ...(tab === 'hms' ? [] : [{ key: 'update_time', label: '快照更新时间', kind: 'time' } as Field])]
}

export function detailFields (tab: TabKey): Field[] {
  return [...identity, ...(tab === 'hms' ? hms : tab === 'dock' ? dock : drone),
    { key: 'id', label: '记录 ID' }, { key: 'topic_dock_sn', label: '主题机巢 SN' },
    ...(tab === 'hms'
      ? [{ key: 'message_id', label: '消息 ID' }, { key: 'imminent', label: '瞬时告警源标记', kind: 'bool' }, { key: 'in_the_sky', label: '空中源标记', kind: 'bool' }]
      : [{ key: 'task_id', label: '任务 ID' }, { key: 'flighttask_step_code', label: '任务步骤', kind: 'code' },
          { key: 'drone_message_id', label: '无人机消息 ID' }, { key: 'dock_message_id', label: '机巢消息 ID' }, { key: 'create_time', label: '入库时间', kind: 'time' }]) as Field[],
    { key: 'update_time', label: '记录更新时间', kind: 'time' }]
}
