// src/api/config.ts
const CURRENT_CONFIG: any = window.CURRENT_CONFIG || {} // 从全局对象中获取配置

// 如果需要在加载时进行初始化，可以在这里添加逻辑

export { CURRENT_CONFIG }
