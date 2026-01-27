<template>
  <div class="container1">
    <div class="operation">
      <el-form :inline="true" :model="queryForm" label-position="right">
        <el-form-item label="图片名称:">
          <el-input
            v-model="queryForm.name"
            placeholder="请输入图片名称"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <!-- 查询按钮 -->
          <el-button
            class="new_btn"
            type="primary"
            :icon="Search"
            @click="getFiles"
            >查询
          </el-button>
          <!-- 重置按钮 -->
          <el-button class="new_btn1" type="info" :icon="Refresh" @click="reset"
            >重置
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Download"
            @click="createReport()"
          >
            下载报告
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Document"
            @click="viewReport()"
          >
            查看报告
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Refresh"
            @click="resetReport()"
          >
            重置报告
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Download"
            v-if="resultType === 1"
            @click="exportImageZip()"
          >
            导出数据集
          </el-button>
          <el-button
            class="new_btn"
            type="primary"
            :icon="Download"
            v-if="resultType === 0"
            @click="exportOriginImage()"
          >
            导出原图
          </el-button>
        </el-form-item>
      </el-form>
      <!-- <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 70px;"
        @click="CreateThumbnail">
        <span style="margin-left: 5px; font-size: 14px;">测试缩略图</span>
      </el-button> -->
    </div>
    <div class="content" v-loading="loading">
      <div class="table-container">
        <el-table :data="mediaData.data" stripe>
          <!-- 多选框 -->
          <el-table-column type="selection" width="55" />
          <!-- 序号列 -->
          <el-table-column label="序号" type="index" width="80" />
          <!-- 预览图 -->
          <el-table-column label="图片" align="center">
            <template #default="scope">
              <img
                :src="getImageUrl(scope.row.original_image_url)"
                alt="预览图"
                style="width: 100px; height: 100px; object-fit: cover; cursor: pointer;"
                @click="openPreviewModal(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column
            label="分析图"
            align="center"
            v-if="resultType === 1"
          >
            <template #default="scope">
              <img
                :src="getImageUrl(scope.row.defect_image_url)"
                alt="预览图"
                style="width: 100px; height: 100px; object-fit: cover; cursor: pointer;"
                @click="openPreviewAnaysisModal(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="名称" align="center">
            <template #default="scope">
              <div>{{ scope.row.file_name }}</div>
            </template>
          </el-table-column>
          <el-table-column label="文件类型" align="center">
            <template #default="scope">
              <div>
                {{ scope.row.file_name.includes('_T') ? '红外图片' : '可见光图片' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="采集时间" align="center">
            <template #default="scope">
              <div>{{ new Date(scope.row.create_time).toLocaleString() }}</div>
            </template>
          </el-table-column>
          <el-table-column
            label="扇叶名称"
            align="center"
            v-if="resultType === 1"
          >
            <template #default="scope">
              <div>{{ scope.row.fan_code }}</div>
            </template>
          </el-table-column>
          <el-table-column
            label="扇叶部位"
            align="center"
            v-if="resultType === 1"
          >
            <template #default="scope">
              <div>{{ scope.row.fan_part }}</div>
            </template>
          </el-table-column>
          <el-table-column
            label="缺陷主要类型"
            align="center"
            v-if="resultType === 1"
          >
            <template #default="scope">
              <div>{{ scope.row.defect_type }}</div>
            </template>
          </el-table-column>
          <el-table-column
            label="缺陷描述"
            align="center"
            v-if="resultType === 1"
          >
            <template #default="scope">
              <div>{{ scope.row.defect_description  }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="100px" align="center">
            <template #default="scope">
              <el-button
                size="small"
                type="text"
                @click="defectSelect(scope.row)"
                >人工修正</el-button
              >
              <!-- manualCorrection -->
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-container">
        <!-- 分页 -->
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="paginationProp.current"
          :page-sizes="paginationProp.pageSizeOptions"
          :page-size="paginationProp.pageSize"
          :total="paginationProp.total"
          layout="total, sizes, prev, pager, next, jumper"
        >
        </el-pagination>
      </div>
    </div>

    <!-- 人工修正 -->
    <el-dialog title="人工修正" v-model="editDefectVisible" width="500px">
      <div>
        <el-form
          :model="editForm"
          label-width="100px"
          :rules="formRules"
          ref="editFormRef"
        >
          <el-form-item label="缺陷类型" prop="defects" required>
            <el-select v-model="editForm.defects" multiple collapse-tags>
              <el-option
                v-for="item in defectTypeMap"
                :label="item.name"
                :value="item.name"
                :key="item.code"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template v-slot:footer>
        <div class="dialog-footer">
          <el-button @click="editDefectVisible = false" class="nobtn"
            >取 消</el-button
          >
          <el-button type="primary" @click="handleCorrection()" class="okbtn"
            >确 定</el-button
          >
        </div>
      </template>
    </el-dialog>

    <!-- 查看报告 -->
    <el-dialog
      title="报告预览"
      v-model="viewReportVisible"
      width="880px"
      class="view"
    >
      <div v-loading="viewloading" class="doc-preview-container">
        <div id="docContainer" class="docx-container"></div>
      </div>
    </el-dialog>

    <!-- 图片放大弹窗 -->
    <el-dialog v-model="previewVisible" title="原始预览" width="1000px">
      <div class="preview-modal-content">
        <!-- 左侧显示放大图片 -->
        <!-- <div class="preview-main"> -->
        <!-- 添加“上一张”和“下一张”按钮 -->
        <button class="prev-image" @click="showPreviousImage('origin')">‹</button>
        <div style="width: 500px; height: 500px;">
          <img
            :src="getImageUrl(selectedImage.original_image_url)"
            alt="放大图"
            class="preview-image"
            ref="previewImage"
            style="object-fit: contain; width: 500px; height: 500px;"
          />
        </div>

        <button class="next-image" @click="showNextImage('origin')">›</button>
        <!-- </div> -->

        <!-- 右侧显示任务信息 -->
        <div class="preview-info">
          <div class="info-row">
            <strong>任务名称:</strong>
            <input
              type="text"
              :value="jobInfo.job_name"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>名称:</strong>
            <input
              type="text"
              :value="selectedImage.file_name"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>照片类型:</strong>
            <input
              type="text"
              :value="selectedImage.file_name.includes('_T') ? '红外图片' : '可见光图片'"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>航线名称:</strong>
            <input
              type="text"
              :value="jobInfo.file_name"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>照片分辨率:</strong>
            <input
              type="text"
              :value="`${selectedImage.width} * ${selectedImage.height}`"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>拍摄时间:</strong>
            <input
              type="text"
              :value="new Date(selectedImage.create_time).toLocaleString()"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>文件大小:</strong>
            <input
              type="text"
              :value="Number(selectedImage.size).toFixed(2) + 'M'"
              class="info-input"
              readonly
            />
          </div>
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
          <el-button icon="el-icon-rotate-left" @click="rotate" size="small"
            >旋转方向</el-button
          >
          <!-- 重置方向 -->
          <el-button
            icon="el-icon-refresh"
            @click="resetOrientation"
            size="small"
            >重置方向</el-button
          >
          <!-- 下载 -->
          <el-button
            icon="el-icon-download"
            @click="downloadImageLocal(selectedImage, 'origin')"
            size="small"
            >下载图片</el-button
          >
        </div>

        <!-- 下方显示缩略图 -->
        <div class="preview-thumbnails">
          <!-- 左侧滚动按钮 -->
          <!-- <button class="scroll-button left" @click="scrollLeft">&lt;</button> -->
          <div class="thumbnail-container">
            <!-- <el-row gutter="5">
              <el-col  -->
            <div
              v-for="(item, index) in mediaData.data"
              :key="index"
              class="thumbnail-item"
            >
              <img
                :src="getImageUrl(item.original_image_url)"
                alt=""
                class="thumbnail-image"
                :class="{ active: selectedImage === item }"
                @click="selectImage(item, 'origin')"
              />
              <!-- </el-col>
            </el-row> -->
            </div>
          </div>
          <!-- 右侧滚动按钮 -->
          <!-- <button class="scroll-button right" @click="scrollRight">&gt;</button> -->
        </div>
      </div>
    </el-dialog>
    <el-dialog
      v-model="previewAnaysisVisible"
      title="分析图片预览"
      width="1000px"
    >
      <div class="preview-modal-content">
        <!-- 左侧显示放大图片 -->
        <!-- <div class="preview-main"> -->
        <!-- 添加“上一张”和“下一张”按钮 -->
        <button class="prev-image" @click="showPreviousImage('defect')">‹</button>
        <div style="width: 500px; height: 500px;">
          <img
            :src="getImageUrl(selectedImage.defect_image_url)"
            alt="放大图"
            class="preview-image"
            ref="previewImage"
            style="object-fit: contain; width: 500px; height: 500px;"
          />
        </div>

        <button class="next-image" @click="showNextImage('defect')">›</button>
        <!-- </div> -->

        <!-- 右侧显示任务信息 -->
        <div class="preview-info">
          <div class="info-row">
            <strong>任务名称:</strong>
            <input
              type="text"
              :value="jobInfo.job_name"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>名称:</strong>
            <input
              type="text"
              :value="selectedImage.file_name"
              class="info-input"
              readonly
            />
          </div>
          <!-- <div class="info-row">
            <strong>关联点位:</strong>
            <input type="text" :value="selectedImage.point_name" class="info-input" readonly />
          </div> -->
          <div class="info-row">
            <strong>照片类型:</strong>
            <input
              type="text"
              :value="selectedImage.file_name.includes('_T') ? '红外图片' : '可见光图片'"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>航线名称:</strong>
            <input
              type="text"
              :value="jobInfo.file_name"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>照片分辨率:</strong>
            <input
              type="text"
              :value="`${selectedImage.width} * ${selectedImage.height}`"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>拍摄时间:</strong>
            <input
              type="text"
              :value="new Date(selectedImage.create_time).toLocaleString()"
              class="info-input"
              readonly
            />
          </div>
          <div class="info-row">
            <strong>文件大小:</strong>
            <input
              type="text"
              :value="Number(selectedImage.size).toFixed(2) + 'M'"
              class="info-input"
              readonly
            />
          </div>
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
          <el-button icon="el-icon-rotate-left" @click="rotate" size="small"
            >旋转方向</el-button
          >
          <!-- 重置方向 -->
          <el-button
            icon="el-icon-refresh"
            @click="resetOrientation"
            size="small"
            >重置方向</el-button
          >
          <!-- 下载 -->
          <el-button
            icon="el-icon-download"
            @click="downloadImageLocal(selectedImage,'defect')"
            size="small"
            >下载图片</el-button
          >
        </div>

        <!-- 下方显示缩略图 -->
        <div class="preview-thumbnails">
          <!-- 左侧滚动按钮 -->
          <!-- <button class="scroll-button left" @click="scrollLeft">&lt;</button> -->
          <div class="thumbnail-container">
            <!-- <el-row gutter="5">
              <el-col  -->
            <div
              v-for="(item, index) in mediaData.data"
              :key="index"
              class="thumbnail-item"
            >
              <img
                :src="getImageUrl(item.defect_image_url)"
                alt=""
                class="thumbnail-image"
                :class="{ active: selectedImage === item }"
                @click="selectImage(item, 'origin')"
              />
              <!-- </el-col>
            </el-row> -->
            </div>
          </div>
          <!-- 右侧滚动按钮 -->
          <!-- <button class="scroll-button right" @click="scrollRight">&gt;</button> -->
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, inject } from 'vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { CURRENT_CONFIG as config } from '/@/api/http/config'
import { IPage } from '/@/api/http/type'
import { Task } from '/@/api/wayline'
import { downloadFile } from '/@/utils/common'
import { Search, Download, Document, Upload, Refresh, Delete } from '@element-plus/icons-vue'
import { downloadMediaFile, getTaskResultTypeApi, getFlyTaskResultApi, downloadOriginImageZipApi, importLabelImageApi, downloadFlyTaskReportApi, downloadImageZipApi, createFlyTaskReportApi, downloadThumbnail, deleteFlyTaskReportApi } from '/@/api/media'
import { getDefectTypeMapApi, updateDefectTypeApi } from '/@/api/turbine/defect.ts'
import { EDeviceTypeName, ELocalStorageKey, ERouterName } from '/@/types'
import { insertTEMPConfig, insertTEMPConfig1 } from '/@/api/points'
import { CloseOutlined } from '@ant-design/icons-vue'
import { renderAsync } from 'docx-preview'
import { ElMessage } from 'element-plus'
const viewReportVisible = ref(false)
const editDefectVisible = ref(false)
const loading = ref(false) // 全局下载loading
const viewloading = ref(false) // 全局下载loading

const resultType = ref(1)

const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '40'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 0
})

const queryForm = reactive({
  name: '',
})
const editForm = reactive({})
const defectTypeMap = ref([])
const editFormRef = ref(null)
// 表单规则
const formRules = {
  defects: [
    { required: true, message: '请选择缺陷类型', trigger: 'change' }
  ]
}

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

// 图片预览参数
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
const previewVisible = ref(false) // 弹窗显示状态，初始值为 false
const previewAnaysisVisible = ref(false) // 分析图片弹窗显示状态，初始值为 false
const selectedImage = ref(mediaData.data[0]) // 初始选中第一个任务
const selectedIndex = ref(0) // 当前图片索引
const scale = ref(1) // 图片缩放比例
const rotation = ref(0) // 图片旋转角度

onMounted(() => {
  const data = JSON.parse(localStorage.getItem('TaskInfo'))
  jobInfo.job_id = data.job_id
  jobInfo.job_name = data.job_name
  jobInfo.begin_time = data.begin_time
  jobInfo.end_time = data.end_time
  jobInfo.status = data.status
  jobInfo.file_name = data.file_name
  jobInfo.file_id = data.file_id
  getTaskResultType()
  getFiles()
  getDefectTypeMap()
})

/**
 * 工具函数，路径拼接
 */
function getImageUrl (path:string) {
  if (!path) return ''

  const baseURL = config.baseURL || ''
  const imagePath = path.replace(/^\/+/, '') // 移除开头的斜杠

  // 确保baseURL以斜杠结尾，路径不以斜杠开头
  if (baseURL.endsWith('/')) {
    return baseURL + imagePath
  } else {
    return baseURL + '/' + imagePath
  }
}

/**
 * 获取任务结果显示类型
 */

async function getTaskResultType () {
  try {
    const res = await getTaskResultTypeApi(jobInfo.job_id)
    if (res.code !== 0) {
      ElMessage.error('系统异常')
      return
    }
    resultType.value = res.data
  } catch (error) {

  }
}

/**
 * 获取缺陷类型字典
 */
async function getDefectTypeMap () {
  try {
    const res = await getDefectTypeMapApi()
    defectTypeMap.value = res
  } catch (error) {
    ElMessage.error('获取缺陷类型失败!')
  }
}

/**
 * 生成任务结果报告
 */
async function createReport () {
  try {
    loading.value = true
    const response = await createFlyTaskReportApi({
      jobId: jobInfo.job_id
    })
    if (response.code === 0 || response.code === 602) {
      const res = await downloadFlyTaskReportApi(jobInfo.job_id)
      if (!res) {
        loading.value = false
        return
      }
      const data = new Blob([res])
      downloadFile(data, `${jobInfo.job_name}.docx`)
    } else if (response.code === 601) {
      ElMessage.warning('任务结果分析中,请稍后重试!')
    }
  } catch (error) {
    console.error('生成报告失败:', error)
    loading.value = false
  } finally {
    loading.value = false
  }
}

/**
 * 重置任务报告
 */
async function resetReport () {
  try {
    loading.value = true
    const response = await deleteFlyTaskReportApi(jobInfo.job_id)
    if (response.code === 0) {
      const res = await createFlyTaskReportApi({ jobId: jobInfo.job_id })
      if (response.code !== 0 && response.code !== 602) {
        loading.value = false
        ElMessage.warning('重置报告失败!')
        return
      }
    }
    ElMessage.warning('重置报告成功!')
  } catch (error) {
    console.error('生成报告失败:', error)
    loading.value = false
  } finally {
    loading.value = false
  }
}

/**
 * 下载图片数据集
 */
async function exportImageZip () {
  try {
    loading.value = true
    const res = await downloadImageZipApi(jobInfo.job_id)
    if (!res) {
      loading.value = false
      return
    }
    const data = new Blob([res])
    downloadFile(data, `${jobInfo.job_name}.zip`)
  } catch (error) {
    console.error('下载图片数据集失败:', error)
    loading.value = false
  } finally {
    loading.value = false
  }
}

/**
 * 下载初始图片
 */

async function exportOriginImage () {
  try {
    loading.value = true
    const res = await downloadOriginImageZipApi(jobInfo.job_id)
    if (!res) {
      loading.value = false
      return
    }
    const data = new Blob([res])
    downloadFile(data, `${jobInfo.job_name}.zip`)
  } catch (error) {
    console.error('下载图片数据集失败:', error)
    loading.value = false
  } finally {
    loading.value = false
  }
}

/**
 * 查看报告
 */
async function viewReport () {
  try {
    viewReportVisible.value = true
    viewloading.value = true

    // const res = await fetch('/public/fj1top20251218084530.docx')
    // const blob = await res.blob()
    // const docContainer = document.getElementById('docContainer')
    // docContainer.innerHTML = ''
    // 生成报告
    const response = await createFlyTaskReportApi({
      jobId: jobInfo.job_id
    })

    if (response.code === 0 || response.code === 602) {
      // 下载报告
      const res = await downloadFlyTaskReportApi(jobInfo.job_id)
      if (!res) {
        viewloading.value = false
        return
      }
      const data = new Blob([res])
      const docContainer = document.getElementById('docContainer')

      // 清空容器
      docContainer.innerHTML = ''

      // 直接传递Blob对象给renderAsync
      // await renderAsync(data, docContainer)
      await renderAsync(data, docContainer, null, {
        className: 'docx',
        inWrapper: true,
        breakPages: true,
        ignoreWidth: false,
        ignoreHeight: false,
        ignoreFonts: false,
        ignoreLastRenderedPageBreak: false
      })
    } else if (response.code === 601) {
      ElMessage.warning('任务结果分析中,请稍后重试!')
    }
  } catch (error) {
    console.error('生成报告失败:', error)
    viewloading.value = false
  } finally {
    viewloading.value = false
  }
}

/**
 * 人工修正
 */
// 打开弹窗
function defectSelect (row) {
  editForm.id = row.defect_id
  editDefectVisible.value = true
}

// 提交选项
async function handleCorrection () {
  try {
    const valid = await editFormRef.value.validate()
    if (!valid) {
      ElMessage.warning('请检查表单是否填写!')
    }
    const res = await updateDefectTypeApi(editForm)
    if (res.code !== 0) {
      ElMessage.error('修正失败!')
    } else {
      ElMessage.success('修正成功!')
      editDefectVisible.value = false
      getFiles()
    }
  } catch (error) {

  }
}

/**
 * @description: 获取媒体文件
 * @param {string} workspaceId 工作空间id
 * */
const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
async function getFiles () {
  // mediaData.data = res.list
  // paginationProp.total = res.pagination.total
  // getUrls()
  getFlyTaskResultApi({
    job_id: jobInfo.job_id,
    workspace_id: workspaceId,
    page: paginationProp.current,
    pageSize: paginationProp.pageSize,
    ...queryForm
  }).then(res => {
    mediaData.data = res.data.list
    paginationProp.total = res.data.pagination.total
  })
}

// 重置
function reset () {
  queryForm.name = ''
  getFiles()
}

/**
 * @description: 下载分析图片到本地
 * @param {string} workspaceId 工作空间id
 * @param {string} type 图片类型 origin：原图，defect:分析图
 * */

function downloadImageLocal (media: any, type: string) {
  // 判断media是否包含url属性
  let imageUrl = ''
  if (type === 'defect') {
    if (!media || !media.defect_image_url) {
      ElMessage.error('图片地址无效')
      return
    }
    imageUrl = getImageUrl(media.defect_image_ur)
  } else {
    if (!media || !media.original_image_url) {
      ElMessage.error('图片地址无效')
      return
    }
    imageUrl = getImageUrl(media.original_image_url)
  }

  // 从图片地址下载
  fetchImageFromUrl(imageUrl, media.file_name)
    .catch(error => {
      console.error('下载失败:', error)
      ElMessage.error('下载失败: ' + error.message)
    })
    .finally(() => {
    })
}

// 从图片地址获取并下载
async function fetchImageFromUrl (imageUrl: string, fileName: string) {
  try {
    // 获取图片数据
    const response = await fetch(imageUrl, {
      method: 'GET',
      headers: {
        Accept: 'image/*',
      },
      mode: 'cors',
      credentials: 'omit',
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    // 获取blob数据
    const blob = await response.blob()

    if (blob.size === 0) {
      throw new Error('获取的图片数据为空')
    }

    // 检查是否为有效的图片格式
    if (!blob.type.startsWith('image/')) {
      throw new Error('获取的不是有效的图片文件')
    }

    // 调用下载函数
    downloadFile(blob, fileName)

    ElMessage.success('下载成功')
  } catch (error) {
    console.error('获取图片失败:', error)
  }
}

// ===========================================================前端分页功能实现==================================================
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  getFiles()
}
function handleCurrentChange (val: number) {
  paginationProp.current = val
  getFiles()
}

/**
 * 弹窗预览
 * @param row
 */
// 打开预览原始图片弹窗
async function openPreviewModal (row: any) {
  origionImageUrls.value = []
  selectedImage.value = row // 设置选中的图像
  selectedIndex.value = mediaData.data.findIndex((item) => item.file_id === row.file_id) // 更新索引
  // 临时赋值，使得先展示弹窗，然后加载数据
  selectedImage.value.Temp = ''
  selectedImage.value.width = 0
  selectedImage.value.height = 0
  selectedImage.value.size = 0.00
  // 将弹窗显示状态设为 true
  previewVisible.value = true

  getImageAttributes(row.defect_image_url)
}

// 打开预览分析图片的弹窗
async function openPreviewAnaysisModal (row:any) {
  origionImageUrls.value = []
  selectedImage.value = row // 设置选中的图像
  selectedIndex.value = mediaData.data.findIndex((item) => item.file_id === row.file_id) // 更新索引
  // 临时赋值，使得先展示弹窗，然后加载数据
  selectedImage.value.Temp = ''
  selectedImage.value.width = 0
  selectedImage.value.height = 0
  selectedImage.value.size = 0.00
  // 将弹窗显示状态设为 true
  previewAnaysisVisible.value = true
  getImageAttributes(row.defect_image_url)
}

// 获取图片属性信息
async function getImageAttributes (Url: string) {
  try {
    // 先设置图片地址
    const imageUrl = getImageUrl(Url)

    // 获取图片信息
    const img = new Image()

    const imageInfo = await new Promise<{ width: number; height: number; size: number }>((resolve, reject) => {
      img.onload = async () => {
        try {
          // 获取文件大小
          let fileSize = 0.0
          // 如果是远程图片，尝试获取文件大小
          if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
            try {
              const response = await fetch(imageUrl, { method: 'HEAD' })
              const contentLength = response.headers.get('content-length')
              if (contentLength) {
                const sizeInBytes = parseInt(contentLength, 10)
                fileSize = parseFloat((sizeInBytes / (1024 * 1024)).toFixed(2))
              }
            } catch (e) {
              console.warn('无法获取远程图片大小:', e)
            }
          }

          resolve({
            width: img.width,
            height: img.height,
            size: fileSize
          })
        } catch (error) {
          reject(error)
        }
      }

      img.onerror = () => {
        reject(new Error('图片加载失败'))
      }

      img.src = imageUrl

      // 设置超时
      setTimeout(() => {
        if (!img.complete) {
          reject(new Error('图片加载超时'))
        }
      }, 10000)
    })
    // 更新图片信息
    selectedImage.value.width = imageInfo.width
    selectedImage.value.height = imageInfo.height
    selectedImage.value.size = imageInfo.size

    return imageInfo
  } catch (error) {
    console.error('获取图片属性失败:', error)
    selectedImage.value.width = 0
    selectedImage.value.height = 0
    selectedImage.value.size = 0.0

    // 返回默认值
    return {
      width: 0,
      height: 0,
      size: 0.0
    }
  }
}

// 切换到上一张图片
async function showPreviousImage (type:string) {
  if (selectedIndex.value > 0) {
    selectedIndex.value -= 1
    selectedImage.value = mediaData.data[selectedIndex.value]
    // 判断是分析图列表还是原始图列表
    if (type === 'defect') {
      getImageAttributes(selectedImage.value.defect_image_url)
    } else {
      getImageAttributes(selectedImage.value.original_image_url)
    }
  }
}

// 切换到下一张图片
async function showNextImage (type:string) {
  if (selectedIndex.value < mediaData.data.length - 1) {
    selectedIndex.value += 1
    selectedImage.value = mediaData.data[selectedIndex.value]
    if (type === 'defect') {
      getImageAttributes(selectedImage.value.defect_image_url)
    } else {
      getImageAttributes(selectedImage.value.original_image_url)
    }
  }
}

// 选择缩略图
async function selectImage (item: any, type:string) {
  // 设置当前选中的图片
  selectedImage.value = item
  if (type === 'defect') {
    getImageAttributes(selectedImage.value.defect_image_url)
  } else {
    getImageAttributes(selectedImage.value.original_image_url)
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

:deep(.el-tag.el-tag--info){
  --el-tag-text-color: #aeb5c3;
  --el-tag-bg-color:rgb(39 75 109);
}

.okbtn {
    background-color: rgba(7, 75, 208, 1);
    // height: 40px;
    border: 1px solid rgba(0, 64, 147, 1);
}

.okbtn:hover {
    border: 1px solid rgba(0, 112, 209, 1);
}

.nobtn {
    color: white;
    background-color: rgba(255, 255, 255, 0.2);
    // height: 40px;
    border: 1px solid rgba(206, 227, 255, 0.42);
}

.doc-preview-container {
  width: 100%;
  height: 600px;
  overflow: auto;
  background: #f5f5f5;
  display: flex;
  justify-content: center;
  /* 强制所有表格显示边框 */
  :deep(table) {
    border-collapse: collapse !important;
    width: 100% !important;
    margin: 10px 0 !important;
  }

  :deep(table td),
  :deep(table th) {
    border: 1px solid #ddd !important;
    padding: 6px 8px !important;
    min-height: 20px !important;
    vertical-align: top !important;
  }

  :deep(table th) {
    background-color: #f5f5f5 !important;
    font-weight: bold !important;
    text-align: center !important;
  }

  /* 确保表格在分页时不被截断 */
  :deep(table) {
    page-break-inside: avoid !important;
    break-inside: avoid !important;
  }
}

:deep(article) {
  padding: 50px;
}

/* 为所有段落添加分页控制 */
// :deep(.docx-wrapper > section) {
//   page-break-after: auto;
//   page-break-inside: avoid;
//   page-break-before: auto;
// }

// :deep(.docx-wrapper) {
//   background: rgb(210, 210, 210);
// }

:deep(.el-dialog__title) {
  color: white !important;
}

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
  padding-top: 15px;
  padding-left: 15px;
  .new_btn {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    height: 30px;

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
  .delete-bg{
        background-image: linear-gradient(180deg,
        rgb(243, 172, 172) 0,
        rgb(213 53 5) 100%) !important;
  }
  .new_btn1 {
    background-image: linear-gradient(180deg,
        rgba(248, 212, 94, 1) 0,
        rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    height: 30px;

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
