// Keep this module free of Cesium imports: it is also used by the 2D shell.
export function supportsCesium (scope: any = window): boolean {
  const firefox = /Firefox\/(\d+)/.exec(scope.navigator?.userAgent || '')
  return (!firefox || Number(firefox[1]) >= 102) &&
    ['TransformStream', 'ReadableStream', 'WritableStream'].every(name => typeof scope[name] === 'function')
}

let pending: Promise<void> | undefined

export function ensureCesium (): Promise<void> {
  if (!supportsCesium()) return Promise.reject(new Error('当前浏览器不支持本地三维地图'))
  const scope = window as any
  if (scope.Cesium) return Promise.resolve()
  if (pending) return pending
  pending = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `${import.meta.env.BASE_URL}cesium/Cesium.js`
    script.async = true
    const timeout = window.setTimeout(() => finish(new Error('三维地图加载超时')), 30000)
    const finish = (error?: Error) => {
      window.clearTimeout(timeout)
      script.onload = null
      script.onerror = null
      if (error) { script.remove(); reject(error) } else resolve()
    }
    script.onload = () => finish(scope.Cesium ? undefined : new Error('三维地图初始化失败'))
    script.onerror = () => finish(new Error('三维地图文件加载失败'))
    document.head.appendChild(script)
  }).catch(error => { pending = undefined; throw error })
  return pending
}

// Invoke the import only AFTER the runtime is ready, including Vite's preloads.
export async function loadCesiumPage (loader: () => Promise<any>): Promise<any> {
  if (!supportsCesium()) return import('../components/cesium/CesiumUnavailable.vue')
  try {
    await ensureCesium()
    return await loader()
  } catch (error) {
    console.error('Cesium page load failed:', error)
    return import('../components/cesium/CesiumUnavailable.vue')
  }
}
