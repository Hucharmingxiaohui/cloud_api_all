import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { ERouterName } from '/@/types/index'
import { ref } from 'vue'

// 扩展路由元信息类型（移除index/parentIndex，保留核心配置）
declare module 'vue-router' {
  interface RouteMeta {
    /** 是否缓存组件 */
    cache?: boolean
    /** 是否显示在菜单中 */
    showInMenu?: boolean
    /** 是否在新窗口打开 */
    newWindow?: boolean
    /** 菜单标签（优先使用，无则使用name） */
    label?: string
    /** 菜单位置（left/right，子路由继承父路由） */
    position?: 'left' | 'right'
  }
}

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/' + ERouterName.PROJECT,
    meta: { showInMenu: false }
  },
  // 首页
  {
    path: '/' + ERouterName.PROJECT,
    name: ERouterName.PROJECT,
    component: () => import('/@/pages/page-web/index.vue'),
    meta: { showInMenu: false, cache: false }
  },
  // members, devices
  {
    path: '/' + ERouterName.HOME,
    name: ERouterName.HOME,
    component: () => import('/@/pages/page-web/home.vue'),
    meta: { showInMenu: false, cache: false },
    children: [
      {
        path: '/' + ERouterName.MEMBERS,
        name: ERouterName.MEMBERS,
        component: () => import('/@/pages/page-web/projects/members.vue'),
        meta: { showInMenu: false, cache: true }
      }
    ]
  },
  // workspace（主菜单所在路由）
  {
    path: '/' + ERouterName.WORKSPACE,
    name: ERouterName.WORKSPACE,
    component: () => import('/@/pages/page-web/projects/workspace.vue'),
    redirect: '/' + ERouterName.NEW_WAYLINE,
    meta: { showInMenu: false, cache: false },
    children: [
      // 左侧菜单 - 变量管理（父菜单，含子路由）
      // {
      //   path: '/variableMgt',
      //   name: 'variableMgt',
      //   meta: {
      //     showInMenu: true,
      //     label: '变量管理',
      //     position: 'left',
      //     cache: false
      //   },
      //   children: [
      //     {
      //       path: '/variableMgt/fanMgt',
      //       name: 'fanMgt',
      //       component: () => import('/@/components/variableMgt/fanMgt/index.vue'),
      //       meta: {
      //         showInMenu: true,
      //         label: '风机管理',
      //         cache: true
      //       }
      //     },
      //     {
      //       path: '/variableMgt/interestPointMgt',
      //       name: 'interestPointMgt',
      //       component: () => import('/@/components/variableMgt/interestPointMgt/index.vue'),
      //       meta: {
      //         showInMenu: true,
      //         label: '兴趣点管理',
      //         cache: true
      //       }
      //     },
      //     {
      //       path: '/variableMgt/solarMgt',
      //       name: 'solarMgt',
      //       meta: {
      //         showInMenu: true,
      //         label: '光伏管理',
      //         position: 'left',
      //         cache: false
      //       },
      //       children: [
      //         {
      //           path: '/variableMgt/solarMgt/solarPanelMgt',
      //           name: 'solarPanelMgt',
      //           component: () => import('/@/components/variableMgt/solarMgt/solarPanelMgt/index.vue'),
      //           meta: {
      //             showInMenu: true,
      //             label: '光伏巡视区域管理',
      //             cache: true
      //           }
      //         },
      //         {
      //           path: '/variableMgt/solarMgt/solarDeviceMgt',
      //           name: 'solarDeviceMgt',
      //           component: () => import('/@/components/variableMgt/solarMgt/solarDeviceMgt/index.vue'),
      //           meta: {
      //             showInMenu: true,
      //             label: '光伏板设备管理',
      //             cache: true
      //           }
      //         },
      //         {
      //           path: '/variableMgt/solarMgt/solarComponentMgt',
      //           name: 'solarComponentMgt',
      //           component: () => import('/@/components/variableMgt/solarMgt/solarComponentMgt/index.vue'),
      //           meta: {
      //             showInMenu: true,
      //             label: '光伏组件定位管理',
      //             cache: true
      //           }
      //         },
      //         // {
      //         //   path: '/variableMgt/solarMgt/solarPanelLabel',
      //         //   name: 'solarPanelLabel',
      //         //   component: () => import('/@/components/variableMgt/solarMgt/solarPanelLabel/index.vue'),
      //         //   meta: {
      //         //     showInMenu: true,
      //         //     label: '光伏板区域标注',
      //         //     cache: true
      //         //   }
      //         // },
      //       ]
      //     },
      //   ]
      // },
      // {
      //   path: '/' + ERouterName.Points_Management,
      //   name: ERouterName.Points_Management,
      //   component: () => import('/@/components/points/points_manage.vue'),
      //   meta: {
      //     showInMenu: true,
      //     label: '点位管理',
      //     position: 'left',
      //     cache: true
      //   }
      // },
      {
        path: '/' + ERouterName.Indoor_Points,
        name: ERouterName.Indoor_Points,
        component: () => import('/@/components/indoorPoints/index.vue'),
        meta: {
          showInMenu: true,
          label: '室内点位',
          position: 'left',
          cache: true
        }
      },

      {
        path: '/' + ERouterName.DEVICES,
        name: ERouterName.DEVICES,
        component: () => import('/@/pages/page-web/projects/devices.vue'),
        meta: {
          showInMenu: true,
          label: '设备管理',
          position: 'left',
          cache: true
        }
      },
      {
        path: '/remoteDebug',
        name: 'remoteDebug',
        component: () => import(/* webpackPrefetch: true */ '/@/components/task/waylineVideo.vue'),
        meta: {
          showInMenu: false,
          label: '控制台',
          position: 'left',
          cache: true
        }
      },
      {
        path: '/' + ERouterName.LIVESTREAM,
        name: ERouterName.LIVESTREAM,
        component: () => import(/* webpackPrefetch: true */ '/@/pages/page-web/projects/livestream.vue'),
        meta: {
          showInMenu: true,
          label: '视频直播',
          position: 'left',
          cache: false
        },
        children: [
          {
            path: ERouterName.LIVING,
            name: ERouterName.LIVING,
            components: {
              default: () => import('/@/components/livestream-agora.vue'),
              LiveOthers: () => import('/@/components/livestream-others.vue'),
              LiveResults: () => import('/@/components/livestream-results.vue')
            },
            meta: { showInMenu: false, cache: false }
          }
        ]
      },
      {
        path: '/' + ERouterName.DRONE_LIVE,
        name: ERouterName.DRONE_LIVE,
        component: () => import('/@/components/live-video/index.vue'),
        meta: {
          showInMenu: true,
          label: '无人机直播',
          position: 'left',
          cache: false
        }
      },
      // {
      //   path: '/' + ERouterName.NEW_WAYLINE,
      //   name: ERouterName.NEW_WAYLINE,
      //   component: () => import('/@/components/WaylinePanel.vue'),
      //   meta: {
      //     showInMenu: true,
      //     label: '航线管理',
      //     position: 'right',
      //     cache: true
      //   }
      // },
      // 右侧菜单 - 任务管理（父菜单，含子路由）
      {
        path: '/taskManage', // 父菜单路径
        name: 'taskManage',
        meta: {
          showInMenu: true,
          label: '任务管理',
          position: 'right', // 右侧菜单（子路由继承）
          cache: false
        },
        children: [
          // 子菜单 - 普通点位计划
          // {
          //   path: '/taskManage/' + ERouterName.FLY_WAYLINE_PLAN,
          //   name: ERouterName.FLY_WAYLINE_PLAN,
          //   component: () => import('/@/components/task/flyWaylinePlan.vue'),
          //   meta: {
          //     showInMenu: true,
          //     label: '点位航线计划',
          //     cache: true
          //   }
          // },
          {
            path: '/taskManage/common-fly-wayline-plan',
            name: 'commonFlyPlan',
            component: () => import('/@/components/task/commonFlyWaylinePlan.vue'),
            meta: {
              showInMenu: true,
              label: '室内无人机计划',
              cache: true
            }
          },
          // {
          //   path: '/taskManage/solarPlanMgt',
          //   name: 'solarPlanMgt',
          //   component: () => import('../components/task/solarflyPlan/solarPlanMgt.vue'),
          //   meta: {
          //     showInMenu: true,
          //     label: '光伏板计划',
          //     cache: true
          //   }
          // },
          // {
          //   path: '/taskManage/createSolarPlan',
          //   name: 'createSolarPlan',
          //   component: () => import('/@/components/task/solarflyPlan/createSolarPlan.vue'),
          //   meta: {
          //     showInMenu: false,
          //     label: '创建光伏板计划',
          //     cache: true
          //   }
          // },
          //
          // // 子菜单 - 风机计划
          // {
          //   path: '/taskManage/' + ERouterName.FLY_FAN_PLAN,
          //   name: ERouterName.FLY_FAN_PLAN,
          //   component: () => import('/@/components/task/flyFanPlan.vue'),
          //   meta: {
          //     showInMenu: true,
          //     label: '风机计划',
          //     cache: true
          //   }
          // },
          // {
          //   path: '/taskManage/interestPointPlan',
          //   name: 'interestPointPlan',
          //   component: () => import('/@/components/task/interestPointPlan.vue'),
          //   meta: {
          //     showInMenu: true,
          //     label: '兴趣点环绕计划',
          //     cache: true
          //   }
          // },

          // 子菜单 - 飞行任务
          {
            path: '/' + ERouterName.TASK,
            name: ERouterName.TASK,
            component: () => import('/@/components/task/TaskPanel.vue'),
            meta: {
              showInMenu: true,
              label: '飞行任务',
              cache: true
            }
          }
        ]
      },
      // 右侧菜单 - 系统管理（父菜单，含子路由）
      {
        path: '/system-manage', // 父菜单路径
        name: 'systemManage',
        meta: {
          showInMenu: true,
          label: '系统管理',
          position: 'right', // 右侧菜单（子路由继承）
          cache: false
        },
        children: [
          // 子菜单 - 日志管理
          {
            path: '/' + ERouterName.LOGS,
            name: ERouterName.LOGS,
            component: () => import('/@/components/devices/device-log/logManage.vue'),
            meta: {
              showInMenu: false,
              label: '日志管理',
              cache: false
            }
          },
          // 子菜单 - 固件管理（新窗口打开）
          {
            path: '/' + ERouterName.FIRMWARES,
            name: ERouterName.FIRMWARES,
            component: () => import('../pages/page-web/projects/Firmwares.vue'),
            meta: {
              showInMenu: true,
              label: '固件管理',
              newWindow: true, // 新窗口打开
              cache: false
            }
          }
        ]
      },
      // 其他路由（隐藏菜单）
      {
        path: '/' + ERouterName.TSA,
        component: () => import('/@/pages/page-web/projects/tsa.vue'),
        meta: { showInMenu: false, cache: false }
      },
      {
        path: '/' + ERouterName.MEDIA,
        name: ERouterName.MEDIA,
        component: () => import('/@/components/MediaPanel.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/member',
        name: 'member',
        component: () => import('/@/pages/page-web/projects/members1.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/station',
        name: 'station',
        component: () => import('/@/pages/page-web/projects/station.vue'),
        meta: { showInMenu: false, cache: false }
      },
      {
        path: '/' + ERouterName.LAYER,
        name: ERouterName.LAYER,
        component: () => import('/@/pages/page-web/projects/layer.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/test',
        name: 'test',
        component: () => import('/@/components/g-map/mapPanel.vue'),
        meta: { showInMenu: false, cache: false }
      },
      {
        path: '/waylinePoints',
        name: 'waylinePoints',
        component: () => import('/@/components/wayline/wayline_points.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/' + ERouterName.WAYLINE,
        name: ERouterName.WAYLINE,
        component: () => import('/@/pages/page-web/projects/wayline.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/' + ERouterName.Camera_Management,
        name: ERouterName.Camera_Management,
        component: () => import('/@/pages/page-web/projects/camera.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/' + ERouterName.HOSTCONNECT,
        name: ERouterName.HOSTCONNECT,
        component: () => import('/@/components/hostConnect.vue'),
        meta: { showInMenu: false, cache: false }
      },
      {
        path: '/' + ERouterName.Setup_Management,
        name: ERouterName.Setup_Management,
        component: () => import('/@/pages/page-web/projects/setup.vue'),
        redirect: '/platformInfo',
        meta: { showInMenu: false, cache: true },
        children: [
          {
            path: '/platformInfo',
            name: 'platformInfo',
            component: () => import('/@/components/setup/platformInfo.vue'),
            meta: { showInMenu: false, cache: true }
          },
          {
            path: '/apiConfig',
            name: 'apiConfig',
            component: () => import('/@/components/setup/apiConfig.vue'),
            meta: { showInMenu: false, cache: true }
          },
          {
            path: '/taskConfig',
            name: 'taskConfig',
            component: () => import('/@/components/setup/taskSetup.vue'),
            meta: { showInMenu: false, cache: true }
          }
        ]
      },
      {
        path: '/wayline/createWayline',
        name: 'createWayline',
        component: () => import('/@/components/wayline/createWayline.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/wayline/showWayline',
        name: 'showWayline',
        component: () => import('/@/components/wayline/pathTracking.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/task/TaskPanel',
        name: 'TaskPanel',
        component: () => import('/@/components/task/TaskPanel.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/task/taskImages',
        name: 'taskImages',
        component: () => import('/@/components/task/TaskImages.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/task/TaskInfo',
        name: 'TaskInfo',
        component: () => import('/@/components/task/TaskInfo.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/task/TaskHistory',
        name: 'TaskHistory',
        component: () => import('/@/components/task/TaskHistory.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/task/show-info',
        name: 'show-info',
        component: () => import('/@/components/task/showState.vue'),
        meta: { showInMenu: false, cache: false }
      },
      {
        path: '/task/taskResult',
        name: 'taskResult',
        component: () => import('/@/components/task/task_result.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/wayline/Model',
        name: 'model',
        component: () => import('/@/components/cesium/3DMapPanel.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/modelManage',
        name: 'modelManage',
        component: () => import('/@/components/cesium/modelPanel.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/task/createPlan',
        name: 'createPlan',
        component: () => import('/@/components/task/CreatePlan.vue'),
        meta: { showInMenu: false, cache: true },
        children: [
          {
            path: ERouterName.SELECT_PLAN,
            name: ERouterName.SELECT_PLAN,
            components: {
              WaylinePanel: () => import('/@/pages/page-web/projects/wayline.vue'),
              DockPanel: () => import('/@/pages/page-web/projects/dock.vue')
            },
            meta: { showInMenu: false, cache: true }
          }
        ]
      },
      {
        path: '/' + ERouterName.FLIGHT_AREA,
        name: ERouterName.FLIGHT_AREA,
        component: () => import('/@/pages/page-web/projects/flight-area.vue'),
        meta: { showInMenu: false, cache: true }
      },
      {
        path: '/' + ERouterName.Organization,
        name: ERouterName.Organization,
        component: () => import('/@/pages/page-web/projects/organize.vue'),
        meta: { showInMenu: false, cache: true }
      }
    ]
  },
  // pilot 相关路由
  {
    path: '/' + ERouterName.PILOT,
    name: ERouterName.PILOT,
    component: () => import('/@/pages/page-pilot/pilot-index.vue'),
    meta: { showInMenu: false, cache: false }
  },
  {
    path: '/' + ERouterName.PILOT_HOME,
    component: () => import('/@/pages/page-pilot/pilot-home.vue'),
    meta: { showInMenu: false, cache: true }
  },
  {
    path: '/' + ERouterName.PILOT_MEDIA,
    component: () => import('/@/pages/page-pilot/pilot-media.vue'),
    meta: { showInMenu: false, cache: true }
  },
  {
    path: '/' + ERouterName.PILOT_LIVESHARE,
    component: () => import('/@/pages/page-pilot/pilot-liveshare.vue'),
    meta: { showInMenu: false, cache: false }
  },
  {
    path: '/' + ERouterName.PILOT_BIND,
    component: () => import('/@/pages/page-pilot/pilot-bind.vue'),
    meta: { showInMenu: false, cache: false }
  }
]

const isLoading = ref(false)
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior () {
    return { top: 0 }
  }
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  isLoading.value = true
  next()
})

router.afterEach(() => {
  isLoading.value = false
})

// 路由级错误处理：捕获懒加载组件失败等导航错误
router.onError((error) => {
  isLoading.value = false
  console.error('Router navigation error:', error)
})

export { routes, isLoading }
export default router
