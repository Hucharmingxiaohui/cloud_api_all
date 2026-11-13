const statusLabels = ['待执行', '已完成', '执行中', '失败', '已下发待执行', '暂停', '终止']

export function taskStatusText (status: unknown): string {
  if (typeof status !== 'number' && typeof status !== 'string') return '未知'
  const key = String(status)
  return /^[0-6]$/.test(key) ? statusLabels[Number(key)] : '未知'
}

export function parseTaskStatusResponse (response: any, taskId: string): string {
  if (!response || response.success === false ||
      (response.code != null && response.code !== 200) ||
      !(response.success === true || response.code === 200) ||
      response.data?.taskId !== taskId) return '获取失败'
  return taskStatusText(response.data.status ?? response.data.taskStatus)
}

export function createTaskStatusLoader (fetchStatus: (taskId: string) => Promise<unknown>) {
  let generation = 0
  let active = 0
  const queue: Array<() => void> = []
  const isCurrent = (token: number) => token === generation

  function pump () {
    const slots = 3 - active
    for (let i = 0; i < slots && queue.length; i++) queue.shift()!()
  }

  function load (token: number, rows: Array<{ success: unknown; euaTaskId?: string }>, update: (id: string, text: string) => void) {
    const ids = [...new Set(rows
      .filter(row => (row.success === 1 || row.success === '1') && typeof row.euaTaskId === 'string' && row.euaTaskId.trim())
      .map(row => row.euaTaskId!))]
    return Promise.all(ids.map(id => new Promise<void>(resolve => {
      queue.push(() => {
        if (!isCurrent(token)) { resolve(); return }
        active++
        Promise.resolve().then(() => fetchStatus(id))
          .then(response => {
            if (isCurrent(token)) update(id, parseTaskStatusResponse(response, id))
          }, () => {
            if (isCurrent(token)) update(id, '获取失败')
          })
          .finally(() => { active--; resolve(); pump() })
      })
      pump()
    })))
  }

  return { begin: () => ++generation, isCurrent, load }
}
