import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'
import { downloadWaylineFile, getWaylineDetail } from '/@/api/wayline'
import { ELocalStorageKey } from '/@/types'
import { parseKmzBlobToCloudDraft } from './kmzBrowserParser'
import { saveCloudWaylineEditDraft } from './cloudWaylineMapper'

export interface OpenCloudWaylineEditOptions {
  router: Router
  waylineId: string
  routeName?: string
}

export async function openCloudWaylineEdit (options: OpenCloudWaylineEditOptions) {
  const waylineId = String(options.waylineId || '').trim()
  if (!waylineId) {
    ElMessage.error('航线 ID 无效')
    return
  }

  const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)
  if (!workspaceId) {
    ElMessage.error('未获取到工作空间，请重新登录后再试')
    return
  }

  try {
    const routeName = (options.routeName || '').trim() || await resolveWaylineName(workspaceId, waylineId)
    const blob = await downloadWaylineFile(workspaceId, waylineId)
    if (!blob) throw new Error('航线文件下载失败')
    const draft = await parseKmzBlobToCloudDraft(blob, {
      waylineId,
      routeName
    })
    saveCloudWaylineEditDraft(draft)
    await options.router.push({
      path: '/wayline/cloud3d-editor',
      query: { mode: 'edit', waylineId }
    })
  } catch (error) {
    console.error('[cloud3d-edit] failed', error)
    ElMessage.error(error instanceof Error ? error.message : '打开三维编辑失败')
  }
}

async function resolveWaylineName (workspaceId: string, waylineId: string) {
  try {
    const res = await getWaylineDetail(workspaceId, waylineId)
    if (res?.code !== 0 || !res.data) return ''
    const detail = res.data as Record<string, unknown>
    return String(detail.name || detail.wayline_name || detail.file_name || '').trim()
  } catch (error) {
    console.warn('[cloud3d-edit] resolve wayline name failed', error)
    return ''
  }
}
