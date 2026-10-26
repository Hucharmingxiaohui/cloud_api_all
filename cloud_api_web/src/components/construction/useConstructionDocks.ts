import { computed, ref } from 'vue'
import { useMyStore } from '/@/store'
import { getBindingDevices } from '/@/api/manage'
import { EDeviceTypeName } from '/@/types'

export interface ConstructionDockMarker { sn: string; name: string; lng: number; lat: number }

export function useConstructionDocks () {
  const store = useMyStore()
  const names = ref<Array<{ device_sn: string; nickname?: string; device_name?: string }>>([])
  const error = ref('')
  const loading = ref(false)
  const docks = computed<ConstructionDockMarker[]>(() => {
    const telemetry = store.state.deviceState.dockInfo
    const devices = new Map(names.value.map(d => [d.device_sn, d.nickname || d.device_name || d.device_sn]))
    Object.keys(telemetry).forEach(sn => { if (!devices.has(sn)) devices.set(sn, sn) })
    const result: ConstructionDockMarker[] = []
    devices.forEach((name, sn) => {
      const osd = telemetry[sn]?.basic_osd
      if (osd?.longitude == null || osd?.latitude == null) return
      const lng = Number(osd.longitude); const lat = Number(osd.latitude)
      if (!Number.isFinite(lng) || !Number.isFinite(lat) || Math.abs(lng) > 180 || Math.abs(lat) > 90 || (lng === 0 && lat === 0)) return
      result.push({ sn, name, lng, lat })
    })
    return result
  })
  async function refresh () {
    if (loading.value) return
    const workspace = localStorage.getItem('workspace_id')
    if (!workspace) { error.value = '请先登录工作空间以加载机巢'; return }
    loading.value = true; error.value = ''
    try {
      const collected: typeof names.value = []
      let page = 1
      while (true) {
        const response = await getBindingDevices(workspace, { page, page_size: 100, total: -1 }, EDeviceTypeName.Dock)
        if (response.code !== 0) throw new Error('机巢列表加载失败')
        const list = response.data?.list || []
        collected.push(...list)
        if (list.length < 100) break
        page++
      }
      names.value = collected
    } catch { error.value = '机巢列表加载失败，可刷新重试；已有定位仍保留' } finally { loading.value = false }
  }
  return { docks, devices: names, error, loading, refresh }
}
