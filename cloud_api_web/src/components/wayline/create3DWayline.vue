<template>
    <div class="container">
        <div class="left-sidebar">
            <!-- 左侧内容 -->
            <div style="height: 50px; line-height: 50px; border-bottom: 1px solid #4f4f4f; font-weight: 450;">
                <a-row>
                    <a-col :span="1"></a-col>
                    <a-col :span="22">航点标注</a-col>
                    <a-col :span="1"></a-col>
                </a-row>
            </div>
            <div class="scrollbar" :style="{ height: scorllHeight + 'px'}">
                <LayersTree
                :layer-data="mapLayers"
                class="project-layer-content"
                @update:layerData="updateLayerData"
                @check="checkLayer"
                @select="selectLayer"
                @delete="deleteLayer"
                v-model:selectedKeys="selectedKeys"
                v-model:checkedKeys="checkedKeys"
                />
                <!-- <div style="margin-top: 30px;">
                    <span style="width: 100px;margin-right: 10px;">航线名称:</span>
                    <el-input v-model="waylineName" style="width: 200px;height: 20px; font-size: 16px;"></el-input>
                </div> -->
                <div class="flex-row flex-justify-around flex-align-center mt20">
                    <a-button type="primary" @click="commitWayline">提交</a-button>
                </div>
            </div>
            <a-drawer
                title="航线编辑"
                placement="right"
                :closable="true"
                v-model:visible="visible"
                :mask="false"
                wrapClassName="drawer-element-wrapper1"
                @close="closeDrawer"
                width="350"
                :style="{ height: scorllHeight + 'px'}"
            >
                <div>
                    <div class="name element-item">
                        <span class="title">名称:</span>
                        <a-input
                        v-model:value="layerState.layerName"
                        style="width:120px"
                        placeholder="element name"
                        @change="changeLayer"
                        />
                    </div>
                    <el-divider class="divider-color" ></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType === ''" style="margin-top: 10px">
                        <div style="display: flex; justify-content:left;align-items: center; color: aliceblue;">
                          <span class="title">无人机机型:</span>
                          <span class="title" style="margin-left: 10px" v-if="lineState.droneEnumValue === 77">M3E/M3T/M3M</span>
                          <span class="title" style="margin-left: 10px" v-else>M30/M30T</span>
                        </div>
                        <!-- <el-select v-model="lineState.droneEnumValue" placeholder="Select" size="large" class="custom-select" @change="updateLine">
                            <el-option  v-for="item in droneOption" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select> -->
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType === ''"></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType === ''" style="margin-top: 10px">
                        <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">安全起飞高度:</span>
                        </div>
                        <el-slider v-model="lineState.globalHeight" show-input   :min="2" :max="1500" @change="updateLine" class="custom-el-slider"/>
                    </div>
                    <el-divider class="divider-color"  v-if="layerState.currentType === ''"></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType === ''" style="margin-top: 10px">
                        <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">速度(m/s):</span>
                        </div>
                        <el-slider v-model="lineState.autoFlightSpeed"  :min="1" :max="15" @change="updateLine" class="custom-el-slider"/>
                    </div>
                    <el-divider class="divider-color"  v-if="layerState.currentType === ''"></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType === ''" style="margin-top: 10px">
                        <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">相对起飞点高度:</span>
                        </div>
                        <el-slider v-model="lineState.takeOffSecurityHeight" show-input   :min="2" :max="1500" @change="updateLine" class="custom-el-slider"/>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType === ''" ></el-divider>
                    <div v-if="layerState.currentType === ''" style="margin-top: 10px">
                      <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">飞行器偏航角模式(°):</span>
                        </div>
                        <el-select  v-model="lineState.globalWaypointHeadingMode" placeholder="Select" size="large" class="custom-select" @change="updateLine">
                            <el-option v-for="item in pointHeadingModeOption.slice(0, 3)" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType === ''" ></el-divider>
                    <div v-if="layerState.currentType === ''" style="margin-top: 10px">
                      <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">航点间云台俯仰角控制模式:</span>
                        </div>
                        <el-select  v-model="lineState.gimbalPitchMode" size="large" class="custom-select"  @change="updateLine">
                            <el-option v-for="item in gimbalPitchModeOption" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select>
                    </div>
                    <el-divider class="divider-color"  v-if="layerState.currentType === ''"></el-divider>
                    <div v-if="layerState.currentType === ''" style="margin-top: 10px">
                        <span class="title">航点类型:</span>
                        <el-select v-model="lineState.globalWaypointTurnMode"  size="large" class="custom-select" @change="updateLine">
                            <el-option  v-for="item in mypointsoptions" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType === ''" ></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType === ''" style="margin-top: 10px">
                        <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">完成动作:</span>
                        </div>
                        <el-select v-model="lineState.finishAction" placeholder="Select" size="large" class="custom-select" @change="updateLine">
                            <el-option  v-for="item in actionOption" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select>
                    </div>
                    <el-divider class="divider-color"  v-if="layerState.currentType === ''"></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType === ''" style="margin-top: 10px">
                        <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">起始飞行速度:</span>
                        </div>
                        <el-slider v-model="lineState.globalTransitionalSpeed" show-input   :min="1" :max="15" @change="updateLine" class="custom-el-slider"/>
                    </div>
                    <el-divider class="divider-color"  v-if="layerState.currentType === ''"></el-divider>
                    <div
                        class="longitude element-item" v-if="layerState.currentType != ''"
                    >
                        <span class="title">经度:</span>
                        <span style="margin-left: 10px">{{ layerState.longitude}}</span>
                        <!-- <a-input
                        v-model:value="layerState.longitude"
                        style="width:120px"
                        placeholder="longitude"
                        @change="changeLayer"
                        /> -->
                    </div>
                    <div
                        class="latitude element-item"
                        v-if="layerState.currentType != ''"
                    >
                        <span class="title">纬度:</span>
                        <span style="margin-left: 10px">{{ layerState.latitude }}</span>
                        <!-- <a-input
                        v-model:value="layerState.latitude"
                        style="width:120px"
                        placeholder="latitude"
                        @change="changeLayer"
                        /> -->
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType != ''"></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType != ''" style="margin-top: 10px">
                        <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">速度(m/s):</span>
                          <el-checkbox  v-model="boolState.checked1" @change="changeLayer">跟随航线</el-checkbox>
                        </div>
                        <el-slider :disabled="boolState.checked1" v-model="pointState.speed" :min="1" :max="15" @change="changeLayer" class="custom-el-slider"/>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType != ''"></el-divider>
                    <div v-if="layerState.currentType != ''" style="margin-top: 30px;">
                        <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">相对起飞高度(m):</span>
                          <el-checkbox  v-model="boolState.checked2" @change="changeLayer">跟随航线</el-checkbox>
                        </div>
                        <el-input-number  class="custom-input-number" :disabled="boolState.checked2" v-model="pointState.flightHeight" :min="1" :max="200" @change="changeLayer"/>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType != ''"></el-divider>
                    <div v-if="layerState.currentType != ''" style="margin-top: 10px">
                      <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">飞行器偏航角模式(°):</span>
                          <el-checkbox  v-model= 'boolState.checked3' @change="changeLayer">跟随航线</el-checkbox>
                        </div>
                        <el-select :disabled="boolState.checked3" v-model="pointState.waypointHeadingMode" placeholder="Select" size="large" class="custom-select" @change="changeLayer">
                            <el-option v-for="item in pointHeadingModeOption" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select>
                    </div>
                    <div class="latitude element-item" v-if="layerState.currentType !== '' && pointState.waypointHeadingMode === 'smoothTransition'" style="margin-top: 10px">
                        <span class="title">飞行器旋转角度(°):</span>
                        <el-slider v-model="pointState.waypointHeadingAngle" :min="-180" :max="180" @change="changeLayer" class="custom-el-slider"/>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType !== '' && pointState.waypointHeadingMode === 'smoothTransition'" ></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType !== '' && pointState.waypointHeadingMode === 'smoothTransition'" style="margin-top: 10px">
                        <span class="title">飞行器旋转方向:</span>
                        <el-select v-model="pointState.waypointHeadingPathMode" placeholder="Select" size="large" class="custom-select" @change="changeLayer">
                            <el-option v-for="item in pointHeadingPathModeOption" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType != ''"></el-divider>
                    <div v-if="layerState.currentType != ''" style="margin-top: 10px">
                      <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">航点类型:</span>
                          <el-checkbox  v-model= 'boolState.checked4' @change="changeLayer">跟随航线</el-checkbox>
                        </div>
                        <el-select :disabled = "boolState.checked4" v-model="pointState.mypointsType" placeholder="Select" size="large" class="custom-select" @change="changeLayer">
                            <el-option v-for="item in mypointsoptions" :key="item.value" :label="item.label" :value="item.value"
                            />
                        </el-select>
                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType != ''"></el-divider>
                    <div class="latitude element-item" v-if="layerState.currentType != ''" style="margin-top: 10px">
                        <span class="title">云台仰望角(°):</span>
                        <div v-if="lineState.droneEnumValue === 67">
                          <el-slider  :disabled = "lineState.gimbalPitchMode =='manual' " v-model="pointState.PTZ_angle" :min="-120" :max="45" @change="changeLayer" class="custom-el-slider"/>
                        </div>
                        <div v-else>
                          <el-slider  :disabled = "lineState.gimbalPitchMode =='manual' " v-model="pointState.PTZ_angle" :min="-90" :max="35" @change="changeLayer" class="custom-el-slider"/>
                        </div>

                    </div>
                    <el-divider class="divider-color" v-if="layerState.currentType != ''"></el-divider>
                    <div v-if="layerState.currentType != ''" style="margin-top: 10px">
                      <div style="display: flex; justify-content: space-between;color: aliceblue;">
                          <span class="title">航点动作:</span>
                      </div>
                      <div style="width: 300px;height: auto;border: 1px solid white;" v-for="(item, index) in actions" :key="index" >
                          <div style="display: flex; justify-content: space-between;color: aliceblue;padding: 10px 10px 0 10px;">
                            <span class="title"><el-icon style="margin-right: 10px"><Operation /></el-icon>{{item.label }}</span>
                            <span class="title">负载动作<el-icon style="margin-left: 10px;" @click="deleteAction(item)"><DeleteFilled /></el-icon></span>
                          </div>
                          <div v-if="item.value === 'orientedShoot'" style="width: 100%">
                              <el-divider class="divider-color" ></el-divider>
                              <div style="display: flex; align-items: center;">
                                <span class="action-item">云台仰望角0.0°</span>
                                <span class="action-item" v-if="lineState.droneEnumValue === 67">云台偏航角0.0°</span>
                                <span style="width: 20px; padding-left: 10px">
                                   <el-icon v-if="!isExpand" @click="adjustActionParam()"><ArrowDownBold /></el-icon>
                                   <el-icon v-else @click="adjustActionParam()"><ArrowUpBold /></el-icon>
                                </span>
                              </div>
                              <div style="display: flex; align-items: center;">
                                <span class="action-item">飞机偏航角0.0°</span>
                                <span class="action-item">变焦2.0X</span>
                              </div>
                              <div v-if="isExpand">
                                <div class="setup-box" style="margin-top: 20px">
                                  <span>云台俯仰角(°):</span>
                                  <span class="number-show">{{orientedShootParam.gimbalPitchRotateAngle}}</span>
                                </div>
                                <div  v-if="lineState.droneEnumValue === 67" style="padding:0 10px; margin-bottom: 15px;" >
                                  <el-slider v-model="orientedShootParam.gimbalPitchRotateAngle" :show-tooltip="false" :min="-120" :max="45" @change="changeLayer"/>
                                </div>
                                <div  v-else style="padding:0 10px; margin-bottom: 15px;" >
                                  <el-slider v-model="orientedShootParam.gimbalPitchRotateAngle" :show-tooltip="false" :min="-90" :max="35" @change="changeLayer"/>
                                </div>

                                <div class="setup-box" style="margin-top: 20px" v-if="lineState.droneEnumValue === 67">
                                  <span>云台偏航角(°):</span>
                                  <span class="number-show">{{orientedShootParam.gimbalYawRotateAngle}}</span>
                                </div>
                                <div style="padding:0 10px; margin-bottom: 15px;" v-if="lineState.droneEnumValue === 67">
                                  <el-slider v-model="orientedShootParam.gimbalYawRotateAngle" :show-tooltip="false" :min="-180" :max="180" @change="changeLayer"/>
                                </div>

                                <div class="setup-box" style="margin-top: 20px">
                                  <span>飞行器偏航角(°):</span>
                                  <span class="number-show">{{orientedShootParam.aircraftHeading}}</span>
                                </div>
                                <div style="padding:0 10px; margin-bottom: 15px;">
                                  <el-slider v-model="orientedShootParam.aircraftHeading" :show-tooltip="false" :min="-180" :max="180" @change="changeLayer"/>
                                </div>

                                <div class="setup-box" style="margin-top: 20px">
                                  <span>相机变焦(X):</span>
                                  <span class="number-show">{{orientedShootParam.focalLength}}</span>
                                </div>
                                <div style="padding:0 10px; margin-bottom: 15px;" v-if="lineState.droneEnumValue === 67">
                                  <el-slider v-model="orientedShootParam.focalLength" :show-tooltip="false" :min="2" :max="200" @change="changeLayer"/>
                                </div>
                                <div style="padding:0 10px; margin-bottom: 15px;" v-else>
                                  <el-slider v-model="orientedShootParam.focalLength" :show-tooltip="false" :min="1" :max="56" @change="changeLayer"/>
                                </div>
                              </div>

                          </div>
                          <div v-if="item.value === 'gimbalRotate'" style="width: 100%">
                            <div class="setup-box" style="margin-top: 20px">
                              <span>云台俯仰角(°):</span>
                              <span class="number-show">{{gimbalOption.gimbalRotateAngle}}</span>
                            </div>
                            <div  v-if="lineState.droneEnumValue === 67" style="padding:0 10px; margin-bottom: 15px;" >
                              <el-slider v-model="gimbalOption.gimbalRotateAngle" :show-tooltip="false" :min="-120" :max="45" @change="changeLayer"/>
                            </div>
                            <div  v-else style="padding:0 10px; margin-bottom: 15px;" >
                              <el-slider v-model="gimbalOption.gimbalRotateAngle" :show-tooltip="false" :min="-90" :max="35" @change="changeLayer"/>
                            </div>
                          </div>
                          <div v-if="item.value === 'zoom'" style="width: 100%">
                            <div class="setup-box" style="margin-top: 20px">
                              <span>相机变焦(X):</span>
                              <span class="number-show">{{gimbalOption.focalLength}}</span>
                            </div>
                            <div  v-if="lineState.droneEnumValue === 67" style="padding:0 10px; margin-bottom: 15px;" >
                              <el-slider v-model="gimbalOption.focalLength" :show-tooltip="false" :min="2" :max="200" @change="changeLayer"/>
                            </div>
                            <div  v-else style="padding:0 10px; margin-bottom: 15px;" >
                              <el-slider v-model="gimbalOption.focalLength" :show-tooltip="false" :min="1" :max="56" @change="changeLayer"/>
                            </div>
                          </div>
                      </div>
                      <div style="display: flex; justify-content: center">
                         <el-button @click="selectAction" class="custom-button1"><el-icon style="margin-right: 20px;"><Plus /></el-icon>添加动作</el-button>
                      </div>
                      <el-divider class="divider-color" ></el-divider>
                    </div>
                </div>
                <!-- <div class="flex-row flex-justify-around flex-align-center mt20" v-if="layerState.currentType != ''">
                <a-button type="primary" @click="deleteElement">删除</a-button>
                </div> -->
            </a-drawer>
            <div v-show="visible1" style="position: fixed; width:220px; right: 352px;height: fit-content;padding-bottom: 20px; top: 60px; color: aliceblue; z-index: 1000;background-color: rgba(5,32,75,0.9)" >
              <div style="height: 55px; line-height: 55px; border-bottom: 1px solid white; font-weight: 450; margin-bottom: 10px;padding: 0 10px ; display: flex; justify-content: space-between;">
                <span>动作列表</span>
                <span @click="closeDiv"><el-icon><CloseBold /></el-icon></span>
              </div>
              <span style="display: flex;justify-content: center; padding: 10px 0; font-weight: bold;">负载动作</span>
              <div style="display: flex;flex-direction: column;align-items: center;width: 100%; max-width: 220px; margin: auto;">
                  <el-button v-for="(item, index) in pointActionOption.slice(0, 9)" :key="index" class="custom-button" @click="selectedAction(item)">{{ item.label }} </el-button>
              </div>
              <span style="display: flex;justify-content: center; padding: 10px 0; font-weight: bold;">飞机动作</span>
              <div style="display: flex;flex-direction: column;align-items: center;width: 100%; max-width: 220px; margin: auto;">
                  <el-button v-for="(item, index) in pointActionOption.slice(9, 11)" :key="index"  class="custom-button" @click="selectedAction(item)">{{ item.label }} </el-button>
              </div>

            </div>
            <!-- <a-drawer
                title="负载动作"
                :closable="true"
                v-model:visible="visible1"
                :mask="false"
                width="220"
                style="position: fixed; right: 352px;height: 500px;  top: 60px; color: aliceblue;"
                class="custom-drawer"
                @close="handleClose"
              >
                  <div  >
                    <el-button v-for="(item, index) in pointActionOption" :key="index" class="custom-button" @click="selectedAction(item)">{{ item.label }} </el-button>
                  </div>
              </a-drawer> -->
        </div>
        <div class="main-content">
            <!-- 右侧内容 -->
            <showMap ref="myMap" />
        </div>
    </div>
</template>
<script setup>
import { watch, ref, onMounted, nextTick, reactive, computed, onBeforeUnmount } from 'vue'
import showMap from '/@/components/g-map/SetWaylinePoints.vue'
import LayersTree from '/@/components/LayersTree1.vue'
import { GeoType } from '/@/types/mapLayer'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import { MapDoodleColor, MapElementEnum } from '/@/constants/map'
import { generatePoint } from '/@/utils/genjson'
import { useGMapCover } from '/@/hooks/use-g-map-cover'
import { ElMessage } from 'element-plus'
import { commitWaylineFile, commitWaylineFile1 } from '/@/api/wayline'
import { ELocalStorageKey, ERouterName, ELiveStatusValue, EStatusValue } from '/@/types'
import { useMyStore } from '/@/store'
import { useRouter } from 'vue-router'
import { getApp, getRoot } from '/@/root'
import { useGMapManage } from '/@/hooks/use-g-map'
import Layer from '/@/pages/page-web/projects/layer.vue'
import pin19be6b from '/@/assets/icons/pin-19be6b.svg'
const router = useRouter()
// 响应参数定义
const scorllHeight = ref() // 左侧容器滚动高度
const store = useMyStore()
let useGMapCoverHook = useGMapCover(store)
const useGMapManageHook = useGMapManage()
const mapLayers = ref(store.state.Points) // 航点数据
const checkedKeys = ref([])
const selectedKeys = ref([])
const selectedKey = ref('')
const selectedLayer = ref(null)
const visible = ref(false)
const visible1 = ref(false)
const myMap = ref()
const root = getRoot()
const liveState = ref(EStatusValue)
const layerState = reactive({
  layerName: '',
  layerId: '',
  longitude: 0,
  latitude: 0,
  currentType: '', // “LineString”,"Polygon","Point"
  color: '#212121'
})

// 航线参数定义
const lineState = reactive({
  finishAction: 'goHome', // 完成动作
  takeOffSecurityHeight: 20, // 相对起飞高度
  globalTransitionalSpeed: 4, // 飞向首航点的速度
  droneEnumValue: 77, // 无人机机型
  droneSubEnumValue: 0, // 无人机机型绑定的参数\负载,后续更换支持机型要改
  payloadEnumValue: 66,
  payloadSubEnumValue: 0,
  payloadPositionIndex: 0,
  autoFlightSpeed: 5, // 全局速度
  globalHeight: 90, // 全局高度
  globalWaypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature', // 航点类型
  globalWaypointHeadingMode: 'followWayline', // 偏航角模式
  gimbalPitchMode: 'usePointSetting' // 云台俯仰角控制模式
})

// 云台俯仰角控制模式选项
const gimbalPitchModeOption = [
  {
    value: 'usePointSetting',
    label: '依照每个航点设置'
  },
  {
    value: 'manual',
    label: '手动控制'
  }
]

// 无人机偏航旋转方向
const pointHeadingPathModeOption = [
  {
    value: 'clockwise',
    label: '顺时针'
  },
  {
    value: 'counterClockwise',
    label: '逆时针'
  },
  {
    value: 'followBadArc',
    label: '自动'
  }
]
// 完成动作选项
const actionOption = [
  {
    value: 'noAction',
    label: '退出航线模式'
  },
  {
    value: 'goHome',
    label: '自动返航'
  },
  {
    value: 'autoLand',
    label: '原地降落'
  },
  {
    value: 'gotoFirstWaypoint',
    label: '飞向航线起始点悬停'
  }
]
// 无人机机型
const droneOption = [
  {
    value: 67,
    label: 'M30/M30T'
  },
  {
    value: 77,
    label: 'M3E/M3T/M3M'
  }
]
// 航点跟随航线
const boolState = reactive({
  checked1: true,
  checked2: true,
  checked3: true,
  checked4: true
})
const pointHeadingModeOption = [
  {
    value: 'followWayline',
    label: '沿航线方向',
  },
  {
    value: 'manually',
    label: '手动控制',
  },
  {
    value: 'fixed',
    label: '锁定当前偏航角',
  },
  {
    value: 'smoothTransition',
    label: '自定义',
  }
]

// 航点参数定义
const pointState = reactive({
  speed: 5, // 航点速度设置
  flightHeight: 100, // 相对起飞高度
  flightModel: '1', // 偏航角模式
  rotationDirction: '1', // 飞行器旋转方向
  mypointsType: 'toPointAndStopWithDiscontinuityCurvature', // 航点类型
  turn_in: '', // 入弯距离
  PTZ_angle: 1, // 云台仰望角
  waypointHeadingAngle: 0, // 无人机偏航角度
  waypointHeadingMode: 'followWayline', // 无人机偏航模式
  waypointHeadingPathMode: 'followBadArc' // 无人机偏航角度旋转方向
})
// 航点类型选项
const mypointsoptions = [
  {
    value: 'coordinateTurn',
    label: '协调转弯，不过点，提前转弯',
  },
  {
    value: 'toPointAndStopWithDiscontinuityCurvature',
    label: '直线飞行，飞行器到点停',
  },
  {
    value: 'toPointAndPassWithContinuityCurvature',
    label: '平滑过点，提前转弯',
  },
  {
    value: 'toPointAndStopWithContinuityCurvature',
    label: '曲线飞行，飞行器到点停',
  },
  {
    value: 'toPointAndPassWithContinuityCurvature',
    label: '曲线飞行，飞行器过点不停',
  }
]

//= ==============================================================航线动作定义=======================================
// 航点动作序列
const pointActionOption = [
  {
    value: 'takePhoto',
    label: '拍照'
  },
  {
    value: 'panoShot',
    label: '全景拍照'
  },
  {
    value: 'orientedShoot',
    label: '定向拍照'
  },
  {
    value: 'startRecord',
    label: '开始录像'
  },
  {
    value: 'stopRecord',
    label: '结束录像'
  },
  {
    value: 'gimbalRotate',
    label: '云台俯仰角'
  },
  {
    value: 'zoom',
    label: '相机变焦'
  },
  {
    value: 'multipleDistanceTakePhoto',
    label: '等距离拍照'
  },
  {
    value: 'multipleTimingTakePhoto',
    label: '等时间拍照'
  },
  {
    value: 'hover',
    label: '悬停等待'
  },
  {
    value: 'rotateYaw',
    label: '飞行器偏航'
  }
]
// 定向拍照
const isExpand = ref(false)
const orientedShootParam = reactive({
  gimbalPitchRotateAngle: 0, // 云台俯仰角
  gimbalYawRotateAngle: 0, // 云台偏航角
  aircraftHeading: 0, // 飞行器偏航角
  focalLength: 2 // 相机变焦
})

// 云台俯仰角
const gimbalOption = reactive({
  gimbalRotateAngle: 0,
  focalLength: 2
})

// 方法定义
onMounted(() => {
  const app = getApp()
  useGMapManageHook.globalPropertiesConfig(app)
  nextTick()
  // mapLayers.value =
  init()
  scorllHeight.value = window.innerHeight - 60

  // console.log('当前数据', store.state.Points)
})
onBeforeUnmount(() => {
  const Layers = store.state.Points
  Layers[0].elements = []
  store.commit('SET_POINTS', Layers)
  localStorage.removeItem('wayline')
})
function closeDiv () {
  visible1.value = false
}

const payloadMap = {
  77: {
    0: 66,
    1: 67,
    2: 68,
  },
  67: {
    0: 52,
    1: 53,
  },
  91: {
    0: 80,
    1: 81,
  },
}
function commitWayline () {
  const points = store.state.Points
  const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)
  const myPlacemark = []
  const data = {
    missionConfig: {},
    folder: {}
  }

  const point = points[0]
  const elements = point.elements
  const missionConfig = point.missionConfig
  const folder = point.folder

  elements.forEach((item, index) => {
    const coordStr = item.resource.content.geometry.coordinates
    const pointObj = {
      coordinates: `${coordStr[0]},${coordStr[1]}`
    }
    item.Placemark = {
      ...item.Placemark,
      point: pointObj,
      waypointHeadingParam: {
        waypointHeadingAngle: item.Placemark.waypointHeadingAngle
      },
      waypointTurnParam: {
        waypointTurnMode: item.Placemark.mypointsType
      }
    }
    if (item.Placemark.actionGroup && item.Placemark.actionGroup.actionList) {
      item.Placemark.actionGroup = {
        actionGroupStartIndex: index,
        actionGroupEndIndex: index,
        actionTrigger: {
          actionTriggerType: 'reachPoint',
          actionTriggerParam: 0
        },
        actionList: item.Placemark.actionGroup.actionList
      }
    } else {
      delete item.Placemark.actionGroup
    }
    myPlacemark.push(item.Placemark)
  })

  missionConfig.fileName = point.name
  // missionConfig.droneInfo.droneSubEnumValue = missionConfig.droneInfo.droneEnumValue === 77 ? 0 : 1;
  // missionConfig.payloadInfo.payloadEnumValue = missionConfig.droneInfo.droneEnumValue === 77 ? 66 : 53;
  const droneEnumValue = missionConfig.droneInfo.droneEnumValue
  const droneSubEnumValue = missionConfig.droneInfo.droneSubEnumValue

  // 根据映射对象获取 payloadEnumValue
  if (payloadMap[droneEnumValue] && payloadMap[droneEnumValue][droneSubEnumValue] !== undefined) {
    missionConfig.payloadInfo.payloadEnumValue = payloadMap[droneEnumValue][droneSubEnumValue]
  }
  folder.placeMarks = myPlacemark
  folder.waylineCoordinateSysParam = {}
  folder.payloadParam = {
    payloadPositionIndex: 0,
    imageFormat: missionConfig.droneInfo.droneEnumValue === 77 ? 'wide,zoom' : 'wide,ir,zoom' // 只考虑两种机型，需要扩展
  }
  folder.globalWaypointHeadingParam = {
    waypointHeadingMode: lineState.globalWaypointHeadingMode
  }
  data.folder = folder
  data.missionConfig = missionConfig

  console.log('提交的数据', data)

  commitWaylineFile1(workspaceId, data).then(res => {
    if (res.code !== 0) {
      return
    }
    router.push({ path: '/wayline' })
    ElMessage({
      message: '航线保存成功!',
      type: 'success',
      plain: true,
    })
  })
}

// =================================================================================================================================================================================
// 处理拖动更新时间
// 处理更新事件
const updateLayerData = (newData) => {
  // layerData.value = newData;
  // console.log('我i正在被拖动', newData)
  localStorage.setItem('wayline', JSON.stringify(newData[0]))
  mapLayers.value = newData
  myMap.value.drawLinebyDrag(newData) // 绘制航线drawLinebyDrag
}
// 显示动作选项界面
function selectAction () {
  visible1.value = true
}
// 选中动作响应事件
const actions = ref([]) // 选中的事件列表
function selectedAction (item) {
  visible1.value = false
  // 判断当前航点动作中是否已有
  if (actions.value.length === 0) {
    actions.value.push(item)
  } else if (!actions.value.some(existingItem => existingItem.value === item.value)) {
    actions.value.push(item)
  }

  // actions.value.push(item)
  changeLayer()
}

function adjustActionParam () {
  isExpand.value = !isExpand.value
}

// 删除动作响应事件
function deleteAction (val) {
  actions.value = actions.value.filter(item => item.value !== val.value)
  changeLayer()
}
// 初始化,在仓库中添加航线参数
function init () {
  // 判断是否能是页面刷新，刷新时，从浏览器内存中获取数据
  const waylineData = localStorage.getItem('wayline')
  const Layers = JSON.parse(JSON.stringify(store.state.Points))// 深拷贝，避免直接引用

  if (waylineData) {
    console.log('我被执行2')
    const parsedData = JSON.parse(waylineData)
    // 确保 parsedData 的结构与 Layers[0] 相同
    if (parsedData && typeof parsedData === 'object') {
      // 更新 Layers 的第一个元素
      Layers[0] = { ...Layers[0], ...parsedData }// 合并数据
      setTimeout(() => {
        useGMapCoverHook = useGMapCover()
        initMapCover(Layers[0])
        console.log('添加浏览器内存')
        const coordinates = Layers[0].elements.map(point => {
          const { coordinates } = point.resource.content.geometry
          return wgs84togcj02(coordinates[0], coordinates[1]) // 只提取经纬度
        })
        // showWayline(coordinates)
        myMap.value.drawWayline(coordinates)
        // const AMap = root.$aMap
        // if (!AMap) {
        //   console.error('AMap is not defined')
        // }
      }, 1000)
      mapLayers.value = Layers
      lineState.droneEnumValue = Layers[0].missionConfig.droneInfo.droneEnumValue
      store.commit('SET_POINTS', Layers)
    }
  } else {
    // 从航线编辑界面过来
    console.log('我被执行')
    setTimeout(() => {
      const Layers = store.state.Points
      mapLayers.value = Layers
      lineState.droneEnumValue = Layers[0].missionConfig.droneInfo.droneEnumValue
      myMap.value.drawWayline() // 绘制航线
      localStorage.setItem('wayline', JSON.stringify(Layers[0]))
    }, 500)
  }
  // 从航线编辑界面过来
  // setTimeout(() => {
  //   // console.log('sd', Layers)
  //   // const Layers = store.state.Points
  //   // mapLayers.value = Layers
  //   // myMap.value.drawWayline() // 绘制航线
  //   // localStorage.setItem('wayline', JSON.stringify(Layers[0]))
  // }, 500)
  // console.log('sdef', liveState)
}
// 页面刷新，航线重新绘制事件---------------------------------------------------------------------------------------------------------------------------------
function initMapCover (item) {
  if (item.elements) {
    // console.log('测试数据', item.elements)
    setMapCoverByElement(item.elements)
  }
}
function setMapCoverByElement (elements) {
  elements.forEach(element => {
    const name = element.name
    const color = element.resource?.content.properties.color
    const type = element.resource?.type // 这里不再强制转换为数字
    updateMapElement(element, name, color)
  })
}

function updateMapElement (element, name, color) {
  const geoType = element.resource?.content.geometry.type
  const id = element.id
  const type = element.resource?.type // 这里不再强制转换为数字
  if (MapElementEnum.PIN === type) {
    // const coordinates = element.resource?.content.geometry.coordinates
    const coordStr = element.resource?.content.geometry.coordinates
    // console.log('前', ele.resource.content.geometry.coordinates)
    const coordinates = wgs84togcj02(coordStr[0], coordStr[1])
    useGMapCoverHook.updatePinElement(id, name, coordinates, color)
    useGMapCoverHook.updatePinElementTitle(id, name)
    console.log('saf', name)
  } else if (MapElementEnum.LINE === type && geoType === 'LineString') {
    const coordinates = element.resource?.content.geometry.coordinates
    useGMapCoverHook.updatePolylineElement(id, name, coordinates, color)
  } else if (MapElementEnum.POLY === type && geoType === 'Polygon') {
    const coordinates = element.resource?.content.geometry.coordinates
    useGMapCoverHook.updatePolygonElement(id, name, coordinates, color)
  }
}
// ------------------------------------------------------------------------航点编辑函数-------------------------------------------------------------------------------------
//  航线数据更新
function updateLine () {
  const Layers = store.state.Points
  Layers[0].missionConfig.droneInfo.droneEnumValue = lineState.droneEnumValue
  Layers[0].missionConfig.finishAction = lineState.finishAction
  Layers[0].missionConfig.takeOffSecurityHeight = lineState.takeOffSecurityHeight
  Layers[0].missionConfig.globalTransitionalSpeed = lineState.globalTransitionalSpeed
  Layers[0].folder.autoFlightSpeed = lineState.autoFlightSpeed
  Layers[0].folder.globalHeight = lineState.globalHeight
  Layers[0].folder.globalWaypointHeadingParam.waypointHeadingMode = lineState.globalWaypointHeadingMode
  Layers[0].folder.globalWaypointTurnMode = lineState.globalWaypointTurnMode
  Layers[0].folder.gimbalPitchMode = lineState.gimbalPitchMode
  store.commit('SET_POINTS', Layers)
}
// 选中航线/航点,显示信息
function checkLayer (keys) {
}
function selectLayer (keys, e) {
  // console.log('selectLayer', e.node.eventKey, e.selected)
  if (e.selected) {
    selectedKey.value = e.node.eventKey
    selectedLayer.value = getCurrentLayer(selectedKey.value)
    actions.value = [] // 更新选中动作
    // 重置 定向拍照的参数
    orientedShootParam.gimbalPitchRotateAngle = 0
    orientedShootParam.gimbalYawRotateAngle = 0
    orientedShootParam.aircraftHeading = 0
    orientedShootParam.focalLength = 2
    setBaseInfo()
  }
  visible.value = e.selected // 更新仓库，抽屉显示，标注工具位置变化
  store.commit('SET_DRAW_VISIBLE_INFO', visible.value)
  // store.dispatch('updateElement', { type: 'is_select', id: e.node.eventKey, bool: e.selected })
}
watch(
  () => store.state.Points,
  newData => {
    mapLayers.value = newData
  },
  {
    deep: true
  }
)
function getCurrentLayer (id) {
  const Layers = store.state.Points
  const key = id.replaceAll('resource__', '')
  // console.log('selectedKey.value', selectedKey.value)
  let layer = null
  const findCan = function (V) {
    V.forEach(item => {
      if (item.id === key) {
        layer = item
      }
      if (item.elements) {
        findCan(item.elements)
      }
    })
  }
  findCan(Layers)
  return layer
}
function setBaseInfo () {
  const layer = selectedLayer.value
  if (layer) {
    const geoType = layer.resource?.content.geometry.type
    layerState.layerName = layer.name
    layerState.layerId = layer.id
    if (geoType) { // 显示站点信息
      layerState.currentType = geoType
      layerState.color = layer.resource?.content.properties.color
      const coordinate = gcj02towgs84(layer.resource?.content.geometry.coordinates[0], layer.resource?.content.geometry.coordinates[1])
      layerState.longitude = coordinate[0]
      layerState.latitude = coordinate[1]

      if (store.state.Points[0].gimbalPitchMode !== 'manual') {
        pointState.PTZ_angle = layer.Placemark.gimbalPitchAngle
      }

      // 判断4类参数是否跟随航线
      if (layer.Placemark.useGlobalSpeed) {
        boolState.checked1 = true
      } else {
        boolState.checked1 = false
        pointState.speed = layer.Placemark.waypointSpeed // 速度
      }
      if (layer.Placemark.useGlobalHeight) {
        boolState.checked2 = true
      } else {
        boolState.checked2 = false
        pointState.flightHeight = layer.Placemark.height // 高度
      }
      if (layer.Placemark.useGlobalHeadingParam) { // 偏航角模式
        boolState.checked3 = true
      } else {
        boolState.checked3 = false
        pointState.waypointHeadingMode = layer.Placemark.waypointHeadingParam.waypointHeadingMode
        if (pointState.waypointHeadingMode === 'smoothTransition') {
          pointState.waypointHeadingAngle = layer.Placemark.waypointHeadingParam.waypointHeadingAngle
          pointState.waypointHeadingPathMode = layer.Placemark.waypointHeadingParam.waypointHeadingPathMode
        }
      }
      if (layer.Placemark.useGlobalTurnParam) { // 航点类型
        boolState.checked4 = true
      } else {
        boolState.checked4 = false
        pointState.mypointsType = layer.Placemark.waypointTurnParam.waypointTurnMode
      }

      // 动作序列
      if (layer.Placemark.actionGroup && layer.Placemark.actionGroup.actionList && layer.Placemark.actionGroup.actionList.length > 0) {
        // 创建一个Set来存储actionGroup中的唯一actionActuatorFunc
        // const actionSet = new Set(layer.Placemark.actionGroup.actionList.map(item => item.actionActuatorFunc))
        const actionSet = new Map(layer.Placemark.actionGroup.actionList.map(item => {
          return [item.actionActuatorFunc, item.actionActuatorFuncParam]
        }))
        // 遍历pointActionOption并将匹配的项添加到action.value数组中
        // console.log('sdff', actionSet)
        pointActionOption.forEach(option => {
          if (actionSet.has(option.value)) {
            const actionParam = actionSet.get(option.value) // 获取对应的actionActuatorFuncParam
            // 判断是否为定向拍照
            if (option.value === 'orientedShoot') {
              // 使用获取到的actionParam来设置orientedShootParam
              orientedShootParam.gimbalPitchRotateAngle = actionParam.orientedShoot.gimbalPitchRotateAngle
              orientedShootParam.gimbalYawRotateAngle = actionParam.orientedShoot.gimbalYawRotateAngle
              orientedShootParam.aircraftHeading = actionParam.orientedShoot.aircraftHeading
              orientedShootParam.focalLength = actionParam.orientedShoot.focalLength / 24
            }
            if (option.value === 'gimbalRotate') { // 判断是否云台俯仰角
              gimbalOption.gimbalRotateAngle = actionParam.gimbalRotate.gimbalPitchRotateAngle
            }
            if (option.value === 'zoom') { // 判断是否相机变焦
              gimbalOption.focalLength = actionParam.zoom.focalLength
            }
            // orientedShootParam
            actions.value.push(option)
          }
        })
      }
    } else { // 显示航线信息
      layerState.currentType = ''
      lineState.finishAction = layer.missionConfig.finishAction
      lineState.takeOffSecurityHeight = layer.missionConfig.takeOffSecurityHeight
      lineState.globalTransitionalSpeed = layer.missionConfig.globalTransitionalSpeed
      lineState.autoFlightSpeed = layer.folder.autoFlightSpeed
      lineState.globalHeight = layer.folder.globalHeight
      lineState.globalWaypointTurnMode = layer.folder.globalWaypointTurnMode
      lineState.gimbalPitchMode = layer.folder.gimbalPitchMode
    }
  }
}

// 地图元素修改事件
//     修改仓库值
function changeLayer (val) {
  if (layerState.currentType === GeoType.Point) {
    // const position = {
    //   height: 0,
    //   latitude: Number(layerState.latitude || 0),
    //   longitude: Number(layerState.longitude || 0)
    // }
    //  查找到仓库中对应的元素,修改更新值
    updateLayer(selectedKey.value)
  } else {
    // 更新航线数据
    updateWayline()
  }
  // 每次修改，都保存一份数据在浏览器内存，放置刷新
  const Layers = store.state.Points
  localStorage.setItem('wayline', JSON.stringify(Layers[0]))
}
// 修改航线信息
function updateWayline () {
  const Layers = store.state.Points
  Layers[0].name = layerState.layerName
  store.commit('SET_POINTS', Layers)
}
// 修改航点信息
function updateLayer (id) {
  const Layers = store.state.Points
  const key = id.replaceAll('resource__', '')
  // 寻找站点元素,修改
  if (Layers[0].id === key) {
    Layers[0].name = layerState.layerName
  } else {
    const findCan = function (V) {
      V.forEach(item => {
        if (item.id === key) {
          if (item.name !== layerState.layerName) { // 修改航点名字
            item.name = layerState.layerName
            useGMapCoverHook.updatePinElementTitle(item.id, item.name)
          }

          // 速度 高度 偏航角 航点类型
          if (!boolState.checked1) {
            delete item.Placemark.useGlobalSpeed
            item.Placemark.waypointSpeed = pointState.speed
          } else {
            delete item.Placemark.waypointSpeed
            item.Placemark.useGlobalSpeed = 1
          }

          if (!boolState.checked2) {
            delete item.Placemark.useGlobalHeight
            item.Placemark.height = pointState.flightHeight
          } else {
            delete item.Placemark.height
            item.Placemark.useGlobalHeight = 1
          }

          if (!boolState.checked3) {
            delete item.Placemark.useGlobalHeadingParam
            if (item.Placemark.waypointHeadingParam) {
              item.Placemark.waypointHeadingParam.waypointHeadingMode = pointState.waypointHeadingMode
              if (pointState.waypointHeadingMode === 'smoothTransition') {
                item.Placemark.waypointHeadingParam.waypointHeadingAngle = pointState.waypointHeadingAngle
                item.Placemark.waypointHeadingParam.waypointHeadingPathMode = pointState.waypointHeadingPathMode
              } else {
                delete item.Placemark.waypointHeadingParam.waypointHeadingAngle
              }
            } else {
              item.Placemark.waypointHeadingParam = {}
              item.Placemark.waypointHeadingParam.waypointHeadingMode = pointState.waypointHeadingMode
              if (pointState.waypointHeadingMode === 'smoothTransition') {
                item.Placemark.waypointHeadingParam.waypointHeadingAngle = pointState.waypointHeadingAngle
                item.Placemark.waypointHeadingParam.waypointHeadingPathMode = pointState.waypointHeadingPathMode
              } else {
                delete item.Placemark.waypointHeadingParam.waypointHeadingAngle
              }
            }
          } else {
            delete item.Placemark.waypointHeadingParam
            item.Placemark.useGlobalHeadingParam = 1
          }

          if (!boolState.checked4) {
            delete item.Placemark.useGlobalTurnParam
            if (item.Placemark.waypointTurnParam) {
              item.Placemark.waypointTurnParam.waypointTurnMode = pointState.mypointsType
            } else {
              item.Placemark.waypointTurnParam = {}
              item.Placemark.waypointTurnParam.waypointTurnMode = pointState.mypointsType
            }
          } else {
            delete item.Placemark.waypointTurnParam
            item.Placemark.useGlobalTurnParam = 1
          }

          // 云台仰望角
          if (store.state.Points[0].folder.gimbalPitchMode === 'manual') {
            delete item.Placemark.gimbalPitchAngle
          } else {
            item.Placemark.gimbalPitchAngle = pointState.PTZ_angle
          }

          // 动作序列
          if (actions.value.length > 0) { // 添加无人机动作
            const localActionGroup = []
            actions.value.forEach(item => {
              if (item.value === 'orientedShoot') { // 定向拍照
                const node = {
                  actionActuatorFunc: item.value,
                  actionActuatorFuncParam: {
                    [item.value]: {
                      gimbalPitchRotateAngle: orientedShootParam.gimbalPitchRotateAngle,
                      gimbalYawRotateAngle: orientedShootParam.gimbalYawRotateAngle,
                      aircraftHeading: orientedShootParam.aircraftHeading,
                      focalLength: orientedShootParam.focalLength * 24
                    }
                  }
                }
                localActionGroup.push(node)
              } else if (item.value === 'gimbalRotate') { // 云台俯仰角
                const node = {
                  actionActuatorFunc: item.value,
                  actionActuatorFuncParam: {
                    [item.value]: {
                      gimbalPitchRotateEnable: 1,
                      gimbalPitchRotateAngle: gimbalOption.gimbalRotateAngle,
                    }
                  }
                }
                localActionGroup.push(node)
              } else if (item.value === 'zoom') { // 相机变焦
                const node = {
                  actionActuatorFunc: item.value,
                  actionActuatorFuncParam: {
                    [item.value]: {
                      focalLength: gimbalOption.focalLength,
                    }
                  }
                }
                localActionGroup.push(node)
              } else {
                const node = {
                  actionActuatorFunc: item.value,
                  actionActuatorFuncParam: {
                    [item.value]: {}
                  }
                }
                localActionGroup.push(node)
              }
            })
            item.Placemark.actionGroup.actionList = localActionGroup
          }
        }
        if (item.elements) {
          findCan(item.elements)
        }
      })
    }
    findCan(Layers)
  }
  store.commit('SET_POINTS', Layers)
  // 此处应些更新地图标记
}
// 删除航点
function deleteLayer (event) {
  const { id, parentId } = event
  deleteElement(id)
  // 删除层的逻辑
  // console.log('Deleted Layer ID:', id)
  // console.log('测试', selectedKey.value.replaceAll('resource__', ''))
  // if (parentId) {
  //   console.log('Parent Layer ID:', parentId)
  // }
}
function deleteElement (key) {
  const Layers = store.state.Points
  // const key = selectedKey.value.replaceAll('resource__', '')
  // 寻找航点元素, 删除
  let updatedLayers = null
  if (Layers[0].id === key) {
    Layers[0].name = layerState.layerName
  } else {
    updatedLayers = Layers.map(layer => {
      return {
        ...layer,
        elements: layer.elements.filter(element => element.id !== key)
      }
    })
  }
  mapLayers.value = updatedLayers
  store.commit('SET_POINTS', mapLayers.value)
  // // 删除地图标记
  const elementid = key
  useGMapCoverHook.removeCoverFromMap(elementid)
  // 重新绘制折线
  myMap.value.removeAllWayline(key)
  closeDrawer()
}
// 关闭弹窗
function closeDrawer () {
  visible.value = false
  store.commit('SET_DRAW_VISIBLE_INFO', visible.value)
  selectedKeys.value = []
}
</script>

<style lang="scss" >
.container {
  display: flex;
  height: 100vh; /* 设置整个容器占据整个视窗高度 */
  width: 100%
}

.left-sidebar {
  width: 300px; /* 左侧宽度为300px */
  background-color: #f0f0f0;
  padding: 20px;
  background-color:#05204B;
  color: aliceblue;
}
.divider-color{
  border-color: rgba(252, 252, 252, 0.208);
  margin: 20px auto;
}
.main-content {
  flex: 1; /* 右侧内容占据剩余所有空间 */
  background-color: #e0e0e08a;
  padding: 0;
}
.scrollbar {
    overflow: auto;
}
.custom-input-number {
  margin-top: 10px;
  .el-input__wrapper {
    background-color: #05204B;
    color: white !important;
  }
  .el-input.is-disabled .el-input__wrapper {
    background-color: #05204B !important; /* Background color for disabled state */
    color: rgb(6, 6, 6) !important; /* Font color for disabled state */
  }
  .el-input-number__increase{
    background: #05204B;
    color: white;
  }
  .el-input-number__decrease{
    background: #05204B;
    color: white;
  }
}
.custom-el-slider{
  .el-slider__runway{
    background-color: #565859;
  }
  .el-slider__bar {
    background-color: white;
  }
  .el-input-number__increase{
    background: #05204B;
    color: white !important;
  }
  .el-input-number__decrease{
    background: #05204B;
    color: white !important;
  }
  .el-input__wrapper {
    background-color: #05204B;
    color: white !important;
  }
  .el-input.is-disabled .el-input__wrapper {
    background-color: #05204B !important; /* Background color for disabled state */
    color: rgb(188, 182, 182) !important; /* Font color for disabled state */
  }
}
.custom-select{
  margin-top: 10px;
  width: 300px;
  .el-select--large .el-select__wrapper{
    min-height: 30px;
  }
  .el-select__wrapper{
    background-color:#05204B;
  }
  .custom-select .el-select-dropdown {
    background-color: transparent !important; /* 透明背景 */
    border: 1px solid blue !important; /* 边框颜色 */
  }

  .custom-select .el-select-dropdown__item {
    background-color: #05204B !important; /* 下拉项背景色 */
    color: #fff !important; /* 字体颜色 */
  }

  .el-select.is-disabled .el-select__dropdown{
    background:#05204B;
    color: white;
  }
  .el-select__placeholder{
    color: white;
  }
}

.setup-box{
  padding: 5px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #ffffff;
  font-style: normal;
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
.action-item{
  width: 120px;
  font-size: 14px;
  padding-left: 10px;
  padding-bottom: 5px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
// 动作抽屉样式
// .custom-drawer .ant-drawer-content {
//   background-color: rgba(5,32,75,0.9); /* 红色背景，透明度0.4 */
// }
// .custom-drawer .ant-drawer-header {
//   background-color: rgba(5,32,75,0.5); /* 红色背景，透明度0.4 */
// }
// .custom-drawer .ant-drawer-title {
//   color: white; /* 标题文字颜色为白色 */
// }
// .custom-drawer .ant-drawer-body {
//   padding: 10px; /* 标题文字颜色为白色 */
// }
// .custom-drawer .ant-drawer-close{
//   color: white;
// }

// 按钮样式
.custom-button{
  background-color: rgba(40, 63, 101, 0.843);
  border: 1px solid white;
  color: white;
  width: 200px;
  height: 40px;
  margin: 0 10px 10px 10px;
  &:hover{
    background-color: darkblue;
  }
}
.custom-button1{
  background-color: rgba(5,32,75);
  border: 1px solid white;
  color: white;
  width: 200px;
  height: 30px;
  margin-top: 20px;
  &:hover{
    background-color: darkblue;
  }
}
.drawer-element-wrapper1 {
    // background-color: blue;
    position: fixed; /* 确保位置固定 */
    top: 60px;
    .ant-drawer-content {
      background-color: #05204B;
      color: $text-white-basic;
      .ant-drawer-header {
        background-color: #05204B;
        .ant-drawer-title {
          color: $text-white-basic;
        }
        .ant-drawer-close {
          color: $text-white-basic;
        }
      }
      .ant-input {
        background-color: #05204B;
        border-color: $dark-border;
        color: $text-white-basic;
      }
    }
    .color-content {
      display: flex;
      align-items: center;
      margin-top: 8px;
      .color-item {
        cursor: pointer;
        width: 18px;
        height: 18px;
        line-height: 18px;
        display: flex;
        align-items: center;
        margin-left: 5px;
      }
    }
    .title {
      display: inline-flex;
      // display: flex; /* 使用 Flexbox */
      align-items: center; /* 垂直居中对齐 */
      width: auto;
    }
    .element-item {
      margin-bottom: 10px;
    }
}
</style>
