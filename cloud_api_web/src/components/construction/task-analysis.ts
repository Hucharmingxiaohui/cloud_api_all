import type { ConstructionAnalysis } from '/@/api/construction'

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
