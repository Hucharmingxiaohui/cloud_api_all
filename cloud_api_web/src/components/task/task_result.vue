<template>
  <div class="container1">
    <div class="operation">
      <!-- 场站/线路选择框 -->
      <div style="margin: 0 10px;">
        <el-select v-model="selectedScheme" placeholder="所有类型" class="select-operation" style="width: 200px;">
          <el-option label="所有类型1" value="所有类型1"></el-option>
          <el-option label="所有类型2" value="所有类型2"></el-option>
          <el-option label="所有类型3" value="所有类型3"></el-option>
        </el-select>
      </div>

      <div>
        <el-input v-model="searchTerm" placeholder="请输入名称搜索" style="width: 200px;"></el-input>
      </div>
      <!-- 查询按钮 -->
      <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 70px;">
        <!-- <img class="thumbnail_1" referrerpolicy="no-referrer" src="../../assets/v4/search.png" /> -->
        <span style="margin-left: 5px; font-size: 14px;">查询</span>
      </el-button>
      <el-button class="new_btn2" type="primary" style="margin-left: 30px; width: 90px;" :icon="Download">
        下载报告
      </el-button>
      <el-button class="new_btn2" type="primary" style="margin-left: 30px; width: 90px;" :icon="Document">
        查看报告
      </el-button>

      <!-- <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 70px;"
        @click="CreateThumbnail">
        <span style="margin-left: 5px; font-size: 14px;">测试缩略图</span>
      </el-button> -->
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="paginatedData" stripe>

          <!-- 多选框 -->
          <el-table-column type="selection" width="55" />
          <!-- 序号列 -->
          <!-- <el-table-column label="序号" type="index" width="80" /> -->
          <el-table-column label="序号" align='center' width="60">
            <template #default="scope">
              {{ scope.$index + (currentPage - 1) * pageSize + 1 }}
            </template>
          </el-table-column>
          <!-- 预览图 -->
          <el-table-column label="预览图" width="150">
            <template #default="scope">
              <img :src="scope.row.url" alt="预览图" style="width: 100px; height: 100px; object-fit: cover; cursor: pointer;"
                @click="openPreviewModal(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="名称">
            <template #default="scope">
              <div>{{ scope.row.file_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="文件类型" width="150">
            <template #default="scope">
              <div>
                {{ scope.row.file_name.includes('_T') ? '红外图片' : '可见光图片' }}
              </div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="识别结果" width="150">
              <template #default="scope">
                <div>
                  <div>{{ scope.row.file_name.includes('_T') ? getHighestTemp(scope.row.Temp) + '°C' : '' }}</div>
                </div>
              </template>
            </el-table-column> -->
          <el-table-column label="采集时间">
            <template #default="scope">
              <div>{{ new Date(scope.row.create_time).toLocaleString() }}</div>
            </template>
          </el-table-column>
          <el-table-column label="是否原图">
            <template #default="scope">
              <div v-if="scope.row.is_original">是</div>
              <div v-else>否</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250px">
            <template #default="scope">
              <el-button size="small" type="text" @click="downloadMediaLocal(scope.row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-container">
        <!-- 分页 -->
        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
          :page-sizes="[10, 20, 40, 100]" :page-size="pageSize" :total="totalItems"
          layout="total, sizes, prev, pager, next, jumper">
        </el-pagination>
      </div>
    </div>
    <!-- 图片放大弹窗 -->
    <el-dialog v-model="previewVisible" :before-close="handleClose" width="1000px">
      <div class="preview-modal-content">
        <!-- 左侧显示放大图片 -->
        <!-- <div class="preview-main"> -->
        <!-- 添加“上一张”和“下一张”按钮 -->
        <button class="prev-image" @click="showPreviousImage">‹</button>
        <img :src="selectedImage.url" alt="放大图" class="preview-image" ref="previewImage"
          style="object-fit: contain; width: 500px; height: 500px;" />
        <button class="next-image" @click="showNextImage">›</button>
        <!-- </div> -->

        <!-- 右侧显示任务信息 -->
        <div class="preview-info">
          <div class="info-row">
            <strong>任务名称:</strong>
            <input type="text" :value="jobInfo.job_name" class="info-input" readonly />
          </div>
          <div class="info-row">
            <strong>名称:</strong>
            <input type="text" :value="selectedImage.file_name" class="info-input" readonly />
          </div>
          <!-- <div class="info-row">
            <strong>关联点位:</strong>
            <input type="text" :value="selectedImage.point_name" class="info-input" readonly />
          </div> -->
          <div class="info-row">
            <strong>照片类型:</strong>
            <input type="text" :value="selectedImage.file_name.includes('_T') ? '红外图片' : '可见光图片'" class="info-input"
              readonly />
          </div>
          <div class="info-row">
            <strong>航线名称:</strong>
            <input type="text" :value="jobInfo.file_name" class="info-input" readonly />
          </div>
          <div class="info-row">
            <strong>照片分辨率:</strong>
            <input type="text" :value="`${selectedImage.width} * ${selectedImage.height}`" class="info-input" readonly />
          </div>
          <div class="info-row" v-if="selectedImage.file_name.includes('_T')">
            <strong>温度:</strong>
            <input type="text" :value="getHighestTemp(selectedImage.Temp) + '°C'" class="info-input" readonly />
          </div>
          <div class="info-row">
            <strong>拍摄时间:</strong>
            <input type="text" :value="new Date(selectedImage.create_time).toLocaleString()" class="info-input"
              readonly />
          </div>
          <div class="info-row">
            <strong>文件大小:</strong>
            <input type="text" :value="Number(selectedImage.size).toFixed(2) + 'M'" class="info-input" readonly />
          </div>
          <button @click="handleTempConfig" class="btn" v-if="selectedImage.file_name.includes('_T')">测温规则配置</button>
        </div>
      </div>

      <!-- 放大图操作按钮 -->
      <div class="preview-container">
        <div class="preview-actions">
          <!-- 放大 -->
          <!-- <el-button icon="el-icon-zoom-in" @click="zoomIn" size="small"></el-button> -->
          <!-- 缩小 -->
          <!-- <el-button icon="el-icon-zoom-out" @click="zoomOut" size="small"></el-button> -->
          <!-- 旋转 -->
          <el-button icon="el-icon-rotate-left" @click="rotate" size="small">旋转方向</el-button>
          <!-- 重置方向 -->
          <el-button icon="el-icon-refresh" @click="resetOrientation" size="small">重置方向</el-button>
          <!-- 下载 -->
          <el-button icon="el-icon-download" @click="downloadMediaLocal(selectedImage)" size="small">下载图片</el-button>
        </div>

        <!-- 下方显示缩略图 -->
        <div class="preview-thumbnails">
          <!-- 左侧滚动按钮 -->
          <button class="scroll-button left" @click="scrollLeft">&lt;</button>
          <div class="thumbnail-container">
            <!-- <el-row gutter="5">
              <el-col  -->
            <div v-for="(item, index) in mediaData.data" :key="index" class="thumbnail-item">
                <!-- <img :src="item.url" alt="缩略图" class="thumbnail-image" :class="{ active: selectedImage === item }" -->
              <img :src="item.url" alt="" class="thumbnail-image" :class="{ active: selectedImage === item }"
                @click="selectImage(item)" />
              <!-- </el-col>
            </el-row> -->
            </div>
          </div>
          <!-- 右侧滚动按钮 -->
          <button class="scroll-button right" @click="scrollRight">&gt;</button>
        </div>
      </div>
    </el-dialog>
    <!-- 红外测温 -->
    <div class="TEMPPanel" v-show="showTempConfig" v-drag-window>
      <div style="height: 40px; width: 100%; border-bottom: 1px solid #fff; padding-left: 10px;" class="drag-title">红外测温配置
      </div>
      <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closeTempConfig">
        <CloseOutlined />
      </a>
      <div class="content1">
        <div class="content-left">
          <el-select v-model="tempType" placeholder="请选择" size="large" class="select-operation" :teleported='false'
            @change="updateTempType">
            <el-option v-for="item in tempTypeOption" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <div class="button-wrapper">
            <!-- 新增字段和输入框 -->
            <div class="temp-fields">
              <el-form label-position="top" label-width="100px">
                <el-row style="margin-top: 15px;">
                  <!-- 最高温度 -->
                  <el-col :span="24">
                    <el-form-item label="最高温度°C" style="color: #fff;" v-if="tempType == 2">
                      <el-input v-model="tt.max_tem" placeholder="最高温度" class="info-input1"></el-input>
                    </el-form-item>
                    <el-form-item label="温度°C" style="color: #fff;" v-else>
                      <el-input v-model="tt.point_tem" placeholder="温度" class="info-input1"> </el-input>
                    </el-form-item>
                  </el-col>
                  <!-- 最高温度坐标 -->
                  <!-- <el-col :span="24">
                    <el-form-item label="最高温度坐标" :label-style="{ color: 'white' }" v-if="tempType == 2">
                      <el-input v-model="tt" placeholder="最高温度坐标"></el-input>
                    </el-form-item>
                    <el-form-item label="温度坐标" :label-style="{ color: 'white' }" v-else>
                      <el-input v-model="tt" placeholder="温度坐标"></el-input>
                    </el-form-item>
                  </el-col> -->
                  <!-- 最低温度 -->
                  <el-col :span="24">
                    <el-form-item label="最低温度°C" :label-style="{ color: 'white' }" v-if="tempType == 2">
                      <el-input v-model="tt.min_tem" placeholder="最低温度" class="info-input1"></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item label="平均温度°C" :label-style="{ color: 'white' }" v-if="tempType == 2">
                      <el-input v-model="tt.average_tem" placeholder="最低温度" class="info-input1"></el-input>
                    </el-form-item>
                  </el-col>
                  <!-- 最低温度坐标 -->
                  <!-- <el-col :span="24">
                    <el-form-item label="最低温度坐标" :label-style="{ color: 'white' }"  v-if="tempType ==2">
                      <el-input v-model="tt" placeholder="最低温度坐标"></el-input>
                    </el-form-item>
                  </el-col> -->
                </el-row>
              </el-form>
            </div>
            <!-- 保存按钮 -->
            <!-- <el-button class="btn1" @click="saveTEMPConfig">保存</el-button> -->
          </div>
        </div>
        <div class="content-right">
          <canvas ref="canvas" @mousedown="startDrawing" @mousemove="draw" @mouseup="stopDrawing"
            style="height: 100%;width: 100%"></canvas>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, inject } from 'vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { IPage } from '/@/api/http/type'
import { Task } from '/@/api/wayline'
import { useFormatTask } from './use-format-task'
import { saveAs } from 'file-saver' // 导入文件保存工具
import { useRouter } from 'vue-router'
import { downloadFile } from '/@/utils/common'
import { Search, Download, Document } from '@element-plus/icons-vue'
import { downloadMediaFile, getFlyTaskResultApi, getMediaFiles, getOneImage, deleteOneImage, getTaskResultById, getThumbnailById, downloadThumbnail } from '/@/api/media'
import { EDeviceTypeName, ELocalStorageKey, ERouterName } from '/@/types'
import { insertTEMPConfig, insertTEMPConfig1 } from '/@/api/points'
import { CloseOutlined } from '@ant-design/icons-vue'
import { consoleLog } from '/@/utils/logger'
import { message } from 'ant-design-vue'
const router = useRouter()
const searchTerm = ref('') // 存储搜索关键字
const selectedScheme = ref<string | null>(null)
const { formatTaskTime } = useFormatTask()

// ===========================================================请求数据===========================================================================================
const jobInfo = reactive({
  job_id: '',
  job_name: '',
  begin_time: '',
  end_time: '',
  status: '',
  file_name: '',
  file_id: ''
})

const origionImageUrls = ref([]) // 临时存放原图下载urls

onMounted(() => {
  const data = JSON.parse(localStorage.getItem('TaskInfo'))
  jobInfo.job_id = data.job_id
  // 'e955f015-0846-4304-be4e-d7ade4ef1e8b'
  // data.job_id
  jobInfo.job_name = data.job_name
  jobInfo.begin_time = data.begin_time
  jobInfo.end_time = data.end_time
  jobInfo.status = data.status
  jobInfo.file_name = data.file_name
  jobInfo.file_id = data.file_id

  getFiles()
  // setTimeout(() => {
  //   downloadMedia(mediaData.data[0].file_id, mediaData.data[0].file_name)
  // }, 1000)
})

interface MediaFile {
  fingerprint: string,
  drone: string,
  payload: string,
  is_original: string,
  file_name: string,
  file_path: string,
  create_time: string,
  file_id: string,
  point_name: string
}

const mediaData = reactive({
  data: [] as MediaFile[]
})

/**
 * @description: 获取媒体文件
 * @param {string} workspaceId 工作空间id
 * */
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
async function getFiles () {
  // getTaskResultById(jobInfo.job_id, workspaceId, jobInfo.file_id).then(res => {
  //   mediaData.data = transformData(res.data)
  //   // console.log('原任务结果', mediaData.data)
  //   getUrls()
  // })
  getFlyTaskResultApi(jobInfo.job_id, workspaceId, jobInfo.file_id).then(res => {
    mediaData.data = res.data
    console.log('原任务结果', res)
    getUrls()
  })
}

// 获取图片的下载路径
// async function getUrls () {
//   for (const item of mediaData.data) {
//     // const url = null
//     // const Temp = null
//     // const height = null
//     // const width = null
//     // const fileSizeInMB = null
//     // 解构赋值获取url、width和height

//     const { url: mediaUrl } = await downloadThumbnailInfo(item.file_id, item.file_name)

//     // 将返回的值赋给item
//     item.url = mediaUrl
//   }
// }

async function getUrls () {
  for (const item of mediaData.data) {
    const url = null
    const Temp = null
    const height = null
    const width = null
    const fileSizeInMB = null
    // 解构赋值获取url、width和height
    const { url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, fileSizeInMB: mediasize } = await downloadMedia(item.file_id, item.file_name)

    // 将返回的值赋给item
    item.url = mediaUrl
  }
}

// 转换成目标数据格式
function transformData (data) {
  return data.pointCountList.map(item => {
    return item.mediaFileDTOS.map(media => {
      return {
        file_id: media.file_id,
        file_name: media.file_name,
        create_time: media.create_time,
        pointPosition: item.pointPosition,
        point_name: item.pubWaylinePointDfEntity ? item.pubWaylinePointDfEntity.point_name : ''
      }
    })
  }).flat() // 使用 flat() 将嵌套的数组扁平化
}

/**
 * @description: 下载图片，获取图片得地址
 * @param {string} workspaceId 工作空间id
 * */
function downloadThumbnailInfo (file_id: string, file_name: string) {
  return new Promise((resolve, reject) => {
    // downloadMediaFile(workspaceId, file_id).then(res => {
    downloadThumbnail(workspaceId, file_id).then(res => {
      if (!res) {
        return reject(new Error('Failed to download media file'))
      }
      const data = new Blob([res])
      const imgUrl = URL.createObjectURL(data)
      resolve({ url: imgUrl })
    }).catch(error => {
      reject(new Error(error.message || 'Failed to download media file'))
    })
  })
}

/**
 * @description: 下载图片，获取图片得地址
 * @param {string} workspaceId 工作空间id
 * */
function downloadMedia (file_id: string, file_name: string) {
  return new Promise((resolve, reject) => {
    downloadMediaFile(workspaceId, file_id).then(res => {
      if (!res) {
        return reject(new Error('Failed to download media file'))
      }
      const data = new Blob([res])
      const imgUrl = URL.createObjectURL(data)
      const img = new Image()

      // 等图片加载完成后获取其宽高
      img.onload = async () => {
        // 获取文件的宽高
        const height = img.height
        const width = img.width

        // 获取文件的大小（字节）
        const fileSizeInBytes = data.size
        // console.log('文件字节大小:', fileSizeInBytes)

        // 将字节转换为MB
        const fileSizeInMB = fileSizeInBytes / (1024 * 1024)

        let Temp = ''

        // 如果文件名包含 '_T.'，调用 getTEMP 获取宽度
        if (file_name.includes('_T')) {
          Temp = await getTEMP(file_id, 1, 1, 500, 500)
        }

        // 返回图片的URL以及宽
        resolve({ url: imgUrl, Temp, width, height, fileSizeInMB })
      }
      // 为Image对象设置图片的src属性，开始加载图片
      img.src = imgUrl
    }).catch(error => {
      reject(new Error(error.message || 'Failed to download media file'))
    })
  })
}

/**
 * 下载原图相关信息，避免重复下载
 * @param file_id
 * @param file_name
 */
function getOrigionImage (file_id: string, file_name: string) {
  return new Promise((resolve, reject) => {
    downloadMediaFile(workspaceId, file_id).then(res => {
      if (!res) {
        return reject(new Error('Failed to download media file'))
      }
      const data = new Blob([res])
      const imgUrl = URL.createObjectURL(data)
      const img = new Image()

      // 等图片加载完成后获取其宽高
      img.onload = async () => {
        // 获取文件的宽高
        const height = img.height
        const width = img.width

        // 获取文件的大小（字节）
        const fileSizeInBytes = data.size
        // console.log('文件字节大小:', fileSizeInBytes)

        // 将字节转换为MB
        const fileSizeInMB = fileSizeInBytes / (1024 * 1024)

        let Temp = ''

        // 如果文件名包含 '_T.'，调用 getTEMP 获取宽度
        if (file_name.includes('_T')) {
          Temp = await getTEMP(file_id, 1, 1, 500, 500)
        }
        const imageInfo = {
          file_id: file_id as string,
          url: imgUrl as string,
          Temp: Temp as string,
          width: width as number,
          height: height as number,
          size: fileSizeInMB as number
        }
        origionImageUrls.value.push(imageInfo)
        // 返回图片的URL以及宽
        resolve({ url: imgUrl, Temp, width, height, fileSizeInMB })
      }
      // 为Image对象设置图片的src属性，开始加载图片
      img.src = imgUrl
    }).catch(error => {
      reject(new Error(error.message || 'Failed to download media file'))
    })
  })
}

/**
 * @description: 下载图片到本地
 * @param {string} workspaceId 工作空间id
 * */
const loading = ref(false)
function downloadMediaLocal (media: any) {
  loading.value = true
  // downloadThumbnail(workspaceId, media.file_id).then(res => {
  downloadMediaFile(workspaceId, media.file_id).then(res => {
    if (!res) {
      return
    }
    const data = new Blob([res])
    downloadFile(data, media.file_name)
  }).finally(() => {
    loading.value = false
  })
}

/**
 * 生成缩略图
 * @param file_id
 * @param workspace_id
 */
const file_id = '94ca802b-40a0-4731-bb59-1e9008d8d7b8'
function CreateThumbnail () {
  getThumbnailById('94ca802b-40a0-4731-bb59-1e9008d8d7b8', 'e3dea0f5-37f2-4d79-ae58-490af3228069').then(res => {
    if (res.code !== 0) {
      message.error('缩略图生成失败!')
      return
    }
    message.success('缩略图生成成功')
  })
}

// getTEMP
// ==========================================================================红外测温=====================================================================================

/**
 * @description: 打开测温弹窗
 * @param {string} waylineInfo 航线信息
 * */
const context = ref<CanvasRenderingContext2D | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)
const send_image = ref()
const image = ref<HTMLImageElement | null>(null) // 使用 ref 来处理响应式 Image
const img_size = ref({ width: '', height: '', canvas_width: '', canvas_height: '' })
const showTempConfig = ref(false)
function handleTempConfig (val: any) {
  tt.value = ''
  showTempConfig.value = true
  context.value?.clearRect(0, 0, canvas.value?.width || 0, canvas.value?.height || 0)
  // console.log('saffwafa', selectedImage.value)
  setTimeout(() => {
    drawImage(selectedImage.value.url)
  }, 1000)
}

/**
 * @description: 关闭测温弹窗
 * */
function closeTempConfig () {
  showTempConfig.value = false
}

const tempType = ref(1)
const tempTypeOption = [
  {
    label: '点测温',
    value: 1
  },
  {
    label: '区域测温',
    value: 2
  }
]

/**
 * @description: 更新测温类型
 * @param {Number} selectedValue 选中类型
 * */
function updateTempType (selectedValue) {
  isDrawing = false
  context.value.clearRect(0, 0, canvas.value.width, canvas.value.height)
  context.value?.drawImage(image.value, 0, 0, canvas.value.width, canvas.value.height) // 重绘图片
}

/**
 * @description: 提交测温配置
 * @param {number} tempType 选中类型
 * @param {number} firstPoint_x  第一个点坐标
 * @param {number} firstPoint_y  第一个点坐标
 * @param {number} secondPoint_x  第一个点坐标
 * @param {number} secondPoint_y  第一个点坐标
 * */
const points = reactive({
  firstPoint_x: 0,
  firstPoint_y: 0,
  secondPoint_x: 0,
  secondPoint_y: 0,
})
function saveTEMPConfig () {
  if (tempType.value === 2) {
    getTEMP(selectedImage.value.file_id, points.firstPoint_x, points.firstPoint_y, points.secondPoint_x, points.secondPoint_y)
  } else {
    getTEMP1(selectedImage.value.file_id, points.firstPoint_x, points.firstPoint_y)
  }
}

/**
 * @description: 图片绘制方法实现
 * */
// 计算并保存缩放比例
let scaleX = 1
let scaleY = 1
let scaleX1 = 1
let scaleY1 = 1
// 加载并绘制图片
const drawImage = (imageUrl: string) => {
  context.value = canvas.value?.getContext('2d')
  const img = new Image()
  img.onload = () => {
    send_image.value = imageUrl // 保存图片的 base64 数据或 URL
    image.value = img
    const height = 600 // 固定高度
    const width = 600 // 固定宽度
    scaleY = img.height / height
    scaleX = img.width / width
    scaleX1 = 512 / img.height
    scaleY1 = 640 / img.width
    if (canvas.value && context.value) {
      canvas.value.width = width
      canvas.value.height = height
      context.value.clearRect(0, 0, canvas.value.width, canvas.value.height) // 清空 Canvas
      context.value.drawImage(img, 0, 0, width, height) // 绘制图片
      img_size.value = {
        width: img.width.toString(),
        height: img.height.toString(),
        canvas_width: width.toString(),
        canvas_height: height.toString(),
      }
    }
  }
  img.src = imageUrl // 设置图片来源
}
/**
 * @description: 鼠标触发绘制
 * */
let isDrawing = false
let startX = 0
let startY = 0
let dianx = 0
let diany = 0
// 开始画框的函数，鼠标点击时触发，
const startDrawing = (event) => {
  if (tempType.value === 2) { // 只在区域测温模式下启用绘制
    isDrawing = true
    startX = event.offsetX
    startY = event.offsetY
    dianx = event.offsetX
    diany = event.offsetY
  } else if (tempType.value === 1) { // 在点测温模式下启用点击绘制
    // 点测温，记录点击的位置
    drawPoint(event)
  }
}

/**
 * @description: 绘制函数，绘制点和框
 * @param 鼠标对象 MouseEvent
 * */
const aa = ref('')
const result = ref()
const draw = (event: MouseEvent) => {
  if (tempType.value === 2 && isDrawing) { // 只有在区域测温时才执行绘制
    const currentX = event.offsetX
    const currentY = event.offsetY
    const width = currentX - startX
    const height = currentY - startY

    // 清空之前的绘制内容
    context.value?.clearRect(0, 0, canvas.value?.width || 0, canvas.value?.height || 0)

    // 重新绘制图片
    if (image.value) {
      context.value?.drawImage(image.value, 0, 0, canvas.value.width, canvas.value.height)
    }

    // 计算矩形的起始位置和宽高，确保坐标顺序正确
    const rectX = width < 0 ? currentX : startX
    const rectY = height < 0 ? currentY : startY
    const rectWidth = Math.abs(width)
    const rectHeight = Math.abs(height)

    // 绘制新的矩形
    context.value!.strokeStyle = 'red' // 设置边框颜色为红色
    context.value?.beginPath()
    context.value?.rect(rectX, rectY, rectWidth, rectHeight)
    context.value?.stroke()
    points.firstPoint_x = Math.round(rectX * scaleX * scaleX1)
    points.firstPoint_y = Math.round(rectY * scaleY * scaleY1)
    points.secondPoint_x = Math.round(points.firstPoint_x + rectWidth * scaleX * scaleX1)
    points.secondPoint_y = Math.round(points.firstPoint_y + rectHeight * scaleY * scaleY1)
    console.log('测试',)
    // points.firstPoint_x = Math.round(rectX)
    // points.firstPoint_y = Math.round(rectY)
    // points.secondPoint_x = Math.round(points.firstPoint_x + rectWidth)
    // points.secondPoint_y = Math.round(points.firstPoint_y + rectHeight)

    // insertTEMPConfig(file_path.value, left_top_x.value, left_top_y.value, right_bottom_x.value, right_bottom_y.value).then(res => {
    //   if (!res) {
    //     return
    //   }
    //   result.value = res.data
    // })
  }
}

/**
 * @description: 框测温，停止绘制框的函数，鼠标松开时触发
 * */

const stopDrawing = () => {
  if (isDrawing) {
    isDrawing = false
    console.log('Rectangular drawing stopped')

    getTEMP(selectedImage.value.file_id, points.firstPoint_x, points.firstPoint_y, points.secondPoint_x, points.secondPoint_y)
    //   if (tempType.value === 2) {
    //   getTEMP(selectedImage.value.file_id, points.firstPoint_x, points.firstPoint_y, points.secondPoint_x, points.secondPoint_y)
    // } else {
    //   getTEMP1(selectedImage.value.file_id, points.firstPoint_x, points.firstPoint_y)
    // }
  }
}

/**
 * @description: 绘制测温点
 * @param 鼠标对象 event
 * */
function drawPoint (event) {
  const pointX = event.offsetX
  const pointY = event.offsetY

  // 清空画布并重新绘制图片
  context.value?.clearRect(0, 0, canvas.value?.width || 0, canvas.value?.height || 0)
  if (image.value) {
    context.value?.drawImage(image.value, 0, 0, canvas.value.width, canvas.value.height)
  }

  // 确保 context.value 是有效的 2d 上下文对象, 绘制一个圆点，作为测温点
  if (context.value) {
    const radius = 5
    context.value.beginPath()
    context.value.arc(pointX, pointY, radius, 0, Math.PI * 2)
    context.value.fillStyle = 'red'
    context.value.fill()
  }
  points.firstPoint_x = Math.round(pointX * scaleX * scaleX1)
  points.firstPoint_y = Math.round(pointY * scaleY * scaleY1)
  // points.firstPoint_x = Math.round(pointX)
  // points.firstPoint_y = Math.round(pointY)
  getTEMP1(selectedImage.value.file_id, points.firstPoint_x, points.firstPoint_y)
}

/**
 * @description: 获取测温结果
 * @param {string} file_id 文件名字
 * @param {number} firstPoint_x  第一个点坐标
 * @param {number} firstPoint_y  第一个点坐标
 * @param {number} secondPoint_x  第二个点坐标
 * @param {number} secondPoint_y  第二个点坐标
 * @param {string} workspaceId  工作空间id
 * */
const tt = ref('')
function getTEMP (file_id: string, firstPoint_x: number, firstPoint_y: number, secondPoint_x: number, secondPoint_y: number): Promise<string> {
  const obj = {
    left_top_x: firstPoint_x, // 可以是数字或字符串，具体根据实际需要调整
    left_top_y: firstPoint_y,
    right_bottom_x: secondPoint_x,
    right_bottom_y: secondPoint_y
  }
  return insertTEMPConfig(workspaceId, file_id, obj)
    .then(res => {
      // res.data.average_tem = parseFloat(res.data.average_tem.toFixed(1))
      tt.value = res.data
      // console.log('1111111111111111', tt.value)
      return res.data // 假设 res.data 是你想要的字符串
    })
}

function getTEMP1 (file_id: string, firstPoint_x: number, firstPoint_y: number) {
  const obj = {
    point_x: firstPoint_x,
    point_y: firstPoint_y,
  }
  return insertTEMPConfig1(workspaceId, file_id, obj)
    .then(res => {
      tt.value = res.data
      return res.data // 假设 res.data 是你想要的字符串
    })
}

/**
 * @description: 获取选中记录的最高温度
 * */
function getHighestTemp (val: any) {
  // console.log('测温结果', val)
  if (!val) {
    return ''
  }
  const Temp = '(最低温度:' + val.min_tem + ',最高温度:' + val.max_tem + ')'
  return Temp // 返回最高温度（H）
}
// ======================================================================================================================================================================
const body: IPage = {
  page: 1,
  total: 0,
  page_size: 5
}
const paginationProp = reactive({
  pageSizeOptions: ['20', '15', '20'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 5,
  current: 1,
  total: 0
})
type Pagination = TableState['pagination']

// 表格数据
const plansData = reactive({
  data: [] as Task[], // 任务列表
  selectedTasks: [] as Task[], // 用户选中的任务
})

const open = ref<boolean>(false)
// ===========================================================前端分页功能实现==================================================
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = computed(() => mediaData.data.length)
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return mediaData.data.slice(start, end)
})
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
}
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
}
// ============================================================分页数据==========================================================
// 分页事件
// function handleSizeChange (val: number) {
//   paginationProp.pageSize = val
//   refreshData(paginationProp)
// }
// function handleCurrentChange (val: number) {
//   paginationProp.current = val
//   refreshData(paginationProp)
// }

// function refreshData (page: Pagination) {
//   body.page = page?.current!
//   body.page_size = page?.pageSize!
// }
//= ================================================================================================================================

const previewVisible = ref(false) // 弹窗显示状态，初始值为 false
const selectedImage = ref(mediaData.data[0]) // 初始选中第一个任务
const selectedIndex = ref(0) // 当前图片索引
const scale = ref(1) // 图片缩放比例
const rotation = ref(0) // 图片旋转角度

// 打开预览弹窗
async function openPreviewModal (row: any) {
  selectedImage.value = row // 设置选中的图像
  selectedIndex.value = mediaData.data.findIndex((item) => item.file_id === row.file_id) // 更新索引
  // 临时赋值，使得先展示弹窗，然后加载数据
  selectedImage.value.Temp = ''
  selectedImage.value.width = 0
  selectedImage.value.height = 0
  selectedImage.value.size = 0.00
  // 将弹窗显示状态设为 true
  previewVisible.value = true
  const index = origionImageUrls.value.findIndex(val => val.file_id === selectedIndex.value.file_id)
  if (index !== -1) {
    updateSelectedImage(origionImageUrls.value[index])
  } else {
    const { url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, fileSizeInMB: mediasize } = await getOrigionImage(selectedImage.value.file_id, selectedImage.value.file_name)
    updateSelectedImage({ url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, size: mediasize })
  }
}

// 赋值函数
function updateSelectedImage (imageInfo: { url: string, Temp: string, width: number, height: number, size: number }) {
  selectedImage.value.url = imageInfo.url
  selectedImage.value.Temp = imageInfo.Temp
  selectedImage.value.width = imageInfo.width
  selectedImage.value.height = imageInfo.height
  selectedImage.value.size = imageInfo.size
}

// 切换到上一张图片
async function showPreviousImage () {
  if (selectedIndex.value > 0) {
    selectedIndex.value -= 1
    selectedImage.value = mediaData.data[selectedIndex.value]
    const index = origionImageUrls.value.findIndex(val => val.file_id === selectedIndex.value.file_id)
    if (index !== -1) {
      updateSelectedImage(origionImageUrls.value[index])
    } else {
      const { url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, fileSizeInMB: mediasize } = await getOrigionImage(selectedImage.value.file_id, selectedImage.value.file_name)
      updateSelectedImage({ url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, size: mediasize })
    }
  }
}

// 切换到下一张图片
async function showNextImage () {
  if (selectedIndex.value < mediaData.data.length - 1) {
    selectedIndex.value += 1
    selectedImage.value = mediaData.data[selectedIndex.value]
    const index = origionImageUrls.value.findIndex(val => val.file_id === selectedIndex.value.file_id)
    if (index !== -1) {
      updateSelectedImage(origionImageUrls.value[index])
    } else {
      const { url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, fileSizeInMB: mediasize } = await getOrigionImage(selectedImage.value.file_id, selectedImage.value.file_name)
      updateSelectedImage({ url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, size: mediasize })
    }
  }
}

// 选择缩略图
async function selectImage (item: any) {
  // 设置当前选中的图片
  selectedImage.value = item
  // 判断选中图片是否已下载
  const index = origionImageUrls.value.findIndex(val => val.file_id === item.file_id)
  if (index !== -1) {
    updateSelectedImage(origionImageUrls.value[index])
  } else {
    const { url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, fileSizeInMB: mediasize } = await getOrigionImage(item.file_id, item.file_name)
    updateSelectedImage({ url: mediaUrl, Temp: mediaTemp, width: mediaWidth, height: mediaHeight, size: mediasize })
  }

  // 获取缩略图容器和当前选中的图片
  const container = document.querySelector('.thumbnail-container') as HTMLElement
  const activeThumbnail = document.querySelector('.thumbnail-image.active') as HTMLElement

  if (container && activeThumbnail) {
    const containerRect = container.getBoundingClientRect()
    const thumbnailRect = activeThumbnail.getBoundingClientRect()

    // 检查选中图片是否超出左边界
    if (thumbnailRect.left < containerRect.left) {
      container.scrollTo({
        left: container.scrollLeft - (containerRect.left - thumbnailRect.left),
        behavior: 'smooth',
      })
    }

    // 检查选中图片是否超出右边界
    if (thumbnailRect.right > containerRect.right) {
      container.scrollTo({
        left: container.scrollLeft + (thumbnailRect.right - containerRect.right),
        behavior: 'smooth',
      })
    }
  }
}

// 关闭弹窗
function handleClose () {
  previewVisible.value = false
}

// // 放大图片
// function zoomIn() {
//   scale.value += 0.1
//   updateImageStyle()
// }

// // 缩小图片
// function zoomOut() {
//   scale.value = Math.max(0.1, scale.value - 0.1); // 限制最小缩放比例
//   updateImageStyle();
// }

// 旋转图片
function rotate () {
  rotation.value += 90
  updateImageStyle()
}

// 重置图片方向和大小
function resetOrientation () {
  scale.value = 1
  rotation.value = 0
  updateImageStyle()
}

// 更新图片的样式
function updateImageStyle () {
  const img = document.querySelector('.preview-image') as HTMLImageElement
  if (img) {
    // 只在需要的时候进行缩放和旋转
    img.style.transform = `scale(${scale.value}) rotate(${rotation.value}deg)`
    img.style.transformOrigin = 'center center' // 保持缩放和旋转以图片中心为基准
  }
}

function scrollLeft () {
  const container = document.querySelector('.thumbnail-container') as HTMLElement
  container.scrollBy({ left: -100, behavior: 'smooth' }) // 向左滚动 100px
}

function scrollRight () {
  const container = document.querySelector('.thumbnail-container') as HTMLElement
  container.scrollBy({ left: 100, behavior: 'smooth' }) // 向右滚动 100px
}

</script>

<style lang="scss" scoped>
.container1 {
  // height: 100%;
  width: 100vw;
  // padding: 10px;
  display: flex;
  flex-direction: column;
  /* 使子元素垂直排列 */
}

.ellipsis {
  white-space: nowrap;
  /* 防止换行 */
  overflow: hidden;
  /* 隐藏超出部分 */
  text-overflow: ellipsis;
  /* 显示省略号 */
}

// 下拉框
.select-operation {
  :deep(.el-select__placeholder) {
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
  }

  :deep(.el-select__wrapper) {

    // background: rgba(59, 116, 255, 0.15);
    background-color: #0B2756;
    // box-shadow: inset 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
    box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
    // border: 1px solid #719fff;
    // border-radius: 4px;
    width: 200px;
    height: 30px;
  }

  /**修改下拉图标颜色 */
  :deep(.el-select__caret) {
    color: #ffffff;
  }

  :deep(.el-select-dropdown) {
    background: #012b78;
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 14px;
    font-weight: 500;
    color: #ffffff;
  }

  :deep(.el-select-dropdown__item.is-hovering) {

    background-color: skyblue;
  }
}

// 操作部分
// 操作部分
.operation {
  display: flex;
  // justify-items: center; /* 这里可能是错误的，flexbox 中应该使用 justify-content */
  align-items: center;
  /* 这个会确保 label 在垂直方向居中 */
  background-color: rgba(1, 36, 98, 1);
  border-radius: 4px;
  // width: 100%;
  height: 60px;
  margin: 31px 12px 0 12px;

  .label {
    height: 60px;
    display: flex;
    /* 这个可以保留，确保子元素居中 */
    align-items: center;
    /* 垂直居中 */
    justify-content: center;
    /* 水平居中 */
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
    margin: 0 10px 0 30px;
  }
  .new_btn2 {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    height: 37px;
  }

  .new_btn {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    width: 108px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 56px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

  .new_btn1 {
    background-image: linear-gradient(180deg,
        rgba(248, 212, 94, 1) 0,
        rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    width: 70px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 30px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

}

.content {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

/* 公共按钮样式 */
.custom-btn {
  background-color: rgba(51, 122, 255, 0.12);
  height: 28px;
  border: 1px solid rgba(0, 64, 147, 1);
  margin: 7px;
}

/* 立即执行按钮 */
.custom-execute-btn {
  @extend .custom-btn;
  width: 76px;
  margin-left: 7px;
}

/* 复制按钮 */
.custom-copy-btn {
  @extend .custom-btn;
  width: 40px;
  margin-left: 7px;
}

/* 编辑按钮 */
.custom-edit-btn {
  @extend .custom-btn;
  width: 40px;
  margin-left: 7px;
}

.custom-delete-btn {
  background-color: rgba(255, 92, 51, 0.19);
  border-radius: 4px;
  height: 28px;
  color: rgba(255, 215, 215, 1);
  border: 1px solid rgba(255, 132, 132, 1);
  width: 40px;
  margin: 7px;
}

.action-buttons {
  display: flex;
  /* 使用 flex 布局 */
  gap: 8px;
  /* 按钮之间的间距 */
  justify-content: flex-start;
  /* 如果需要调整对齐方式，可以改为 center 或 space-between */
  align-items: center;
  /* 垂直方向对齐 */
}

.custom-execute-btn,
.custom-copy-btn,
.custom-edit-btn,
.custom-delete-btn {
  margin: 0;
  /* 清除之前的外边距 */
  width: auto;
  /* 根据内容自适应宽度 */
}

.pagination-container {
  position: absolute;
  bottom: 40px;
  /* 距离底部的距离，可调整 */
  left: 50%;
  /* 距离右边的距离，可调整 */
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.el-dialog__body) {
  background-color: #0B2756;
  /* 内容区域背景 */
  color: white;
}

:deep(.el-input) {
  background-color: #123974;
  /* 输入框背景 */
  color: white;
  /* 输入框文字颜色 */
}

:deep(.el-input) {
  --el-input-border-color: #1d4292;
}

::v-deep .el-input__wrapper {
  background-color: #0B2756;
  box-shadow: 0px 0px 2px 2px rgba(34, 135, 255, 0.5);
}

:deep(.el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}

.btn {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

.btn1 {
  border: 2px solid #ad2d0d;
  background: linear-gradient(to top, #ff1e00, #f32906);
  color: rgba(255, 255, 255, 0.762);
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #0A2D63;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

.table-container {
  flex-grow: 1;
  overflow: hidden;
  // height: 500px;
  overflow-y: auto;
}

::v-deep .el-table tr {
  background-color: #011C4B !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #2264a7;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: rgba(0, 114, 245, 0.6) !important;
}

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1);
}

::v-deep .el-table td {
  border: 2px solid #01123288;
  /* 设置列的边框颜色和粗细 */
  font-size: 16px;
  font-weight: 500;
}

// 表格样式
::v-deep .el-table {
  .cell {
    // text-align: center;
    display: flex;
    justify-content: center;
  }
}

// // 表头大小
::v-deep .el-table th {
  height: 50px;
  font-size: 16px !important;
  /* 如果你需要修改表头字体大小，设置一个不同的大小 */
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
}

//分页数据
/* 修改前后箭头未点击时的背景颜色 */
:deep .el-pagination .btn-prev,
:deep .el-pagination .btn-next {
  background-color: #062254 !important;
  color: #fff;
}

/* 修改未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled):hover {
  background-color: #2264a7 !important;
}

/* 未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled) {
  background-color: #062254 !important;
  color: #fff;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #124AAD !important; //修改默认的背景色
  color: #fff;
}

::v-deep el-pager {
  background-color: #0B2756;
}

/* 移除表格行的底部边框 */
::v-deep .el-table__body-wrapper .el-table__row {
  border-bottom: none !important;
  /* 或者： */
  box-shadow: none !important;
}

/* 如果仍然看到白色线条，可能是阴影效果的原因 */
::v-deep .el-table__body-wrapper .el-table__row td {
  border-bottom: none !important;
}

// @media (max-width: 768px) {
//   .container {
//     padding: 10px;
//   }

//   .el-table {
//     font-size: 14px;
//   }
// }

.TEMPPanel {
  padding: 10px 0 0 0;
  position: absolute;
  left: 0;
  top: 200px;
  margin-left: 345px;
  width: 940px;
  height: 690px;
  z-index: 3000;
  // background: #232323;
  // background: rgba(59, 116, 255, 0.2);
  background-color: #205CA1;
  color: #fff;

  .content1 {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #06346A;
    padding: 20px;

    .content-left {
      width: 300px;
      // background-color:#1d4292;
      height: 600px;
      border-right: 1px solid #023956;
    }

    .content-right {
      width: 600px;
      background-color: #1d4292;
      height: 600px;
      border: 3px dashed #3667A7;
    }

  }
}

// 输入框
:deep(.el-input) {
  --el-input-border-color: #1d4292;
}

:deep(.el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}

.btn {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

.btn1 {
  margin: 2px 0;
  background-image: linear-gradient(180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%);
  /* 按钮的背景颜色 */
  color: #FFFFFF;
  /* 按钮文字颜色 */
  border: none;
  /* 去掉按钮边框 */
  // box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2); /* 添加一些阴影 */
  transition: background-color 0.3s ease;
}

// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #0A2D63;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

.header {
  width: 100%;
  height: 60px;
  background: #fff;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: #000;
}

/* 使用 ::v-deep 确保深层选择器应用 */
::v-deep .custom-drawer .ant-drawer-header {
  background-color: black;
  /* 头部背景颜色 */
  color: white;
  /* 头部文字颜色 */
}

//分页数据
/* 修改前后箭头未点击时的背景颜色 */
:deep .el-pagination .btn-prev,
:deep .el-pagination .btn-next {
  background-color: #062254 !important;
  color: #fff;
}

/* 修改未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled):hover {
  background-color: #2264a7 !important;
}

/* 未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled) {
  background-color: #062254 !important;
  color: #fff;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #124AAD !important; //修改默认的背景色
  color: #fff;
}

::v-deep el-pager {
  background-color: #0B2756;
}

:deep(.el-form-item__label) {
  background-color: transparent;
  color: white !important;
}

//弹窗 图片显示部分==========================================================================================
.preview-modal-content {
  display: flex;
  // margin: 0;
  // justify-content: space-between;
  background: transparent;
  border: 1px solid rgb(37, 54, 83);
  /* 深蓝渐变背景 */
  padding: 5px 5px;
  /* 添加内边距，确保文本与边框有距离 */
  width: 510px;

  /* 增加宽度 */
  border-radius: 2px;
}

.prev-image,
.next-image {
  position: absolute;
  top: 40%;
  transform: translateX(-10%);
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  font-size: 20px;
  padding: 10px;
  cursor: pointer;
}

.prev-image {
  left: 23px;
}

.next-image {
  right: 477px;
}

//弹窗======================================================================================================
// 修改弹窗背景颜色
// :deep(.el-dialog__wrapper) {
//   background-color: #0B2756 !important;
//   /* 设置背景为深蓝色 */

// }

// 修改弹窗主体颜色
::v-deep .el-dialog {
  background-color: #0B2756 !important;
  /* 弹窗内的背景颜色 */
  color: #ffffff;
  /* 文本颜色改为白色 */
}

.preview-info {
  background: transparent;
  border: 1px solid rgb(37, 54, 83);
  /* 深蓝渐变背景 */
  padding: 10px 10px;
  /* 添加内边距，确保文本与边框有距离 */
  width: 420px;
  /* 增加宽度 */
  border-radius: 2px;
  /* 圆角边框 */
  color: #ffffff;
  /* 文本颜色 */
  font-size: 14px;
  /* 字体大小 */
  // box-shadow: 0 6px 10px rgba(0, 0, 0, 0.15);
  /* 添加柔和阴影效果 */
  line-height: 1;
  /* 行间距，避免文字挤在一起 */
  margin-left: 30px;
}

.preview-info .info-row {
  display: flex;
  /* 使用flex布局，使字段和输入框在同一行显示 */
  width: 400px;
  align-items: center;
  /* 垂直居中对齐 */
  margin-bottom: 15px;
  color: #fff;
  /* 增加底部间距 */
}

// .preview-info div {
//   margin-bottom: 15px;
//   /* 增加底部间距，使每个信息块之间有空隙 */
// }

.preview-info strong {
  font-weight: 600;
  /* 强调标签的加粗样式 */
  width: 100px;
  margin-right: 10px;
  /* 标签和输入框之间的间距 */
}

.preview-info .btn {
  background-color: #00D2BE;
  /* 设置按钮背景色 */
  color: #ffffff;
  /* 按钮文字颜色 */
  padding: 10px 16px;
  /* 按钮内边距 */
  border: none;
  /* 去掉按钮边框 */
  border-radius: 25px;
  /* 圆角按钮 */
  cursor: pointer;
  /* 鼠标悬停时显示手型光标 */
  font-size: 14px;
  /* 按钮文字大小 */
  width: 100%;
  /* 按钮占满宽度 */
  transition: background-color 0.3s;
  /* 按钮背景色的过渡效果 */
}

.preview-info .btn:hover {
  background-color: #019F91;
  /* 按钮悬停时的背景色变化 */
}

.info-input {
  display: flex;
  justify-content: space-between;
  background-color: transparent;
  /* 设置输入框的背景颜色 */
  color: #fff;
  /* 设置输入框中文字的颜色 */
  border: 1px solid rgb(37, 54, 83);
  /* 输入框边框颜色 */
  padding: 5px 10px;
  /* 输入框的内边距 */
  width: calc(100% - 20px);
  /* 让输入框占据整个宽度，减去内边距 */
  border-radius: 8px;
  /* 输入框的圆角 */
  font-size: 14px;
  /* 输入框字体大小 */
  margin-top: 3px;
  /* 给输入框增加顶部间距 */
  cursor: pointer;
  /* 鼠标悬停时显示手型光标 */
}

.info-input:read-only {
  background-color: transparent;
  /* 只读时背景色 */
}

.info-input:focus {
  outline: none;
  /* 去掉焦点边框 */
  border-color: #00D2BE;
  /* 聚焦时边框颜色变化 */
}

/* 温度字段区域 */
.temp-fields {
  margin-top: 2px;
  color: white !important;

}

.info-input1 {
  width: 250px;

  :deep(.el-input__inner) {
    color: #fff;
  }
}

::v-deep .el-input__inner {
  color: white;
}

//=================================================================================================================
// 修改缩略图容器背景颜色
.preview-thumbnails {
  position: relative;
  display: flex;
  margin-left: 0px;
  align-items: center;
  background-color: #0b2756;
  /* 缩略图背景 */
  padding: 10px;
  overflow: hidden;
  /* 防止滚动条影响布局 */
  width: 910px;
  // overflow-x: auto; /* 启用横向滚动 */
  // white-space: nowrap; /* 防止子元素换行 */
}

.thumbnail-container {
  display: flex;
  /* 使用 Flexbox 布局 */
  // gap: 10px; /* 设置缩略图间距 */
  margin-left: -8px;
  margin-top: -8px;
  border: 1px solid rgb(37, 54, 83);
  /* 左侧负间距 */
  overflow-x: auto;
  /* 启用水平滚动 */
  scroll-behavior: smooth;
  /* 滚动时的平滑效果 */
  white-space: nowrap;
  /* 防止子元素换行 */
  padding: 5px 0;
  /* 内边距 */
  flex: 1;
  /* 让容器占据可用空间 */
  height: 80px;
}

.thumbnail-item {
  // margin-left: 8px; /* 子项设置间距 */
  flex: 0 0 auto;
  /* 防止缩略图被压缩 */
}

.thumbnail-image {
  width: 60px;
  /* 缩略图宽度 */
  height: 60px;
  /* 缩略图高度，保持统一尺寸 */
  object-fit: cover;
  /* 保持图像比例 */
  cursor: pointer;
  border: 2px solid transparent;
  /* 默认边框透明 */
  border-radius: 4px;
  /* 可选，缩略图圆角效果 */
  cursor: pointer;
  /* 鼠标悬停时变为点击手型 */
}

.thumbnail-image.active {
  border: 5px solid #ffffff;
  border-radius: 4px;
  // border-color: #fff; /* 激活状态时的白色边框 */
}

/* 滚动按钮样式 */
.scroll-button {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  cursor: pointer;
  z-index: 10;
}

.scroll-button.left {
  left: 5px;
}

.scroll-button.right {
  right: 5px;
}

.scroll-button:hover {
  background: rgba(0, 0, 0, 0.8);
}

/* 整个预览区域的容器 */
.preview-container {
  display: flex;
  align-items: flex-start;
  /* 使按钮和缩略图在同一行并对齐 */
  justify-content: flex-start;
  width: 100%;
  background-color: #0B2756;
  padding: 10px;
}

.preview-actions {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  margin-right: 15px;
  background-color: #0B2756;
  // gap: 5px;
  /* 设置背景颜色为蓝色 */
  // padding: 10px;
  // width: 790px;
}

.preview-actions .el-button {
  margin: 2px 0;
  background-image: linear-gradient(180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%);
  /* 按钮的背景颜色 */
  color: #FFFFFF;
  /* 按钮文字颜色 */
  border: none;
  /* 去掉按钮边框 */
  // box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2); /* 添加一些阴影 */
  transition: background-color 0.3s ease;
  /* 增加交互动画效果 */
}

.preview-actions .el-button:nth-child(2) {
  /* 重置按钮 */
  background-image: linear-gradient(180deg,
      rgba(248, 212, 94, 1) 0,
      rgba(227, 157, 6, 1) 100%);
}

.preview-actions .el-button:hover {
  background-color: #1B5AD9;
  /* 鼠标悬停时的背景颜色 */
  color: #FFFFFF;
  /* 鼠标悬停时文字颜色 */
}
</style>
