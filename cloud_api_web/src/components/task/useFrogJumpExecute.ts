import { h, ref } from 'vue'
import { ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import { DistributeFlyPlan } from '/@/api/wayline'
import { getBindingDevices } from '/@/api/manage'
import { ELocalStorageKey } from '/@/types/enums'

const DOCK_DOMAIN = 3

export async function executePlanWithFrogJumpMode (plan: any, onSuccess: () => void) {
  try {
    await ElMessageBox.confirm('请选择任务执行模式', '下发任务', {
      confirmButtonText: '蛙跳模式',
      cancelButtonText: '普通模式',
      distinguishCancelAndClose: true,
      type: 'warning',
    })
    await executeFrogJumpPlan(plan, onSuccess)
  } catch (action) {
    if (action === 'cancel') {
      await executeNormalPlan(plan, onSuccess)
    }
  }
}

async function executeNormalPlan (plan: any, onSuccess: () => void) {
  const res = await DistributeFlyPlan(plan)
  if (res.code !== 0) {
    ElMessage.error(res.message || '任务下发失败')
    return
  }
  onSuccess()
}

async function executeFrogJumpPlan (plan: any, onSuccess: () => void) {
  const landingDockSn = await selectLandingDock(plan.dock_sn)
  if (!landingDockSn) {
    return
  }
  const res = await DistributeFlyPlan({
    ...plan,
    frog_jump_mode: true,
    landing_dock_sn: landingDockSn,
  })
  if (res.code !== 0) {
    ElMessage.error(res.message || '蛙跳任务下发失败')
    return
  }
  onSuccess()
}

async function selectLandingDock (takeoffDockSn: string) {
  const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId) || ''
  const selectedLandingDockSn = ref('')
  const res = await getBindingDevices(workspaceId, { page: 1, total: 0, page_size: 1000 }, DOCK_DOMAIN)
  if (res.code !== 0) {
    ElMessage.error(res.message || '获取机场列表失败')
    return ''
  }
  const dockList = (res.data?.list || []).filter((item: any) => item.device_sn && item.device_sn !== takeoffDockSn)
  if (dockList.length === 0) {
    ElMessage.warning('没有可选择的降落机场')
    return ''
  }
  selectedLandingDockSn.value = dockList[0].device_sn

  try {
    await ElMessageBox.confirm(
      h('div', { style: 'padding-top: 8px;' }, [
        h('div', { style: 'margin-bottom: 8px;' }, `起飞机场：${takeoffDockSn}`),
        h(ElSelect, {
          modelValue: selectedLandingDockSn.value,
          'onUpdate:modelValue': (value: string) => { selectedLandingDockSn.value = value },
          placeholder: '请选择降落机场',
          style: 'width: 100%;',
        }, () => dockList.map((item: any) => h(ElOption, {
          key: item.device_sn,
          label: item.nickname || item.device_name || item.device_sn,
          value: item.device_sn,
        }))),
      ]),
      '选择降落机场',
      {
        confirmButtonText: '确认下发',
        cancelButtonText: '取消',
        beforeClose: (action, _instance, done) => {
          if (action === 'confirm' && !selectedLandingDockSn.value) {
            ElMessage.warning('请选择降落机场')
            return
          }
          done()
        },
      }
    )
    return selectedLandingDockSn.value
  } catch (e) {
    return ''
  }
}
