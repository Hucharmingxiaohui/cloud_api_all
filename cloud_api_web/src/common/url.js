/**
 * 工具函数，路径拼接
 */
export function getImageUrl (baseURL:string, path:string) {
  if (!path) return ''
  if (!baseURL) return ''

  const lastPath = path.replace(/^\/+/, '') // 移除开头的斜杠

  // 确保baseURL以斜杠结尾，路径不以斜杠开头
  if (baseURL.endsWith('/')) {
    return baseURL + lastPath
  } else {
    return baseURL + '/' + lastPath
  }
}
