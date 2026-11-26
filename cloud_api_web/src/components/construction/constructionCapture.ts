export function constructionCaptureActions (lens = 'wide') {
  if (!['wide', 'ir', 'wide,ir'].includes(lens)) throw Error('请选择有效拍照镜头')
  return [
    { actionActuatorFunc: 'takePhoto', actionActuatorFuncParam: { takePhoto: { fileSuffix: '航点', payloadPositionIndex: 0, useGlobalPayloadLensIndex: 0, payloadLensIndex: lens } } }
  ]
}

/** Explicit repair only: keep existing actions and never duplicate photo actions. */
export function addMissingConstructionCapture (elements: any[], lens = 'wide') {
  let added = 0
  for (const point of elements) {
    const placemark = point.Placemark
    const group = placemark.actionGroup || {}
    const actions = group.actionList || []
    if (actions.some(a => ['takePhoto', 'orientedShoot'].includes(a.actionActuatorFunc))) continue
    placemark.actionGroup = { ...group, actionGroupMode: 'sequence', actionList: [...actions, ...constructionCaptureActions(lens)] }
    added++
  }
  return added
}
