<template>
  <div class="row new_row" >
    <div class="show-state1">
      <span class="iconfont icon-tubiaozhizuomoban1 " style="font-size: 25px;"> </span>
      <span class="show-icon">暂无控制权限</span>
    </div>
    <el-row
      :gutter="10"
      style="display: flex; align-items: center;"
    >
     <el-col :span="4">
        <span class="text-show">虚拟摇杆</span>
      </el-col>
      <el-col :span="16">
          <div style="display: flex; align-items: center; margin-left: 0px; margin-top: 0px">
            <img src="../../assets/v3/icon/rightBottom2.png"  style="width: 100px; height: auto;">
            <img src="../../assets/v3/icon/rightBottom3.png"  style="width: 100px; height: auto;"/>
          </div>

          <div class="joystick-box">
            <div v-show="!isMouseDown" class="fixedJoystick"></div>
            <div ref="joystickDirection"  class="joystick-container"></div>
          </div>

          <!-- <div style="display: flex; align-items: center;justify-content: center;">
            <span class="desc">虚拟摇杆</span>
          </div>      -->
      </el-col>

      <el-col :span="4">
      </el-col>
    </el-row>

    <div class="middleUavRight4">
      <span>
        <el-button class="btn" @click="getControlAuth">急停</el-button>
      </span>
      <span>
        <el-button class="btn" @click="releaseControlAuth">一键起飞</el-button>
      </span>
      <span>
        <el-button class="btn" @click="releaseControlAuth">返航</el-button>
      </span>
      <span>
        <el-button class="btn" @click="releaseControlAuth">指点飞行</el-button>
      </span>
      <span>
        <el-button class="btn" @click="releaseControlAuth">取消指点飞行</el-button>
      </span>
    </div>
  </div>
</template>
<script setup>
import { defineComponent, toRefs, computed, ref, reactive, onMounted } from 'vue'
import nipplejs from 'nipplejs'
import { requestControlAuto, deleteControlAuto, changeVideoType, changeCameraMode, cameraRecording, cameraPhotoTake, PTZControl, PTZReset, FocalLengthSet, exposureValueSet, exposureModeSet } from '/@/api/pilot-control/pilot-control'
import { ElMessage } from 'element-plus'

// 配置项，我暂时用到的是这些，有其他需要可以添加
const joystickDirection = ref(null)
const isMouseDown = ref(false)
onMounted(() => {
  const options = {
    zone: joystickDirection.value,
    mode: 'dynamic',
    position: { left: '50%', top: '50%' },
    color: 'blue',
  }
  // dynamic
  const manager = nipplejs.create(options)

  manager.on('move', (evt, data) => {
    isMouseDown.value = true
    const dd = data.vector
    const dse = '2' + '_' + dd.x + '_' + dd.y
    // sendMessage(dse);
    console.log(dse)
  })

  manager.on('end', (evt, data) => {
    const dss = '2' + '_' + 0 + '_' + 0
    // sendMessage(dss);
    console.log(dss)
    isMouseDown.value = false
  })
})
</script>

<style lang="scss" scoped>
.joystick-box{
  position: relative;
  left: 30px;
  top: -47px;
  width: 140px;
  height: 140px;
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  border: 1px solid #719fff;
  background: #012b78;
  border-radius: 50%;
}

// 固定摇杆，在鼠标未点击时显示
.fixedJoystick{
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%); /* 移动到中心 */
  width: 80px; /* 小圆的宽度 */
  height: 80px; /* 小圆的高度 */
  // transform: translate(-50%, -50%); /* 移动到中心 */
  // margin: -25px 0 0 -25px; /* 负边距使小圆心点在父容器的中心 */
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  border: 1px solid #719fff;; /* 小圆的边框 */
  border-radius: 50%; /* 小圆形 */
  background: #012b78;

}

.joystick-box:hover .fixedJoystick{
  background-color:skyblue
}
.joystick-container{
  width: 100%;
  height: 100%;
  position: relative;
  background-color: rgba(0, 0, 255, 0.1); /* 半透明蓝色背景，可视化区域 */
  border-radius: 50%;
  border: 2px dashed blue; /* 虚线边框，便于观察 */
  padding: 0;
}

.desc{
  text-align: center;
  color: #ffffff;
  font-size: 14px;
}
.show-state{
  margin-bottom: 10px;
  margin-left: 40px;
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #FFCF34;
  display: flex;
  align-items: center;
  animation: scroll-left 10s linear infinite;
}
.show-state1{
  margin-bottom: 10px;
  margin-left: 40px;
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #219c23;
  display: flex;
  align-items: center;
  animation: scroll-left 10s linear infinite;
}
/* 定义滚动动画 */
@keyframes scroll-left {
  0% {
    transform: translateX(100%); /* 从右侧外部开始 */
  }
  100% {
    transform: translateX(-100%); /* 滚动到左侧外部 */
  }
}
.text-show {
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #ffffff;
  font-style: normal;
  letter-spacing: 2px;
  writing-mode: vertical-rl;
  // line-height: 20px; /* 设置合适的行高，确保每行文本的高度一致 */
  height: auto; /* 设置一个固定的高度，确保文本区域高度一致 */
  display: flex;
  justify-content: center;
}
.show-icon{
  margin-left: 10px;

}
.button-disabled {
  background-color: gray !important;
  cursor: not-allowed;
}
.center-layout{
  display: flex;
  align-items: center;
}
.sector {
  box-sizing: content-box;
  position: relative;
  left: -2px;
  top: -47px;
  right: 30px;
  bottom: 6px;
  width: 145px;
  height: 145px;
  margin: 0 auto;
  border: 1px solid #0a1e41;
  border-radius: 50%;
  // background: #0a1e41;
  overflow: hidden;
  transform: rotate(-5deg);
}

.sector .box {
  position: absolute;
  width: 145px;
  height: 145px;
  border-radius: 0%;
  border: none;
  box-shadow: none;
  clip-path: url(#sector);
}

.sector .box {
  transition: 0.5s;
  cursor: pointer;
  background: #012b78;
}
.sector .center {
  transition: 0.5s;
  cursor: pointer;
  background: #005cb4;
}
.sector .box:hover,
.sector .center:hover {
  background: skyblue;
}

.sector .s1 {
  transform: rotate(0deg);
}

.sector .s2 {
  transform: rotate(90deg);
}

.sector .s3 {
  transform: rotate(180deg);
}

.sector .s4 {
  transform: rotate(270deg);
}

// .sector .s5 {
//   transform: rotate(202.5deg);
// }

// .sector .s6 {
//   transform: rotate(247.5deg);
// }

// .sector .s7 {
//   transform: rotate(292.5deg);
// }

// .sector .s8 {
//   transform: rotate(337.5deg);
// }

.sector .center {
  width: 68px;
  height: 68px;
  position: absolute;
  left: 36px;
  top: 36px;
  box-sizing: content-box;
  border: 2px solid #0a1e41;
  border-radius: 50%;
}

.arrow {
  float: none;
  transform: rotate(-90.5deg);
  width: 100%;
  height: 100%;

  margin: 0;
  padding: 0;
  padding-top: 11px;
  //padding-left: 2px;
  box-sizing: border-box;
  border: none;
  color: white;
  text-align: center;
  font-size: 18px;
}
.arrow-reset{
  // float: none;
  transform: rotate(5deg);
  // transform: rotate(5deg);
  width: 100%;
  height: 100%;
  // margin: 0;
  // padding: 0;
  // padding-top: 11px;
  //padding-left: 2px;
  box-sizing: border-box;
  border: none;
  color: white;
  // text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #ffffff;
  font-style: normal;
  letter-spacing: 2px;
  // writing-mode: vertical-rl;
}
.button-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  position: relative;
  top: -20px;
}
.button-circle {
  background: rgba(59, 116, 255, 0.15);
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  color: #ffffff;
  border: none;
  width: 30px;
  height: 30px;
  transition: 0.5s;
  border-radius: 50%;
  cursor: pointer;
}

.button-circle:hover {
  background: skyblue;
}

.middleUavRight4 {
        // width: 50%;
        display: grid;
        grid-template-columns: repeat(2, 1fr); /* 两列布局 */
        gap: 12px;
        position: relative;
        left: 10px;
        top:0px;
        .uav-operation-info {
          display: flex;
          align-items: center;
          background: rgba(59, 116, 255, 0.15);
          -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          border: 1px solid #719fff;
          border-radius: 4px;
          width: 150px;
          height: 36px;
          padding-left: 5px;
          font-family: PingFangSC, PingFang SC;
          font-weight: 500;
          font-size: 14px;
          color: #ffffff;
          text-align: left;
          font-style: normal;
          letter-spacing: 1px;
        }
        // 下拉框
        .select-operation{
          :deep(.el-select__placeholder){
            font-size: 14px;
            font-weight: 500;
            color: #ffffff;

          }
          :deep(.el-select__wrapper) {

            background: rgba(59, 116, 255, 0.15);
            box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
            border: 1px solid #719fff;
            border-radius: 4px;
            width: 150px;
            height: 36px;
            padding-left: 30px;
            text-align: center;
          }
          /**修改下拉图标颜色 */
          :deep(.el-select__caret){
            color: #ffffff;
          }

          :deep(.el-select-dropdown){
            background: #012b78;
            box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
            border: 1px solid #719fff;

          }
          :deep(.el-select-dropdown__item){
            font-size: 14px;
            font-weight: 500;
            color: #ffffff;
          }
          :deep(.el-select-dropdown__item.is-hovering){

            background-color: skyblue;
          }
        }
        // 按钮
        .btn{
          background: rgba(59, 116, 255, 0.15);
          -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          border: 1px solid #719fff;
          border-radius: 4px;
          width: 150px;
          height: 36px;
          font-family: PingFangSC, PingFang SC;
          font-weight: 500;
          font-size: 14px;
          color: #ffffff;
          &:hover{
            background: skyblue;
          }

        }
      }
.number-show{
  height: 30px;
  width: 45px;
  // background-color:rgba(59, 116, 255, 0.15);
  background: linear-gradient(
    to bottom,
    rgba(59, 116, 255, 0.6) 0%,
    rgba(59, 116, 255, 0) 50%,
    rgba(59, 116, 255, 0.6) 100%
  );
  border-radius: 4px;
  border: 1px solid #719fff;
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  color: #ffffff;
  font-weight: 500;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
