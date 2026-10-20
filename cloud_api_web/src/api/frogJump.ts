import request, { IWorkspaceResponse } from '/@/api/http/request'

export interface LandingEndpoint {
  longitude: number
  latitude: number
  wayline_id?: number
  waypoint_index?: number
}

export interface LandingCandidate {
  dock_sn: string
  name: string
  distance_meters: number | null
  online: boolean | null
  selectable: boolean
  reason: string
  recommended: boolean
}

export interface LandingRecommendation {
  endpoint: LandingEndpoint | null
  candidates: LandingCandidate[]
  recommended_dock_sn: string
  message: string
}

export async function getLandingRecommendation (fileId: string, takeoffDockSn: string): Promise<IWorkspaceResponse<LandingRecommendation>> {
  const result = await request.get('/api/v1/frog-jump/landing-recommendation', {
    params: { file_id: fileId, takeoff_dock_sn: takeoffDockSn },
  })
  return result.data
}
