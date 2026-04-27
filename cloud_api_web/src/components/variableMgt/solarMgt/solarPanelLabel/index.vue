<template>
  <div class="solar-container">
    <!-- 左侧选择光伏板区域 -->
    <div class="left-panel">
      <div class="panel-header">光伏区域标注</div>
      <div class="region-title"> 光伏板区域:</div>
      <div style="margin-left: 15px; margin-top: 10px;">
        <el-select v-model="selectedPanelId" filterable>
            <el-option
                v-for="item in solarPanelList"
                :label="item.solar_panel_name"
                :value="item.id"
                :key="item.id"
            ></el-option>
        </el-select>
      </div>
      <div class="region-title"> 绘制区域列表:</div>
      <div class="region-list">
        <div
          v-for="region in regionList"
          :key="region.id"
          :class="['region-item', selectedRegionId === region.id ? 'selected' : '']"
        >
          <!-- 非编辑模式 -->
          <template v-if="!region.isEditing">
            <span class="region-name" @click="selectRegion(region.id)">{{ region.name }}</span>
            <div class="region-actions">
              <EditOutlined class="action-icon edit-icon" @click.stop="startEdit(region)" />
              <DeleteOutlined class="action-icon delete-icon" @click.stop="handleDelete(region.id)" />
            </div>
          </template>

          <!-- 编辑模式 -->
          <template v-else>
            <el-input
              v-model="region.editName"
              size="small"
              class="edit-input"
              @keyup.enter="confirmEdit(region)"
            />
            <div class="region-actions">
              <CheckOutlined class="action-icon check-icon" @click.stop="confirmEdit(region)" />
              <CloseOutlined class="action-icon close-icon" @click.stop="cancelEdit(region)" />
            </div>
          </template>
        </div>
        <div v-if="regionList.length === 0" class="empty-text">暂无区域，请选择光伏板区域，并在右侧地图绘制</div>
      </div>
      <div class="panel-footer">
        <el-button type="primary" :disabled="!selectedRegionId" @click="handleView">
          查看
        </el-button>
      </div>
    </div>

    <!-- 右侧加载地图 -->
    <div class="map-container">
      <solarPanelMap ref="mapRef" @polygon-drawn="onPolygonDrawn" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  CheckOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import solarPanelMap from './setSolarPanelMap.vue'
import { getAllSolarPanelApi } from '/@/api/turbine/turbineMgt'

const mapRef = ref(null)
const regionList = ref([]) // 存储绘制区域坐标
const selectedRegionId = ref('')

const solarPanelList = ref([]) // 光伏板区域列表
const selectedPanelId = ref('') // 选中的光伏板区域id

onMounted(() => {
  getSolarPanel()
})

// 绘制完成回调
function onPolygonDrawn (data) {
  // 自动生成区域名称：区域1、区域2...
  const regionNumber = regionList.value.length + 1
  const regionName = `区域${regionNumber}`

  regionList.value.push({
    id: data.id,
    name: regionName,
    coordinates: data.coordinates,
    isEditing: false,
    editName: '',
  })

  // 自动选中新绘制的区域
  selectedRegionId.value = data.id

  // 同步更新子组件中 marker 的名称
  mapRef.value?.updateRegionName(data.id, regionName)
}

/**
 * @description: 查询光伏板ID列表
 * @param {string}
 * */
function getSolarPanel () {
  try {
    getAllSolarPanelApi({
      pageSize: 10000,
      pageNo: 1,
      solar_panel_name: '',
      id: ''
    }).then(res => {
      if (res.code !== 0) {
        return
      }
      solarPanelList.value = res.data.list
    })
  } catch (error) {
  }
}

// 选择区域
function selectRegion (regionId) {
  if (selectedRegionId.value === regionId) {
    selectedRegionId.value = ''
  } else {
    selectedRegionId.value = regionId
  }
}

// 开始编辑
function startEdit (region) {
  region.editName = region.name
  region.isEditing = true
}

// 确认编辑
function confirmEdit (region) {
  const newName = region.editName.trim()
  if (!newName) {
    ElMessage.warning('区域名称不能为空')
    return
  }
  if (newName === region.name) {
    region.isEditing = false
    return
  }

  // 调用子组件方法更新 marker
  const success = mapRef.value?.updateRegionName(region.id, newName)
  if (success !== false) {
    region.name = newName
    region.isEditing = false
    ElMessage.success('修改成功')
  } else {
    ElMessage.error('修改失败')
  }
}

// 取消编辑
function cancelEdit (region) {
  region.editName = ''
  region.isEditing = false
}

// 删除区域
function handleDelete (regionId) {
  ElMessageBox.confirm('确定要删除该区域吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      // 调用子组件方法删除地图上的多边形和 marker
      const success = mapRef.value?.deleteRegion(regionId)
      if (success) {
        const index = regionList.value.findIndex(item => item.id === regionId)
        if (index > -1) {
          regionList.value.splice(index, 1)
        }
        if (selectedRegionId.value === regionId) {
          selectedRegionId.value = ''
        }
        renameRegions()
        ElMessage.success('删除成功')
      } else {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {
      // 取消删除
    })
}

// 重新命名区域（保持区域1、区域2...顺序）
function renameRegions () {
  regionList.value.forEach((region, index) => {
    const newName = `区域${index + 1}`
    region.name = newName
    // 同步更新地图上的 marker 名称
    mapRef.value?.updateRegionName(region.id, newName)
  })
}

// 查看选中区域的坐标
function handleView () {
  if (!selectedRegionId.value) {
    ElMessage.warning('请先选择一个区域')
    return
  }

  const region = regionList.value.find(item => item.id === selectedRegionId.value)
  if (region && region.coordinates) {
    console.log(`【${region.name}】的 WGS84 坐标：`, region.coordinates)
    ElMessage.success(`已在控制台打印【${region.name}】的坐标，请按 F12 查看`)
  } else {
    ElMessage.error('未找到区域坐标信息')
  }
}
</script>

<style scoped lang="scss">

// element组件

:deep(.el-select__placeholder) {
    color: #f3f4f6;
}

:deep(.el-select__wrapper) {
    background-color: #0B2756;
    width: 275px;
    height: 30px;
    border: 1px solid #409EFF;
    box-shadow: none !important; /* 移除可能的内置阴影 */

}

.solar-container {
  display: flex;
  height: 87vh;

  .left-panel {
    width: 300px;
    height: 100%;
    background-color: #05204B;
    display: flex;
    flex-direction: column;

    .panel-header {
      width: 100%;
      color: aliceblue;
      height: 50px;
      line-height: 50px;
      border-bottom: 1px solid #4f4f4f;
      font-weight: 450;
      padding-left: 15px;
    }
    .region-title {
      margin-top: 10px;
      width: 100%;
      color: aliceblue;
      height: 30px;
      line-height: 30px;
      padding-left: 15px;
    }

    .region-list {
      flex: 1;
      overflow-y: auto;
      padding: 10px;

      .region-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 12px;
        margin-bottom: 8px;
        background-color: rgba(255, 255, 255, 0.05);
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          background-color: rgba(255, 255, 255, 0.1);
        }

        &.selected {
          background-color: rgba(64, 158, 255, 0.2);
          border: 1px solid #409EFF;
        }

        .region-name {
          color: #fff;
          font-size: 14px;
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          margin-right: 8px;
        }

        .edit-input {
          flex: 1;
          margin-right: 8px;
          :deep(.el-input){
             border-color: #409EFF !important;
          }
          :deep(.el-input__inner) {
            background-color: rgba(101, 156, 245, 0.1);
            color: #fff;
            border-color: #409EFF !important;

            &::placeholder {
              color: #c1c3c6;
            }
          }
          :deep(.el-input__wrapper){
             background-color: rgba(11, 36, 76, 0.684);
             border-color: #409EFF !important;
          }
        }

        .region-actions {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;
        }

        .action-icon {
          font-size: 14px;
          cursor: pointer;
          padding: 4px;
          transition: color 0.2s;

          &:hover {
            opacity: 0.8;
          }
        }

        .edit-icon {
          color: #409EFF;
        }

        .delete-icon {
          color: #f56c6c;

          &:hover {
            color: #ff4d4f;
          }
        }

        .check-icon {
          color: #67c23a;
        }

        .close-icon {
          color: #909399;

          &:hover {
            color: #c0c4cc;
          }
        }
      }

      .empty-text {
        color: #909399;
        text-align: center;
        padding: 40px 20px;
        font-size: 14px;
      }
    }

    .panel-footer {
      padding: 15px;
      border-top: 1px solid #4f4f4f;
      display: flex;
      justify-content: center;

      .el-button {
        width: 120px;
      }
    }
  }

  .map-container {
    width: calc(100% - 300px);
    height: 100%;
  }
}
</style>
