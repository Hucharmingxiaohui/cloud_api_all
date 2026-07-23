import JSZip from 'jszip'
import {
  mapParsedWaylineToCloudDraft,
  type CloudWaylineDraft
} from './cloudWaylineMapper'

function localName (name: string): string {
  return String(name || '').replace(/^.*:/, '')
}

function textOf (el: Element | null | undefined): string {
  return (el?.textContent || '').trim()
}

function childrenByLocal (parent: Element, name: string): Element[] {
  return Array.from(parent.children).filter(child => localName(child.tagName) === name)
}

function firstChildByLocal (parent: Element, name: string): Element | null {
  return childrenByLocal(parent, name)[0] || null
}

function parseAction (actionEl: Element): Record<string, any> {
  const action: Record<string, any> = {
    actionActuatorFunc: textOf(firstChildByLocal(actionEl, 'actionActuatorFunc'))
  }
  const paramEl = firstChildByLocal(actionEl, 'actionActuatorFuncParam')
  if (!paramEl) return action

  const param: Record<string, any> = {}
  for (const child of Array.from(paramEl.children)) {
    const key = localName(child.tagName)
    const value = textOf(child)
    if (!key) continue
    param[key] = value
  }

  const func = action.actionActuatorFunc
  if (func === 'orientedShoot') action.actionActuatorFuncParam = { orientedShoot: param }
  else if (func === 'rotateYaw') action.actionActuatorFuncParam = { rotateYaw: param }
  else if (func === 'gimbalRotate') action.actionActuatorFuncParam = { gimbalRotate: param }
  else if (func === 'zoom') action.actionActuatorFuncParam = { zoom: param }
  else if (func === 'takePhoto') action.actionActuatorFuncParam = { takePhoto: param }
  else if (func === 'hover') action.actionActuatorFuncParam = { hover: param }
  else action.actionActuatorFuncParam = param

  return action
}

function parsePlacemark (placemarkEl: Element): Record<string, any> {
  const placemark: Record<string, any> = {}
  const pointEl = firstChildByLocal(placemarkEl, 'Point')
  if (pointEl) {
    placemark.point = {
      coordinates: textOf(firstChildByLocal(pointEl, 'coordinates'))
    }
  }

  for (const child of Array.from(placemarkEl.children)) {
    const name = localName(child.tagName)
    if (name === 'Point' || name === 'actionGroup' || name === 'waypointHeadingParam' || name === 'waypointTurnParam') continue
    const value = textOf(child)
    if (value !== '') placemark[name] = value
  }

  const headingEl = firstChildByLocal(placemarkEl, 'waypointHeadingParam')
  if (headingEl) {
    const heading: Record<string, any> = {}
    for (const child of Array.from(headingEl.children)) {
      heading[localName(child.tagName)] = textOf(child)
    }
    placemark.waypointHeadingParam = heading
  }

  const actionGroupEls = childrenByLocal(placemarkEl, 'actionGroup')
  if (actionGroupEls.length) {
    const groups = actionGroupEls.map(groupEl => {
      const group: Record<string, any> = { actionList: [] as Record<string, any>[] }
      for (const child of Array.from(groupEl.children)) {
        const name = localName(child.tagName)
        if (name === 'action') group.actionList.push(parseAction(child))
        else group[name] = textOf(child)
      }
      return group
    })
    placemark.actionGroup = groups.length === 1 ? groups[0] : groups
  }

  return placemark
}

function parseKmlXml (xmlText: string): Record<string, any> {
  const doc = new DOMParser().parseFromString(xmlText, 'application/xml')
  if (doc.querySelector('parsererror')) {
    throw new Error('KMZ 内 KML/WPML 解析失败')
  }

  const documentEl = doc.getElementsByTagNameNS('*', 'Document')[0] || doc.documentElement
  const missionEl = firstChildByLocal(documentEl, 'missionConfig')
  const folderEl = firstChildByLocal(documentEl, 'Folder') || firstChildByLocal(documentEl, 'folder')

  const missionConfig: Record<string, any> = {}
  if (missionEl) {
    for (const child of Array.from(missionEl.children)) {
      const name = localName(child.tagName)
      if (name === 'droneInfo' || name === 'payloadInfo') {
        const nested: Record<string, any> = {}
        for (const n of Array.from(child.children)) nested[localName(n.tagName)] = textOf(n)
        missionConfig[name] = nested
      } else {
        missionConfig[name] = textOf(child)
      }
    }
  }

  const folder: Record<string, any> = { placeMarks: [] as Record<string, any>[] }
  if (folderEl) {
    for (const child of Array.from(folderEl.children)) {
      const name = localName(child.tagName)
      if (name === 'Placemark') {
        folder.placeMarks.push(parsePlacemark(child))
      } else if (name === 'globalWaypointHeadingParam' || name === 'waylineCoordinateSysParam' || name === 'payloadParam') {
        const nested: Record<string, any> = {}
        for (const n of Array.from(child.children)) nested[localName(n.tagName)] = textOf(n)
        folder[name] = nested
      } else {
        folder[name] = textOf(child)
      }
    }
  }

  return { missionConfig, folder }
}

function pickBestXml (files: Array<{ name: string; text: string }>): string {
  if (!files.length) throw new Error('KMZ 中未找到 kml/wpml')
  // 优先 waylines.wpml（执行层，高度/动作更完整），否则 template.kml
  const wpml = files.find(f => /waylines\.wpml$/i.test(f.name) || f.name.toLowerCase().endsWith('.wpml'))
  if (wpml) return wpml.text
  const kml = files.find(f => /template\.kml$/i.test(f.name) || f.name.toLowerCase().endsWith('.kml'))
  if (kml) return kml.text
  return files[0].text
}

/**
 * 浏览器端解析 KMZ blob → 云渲染编辑草稿（不调用后端解析接口）
 */
export async function parseKmzBlobToCloudDraft (
  blob: Blob,
  options: { waylineId: string; routeName?: string }
): Promise<CloudWaylineDraft> {
  const zip = await JSZip.loadAsync(blob)
  const xmlFiles: Array<{ name: string; text: string }> = []
  const entries = Object.keys(zip.files)
  for (const name of entries) {
    const file = zip.files[name]
    if (file.dir) continue
    if (!/\.(kml|wpml)$/i.test(name)) continue
    xmlFiles.push({ name, text: await file.async('text') })
  }
  const xmlText = pickBestXml(xmlFiles)
  const wayline = parseKmlXml(xmlText)
  if (!wayline.folder?.placeMarks?.length) {
    // 若优先 wpml 无点，再试 kml
    const kml = xmlFiles.find(f => f.name.toLowerCase().endsWith('.kml'))
    if (kml && kml.text !== xmlText) {
      const kmlWayline = parseKmlXml(kml.text)
      if (kmlWayline.folder?.placeMarks?.length) {
        return mapParsedWaylineToCloudDraft(kmlWayline, options)
      }
    }
  }
  return mapParsedWaylineToCloudDraft(wayline, options)
}
