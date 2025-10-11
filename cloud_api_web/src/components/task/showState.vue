<template>
    <div class="control-panel">
        <!-- Tab切换部分 -->
        <div class="tabs">
            <button :class="{ active: activeTab === '无人机状态' }" @click="activeTab = '无人机状态'">无人机状态</button>
            <button :class="{ active: activeTab === '机库状态' }" @click="activeTab = '机库状态'">机库状态</button>
        </div>

        <!-- 无人机状态页面 -->
        <div v-if="activeTab === '无人机状态'" class="drone-status-panel">
            <!-- 状态部分 -->
            <div class="status-container">
                <div class="status-text">
                    <span>状态</span>
                    <span class="status-value">{{ droneData.UAVstatus }}</span>
                </div>
                <div class="coordinates-container">
                    <span class="coordinates">经度: {{ droneData.longitude }}</span>
                    <span class="coordinates">纬度: {{ droneData.latitude }}</span>
                    <span class="coordinates">高度: {{ droneData.altitude }}</span>
                </div>
            </div>
            <!-- 传感器和信号状态 -->
            <div class="grid-container">
                <div class="grid-item">
                    <span>遥控器状态</span>
                    <span class="status-active">{{ droneData.rcStatus }}</span>
                </div>
                <div class="grid-item">
                    <span>遥控器信号</span>
                    <span class="status-value">{{ droneData.rcSignal }}</span>
                </div>
                <div class="grid-item">
                    <span>图传信号</span>
                    <span class="status-value">{{ droneData.videoSignal }}</span>
                </div>

                <!-- 速度、角度、位置相关信息 -->
                <div class="grid-item">
                    <span>水平速度(m/s)</span>
                    <span class="status-value">{{ droneData.horizontalSpeed }}</span>
                </div>
                <div class="grid-item">
                    <span>垂直速度(m/s)</span>
                    <span class="status-value">{{ droneData.verticalSpeed }}</span>
                </div>
                <div class="grid-item">
                    <span>偏航角</span>
                    <span class="status-value">{{ droneData.yawAngle }}°</span>
                </div>
                <div class="grid-item">
                    <span>相对高度(m)</span>
                    <span class="status-value">{{ droneData.relativeHeight }}</span>
                </div>
                <div class="grid-item">
                    <span>仿地高度(m)</span>
                    <span class="status-value">{{ droneData.groundHeight }}</span>
                </div>
                <div class="grid-item">
                    <span>云台俯仰角</span>
                    <span class="status-value">{{ droneData.gimbalPitch }}°</span>
                </div>

                <!-- 其他信息 -->
                <div class="grid-item">
                    <span>变焦信数</span>
                    <span class="status-value">{{ droneData.zoomLevel }}</span>
                </div>
                <div class="grid-item">
                    <span>电池温度(°C)</span>
                    <span class="status-value">{{ droneData.batteryTemp }}°</span>
                </div>
                <div class="grid-item">
                    <span>电池电量</span>
                    <span class="status-value">{{ droneData.batteryLevel }}</span>
                </div>
                <div class="grid-item">
                    <span>电池电压(V)</span>
                    <span class="status-value">{{ droneData.batteryVoltage }}</span>
                </div>
                <div class="grid-item">
                    <span>电池循环次数</span>
                    <span class="status-value">{{ droneData.batteryCycles }}</span>
                </div>
                <div class="grid-item">
                    <span>目标航点名称</span>
                    <span class="status-link">{{ droneData.targetWaypoint }}</span>
                </div>
                <div class="grid-item">
                    <span>最高温</span>
                    <span class="status-value">{{ droneData.maxTemp }}</span>
                </div>
                <div class="grid-item">
                    <span>最低温</span>
                    <span class="status-value">{{ droneData.minTemp }}</span>
                </div>
                <div class="grid-item">
                    <span>平均温</span>
                    <span class="status-value">{{ droneData.avgTemp }}</span>
                </div>
            </div>

            <!-- 操作按钮 -->
            <div class="operations">
                <button @click="transmitCommand">暂停</button>
                <button @click="calibrate">恢复</button>
                <button @click="forcereturn">强制返航</button>
            </div>
        </div>

        <!-- 机库状态页面 -->
        <div v-if="activeTab === '机库状态'" class="hangar-status-panel">
            <!-- 状态部分 -->
            <div class="status-row">
                <span>机库状态</span>
                <span class="status-value connected">{{ deviceData.hangarStatus }}</span>
                <span>舱门状态</span>
                <span class="status-value closed">{{ deviceData.cabinDoorStatus }}</span>
            </div>

            <div class="status-row">
                <span>推杆状态</span>
                <span class="status-value active">{{ deviceData.pushRodStatus }}</span>
                <span>空调状态</span>
                <span class="status-value off">{{ deviceData.airConditionStatus }}</span>
            </div>

            <div class="status-row">
                <span>充电状态</span>
                <span class="status-value not-charging">{{ deviceData.chargingStatus }}</span>
            </div>

            <!-- 操作部分 -->
            <div class="operations">
                <div class="operation-row">
                    <span>舱门</span>
                    <button @click="openDoor">打开</button>
                    <button @click="closeDoor">关闭</button>
                </div>
                <div class="operation-row">
                    <span>推杆</span>
                    <button @click="resetPusher">复位</button>
                    <button @click="clampPusher">夹紧</button>
                </div>
                <div class="operation-row">
                    <span>空调</span>
                    <button @click="turnOnAC">打开</button>
                    <button @click="turnOffAC">关闭</button>
                </div>
                <div class="operation-row">
                    <span>手柄</span>
                    <button @click="openHandle">打开</button>
                    <button @click="closeHandle">关闭</button>
                </div>
                <div class="operation-row">
                    <span>充电</span>
                    <button @click="startCharging">开机</button>
                    <button @click="stopCharging">关机</button>
                    <button @click="charge">充电</button>
                    <button @click="standby">待机</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
const droneData = reactive(
  {
    UAVstatus: '开机',
    longitude: '0',
    latitude: '0',
    altitude: '0',
    rcStatus: '开机', // 遥控器状态
    rcSignal: '80%', // 遥控器信号
    videoSignal: '80%', // 图传信号
    horizontalSpeed: '5.5', // 水平速度（单位：m/s）
    verticalSpeed: '0.0', // 垂直速度（单位：m/s）
    yawAngle: '45.0', // 偏航角（单位：度）
    relativeHeight: '10.0', // 相对高度（单位：m）
    groundHeight: '5.0', // 仿地高度（单位：m）
    gimbalPitch: '-15.0', // 云台俯仰角（单位：度）
    zoomLevel: '2.0', // 变焦信数
    batteryTemp: '43.7', // 电池温度（单位：°C）
    batteryLevel: '45%', // 电池电量
    batteryVoltage: '15.35', // 电池电压（单位：V）
    batteryCycles: '15', // 电池循环次数
    targetWaypoint: '回巢点', // 目标航点名称
    maxTemp: '--', // 最高温
    minTemp: '--', // 最低温
    avgTemp: '--' // 平均温

  }
)

const deviceData = reactive(
  {
    hangarStatus: '连接',
    cabinDoorStatus: '关闭',
    pushRodStatus: '夹紧',
    airConditionStatus: '关闭',
    chargingStatus: '未充电'

  }
)

// 使用ref来定义响应式的变量
const activeTab = ref('无人机状态') // 默认显示无人机状态

// 无人机状态的按钮功能
const transmitCommand = () => {
  alert('暂停指令发送')
}

const calibrate = () => {
  alert('校准完成')
}

const forcereturn = () => {
  alert('强制返航完成')
}

// 机库状态的按钮功能
const openDoor = () => {
  alert('舱门打开')
}

const closeDoor = () => {
  alert('舱门关闭')
}

const resetPusher = () => {
  alert('推杆复位')
}

const clampPusher = () => {
  alert('推杆夹紧')
}

const turnOnAC = () => {
  alert('空调打开')
}

const turnOffAC = () => {
  alert('空调关闭')
}

const openHandle = () => {
  alert('手柄打开')
}

const closeHandle = () => {
  alert('手柄关闭')
}

const startCharging = () => {
  alert('充电开机')
}

const stopCharging = () => {
  alert('充电关机')
}

const charge = () => {
  alert('正在充电')
}

const standby = () => {
  alert('待机')
}

</script>

<style scoped>
.control-panel {
    padding: 10px;
    /* 增加padding */
    background-color: #222;
    color: #fff;
    border-radius: 10px;
    width: 600px;
    height: 98vh;
    /* 放大宽度 */
    transform: scale(0.9);
    /* 放大整体布局 */
    transform-origin: top left;
    /* 使放大效果从左上角开始 */
}

.tabs {
    display: flex;
    justify-content: space-between;
    margin-bottom: 30px;
    /* 增加间距 */
}

.tabs button {
    padding: 15px;
    /* 增大按钮的padding */
    border: none;
    background-color: #444;
    color: #fff;
    cursor: pointer;
    flex: 1;
    text-align: center;
    margin: 0 7.5px;
    /* 调整按钮之间的间距 */
    font-size: 18px;
    /* 增大按钮文字大小 */
}

.tabs button.active {
    background-color: #6200ea;
}

.drone-status-panel,
.hangar-status-panel {
    background-color: #1b1f23;
    color: #ffffff;
    padding: 30px;
    /* 增大padding */
    border-radius: 8px;
    font-size: 18px;
    /* 增大面板内的文字大小 */
}

.status-container {
    display: flex;
    justify-content: space-between;
    /* align-items: flex-start; */
    margin-bottom: 30px;
    /* 增加底部间距 */
}

.status-value {
    color: #00bfff;
}

.status-text {
    display: flex;
    flex-direction: column;
    padding-left: 45px;
    /* 增大padding */
    margin-top: 15px;
    /* 调整上边距 */
    font-size: 18px;
    /* 增大状态文字的字体大小 */
}

.coordinates-container {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    font-size: 18px;
    /* 增大坐标的字体大小 */
}

.status-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 15px;
}

.status-value,
.coordinates {
    color: #00bfff;
    font-size: 18px;
    /* 增大状态值和坐标的字体 */
}

.operations {
    display: flex;
    align-items: center;
    /* 让子元素居中对齐 */
    justify-content: space-between;
    flex-wrap: wrap;
    margin-top: 30px;
    /* 增大顶部间距 */
}

.operation-row {
    width: 100%;
    /* 每行占满 */
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    margin-bottom: 30px;
    /* 增大底部间距 */
    align-items: center;
}

.operations button,
.operation-row button {
    background-color: #007bff;
    color: #ffffff;
    border: none;
    padding: 12px 18px;
    /* 增大按钮的padding */
    border-radius: 5px;
    cursor: pointer;
    font-size: 18px;
    /* 增大按钮文字大小 */
}

.operations button:hover,
.operation-row button:hover {
    background-color: #0056b3;
}

.grid-container {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
    /* 增加网格项的间距 */
    margin-bottom: 30px;
}

.grid-item {
    background-color: #2a2d33;
    padding: 15px;
    border-radius: 5px;
    font-size: 18px;
    text-align: center;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.grid-item span:first-child {
    font-size: 18px;
    color: #ffffff;
}

.grid-item span:last-child {
    font-size: 18px;
    color: #00bfff;
}

.status-active {
    color: #00ff00;
}

.status-link {
    color: #1e90ff;
    text-decoration: underline;
    cursor: pointer;
}

.connected {
    color: #00ff00;
}

.closed {
    color: #ff4500;
}

.active {
    color: #32cd32;
}

.not-charging {
    color: #ff4500;
}
</style>
