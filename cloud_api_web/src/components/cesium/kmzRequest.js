import axios from 'axios'

// 生成航线请求并发送给后端
export async function generateWaylineRequest (waypointsData, selectedWayline) {
  try {
    // 生成模板数据
    const templateData = {
      routeName: selectedWayline,
      templateType: 'waypoint',
      takeOffRefPoint: '37.5198337593825,121.365897020867,108.75',
      droneType: 100,
      subDroneType: 1,
      payloadType: 99,
      payloadPosition: 0,
      executeHeightMode: 'WGS84',
      distance: 76.0107269287109,
      duration: 37.8793931007385,
      imageFormat: 'visible,ir',
      finishAction: 'noAction', // 'returnToHome',
      exitOnRcLostAction: 'goBack',
      globalHeight: 50,
      autoFlightSpeed: 10,
      waypointHeadingReq: {
        waypointHeadingMode: 'fixed',
        waypointHeadingAngle: 0
      },
      waypointTurnReq: {
        waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature',
        useStraightLine: 1
      },
      gimbalPitchMode: 'usePointSetting',
      startActionList: [
        {
          actionIndex: 0,
          gimbalYawRotateAngle: -90,
          hoverTime: 1.0,
          takePhotoType: 2,
          useGlobalImageFormat: 1,
          imageFormat: 'ir,visible',
          gimbalPitchRotateAngle: -90,
          zoom: 1.0
        },
        {
          actionIndex: 1,
          gimbalYawRotateAngle: -90,
          hoverTime: 1.0,
          takePhotoType: 2,
          useGlobalImageFormat: 1,
          imageFormat: 'ir,visable',
          gimbalPitchRotateAngle: -90,
          zoom: 1.0
        }
      ],
      routePointList: [] // 存储航点信息，将根据航点数据动态生成
    }

    // 遍历航点数据，生成 routePointList 内容
    waypointsData.forEach((point, index) => {
      console.log(`第 ${index + 1} 个航点：`, point)
      const routePoint = {
        routePointIndex: index, // 航点索引
        longitude: point.longitude, // 经度
        latitude: point.latitude, // 纬度
        height: point.height, // - 8.12, // 高度
        speed: 5.0, // 默认速度
        gimbalPitchAngle: point.camera_params.pitch, // 默认俯仰角
        waypointHeadingReq: {
          waypointHeadingMode: 'fixed',
          waypointHeadingAngle: point.camera_params.heading // 默认航向角
        },
        waypointTurnReq: {
          waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature',
          useStraightLine: 1
        },
        actions: [
          {
            actionIndex: 0,
            gimbalYawRotateAngle: point.camera_params.heading + 3.3, // 目前拍出来的画面偏左，向右补偿4度
            gimbalPitchRotateAngle: point.camera_params.pitch - 1.5, /// / 目前拍出来的画面偏上，向下补偿两度
            gimbalControlMode: 'absolute',
            takePhotoType: 2,
            useGlobalImageFormat: 0,
            imageFormat: 'ir,visable',
            payloadPositionIndex: 0,
            useGlobalPayloadLensIndex: 0,
            payloadLensIndex: 'ir,visable',
            orientedCameraType: 81,
            accurateFrameValid: 1,
            orientedPhotoMode: 'normalPhoto',
            focalLength: point.camera_params.focalLength,
            gimbalPort: 0,
            imageWidth: 960,
            imageHeight: 720,
            orientedCameraApertue: 440,
            orientedCameraLuminance: 3800,
            orientedCameraShutterTime: 0.003,
            orientedCameraISO: 100,
            AFPos: 159,
            focusX: 480,
            focusY: 360,
            focusRegionWidth: 480,
            focusRegionHeight: 360
          },
          {
            actionIndex: 1,
            aircraftHeading: -156,
            aircraftPathMode: 'counterClockwise'
          }
        ],
        timeInterval: 2,
        distanceInterval: 0,
        endIntervalRouteIndex: index,
        actionGroup: {
          actionGroupId: index + 1,
          actionGroupStartIndex: 0,
          actionGroupEndIndex: 1,
          actionGroupMode: 'sequence',
          actionTrigger: {
            actionTriggerType: 'reachPoint'
          }
        }
      }

      // 将生成的航点数据添加到 routePointList 中
      templateData.routePointList.push(routePoint)
    })
    console.log('发送的数据:', JSON.stringify(templateData, null, 2)) // 格式化打印数据
    // 调用接口发送生成的航线数据
    // const response = await axios.post('/api/buildKmz', templateData)//本地测试使用vite本地代理

    // 不能对返回kmz下载
    // const isDev = import.meta.env.MODE === 'development'
    // const baseURL = isDev
    //   ? '/api' // 开发用 Vite proxy
    //   : window.CURRENT_CONFIG?.kmzURL?.replace(/\/$/, '') || '/api' // 生产读取 config.js 或 fallback  上线读取

    // const http = axios.create({
    //   baseURL,
    //   timeout: 10000
    // })
    // const response = await http.post('/buildKmz', templateData)

    // if (response.status === 200) {
    //   return response.data // 返回后端的响应数据
    // } else {
    //   throw new Error('请求失败')
    // }

    // 能够返回kmz下载
    const isDev = import.meta.env.MODE === 'development'
    const baseURL = isDev
      ? '/api'
      : (window.CURRENT_CONFIG?.kmzURL?.replace(/\/$/, '') || '/api')

    const http = axios.create({
      baseURL,
      timeout: 10000,
      responseType: 'blob', // ← 关键：以二进制拿回
    })

    const res = await http.post('/buildKmz', templateData)

    // 兼容后端可能返回错误JSON的情况：先判断是不是json
    const ctype = res.headers['content-type'] || ''
    const isJson = ctype.includes('application/json')
    if (isJson) {
      // 读取错误信息
      const text = await res.data.text?.() || await new Response(res.data).text()
      try { throw new Error(JSON.parse(text).message || text) } catch { throw new Error(text) }
    }

    // 解析文件名（从 Content-Disposition 拿；拿不到就兜底）
    const cd = res.headers['content-disposition'] || ''
    const m = cd.match(/filename\*=UTF-8''([^;]+)|filename="([^"]+)"/)
    const filename = decodeURIComponent(m?.[1] || m?.[2] || ((templateData.routeName || '默认航线') + '.kmz'))

    // 触发浏览器下载
    const blob = new Blob([res.data], { type: 'application/vnd.google-earth.kmz' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)

    // 如需把结果也返回给调用者：
    return { ok: true, filename }
  } catch (error) {
    console.error('请求错误:', error)
    throw error // 确保错误能被外层捕获
  }
}
