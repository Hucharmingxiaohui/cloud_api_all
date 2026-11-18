import { fromArrayBuffer } from 'geotiff'

export interface Egm96Model { undulation: (lng: number, lat: number) => number }
const HASH = 'db493027562c9b004d7220fa881f5603adada4e1c5029b933fa7de4547b0e78d'

export async function decodeEgm96 (buffer: ArrayBuffer): Promise<Egm96Model> {
  const digest = await crypto.subtle.digest('SHA-256', buffer)
  const hash = Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, '0')).join('')
  if (hash !== HASH) throw new Error('EGM96 模型校验失败，请检查部署资源')
  const tiff = await fromArrayBuffer(buffer)
  const image = await tiff.getImage()
  const [x, y] = image.getOrigin(); const [dx, dy] = image.getResolution()
  if (image.getWidth() !== 1440 || image.getHeight() !== 721 || x !== -180 || y !== 90 || dx !== 0.25 || dy !== -0.25 || image.getGeoKeys().GTRasterTypeGeoKey !== 2) throw new Error('EGM96 网格定义不匹配')
  const values = await image.readRasters({ samples: [0], interleave: true }) as Float32Array
  function sample (column: number, row: number) { return Number(values[row * 1440 + ((column % 1440) + 1440) % 1440]) }
  return {
    undulation (lng, lat) {
      if (!Number.isFinite(lng) || !Number.isFinite(lat) || Math.abs(lng) > 180 || Math.abs(lat) > 90) throw new Error('EGM96 查询坐标无效')
      const cx = (lng + 180) / 0.25; const cy = (90 - lat) / 0.25
      const col = Math.floor(cx); const row = Math.min(719, Math.floor(cy)); const fx = cx - col; const fy = cy - row
      const n = (sample(col, row) * (1 - fx) + sample(col + 1, row) * fx) * (1 - fy) + (sample(col, row + 1) * (1 - fx) + sample(col + 1, row + 1) * fx) * fy
      if (!Number.isFinite(n) || Math.abs(n) > 200) throw new Error('EGM96 高程换算失败')
      return n
    }
  }
}

let loading: Promise<Egm96Model> | undefined
export function loadEgm96 (): Promise<Egm96Model> {
  if (!loading) {
    loading = fetch(`${import.meta.env.BASE_URL}geoid/us_nga_egm96_15.tif`).then(async response => {
      if (!response.ok) throw new Error('EGM96 模型加载失败，请检查静态资源部署')
      return decodeEgm96(await response.arrayBuffer())
    }).catch(error => { loading = undefined; throw error })
  }
  return loading
}
