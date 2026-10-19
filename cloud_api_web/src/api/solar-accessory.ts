import request from '/@/api/http/request'

export interface AccessoryWaypoint {
  id?: string
  name: string
  sort_order: number
  longitude: number | null
  latitude: number | null
  height: number | null
  heading: number | null
  pitch: number | null
  focal_length: number | null
  action: string
  image_format: string
  enabled: boolean
}

export interface SolarAccessory {
  id?: string
  orthophoto_id: string
  name: string
  device_type: string
  longitude: number | null
  latitude: number | null
  height: number | null
  enabled: boolean
  waypoints: AccessoryWaypoint[]
}

async function unwrap (promise: Promise<any>) {
  const response = (await promise).data
  if (response.code !== 0) throw new Error(response.message || '操作失败')
  return response.data
}

export const listSolarAccessories = (orthophotoId: string): Promise<SolarAccessory[]> =>
  unwrap(request.get('/api/solarAccessory/list', { params: { orthophoto_id: orthophotoId } }))

export const saveSolarAccessory = (device: SolarAccessory): Promise<SolarAccessory> =>
  unwrap(request.post('/api/solarAccessory/save', device))

export const deleteSolarAccessory = (id: string, orthophotoId: string) =>
  unwrap(request.post('/api/solarAccessory/delete', { id, orthophoto_id: orthophotoId }))

export const convertAccessoryCoordinates = (orthophotoId: string, direction: 'pixel_to_geo' | 'geo_to_pixel', points: any[]) =>
  unwrap(request.post('/api/solarAccessory/coordinates', { orthophoto_id: orthophotoId, direction, points }))
