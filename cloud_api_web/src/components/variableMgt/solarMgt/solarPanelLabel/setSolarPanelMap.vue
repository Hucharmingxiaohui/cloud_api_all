<template>
  <div class="g-map-wrapper">
    <!-- 地图区域 -->
    <div id="g-container" :style="{ width: '100%', height: '100%' }" />
    <!-- 绘制面板 -->
    <div class="g-action-panel">
      <div :class="state.currentType === 'polygon' ? 'g-action-item selection' : 'g-action-item'" @click="draw('polygon', true)">
        <a><BorderOutlined class="fz18" /></a>
      </div>
      <div v-if="mouseMode" class="g-action-item" @click="draw('off', false)">
        <a style="color: red;"><CloseOutlined /></a>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, reactive, ref } from 'vue'
import { generatePolyContent } from '/@/utils/map-layer-utils'
import { MapDoodleType } from '/@/constants/map'
import { useGMapManage } from '/@/hooks/use-g-map'
import { useMouseTool } from '/@/hooks/use-mouse-tool'
import { getApp, getRoot } from '/@/root'
import { GeojsonCoordinate } from '/@/types/map'
import { MapDoodleEnum } from '/@/types/map-enum'
import { uuidv4 } from '/@/utils/uuid'
import { gcj02towgs84 } from '/@/vendors/coordtransform'
import { BorderOutlined, CloseOutlined } from '@ant-design/icons-vue'

export default defineComponent({
  components: {
    BorderOutlined,
    CloseOutlined,
  },
  name: 'SolarPanelMap',
  emits: ['polygon-drawn'],
  setup (props, { emit, expose }) {
    const useMouseToolHook = useMouseTool()
    const useGMapManageHook = useGMapManage()
    const root = getRoot()

    const mouseMode = ref(false)

    // 存储绘制的多边形区域和对应的 marker
    const regionMap = ref<Map<string, {
      id: string
      name: string
      coordinates: GeojsonCoordinate[]
      polygon: any
      marker: any
    }>>(new Map())

    const state = reactive({
      currentType: '',
    })

    // 绘制多边形
    function draw (type: MapDoodleType, bool: boolean) {
      state.currentType = type
      mouseMode.value = bool
      useMouseToolHook.mouseTool(type, getDrawCallback)
    }

    onMounted(() => {
      const app = getApp()
      useGMapManageHook.globalPropertiesConfig(app)
    })

    // 绘制完成回调
    function getDrawCallback ({ obj }: { obj: any }) {
      if (state.currentType === MapDoodleEnum.POLYGON) {
        postPolygonResource(obj)
      }
    }

    // 计算多边形中心点（取顶点平均值）
    function calculateCenter (path: any[]): GeojsonCoordinate {
      let lngSum = 0
      let latSum = 0
      path.forEach((p: any) => {
        lngSum += p.lng
        latSum += p.lat
      })
      return [lngSum / path.length, latSum / path.length]
    }

    // 创建区域名称 marker
    function createRegionMarker (name: string, center: GeojsonCoordinate, regionId: string) {
      const AMap = root.$aMap
      const marker = new AMap.Marker({
        position: new AMap.LngLat(center[0], center[1]),
        title: name,
        content: `<div style="background: rgba(0,0,0,0.7); color: #fff; padding: 4px 10px; border-radius: 4px; font-size: 13px; white-space: nowrap; border: 1px solid #409EFF;">${name}</div>`,
        offset: new AMap.Pixel(0, -15),
        extData: { regionId },
        zIndex: 100,
      })
      root.$map.add(marker)
      return marker
    }

    // 处理多边形绘制结果
    async function postPolygonResource (obj: any) {
      const req = getPolygonResource(obj)
      // 坐标转换为 WGS84 存储
      const wgs84Coordinates = convertCoordinatesToWGS84(req.resource.content.geometry.coordinates[0])

      // 计算中心点并创建 marker
      const path = obj.getPath()
      const center = calculateCenter(path)
      const marker = createRegionMarker(req.name, center, req.id)

      const regionData = {
        id: req.id,
        name: req.name,
        coordinates: wgs84Coordinates, // WGS84 坐标
        polygon: obj, // 高德地图 polygon 对象
        marker: marker, // 高德地图 marker 对象
      }

      // 存储到本地 Map
      regionMap.value.set(req.id, regionData)

      // 通知父组件
      emit('polygon-drawn', {
        id: req.id,
        name: req.name,
        coordinates: wgs84Coordinates,
      })

      // 设置扩展数据
      obj.setExtData({ id: req.id, name: req.name })
    }

    // 生成多边形资源
    function getPolygonResource (obj: any) {
      const path = obj.getPath()
      const resource = generatePolyContent(path)
      const id = uuidv4()
      // 自动生成区域名称，父组件会重新命名
      return {
        id,
        name: '区域',
        resource,
      }
    }

    // 将高德坐标(GCJ-02)转换为 WGS84
    function convertCoordinatesToWGS84 (coordinates: GeojsonCoordinate[]): GeojsonCoordinate[] {
      return coordinates.map((coord) => {
        const [lng, lat] = gcj02towgs84(coord[0], coord[1])
        return [lng, lat]
      })
    }

    // 删除指定区域
    function deleteRegion (regionId: string): boolean {
      const region = regionMap.value.get(regionId)
      if (region) {
        try {
          // 从地图移除 polygon
          if (region.polygon) {
            if (root.$map && typeof root.$map.remove === 'function') {
              root.$map.remove(region.polygon)
            } else if (typeof region.polygon.setMap === 'function') {
              region.polygon.setMap(null)
            }
          }
          // 从地图移除 marker
          if (region.marker) {
            if (root.$map && typeof root.$map.remove === 'function') {
              root.$map.remove(region.marker)
            } else if (typeof region.marker.setMap === 'function') {
              region.marker.setMap(null)
            }
          }
        } catch (e) {
          console.error('删除地图覆盖物失败:', e)
        }
        // 从存储中删除
        regionMap.value.delete(regionId)
        return true
      }
      console.warn('删除区域失败，未找到 regionId:', regionId)
      return false
    }

    // 更新区域名称（同步更新 marker）
    function updateRegionName (regionId: string, newName: string): boolean {
      const region = regionMap.value.get(regionId)
      if (region && region.marker) {
        try {
          // 更新 marker 的 title
          region.marker.setTitle(newName)
          // 更新 marker 的 content（重新生成 HTML）
          const newContent = `<div style="background: rgba(0,0,0,0.7); color: #fff; padding: 4px 10px; border-radius: 4px; font-size: 13px; white-space: nowrap; border: 1px solid #409EFF;">${newName}</div>`
          region.marker.setContent(newContent)
          // 更新本地存储的名称
          region.name = newName
          return true
        } catch (e) {
          console.error('更新区域名称失败:', e)
          return false
        }
      }
      console.warn('更新区域名称失败，未找到 regionId:', regionId)
      return false
    }

    // 获取指定区域的 WGS84 坐标
    function getRegionCoordinates (regionId: string): GeojsonCoordinate[] | null {
      const region = regionMap.value.get(regionId)
      return region ? region.coordinates : null
    }

    // 暴露方法给父组件
    expose({
      deleteRegion,
      updateRegionName,
      getRegionCoordinates,
    })

    return {
      draw,
      mouseMode,
      state,
    }
  }
})
</script>

<style lang="scss" scoped>
.g-map-wrapper {
  height: 100%;
  width: 100%;
  position: relative;

  .g-action-panel {
    position: absolute;
    top: 80px;
    right: 16px;
    z-index: 10;

    .g-action-item {
      width: 28px;
      height: 28px;
      background: white;
      color: $primary;
      border-radius: 2px;
      line-height: 28px;
      text-align: center;
      margin-bottom: 2px;
      cursor: pointer;
    }

    .g-action-item:hover {
      border: 1px solid $primary;
      border-radius: 2px;
    }
  }

  .selection {
    border: 1px solid $primary;
    border-radius: 2px;
  }
}
</style>
