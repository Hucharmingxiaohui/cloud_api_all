import { createRouter, createWebHistory, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import { ERouterName } from '/@/types/index'
// import CreatePlan from '/@/components/task/CreatePlan.vue'
import WaylinePanel from '/@/pages/page-web/projects/wayline.vue'
import DockPanel from '/@/pages/page-web/projects/dock.vue'
import LiveAgora from '/@/components/livestream-agora.vue'
import LiveOthers from '/@/components/livestream-others.vue'
import LiveResults from '/@/components/livestream-results.vue'
import CreateWayline from '/@/components/wayline/createWayline.vue'
import { ref } from 'vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/' + ERouterName.PROJECT
  },
  // 首页
  {
    path: '/' + ERouterName.PROJECT,
    name: ERouterName.PROJECT,
    component: () => import('/@/pages/page-web/index.vue')
  },
  // members, devices
  {
    path: '/' + ERouterName.HOME,
    name: ERouterName.HOME,
    component: () => import('/@/pages/page-web/home.vue'),
    children: [
      {
        path: '/' + ERouterName.MEMBERS,
        name: ERouterName.MEMBERS,
        component: () => import('/@/pages/page-web/projects/members.vue')
      },
      // {
      //   path: '/' + ERouterName.FIRMWARES,
      //   name: ERouterName.FIRMWARES,
      //   component: () => import('../pages/page-web/projects/Firmwares.vue')
      // }
    ]
  },
  // workspace
  {
    path: '/' + ERouterName.WORKSPACE,
    name: ERouterName.WORKSPACE,
    component: () => import('/@/pages/page-web/projects/workspace.vue'),
    redirect: '/' + ERouterName.NEW_WAYLINE,
    children: [
      {
        path: '/' + ERouterName.TSA,
        component: () => import('/@/pages/page-web/projects/tsa.vue')
      },
      {
        path: '/' + ERouterName.DEVICES,
        name: ERouterName.DEVICES,
        component: () => import('/@/pages/page-web/projects/devices.vue')
      },
      {
        path: '/' + ERouterName.FIRMWARES,
        name: ERouterName.FIRMWARES,
        component: () => import('../pages/page-web/projects/Firmwares.vue')
      },
      {
        path: '/' + ERouterName.MEDIA,
        name: ERouterName.MEDIA,
        component: () => import('/@/components/MediaPanel.vue')
      },
      {
        path: '/remoteDebug',
        name: 'remoteDebug',
        component: () => import('/@/components/task/waylineVideo.vue')
      },
      {
        path: '/member',
        name: 'member',
        component: () => import('/@/pages/page-web/projects/members1.vue')
      },
      {
        path: '/station',
        name: 'station',
        component: () => import('/@/pages/page-web/projects/station.vue')
      },
      {
        path: '/' + ERouterName.LIVESTREAM,
        name: ERouterName.LIVESTREAM,
        component: () => import('/@/pages/page-web/projects/livestream.vue'),
        children: [
          {
            path: ERouterName.LIVING,
            name: ERouterName.LIVING,
            components: {
              LiveAgora,
              LiveOthers,
              LiveResults
            }
          }
        ]
      },
      {
        path: '/test1',
        name: 'test1',
        component: () => import('/@/components/task/PlanInfo.vue')
      },
      {
        path: '/' + ERouterName.LAYER,
        name: ERouterName.LAYER,
        component: () => import('/@/pages/page-web/projects/layer.vue')
      },
      {
        path: '/test',
        name: 'test',
        component: () => import('/@/components/g-map/mapPanel.vue')
      },
      // {
      //   path: '/' + ERouterName.MEDIA,
      //   name: ERouterName.MEDIA,
      //   component: () => import('/@/pages/page-web/projects/media.vue')
      // },
      {
        path: '/' + ERouterName.NEW_WAYLINE,
        name: ERouterName.NEW_WAYLINE,
        component: () => import('/@/components/WaylinePanel.vue'),
      },
      {
        path: '/waylinePoints',
        name: 'waylinePoints',
        component: () => import('/@/components/wayline/wayline_points.vue'),
      },
      {
        path: '/' + ERouterName.WAYLINE,
        name: ERouterName.WAYLINE,
        component: () => import('/@/pages/page-web/projects/wayline.vue'),
      },
      {
        path: '/' + ERouterName.Camera_Management,
        name: ERouterName.Camera_Management,
        component: () => import('/@/pages/page-web/projects/camera.vue'),
      },
      {
        path: '/' + ERouterName.HOSTCONNECT,
        name: ERouterName.HOSTCONNECT,
        component: () => import('/@/components/hostConnect.vue'),
      },
      {
        path: '/' + ERouterName.LOGS,
        name: ERouterName.LOGS,
        component: () => import('/@/components/devices/device-log/logManage.vue'),
      },
      {
        path: '/' + ERouterName.Points_Management,
        name: ERouterName.Points_Management,
        component: () => import('/@/components/points/points_manage.vue'),
      },
      {
        path: '/' + ERouterName.Setup_Management,
        name: ERouterName.Setup_Management,
        component: () => import('/@/pages/page-web/projects/setup.vue'),
        redirect: '/platformInfo',
        children: [
          {
            path: '/platformInfo',
            name: 'platformInfo',
            component: () => import('/@/components/setup/platformInfo.vue'),
          },
          {
            path: '/apiConfig',
            name: 'apiConfig',
            component: () => import('/@/components/setup/apiConfig.vue'),
          },
          {
            path: '/taskConfig',
            name: 'taskConfig',
            component: () => import('/@/components/setup/taskSetup.vue'),
          }
        ]
      },
      {
        path: '/wayline/createWayline',
        name: 'createWayline',
        component: () => import('/@/components/wayline/createWayline.vue'),
      },
      {
        path: '/wayline/showWayline',
        name: 'showWayline',
        component: () => import('/@/components/wayline/pathTracking.vue'),
      },
      {
        path: '/task/TaskPanel',
        name: 'TaskPanel',
        component: () => import('/@/components/task/TaskPanel.vue'),
      },
      {
        path: '/task/taskImages',
        name: 'taskImages',
        component: () => import('/@/components/task/TaskImages.vue'),
      },
      {
        path: '/task/TaskInfo',
        name: 'TaskInfo',
        component: () => import('/@/components/task/TaskInfo.vue'),
      },
      {
        path: '/task/TaskHistory',
        name: 'TaskHistory',
        component: () => import('/@/components/task/TaskHistory.vue'),
      },
      {
        path: '/task/show-info',
        name: 'show-info',
        component: () => import('/@/components/task/showState.vue'),
      },
      {
        path: '/' + ERouterName.FLY_PLAN,
        name: ERouterName.FLY_PLAN,
        component: () => import('/@/components/task/fly_plan.vue'),
      },
      // {
      //   path: '/' + ERouterName.TASK,
      //   name: ERouterName.TASK,
      //   component: () => import('/@/components/task/TaskPanel.vue'),
      //   children: [
      //     {
      //       path: ERouterName.CREATE_PLAN,
      //       name: ERouterName.CREATE_PLAN,
      //       component: CreatePlan,
      //       children: [
      //         {
      //           path: ERouterName.SELECT_PLAN,
      //           name: ERouterName.SELECT_PLAN,
      //           components: {
      //             WaylinePanel,
      //             DockPanel
      //           }
      //         }
      //       ],
      //     },
      //     {
      //       path: 'taskResult',
      //       name: 'taskResult',
      //       component: () => import('/@/components/task/task_result.vue'),
      //     },
      //   ]
      // },
      {
        path: '/' + ERouterName.TASK,
        name: ERouterName.TASK,
        component: () => import('/@/components/task/TaskPanel.vue'),
      },
      {
        path: '/task/taskResult',
        name: 'taskResult',
        component: () => import('/@/components/task/task_result.vue'),
      },
      {
        path: '/wayline/Model',
        name: 'model',
        component: () => import('/@/components/cesium/3DMapPanel.vue'),
      },
      {
        path: '/modelManage',
        name: 'modelManage',
        component: () => import('/@/components/cesium/modelPanel.vue'),
      },
      {
        path: '/fanMgt',
        name: 'fanMgt',
        component: () => import('/@/components/fanMgt/index.vue'),
      },
      {
        path: '/task/createPlan',
        name: 'createPlan',
        component: () => import('/@/components/task/CreatePlan.vue'),
        children: [
          {

            path: ERouterName.SELECT_PLAN,
            name: ERouterName.SELECT_PLAN,
            components: {
              WaylinePanel,
              DockPanel
            }
          },
        ]
      },
      // {
      //   path: '/task/taskResult',
      //   name: 'taskResult',
      //   component: () => import('/@/components/task/task_result.vue'),
      // },
      // {
      //   path: ERouterName.TASK_RESULT,
      //   name: ERouterName.TASK_RESULT,
      //   component: () => import('/@/components/task/task_result.vue')
      // },
      {
        path: '/' + ERouterName.FLIGHT_AREA,
        name: ERouterName.FLIGHT_AREA,
        component: () => import('/@/pages/page-web/projects/flight-area.vue')
      },
      {
        path: '/' + ERouterName.Organization,
        name: ERouterName.Organization,
        component: () => import('/@/pages/page-web/projects/organize.vue')
      },
    ]
  },
  // pilot
  {
    path: '/' + ERouterName.PILOT,
    name: ERouterName.PILOT,
    component: () => import('/@/pages/page-pilot/pilot-index.vue'),
  },
  {
    path: '/' + ERouterName.PILOT_HOME,
    component: () => import('/@/pages/page-pilot/pilot-home.vue')
  },
  {
    path: '/' + ERouterName.PILOT_MEDIA,
    component: () => import('/@/pages/page-pilot/pilot-media.vue')
  },
  {
    path: '/' + ERouterName.PILOT_LIVESHARE,
    component: () => import('/@/pages/page-pilot/pilot-liveshare.vue')
  },
  {
    path: '/' + ERouterName.PILOT_BIND,
    component: () => import('/@/pages/page-pilot/pilot-bind.vue')
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
// 添加全局路由守卫
router.beforeEach((to, from, next) => {
  isLoading.value = true
  next()
})

router.afterEach(() => {
  setTimeout(() => {
    isLoading.value = false
  }, 200)
})

export { isLoading }
export default router
