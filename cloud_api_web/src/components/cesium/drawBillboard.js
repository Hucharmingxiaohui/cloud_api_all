import * as Cesium from 'cesium'
let canvas, context
/**
 * @author: linmaoxin
 * @date: 2024/12/14
 * @description 渲染billboard,可选择 带背景的文字+图标icon、仅背景的文字 两种类型。1、返回Promise对象，异步获取billboard；2、也可以使用回调分别获取image和pixelOffset。
 * @param {*} options
 * @callback function
 * @returns Promise<object>
 * @example
 * let options = {
        type: 'text-icon',  // 默认 'text-icon'类型(图标+文字)、 'text' 类型(仅文字)
        paddingTo: 20, // 图标距离文字背景
        textPadding: 20, // 文字左右间距
        textBcgWidth: 200, // 文字背景宽度
        textBcgHeight: 50, // 文字背景高度
        iconWidth: 60, // 图标宽度
        iconHeight: 70, // 图标高度
        textBcgUrl: '../assets/label-bcg.png', // 文字背景图片地址
        iconUrl: '../assets/positionIcon.png', // 图标地址
        textUrl:'', // 字体地址
        text: '测试文字测试文字', // 文字内容
        textColor: '#DCD085', // 字体颜色
        textFontSize: 16, // 字体大小
        textFontWeight: 800, // 字体粗细
    }
 */
export function renderBillboard (options, callback) {
  const settings = {
    type: options.type || 'text-icon', // 默认 'text-icon'类型(图标+文字)、 'text' 类型(仅文字)
    paddingTo: options.paddingTo || 5, // 默认 图标距离文字背景
    textPadding: options.textPadding || 20, // 默认 文字左右间距

    textBcgWidth: options.textBcgWidth || 150, // 默认 文字背景宽度
    textBcgHeight: options.textBcgHeight || 40, // 默认 文字背景高度

    iconWidth: options.iconWidth || 40, // 默认 图标宽度
    iconHeight: options.iconHeight || 50, // 默认 图标高度

    textBcgUrl: options.textBcgUrl || console.error('请传入文字背景图片地址 textBcgUrl '), // 文字背景图片地址
    iconUrl: options.iconUrl || console.error('请传入图标图片地址 iconUrl '), // 图标地址
    textUrl: options.textUrl, // 文字地址

    text: options.text || '测试', // 文字内容
    textColor: options.textColor || '#ffffff', // 默认 文字颜色
    textFontSize: options.textFontSize || 16, // 默认 文字大小
    textFontWeight: options.textFontWeight || 500, // 默认 文字粗细
  }

  if (!canvas) {
    canvas = document.createElement('canvas')
  }
  if (!context) {
    context = canvas.getContext('2d', { willReadFrequently: true })
  }
  // 字体初始化
  context.font = `${Number(settings.textFontWeight)} ${Number(settings.textFontSize)}px Microsoft Yahei`
  context.fillStyle = settings.textColor
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.canvas.willReadFrequently = true
  // 根据字数计算宽度 并加上左右间距
  const textMetrics = context.measureText(settings.text)
  settings.textBcgWidth = textMetrics.width + settings.textPadding
  // 画布大小
  if (settings.type === 'text-icon') {
    settings.textBcgWidth = settings.textBcgWidth > settings.iconWidth ? settings.textBcgWidth : settings.iconWidth
    canvas.width = settings.textBcgWidth
    canvas.height = settings.textBcgHeight + settings.paddingTo + settings.iconHeight
  } else if (settings.type === 'text') {
    canvas.width = settings.textBcgWidth
    canvas.height = settings.textBcgHeight
  }
  return new Promise((resolve, reject) => {
    // 加载图片资源
    if (settings.type === 'text-icon') {
      // 字体二次设置
      context.font = `${Number(settings.textFontWeight)} ${Number(settings.textFontSize)}px Microsoft Yahei`
      context.fillStyle = settings.textColor
      context.textAlign = 'center'
      context.textBaseline = 'middle'
      const image1 = new Image()
      const image2 = new Image()
      image1.src = new URL(settings.iconUrl, import.meta.url).href // vite
      image2.src = new URL(settings.textBcgUrl, import.meta.url).href // vite

      Promise.all([
        new Promise((resolve) => { image1.onload = resolve }),
        new Promise((resolve) => { image2.onload = resolve })
      ]).then(() => {
        // 清除画布
        context.clearRect(0, 0, canvas.width, canvas.height)
        context.beginPath()

        // 渲染
        context.drawImage(image2, 0, 0, settings.textBcgWidth, settings.textBcgHeight)
        context.drawImage(image1, (canvas.width / 2) - (settings.iconWidth / 2), (settings.textBcgHeight + settings.paddingTo), settings.iconWidth, settings.iconHeight)
        context.fillText(settings.text, settings.textBcgWidth / 2, settings.textBcgHeight / 2)

        // 判断callback是不是函数
        if (typeof callback === 'function') {
          callback(null, { image: canvas.toDataURL(), pixelOffsetY: Math.round(-canvas.height / 2) })
        }
        resolve({
          image: canvas.toDataURL(),
          pixelOffset: new Cesium.Cartesian2(0, Math.round(-canvas.height / 2))
        })
      }).catch((error) => {
        reject(error)
      })
    } else if (settings.type === 'text') {
      const image2 = new Image()
      image2.src = new URL(settings.textBcgUrl, import.meta.url).href // vite
      image2.onload = () => {
        // 清除画布
        context.clearRect(0, 0, canvas.width, canvas.height)
        context.beginPath()
        // 渲染
        context.drawImage(image2, 0, 0, canvas.width, canvas.height)
        context.fillText(settings.text, settings.textBcgWidth / 2, settings.textBcgHeight / 2)
        // 判断callback是不是函数
        if (typeof callback === 'function') {
          callback(null, { image: canvas.toDataURL(), pixelOffsetY: Math.round(-canvas.height / 2) })
        }
        resolve({
          image: canvas.toDataURL(),
          pixelOffset: new Cesium.Cartesian2(0, Math.round(-canvas.height / 2))
        })
      }
      image2.onerror = (error) => {
        reject(error)
      }
    }
  })
}
