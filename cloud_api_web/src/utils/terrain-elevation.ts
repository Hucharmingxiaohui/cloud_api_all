import { fromArrayBuffer } from 'geotiff'
import { CURRENT_CONFIG } from '/@/api/http/config'
import type { ElevationGrid } from './elevation-profile'

let cached: Promise<ElevationGrid> | undefined
let cachedUrl = ''
export function loadElevation (): Promise<ElevationGrid> {
  const config = CURRENT_CONFIG.terrainElevation
  if (!config?.enabled || !config.url) return Promise.reject(Error('未启用高程数据，请配置 terrainElevation'))
  if (cached && cachedUrl === config.url) return cached
  cachedUrl = config.url
  const request = (async () => {
    const controller = new AbortController(); const timeout = setTimeout(() => controller.abort(), 20000)
    try {
      const response = await fetch(config.url, { signal: controller.signal })
      if (!response.ok) throw Error(`高程文件读取失败（HTTP ${response.status}）`)
      const bytes = await response.arrayBuffer()
      if (bytes.byteLength > 32 * 1024 * 1024) throw Error('高程文件超过前端 32MB 限制，请使用现场裁剪文件')
      const image = await (await fromArrayBuffer(bytes)).getImage()
      const keys = image.getGeoKeys(); const dir = image.getFileDirectory()
      if (keys.GeographicTypeGeoKey !== 4979 || keys.GTRasterTypeGeoKey !== 2 || dir.ModelTransformation || image.getSamplesPerPixel() !== 1) throw Error('需要 EPSG:4979、PixelIsPoint 的单波段椭球高 GeoTIFF')
      const scale = dir.ModelPixelScale; const tie = dir.ModelTiepoint
      if (!scale || !tie || !(scale[0] > 0) || !(scale[1] > 0) || image.getWidth() * image.getHeight() > 2000000) throw Error('高程栅格参数不受支持')
      const values = (await image.readRasters({ samples: [0] }))[0]
      return { width: image.getWidth(), height: image.getHeight(), west: tie[3] - tie[0] * scale[0], north: tie[4] + tie[1] * scale[1], dx: scale[0], dy: -scale[1], noData: Number(image.getGDALNoData()), values } as ElevationGrid
    } catch (error) {
      throw Error(`高程不可用：${error instanceof Error ? error.message : String(error)}。请检查文件路径及格式。`)
    } finally { clearTimeout(timeout) }
  })()
  cached = request
  request.catch(() => { if (cached === request) cached = undefined })
  return request
}
