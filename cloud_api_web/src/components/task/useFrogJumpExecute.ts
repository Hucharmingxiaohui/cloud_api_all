import { h, ref } from 'vue'
import { ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import { DistributeFlyPlan } from '/@/api/wayline'
import { getBindingDevices } from '/@/api/manage'
import { ELocalStorageKey } from '/@/types/enums'

const DOCK_DOMAIN = 3

export async function executePlanWithFrogJumpMode (plan: any, onSuccess: () => void) {
  try {
    await ElMessageBox.confirm(
      h('div', { style: 'width: 100%; padding: 2px 0 0; box-sizing: border-box;' }, [
        h('div', { style: 'font-size: 14px; color: #606266; margin-bottom: 12px;' }, '请选择本次任务的执行模式'),
        h('div', { style: 'display: flex; gap: 12px; width: 100%; box-sizing: border-box;' }, [
          h('div', {
            style: 'flex: 1 1 0; min-width: 0; padding: 13px 14px; border: 1px solid #d9dee7; border-radius: 7px; background: #f7f8fa; box-sizing: border-box;',
          }, [
            h('div', { style: 'font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 7px;' }, '普通模式'),
            h('div', { style: 'font-size: 12px; color: #8a919d; line-height: 1.5; word-break: break-all;' }, '按当前计划绑定机场直接下发执行'),
          ]),
          h('div', {
            style: 'flex: 1 1 0; min-width: 0; padding: 13px 14px; border: 1px solid #a9c8ea; border-radius: 7px; background: #f1f6fc; box-sizing: border-box;',
          }, [
            h('div', { style: 'font-size: 15px; font-weight: 600; color: #2f6fae; margin-bottom: 7px;' }, '蛙跳模式'),
            h('div', { style: 'font-size: 12px; color: #687789; line-height: 1.5; word-break: break-all;' }, '选择降落机场后执行跨机场任务'),
          ]),
        ]),
      ]),
      '下发任务', {
        confirmButtonText: '蛙跳模式',
        cancelButtonText: '普通模式',
        distinguishCancelAndClose: true,
        width: 'min(460px, calc(100vw - 40px))',
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
  const allDockList = res.data?.list || []
  const takeoffDock = allDockList.find((item: any) => item.device_sn === takeoffDockSn)
  const takeoffDockName = takeoffDock?.nickname || takeoffDock?.device_name || takeoffDockSn
  const dockList = allDockList.filter((item: any) => item.device_sn && item.device_sn !== takeoffDockSn)
  if (dockList.length === 0) {
    ElMessage.warning('没有可选择的降落机场')
    return ''
  }
  selectedLandingDockSn.value = dockList[0].device_sn

  try {
    await ElMessageBox.confirm(
      h('div', { style: 'width: 356px; padding: 4px 0 0; box-sizing: border-box;' }, [
        h('div', {
          style: 'display: flex; align-items: stretch; gap: 12px; width: 100%; box-sizing: border-box;',
        }, [
          h('div', {
            style: 'width: 130px; flex: 0 0 130px; min-width: 0; padding: 12px 14px; border-radius: 7px; background: #f7f8fa; border: 1px solid #d9dee7; box-sizing: border-box;',
          }, [
            h('div', { style: 'font-size: 12px; color: #8a919d; margin-bottom: 6px;' }, '起飞机场'),
            h('div', { style: 'font-size: 15px; font-weight: 600; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;' }, takeoffDockName),
          ]),
          h('div', { style: 'width: 18px; flex: 0 0 18px; display: flex; align-items: center; justify-content: center; color: #8b96a3; font-size: 18px; line-height: 1;' }, '→'),
          h('div', { style: 'width: 184px; flex: 0 0 184px; min-width: 0;' }, [
            h('div', { style: 'font-size: 12px; color: #8a919d; margin: 0 0 6px 1px;' }, '降落机场'),
            h(ElSelect, {
              modelValue: selectedLandingDockSn.value,
              'onUpdate:modelValue': (value: string) => { selectedLandingDockSn.value = value },
              placeholder: '请选择降落机场',
              style: 'width: 184px;',
            }, () => dockList.map((item: any) => h(ElOption, {
              key: item.device_sn,
              label: item.nickname || item.device_name || item.device_sn,
              value: item.device_sn,
            }))),
          ]),
        ]),
      ]),
      '选择降落机场',
      {
        confirmButtonText: '确认下发',
        cancelButtonText: '取消',
        width: '420px',
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
