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
  baseURL: string
  renderer?: 'outdoor' | 'align' | 'wayline'
  pointCloudFile: string
  rendererParams: CloudRendererAlignParams
  iceServers: CloudRendererIceServer[]
}

export interface RuntimeConfig {
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
