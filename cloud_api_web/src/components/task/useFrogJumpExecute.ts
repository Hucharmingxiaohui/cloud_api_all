import { h, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DistributeFlyPlan } from '/@/api/wayline'
import { getBindingDevices } from '/@/api/manage'
import { ELocalStorageKey } from '/@/types/enums'
import { getLandingRecommendation, LandingCandidate } from '/@/api/frogJump'
import FrogJumpLandingChooser from './FrogJumpLandingChooser.vue'
import { bindingDevicesToCandidates, normalizeRecommendation } from './frogJumpLanding'

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
        h('div', {
          style: 'margin-top: 12px; padding: 10px 12px; border-radius: 7px; background: #fff7e8; border: 1px solid #f3d19e; color: #7a4d12; font-size: 12px; line-height: 1.6;',
        }, [
          h('span', { style: 'font-weight: 600;' }, '蛙跳模式注意：'),
          h('span', '如机巢位置、备降点位置、RTK信息有变化，或长时间没执行蛙跳，需重启机场和无人机同步最新蛙跳所需报文'),
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
  const landingDockSn = await selectLandingDock(plan)
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

async function selectLandingDock (plan: any) {
  const takeoffDockSn = plan.dock_sn
  const selectedLandingDockSn = ref('')
  let candidates: LandingCandidate[] = []
  let recommendedDockSn = ''
  let warning = ''
  const loading = ElMessage({ message: '正在获取降落机场推荐…', type: 'info', duration: 0 })
  try {
    const fileId = plan.file_id || plan.fileId
    if (!fileId) throw new Error('计划缺少航线文件 ID，无法确定航线终点')
    const res = await getLandingRecommendation(fileId, takeoffDockSn)
    if (res.code !== 0) throw new Error(res.message || '获取降落机场推荐失败')
    if (!res.data || !Array.isArray(res.data.candidates)) throw new Error('推荐响应缺少候选机场列表')
    const recommendation = normalizeRecommendation(res.data, takeoffDockSn)
    if (!recommendation.endpoint) throw new Error(recommendation.message || '无法确定唯一航线终点')
    candidates = recommendation.candidates
    recommendedDockSn = recommendation.recommended_dock_sn
    selectedLandingDockSn.value = recommendedDockSn
    warning = recommendation.message
  } catch (error: any) {
    warning = `推荐不可用：${error.response?.data?.message || error.message || '请求失败'}。请手动选择，距离无法计算。`
    try {
      const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId) || ''
      const res = await getBindingDevices(workspaceId, { page: 1, total: 0, page_size: 1000 }, DOCK_DOMAIN)
      if (res.code !== 0) throw new Error(res.message || '获取机场列表失败')
      candidates = bindingDevicesToCandidates(res.data?.list || [], takeoffDockSn)
    } catch (fallbackError: any) {
      warning += ` 机场列表加载失败：${fallbackError.response?.data?.message || fallbackError.message || '请求失败'}`
    }
  } finally {
    loading.close()
  }
  try {
    await ElMessageBox.confirm(
      () => h(FrogJumpLandingChooser, {
        modelValue: selectedLandingDockSn.value,
        'onUpdate:modelValue': (value: string) => { selectedLandingDockSn.value = value },
        takeoffDockSn,
        candidates,
        recommendedDockSn,
        warning,
      }),
      '选择降落机场',
      {
        confirmButtonText: '确认下发',
        cancelButtonText: '取消',
        width: 'min(720px, calc(100vw - 40px))',
        beforeClose: (action, _instance, done) => {
          if (action === 'confirm' && !candidates.some(item => item.dock_sn === selectedLandingDockSn.value && item.selectable)) {
            ElMessage.warning('请选择可用的降落机场')
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
