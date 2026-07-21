import type { RuntimeConfig } from '/@/types/runtime-config'

const CURRENT_CONFIG: RuntimeConfig = window.CURRENT_CONFIG || {}

// 如果需要在加载时进行初始化，可以在这里添加逻辑

export { CURRENT_CONFIG }
