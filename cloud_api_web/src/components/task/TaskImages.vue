<template>
  <div class="container">
    <div class="header1">
        <div>任务结果</div>
        <div>
            <el-breadcrumb :separator-icon="icon" >
                <el-breadcrumb-item :to="{ path: '/task/TaskHistory' }" class="breadcrumb-item">历史任务</el-breadcrumb-item>
                <el-breadcrumb-item class="breadcrumb-item">任务结果</el-breadcrumb-item>
             </el-breadcrumb>
        </div>

    </div>
    <div class="divider"></div>
    <div class="content">
        <div class="left scrollbar" :style="{ height: scorllHeight + 'px'}" >
            <div class="point-title">点位信息</div>
            <div class="point-content" v-for="(item, index) in points" :key="index" @click="selestPoints(item, index)" :class="{ 'selected1': selectedIndex1 === index }">
                <span><i>航点名称 :</i> {{item.point_name }}</span>
                <span><i>图片数量 :</i>   {{item.data.length }}</span>
                <span ><i>生成时间 : </i> {{item.data[0].create_time }}</span>
                <span style="border-bottom: none;"><i>报告状态 :</i>  使用中</span>
            </div>
        </div>
        <div class="middle">
            <div style="padding: 5px;">
                <el-image style="width: 100%; height: 100%" :src="zoomImg" fit="fill" />
            </div>

        </div>
        <div class="right scrollbar" :style="{ height: scorllHeight + 'px'}">
            <div class="images-box " v-for="(url, index) in urls" :key="index"  @click="selestImg(index)" :class="{ 'selected': selectedIndex === index }">
                <div class="img">
                    <el-image style="width: 100%; height: 100%" :src="url.url" fit="fill" />
                </div>
                <div class="text">
                    <span>航点: {{url.point_name }}</span>
                    <span>时间: {{url.time}}</span>
                    <span>报告状态: 正常</span>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>
<script setup>
import { message } from 'ant-design-vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { ref, reactive, onMounted, shallowRef, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { downloadMediaFile } from '/@/api/media'
import { toRequest } from '/@/api/algorithm'
import { ELocalStorageKey } from '/@/types/enums'
import { useMyStore } from '/@/store'
const route = useRoute()
const router = useRouter()
const jobId = route.query.job_id
const workspaceId = localStorage.getItem('workspace_id') || ''
// 航点信息
const points = ref()
const scorllHeight = ref()
const zoomImg = ref()
const urls = ref([])
const store = useMyStore()
const selectedIndex = ref(null)
const selectedIndex1 = ref(null)
const icon = shallowRef(ArrowRight)
let cancelInit = false // 取消标志,用户点击航点时，终止init()加载所有图片
onMounted(() => {
  scorllHeight.value = window.innerHeight - 150
  // points.value = transformedData(store.state.Imgs)
  points.value = transformedData(JSON.parse(localStorage.getItem('TaskResult')))
  // console.log(points.value)
  if (!points.value || points.value.length === 0) {
    router.push('/task')
  }
  //   points.value = transformedData(JSON.parse(route.query.data))
  init()
  // startAnaysis()
})
onUnmounted(() => {
  cancelInit = true
})

// 初始化
async function init () {
  cancelInit = false // 重置取消标志
  urls.value = [] // 确保 urls 在初始化开始时是空的
  for (const item of points.value) {
    for (const ele of item.data) {
      if (cancelInit) {
        urls.value = []
        console.log('Initialization canceled')
        return
      }
      const url = await downloadMedia(ele.file_id)
      if (url) {
        const node = {
          url: url,
          file_id: ele.file_id,
          point_name: item.point_name,
          time: ele.create_time

        }
        urls.value.push(node)
        zoomImg.value = urls.value[0].url
      }
    }
  }
}
// 点击图片事件
function selestImg (val) {
  selectedIndex.value = val
  zoomImg.value = urls.value[val].url
}

// 点击左侧航点事件
async function selestPoints (imgs, index) {
  // 不取消取消当前的初始化
  // cancelInit = false
  // urls.value = []
  selectedIndex1.value = index
  zoomImg.value = urls.value[selectedIndex1.value].url
  // console.log(index)
  // for (const element of imgs.data) {
  //   if (!urls.value.some(node => node.file_id === element.file_id)) {
  //     const url = await downloadMedia(element.file_id)
  //     if (url) {
  //       const node = {
  //         url: url,
  //         file_id: element.file_id,
  //         point_name: imgs.point_name,
  //         time: element.create_time
  //       }
  //       urls.value.push(node)
  //       zoomImg.value = urls.value[0].url
  //     }
  //   }
  // }
}

// 加载图片
// function downloadMedia (file_id) {
//   let imgUrl = null
//   downloadMediaFile(workspaceId, file_id).then(res => {
//     if (!res) {
//       return
//     }
//     open.value = true
//     const data = new Blob([res])
//     imgUrl = URL.createObjectURL(data)
//     urls.value.push(imgUrl)
//   })
//   return imgUrl
// }
function downloadMedia (file_id) {
  return new Promise((resolve, reject) => {
    downloadMediaFile(workspaceId, file_id).then(res => {
      if (!res) {
        return reject(new Error('Failed to download media file'))
      }
      const data = new Blob([res])
      const imgUrl = URL.createObjectURL(data)
      resolve(imgUrl)
    }).catch(error => {
      reject(new Error(error.message || 'Failed to download media file'))
    })
  })
}
// 数据格式转换
function transformedData (originalData) {
  // Convert the original data to the desired format
  return Object.keys(originalData).map(key => ({
    point_name: key,
    data: originalData[key]
  }))
}
// ----------------------------------------------------------------------进行算法分析-------------------------------------------------------------------------------------------
const mydata = {
  requestHostIp: '172.20.63.177',
  requestHostPort: '8080',
  requestId: '123e4567-e89b-12d3-a456-426614174000',
  objectList: [
    {
      objectId: '121',
      typeList: ['yw_gkxfw'],
      imagePathList: [
        'ftps://minioadmin:minioadmin@172.20.63.157:8021/test1916/wayline/org_5e9013f5b8bb1c28_1719555794000.jpg'
      ]
    }
  ]
}
function startAnaysis () {
  toRequest(mydata).then(res => {
    if (res.code !== 0) {
      return
    }
    // waylinesData.data = [...waylinesData.data, ...res.data.list]
    const data = res.data
    console.log('算法返回', data)
  })
}

</script>
<style lang="scss" scoped>
.container {
  // height: 100%;
  width: 100vw;
  padding: 10px 0;
  display: flex;
  flex-direction: column; /* 使子元素垂直排列 */
}
// 头部  标题 面包屑
.header1 {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px 26px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color:aliceblue;
  display: flex;
  justify-content: space-between;
}
// 分割线
.divider{
    padding: 0 15px;
    height:2px;
    border: 1px solid #194864;
    // border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
}
.content{
   display: flex;
   margin: 10px;
   border: 2px solid #194864;
   height: calc(100vh - 150px);
    .left {
      flex: 0 0 15%;
      border-right: 2px solid #194864;
      display: flex;
      flex-direction: column;
      padding: 10px;
      .point-title{
        height: 30px;
        line-height: 30px;
        font-size: 16px;
        font-weight: bold;
        color: rgba(255, 255, 255, 0.79);
        margin-bottom: 15px;
        &::before{
            content: '';
            // position: absolute;
            height: 14px;
            width: 14px;
            margin-top: 10px;
            display: inline-block;
            border-radius: 50%;
            background-color: #11B4FB;
            border: 2px solid #194864;
            margin-right: 10px;
        }
      }
      .point-content{
        height: fit-content;
        color: rgba(255, 255, 255, 0.79);
        border: 2px solid #1b4c68b4;
        margin-bottom: 15px;
        display: flex;
        flex-direction: column;
        span{
            border-bottom: 2px solid #194864b4;
            padding: 5px;
            margin-right: 10px;
            i{
                margin-right: 10px;
                background: rgba(255, 255, 255, 0.1);
            }
        }
      }
    }
    .middle {
      flex: 0 0 40%;
      border-right: 2px solid #194864;
    }
    .right {
      flex: 0 0 45%;
      padding: 20px 0 20px 20px;
      .images-box{
        margin: 0 15px 15px 0;
        display: inline-block;
        width: calc(100%/3 - 15px);
        box-sizing: border-box;
        vertical-align: top;
        height: 350px;
        // box-shadow: 0 0 0 2px #f5f5f5;
        .img{
            width: 100%;
            height: 230px;
        }
        .text{
            width: 100%;
            height: 120px;
            padding: 10px;
            color: rgba(255, 255, 255, 0.79);;
            background-color: #043661;
            display: flex;
            flex-direction: column;
            span{
            padding: 5px;
            margin-right: 10px;
            }
        }
      }
    }
}
.scrollbar{
    overflow: auto;
}
.images-box.selected {
  box-shadow: 0 0 0 2px #f5f5f5;
}
.point-content.selected1 {
   border: 2px solid #eeeff0b4;
}

// 面包屑
.breadcrumb-item :deep(.el-breadcrumb__inner) {
    color:aliceblue ;
    font-size: 20px;
}
// {
//     font-size: 16px;
//     color:aliceblue !important;
// }
.operation{
 height: fit-content;
 padding: 15px;
 display: flex;
 justify-content: space-between;
 color: rgba(255, 255, 255, 0.762);
 background-color: rgba(0, 112, 209, 0.2);
 font-size: 16px;
 border: 4px solid rgba(0, 112, 209, 1);
 border-bottom: none;
 border-image: linear-gradient(90deg, rgba(54, 143, 232, 0), rgba(0, 112, 209, 1), rgba(54, 143, 232, 0)) 1 1;
 .item1{
  display: flex;
  justify-items: center;
  align-items: center;
 }
}
// 输入框
:deep(.el-input) {
    --el-input-border-color: #1d4292;
}
:deep(.el-input__wrapper) {
    background-color:#0B2756;
  }
:deep(.el-select__wrapper){
  background-color:#0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
}
.btn{
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color:rgba(255, 255, 255, 0.762);
}
</style>
