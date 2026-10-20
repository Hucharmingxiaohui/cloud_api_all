/**
 * 云渲染航线多拍照动作（photo_actions）纯函数测试。
 * 通过 ts.transpileModule 加载源码，fake require 屏蔽 '/@/*' 平台别名依赖。
 * 运行：node scripts/test-cloud-photo-actions.cjs
 */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

function loadModule (rel) {
  const file = path.join(__dirname, '..', 'src', 'components', 'cloudRenderer', rel)
  const src = fs.readFileSync(file, 'utf8')
  const out = ts.transpileModule(src, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2019 }
  }).outputText
  const mod = { exports: {} }
  // 测试加载器：在 Node 里执行 TS 转译产物，需要动态构造模块函数
  // eslint-disable-next-line no-new-func
  new Function('module', 'exports', 'require', out)(mod, mod.exports, () => ({}))
  return mod.exports
}

const kmz = loadModule('cloudWaylineKmz.ts')
const mapper = loadModule('cloudWaylineMapper.ts')

const base = {
  point_name: '变电所_01',
  longitude: 121.3658,
  latitude: 37.5198,
  height: 120.5,
  capture_mode: 'visable',
  speed: 5,
  headingTrue: 90,
  camera_params: { heading: 90, pitch: -45, roll: 0, focalLength: 75 }
}

// 1. normalizeWaypoint 保留并规范化 photo_actions
const normalized = kmz.normalizeWaypoint({
  ...base,
  photo_actions: [
    { imageFormat: 'visable', focalLength: 30, pitch: -45, yaw: 450 },
    { imageFormat: 'ir', focalLength: 75, pitch: -50 }
  ]
}, 0)
assert.equal(normalized.photo_actions?.length ?? -1, 2, '应保留 2 个拍照动作')
// 偏航规范化到 [0,360)：450 -> 90
assert.deepEqual(normalized.photo_actions[0], { imageFormat: 'visable', focalLength: 30, pitch: -45, yaw: 90 })
assert.deepEqual(normalized.photo_actions[1], { imageFormat: 'ir', focalLength: 75, pitch: -50 })
// 偏航缺省/null 不产出字段
const noYaw = kmz.normalizeWaypoint({
  ...base,
  photo_actions: [{ imageFormat: 'visable', focalLength: 30, pitch: -45, yaw: null }]
}, 0)
assert.equal('yaw' in noYaw.photo_actions[0], false, '空偏航应省略字段')

// 2. 非法项剔除、上限 10、缺省 undefined
const twelve = Array.from({ length: 12 }, () => ({ imageFormat: 'visable', focalLength: 30, pitch: -45 }))
assert.equal(kmz.normalizeWaypoint({ ...base, photo_actions: twelve }, 0).photo_actions.length, 10, '上限 10 个')
assert.equal(
  kmz.normalizeWaypoint({ ...base, photo_actions: [{ imageFormat: 'visable', focalLength: 0, pitch: -45 }] }, 0).photo_actions.length,
  0,
  '焦距非法项应剔除'
)
assert.equal(kmz.normalizeWaypoint({ ...base }, 0).photo_actions, undefined, '缺省保持 undefined')

// 3. buildWaylineRequest：多动作逐项生成，actionIndex 从 1 顺延
const multiReq = kmz.buildWaylineRequest('route', [{
  ...base,
  photo_actions: [
    { imageFormat: 'visable', focalLength: 30, pitch: -45, yaw: 200 },
    { imageFormat: 'visable,ir', focalLength: 75, pitch: -50 }
  ]
}])
const multiActions = multiReq.routePointList[0].actions
assert.equal(multiActions.length, 3, '机头动作 + 2 个拍照动作')
assert.equal(multiActions[1].actionIndex, 1)
assert.equal(multiActions[1].takePhotoType, 2)
assert.equal(multiActions[1].imageFormat, 'visable')
assert.equal(multiActions[1].focalLength, 30)
assert.equal(multiActions[1].gimbalPitchRotateAngle, -45)
assert.equal(multiActions[1].gimbalYawRotateAngle, 200, '逐动作偏航写入 KMZ')
assert.equal(multiActions[2].actionIndex, 2)
assert.equal(multiActions[2].imageFormat, 'visable,ir')
assert.equal(multiActions[2].gimbalYawRotateAngle, 90, '缺省偏航跟随航点机头')
assert.equal(multiActions[1].orientedFileSuffix, '变电所_01', '名称搭 orientedFileSuffix 往返')
assert.equal(multiActions[2].orientedFileSuffix, '变电所_01')

// 4. 空数组 = 过渡点（即使 capture_mode 非 none 也不生成拍照动作）
const transitional = kmz.buildWaylineRequest('route', [{ ...base, photo_actions: [] }])
assert.equal(transitional.routePointList[0].actions.length, 1, '空数组仅保留机头动作')

// 5. 缺省 photo_actions = 既有单动作模式（向后兼容）
const single = kmz.buildWaylineRequest('route', [{ ...base }])
assert.equal(single.routePointList[0].actions.length, 2)
assert.equal(single.routePointList[0].actions[1].imageFormat, 'visable')
assert.equal(single.routePointList[0].actions[1].focalLength, 75)

// 6. validateWaypoints：超限报错，合法通过
assert.ok(kmz.validateWaypoints([{ ...base, photo_actions: twelve }]), '超过 10 个应报错')
assert.equal(kmz.validateWaypoints([{ ...base, photo_actions: twelve.slice(0, 10) }]), '')

// 7. KMZ 回读：2 个 orientedShoot 动作 → photo_actions 重建
const draft = mapper.mapParsedWaylineToCloudDraft({
  folder: {
    autoFlightSpeed: 5,
    placemarks: [{
      point: { coordinates: '121.3658,37.5198' },
      ellipsoidHeight: 120.5,
      actionGroup: {
        actionList: [
          { actionActuatorFunc: 'orientedShoot', actionActuatorFuncParam: { gimbalYawRotateAngle: 90, gimbalPitchRotateAngle: -45, focalLength: 30, imageFormat: 'visable', orientedFileSuffix: '变电所_01' } },
          { actionActuatorFunc: 'orientedShoot', actionActuatorFuncParam: { gimbalYawRotateAngle: 200, gimbalPitchRotateAngle: -50, focalLength: 75, imageFormat: 'ir', orientedFileSuffix: '变电所_01' } }
        ]
      }
    }]
  }
}, { waylineId: 'w1', routeName: 'r1' })
const multiPoint = draft.waypoints[0]
assert.equal(multiPoint.point_name, '变电所_01')
assert.equal(multiPoint.headingTrue, 90, '机头取首个动作/航向参数')
assert.equal(multiPoint.photo_actions?.length ?? -1, 2, '回读应重建 2 个拍照动作')
// 与机头一致的偏航省略（跟随语义），不同的偏航显式保留
assert.deepEqual(multiPoint.photo_actions[0], { imageFormat: 'visable', focalLength: 30, pitch: -45 })
assert.deepEqual(multiPoint.photo_actions[1], { imageFormat: 'ir', focalLength: 75, pitch: -50, yaw: 200 })

// 8. 单动作回读不产生 photo_actions（向后兼容）
const singleDraft = mapper.mapParsedWaylineToCloudDraft({
  folder: {
    autoFlightSpeed: 5,
    placemarks: [{
      point: { coordinates: '121.3658,37.5198' },
      ellipsoidHeight: 120.5,
      actionGroup: {
        actionList: [
          { actionActuatorFunc: 'orientedShoot', actionActuatorFuncParam: { gimbalYawRotateAngle: 90, gimbalPitchRotateAngle: -45, focalLength: 75, imageFormat: 'visable', orientedFileSuffix: 'WP_001' } }
        ]
      }
    }]
  }
}, { waylineId: 'w2', routeName: 'r2' })
assert.equal(singleDraft.waypoints[0].photo_actions, undefined, '单动作保持 undefined')

console.log('cloud photo actions tests passed')
