export interface CloudRendererAlignParams {
  alignSplatFile: string
  alignReferenceTileset: string
}

export interface CloudRendererIceServer {
  urls: string
  username?: string
  credential?: string
}

export interface CloudRendererConfig {
  enabled: boolean
  baseURL: string
  renderer?: 'outdoor' | 'align' | 'wayline'
  pointCloudFile: string
  rendererParams: CloudRendererAlignParams
  iceServers: CloudRendererIceServer[]
}

export interface RuntimeConfig {
  /** 工地巡检部署隐藏其他专业菜单；默认关闭，仅布尔 true 开启 */
  constructionOnly?: boolean
  baseURL?: string
  kmzURL?: string
  websocketURL?: string
  rtmpURL?: string
  rtcURL?: string
  flvURL?: string
  UEPixURL?: string
  cloudRenderer?: Partial<CloudRendererConfig>
  [key: string]: unknown
}
