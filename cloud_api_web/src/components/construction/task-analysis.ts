import type { ConstructionAnalysis } from '/@/api/construction'

// Translate persisted platform messages too; leave unknown service details intact.
export function batchMessageText (message?: string) {
  const messages: Record<string, string> = {
    'All protocol results retrieved; partial checks or evidence unavailable. NOT an all-clear': '分析结果已取回，部分检测、区域人数统计或标注图不可用，请查看对应项目的说明。批次完成不代表现场无隐患。',
    'All protocol results retrieved; inspect detections and area status. Not an all-clear': '分析结果已取回，请查看逐项检测和区域统计。批次完成不代表现场无隐患。',
    'Result retrieval or protocol validation pending; saved evidence retained. Retry refresh on the same remote task': '结果仍在取回或校验中，已保存的证据保留。请刷新当前批次，无需重新提交。',
    'Status refresh failed; retained remote task and saved evidence; retry refresh': '进度查询失败，分析任务和已有证据已保留，请稍后刷新。',
    'Algorithm task failed': '分析服务执行失败，请核对服务状态及返回信息。',
    'Remote acceptance is uncertain; do not resubmit': '尚未确认分析服务是否接收任务，请先查询原批次，避免重复提交。',
    'Submission rejected before processing': '分析任务未能提交，请核对提交条件及服务状态。'
  }
  return messages[message || ''] || message || ''
}

export const reportAllowed = (batch: ConstructionAnalysis | null) => batch?.status === 'SUCCEEDED' && typeof batch.report_ready === 'boolean'
export function photoLens (name: string) {
  const thermal = /(?:^|_)T(?=_|\.|$)/i.test(name); const visible = /(?:^|_)V(?=_|\.|$)/i.test(name)
  return thermal === visible ? 'unknown' : thermal ? 'infrared' : 'visible'
}
export function reportFileName (job: string, batch: string) { return Array.from(`工地巡检报告-${job}-${batch}.docx`, char => char.charCodeAt(0) < 32 || char.charCodeAt(0) === 127 || '\\/:*?"<>|'.includes(char) ? '_' : char).join('') }
interface PollOptions {
  fetch: (job: string, batch: string) => Promise<ConstructionAnalysis>
  receive: (batch: ConstructionAnalysis) => void
  error: (error: unknown) => void
  schedule: (callback: () => Promise<void>) => ReturnType<typeof setTimeout>
  cancel: (timer: ReturnType<typeof setTimeout>) => void
}
export function createBatchPoller (options: PollOptions) {
  let generation = 0; let disposed = false; let timer: ReturnType<typeof setTimeout> | undefined
  function reset () { generation++; if (timer !== undefined) options.cancel(timer); timer = undefined }
  function watch (job: string, batch: ConstructionAnalysis) {
    reset(); const token = generation
    if (disposed || !batch.batch_id || !['SUBMITTING', 'QUEUED', 'RUNNING'].includes(batch.status || '')) return
    const id = batch.batch_id
    async function tick () {
      timer = undefined
      if (disposed || token !== generation) return
      try {
        const result = await options.fetch(job, id)
        if (disposed || token !== generation) return
        options.receive(result)
        if (!['SUBMITTING', 'QUEUED', 'RUNNING'].includes(result.status || '')) return
      } catch (error) { if (disposed || token !== generation) return; options.error(error) }
      if (!disposed && token === generation) timer = options.schedule(tick)
    }
    timer = options.schedule(tick)
  }
  return { watch, reset, dispose: () => { disposed = true; reset() } }
}
