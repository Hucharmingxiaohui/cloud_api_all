import request, { IWorkspaceResponse } from '/@/api/http/request'
import { ELocalStorageKey } from '/@/types'

const CONTROL_API_PREFIX = '/control/api/v1'
const workspaceId: string = localStorage.getItem(ELocalStorageKey.WorkspaceId) || ''

export interface SpeakerAudioUploadResp {
  name: string
  url: string
  md5: string
  format: 'pcm'
  object_key?: string
  objectKey?: string
}

export async function uploadSpeakerAudio (dockSn: string, file: Blob): Promise<IWorkspaceResponse<SpeakerAudioUploadResp>> {
  const form = new FormData()
  form.append('dock_sn', dockSn)
  form.append('file', file, 'speaker-record.pcm')
  const resp = await request.post(`${CONTROL_API_PREFIX}/workspaces/${workspaceId}/speaker/audio/upload`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return resp.data
}

export async function deleteSpeakerAudio (objectKey: string): Promise<IWorkspaceResponse<null>> {
  const resp = await request.delete(`${CONTROL_API_PREFIX}/workspaces/${workspaceId}/speaker/audio`, {
    data: { object_key: objectKey, objectKey },
  })
  return resp.data
}
