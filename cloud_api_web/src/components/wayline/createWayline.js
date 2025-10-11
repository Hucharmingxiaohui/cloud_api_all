/**
 * 创建航线文件
 */

const data = {
  missionConfig: { // globalRTHHeight 全局返航高度；
    flyToWaylineMode: 'safely', // 飞向首航点模式
    finishAction: 'goHome', // 航线结束动作
    exitOnRCLost: 'executeLostAction', // 失控是否继续执行航线
    executeRCLostAction: 'goBack', // 失控动作类型
    takeOffSecurityHeight: 20, // 安全起飞高度
    globalTransitionalSpeed: 15, // 全局航线过渡速度，飞行器飞往每条航线首航点的速度
    droneInfo: { // 飞行器机型信息
      droneEnumValue: 77, // 飞行器机型主类型   型号
      droneSubEnumValue: 0 // 飞行器机型子类型 （多光谱、红外..）
    },
    payloadInfo: { // 负载信息
      payloadEnumValue: 66, // 负载机型主类型
      payloadSubEnumValue: 0,
      payloadPositionIndex: 0 // 负载挂载位置
    },
    fileName: '视讯楼顶1234' // 航线名称
  },
  folder: {
    templateType: 'waypoint', // 航线类型
    templateId: 0, // 模板ID
    waylineId: 0,
    executeHeightMode: 'relativeToStartPoint', // 执行高度模式：椭球高模式（WGS84）、相对起飞点高度模式
    waylineCoordinateSysParam: {}, // 坐标系参数
    autoFlightSpeed: 15, // 全局航线飞行速度
    globalHeight: 100, // 全局高度
    caliFlightEnable: 0, // 是否开启标定飞行
    gimbalPitchMode: 'manual', // 云台俯仰角模式
    globalWaypointHeadingParam: { // 全局偏航角模式参数
      waypointHeadingMode: 'followWayline'
    },
    globalWaypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature', // 全局航点类型（全局航点转弯模式）
    globalUseStraightLine: 1, // 全局航段轨迹是否尽量贴合直线 限定globalWaypointTurnMode部分设置后必需
    placeMarks: [
    ],
    payloadParam: { // 负载设置
      payloadPositionIndex: 0, // 负载挂载位置
      imageFormat: 'wide,zoom' // 图片格式列表
    }
  }
}

const pointTemplate = {
  point: {
    coordinates: ''
  },
  ellipsoidHeight: 0,
  height: 0,
  useGlobalHeight: 0,
  useGlobalSpeed: 1,
  useGlobalHeadingParam: 1,
  useGlobalTurnParam: 1,
  waypointSpeed: 0,
  gimbalPitchAngle: 0,
  useStraightLine: 0,
  actionGroup: {
    actionGroupStartIndex: 0,
    actionGroupEndIndex: 0,
    actionTrigger: {
      actionTriggerType: 'reachPoint',
      actionTriggerParam: 0
    },
    actionList: [
      {
        actionActuatorFunc: 'orientedShoot', // 动作类型：定向拍照动作..
        actionActuatorFuncParam: {
          orientedShoot: {
            gimbalPitchRotateAngle: -57.8,
            gimbalRollRotateAngle: 0,
            gimbalYawRotateAngle: 77.87313,
            focusX: 0,
            focusY: 0,
            focusRegionWidth: 0,
            focusRegionHeight: 0,
            focalLength: 154.2,
            aircraftHeading: 77,
            accurateFrameValid: 0,
            payloadPositionIndex: 0,
            payloadLensIndex: 'ir,zoom,wide',
          }
        }
      }
    ]
  },
  isRisky: 0, // 是否危险点
  waypointHeadingParam: {}, // 偏航角参数
  waypointTurnParam: {} // 转弯模式参数
}

/**
 * 生成航线文件
 * @param {[[Lon,lat,height]]} 点位数据
 */
export function createSampleWayline (points) {
  let tempWayLine = []
  // const aa = [[37.51887700000004, 121.365815, 104.16000000201166, 0.0], [37.518889304404105, 121.36578556150079, 110.59132765606046, 1.0], [37.51890160877591, 121.36575612305117, 117.02265666425228, 1.0], [37.51893733974753, 121.36575612305117, 120.06777343899012, 1.0], [37.518963924487416, 121.36567839189483, 124.4349324721843, 1.0], [37.518978204919335, 121.36563009908883, 122.37076354306191, 1.0], [37.51899248534073, 121.36558180623322, 120.30659786239266, 1.0], [37.51901906992678, 121.36550407507995, 124.67377631738782, 1.0], [37.51903335030465, 121.36545578215066, 122.6096192151308, 1.0], [37.51904763067201, 121.36540748917174, 120.54546536225826, 1.0], [37.519061911028864, 121.3653591961432, 118.48131475690752, 1.0], [37.5190761913752, 121.36531090306505, 116.4171674028039, 1.0], [37.519054740781556, 121.36526260993726, 111.30789592210203, 1.0], [37.51905671702005, 121.36524375485438, 102.81240156013519, 1.0], [37.51907099735269, 121.36519546160176, 100.74826088361442, 1.0], [37.51907600000004, 121.365187, 99.87000000104308, 0.0], [37.518726000000036, 121.365113, 96.0, 0.0], [37.51874745097181, 121.36516129310856, 101.10924897994846, 1.0], [37.518733170758175, 121.3652095861675, 103.17339658550918, 1.0], [37.518718890534025, 121.3652578791768, 105.23754744231701, 1.0], [37.51870461029937, 121.36530617213647, 107.30170154664665, 1.0], [37.51869033005421, 121.36535446504652, 109.36585890222341, 1.0], [37.51867604979853, 121.36540275790696, 111.4300195062533, 1.0], [37.5186975006401, 121.36545105071777, 116.53928353264928, 1.0], [37.51874237801971, 121.36552878161903, 118.26232067961246, 1.0], [37.51872809769597, 121.36557707435618, 120.32649014610797, 1.0], [37.518772974957386, 121.36565480528454, 122.04954194277525, 1.0], [37.518782121134244, 121.36573253626409, 120.72749652899802, 1.0], [37.51880554770524, 121.36576197462978, 117.34127717278898, 1.0], [37.518843254751935, 121.36574312030491, 111.89088059682399, 1.0], [37.518866681385155, 121.36577255874425, 108.50466439314187, 1.0], [37.51889010803589, 121.36580199723328, 105.1184503166005, 1.0], [37.51887700000004, 121.365815, 104.16000000201166, 0.0]]

  tempWayLine = points.map((point) => {
    const [longitude, latitude, height, flag] = point
    // 基于模板创建新对象，避免引用同一内存地址
    if (flag) {
      return {
        ...pointTemplate, // 复制模板的现有属性
        point: {
          ...pointTemplate.point, // 复制嵌套对象的属性
          coordinates: `${longitude},${latitude}`
        },
        ellipsoidHeight: `${height}`
      }
    } else {
      return {
        ...pointTemplate, // 复制模板的现有属性
        point: {
          ...pointTemplate.point, // 复制嵌套对象的属性
          coordinates: `${longitude},${latitude}`
        },
        ellipsoidHeight: `${height}`,
        // 新增：清空 actionGroup 中的 actionList
        actionGroup: {
          ...pointTemplate.actionGroup, // 保留 actionGroup 的其他属性
          actionList: [] // 覆盖 actionList 为空数组
        }
      }
    }
  })
  data.folder.placeMarks = tempWayLine
  console.log('输出结果', data)
  return data
}

export function getEllipsoidHeight (lon, lat, h) {
  const R = 6378137
  const f = 1 / 298.257223563
  const e = Math.sqrt(f * (2 - f))

  const N = R / Math.sqrt(1 - e * e * Math.sin(lat) * Math.sin(lat))
  const X = (N + h) * Math.cos(lat) * Math.cos(lon)
  const Y = (N + h) * Math.cos(lat) * Math.sin(lon)
  const Z = (N * (1 - e * e) + h) * Math.sin(lat)
  const a = 6378137
  const b = 6356752.3142
  const e2 = (a * a - b * b) / (a * a)
  const p = Math.sqrt(X * X + Y * Y)
  const theta = Math.atan2(Z * a, p * b)
  const Ing = Math.atan2(Y, X)
  const lat1 = Math.atan2(Z + e2 * b * Math.pow(Math.sin(theta), 3), p - e * a * Math.pow(Math.cos(theta), 3))
  const N1 = a / Math.sqrt(1 - e * e * Math.pow(Math.sin(lat1), 2))
  const h1 = p / Math.cos(lat1) - N1
  return h1
}
