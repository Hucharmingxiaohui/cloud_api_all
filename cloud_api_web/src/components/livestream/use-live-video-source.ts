import { getLiveCapacity } from '/@/api/manage'
import { getImageUrl } from '/@/common/url'

export type LiveVideoRole = 'dock' | 'drone'

export interface ResolveLiveVideoSourceOptions {
  role: LiveVideoRole
  sn?: string
  deviceInfo?: any
  flvBaseUrl: string
}

export interface LiveVideoSource {
  deviceSn: string
  cameraIndex: string
  videoIndex: string
  videoId: string
  flvUrl: string
}

const DEFAULT_DOCK_CAMERA_INDEX = '165-0-7'
const DEFAULT_DRONE_CAMERA_INDEX = '99-0-0'
const DEFAULT_VIDEO_INDEX = 'normal-0'

function resolveDeviceSn (options: ResolveLiveVideoSourceOptions): string {
  const { role, sn, deviceInfo } = options
  if (role === 'dock') {
    return sn || deviceInfo?.gateway?.sn || deviceInfo?.dock?.sn || ''
  }
  return deviceInfo?.dock?.basic_osd?.sub_device?.device_sn || deviceInfo?.device?.device_sn || sn || ''
}

async function resolveCameraIndex (deviceSn: string, role: LiveVideoRole): Promise<string> {
  const fallback = role === 'dock' ? DEFAULT_DOCK_CAMERA_INDEX : DEFAULT_DRONE_CAMERA_INDEX
  if (!deviceSn) {
    return fallback
  }
  try {
    const res = await getLiveCapacity({})
    if (res.code !== 0 || !Array.isArray(res.data)) {
      return fallback
    }
    const device = res.data.find((item: any) => item.sn === deviceSn)
    const camera = device?.cameras_list?.[0] || device?.camera_list?.[0]
    return camera?.index || camera?.camera_index || fallback
  } catch (e) {
    console.warn('[live-video] getLiveCapacity failed, use fallback camera index', e)
    return fallback
  }
}

export async function resolveLiveVideoSource (options: ResolveLiveVideoSourceOptions): Promise<LiveVideoSource | null> {
  const deviceSn = resolveDeviceSn(options)
  if (!deviceSn) {
    return null
  }
  const cameraIndex = await resolveCameraIndex(deviceSn, options.role)
  const videoId = `${deviceSn}/${cameraIndex}/${DEFAULT_VIDEO_INDEX}`
  const flvUrl = getImageUrl(options.flvBaseUrl, `${deviceSn}-${cameraIndex}.flv`)
  return {
    deviceSn,
    cameraIndex,
    videoIndex: DEFAULT_VIDEO_INDEX,
    videoId,
    flvUrl
  }
}
