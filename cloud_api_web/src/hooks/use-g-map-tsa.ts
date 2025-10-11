import store from '/@/store'
import { getRoot } from '/@/root'
import { ELocalStorageKey, EDeviceTypeName } from '/@/types'
import { getDeviceBySn } from '/@/api/manage'
import { message } from 'ant-design-vue'
import dockIcon from '/@/assets/icons/dock.png'
import rcIcon from '/@/assets/icons/rc.png'
import droneIcon from '/@/assets/icons/drone.png'

export function deviceTsaUpdate () {
  const root = getRoot()
  let AMap = root.$aMap

  const icons = new Map([
    [EDeviceTypeName.Aircraft, droneIcon],
    [EDeviceTypeName.Gateway, rcIcon],
    [EDeviceTypeName.Dock, dockIcon]
  ])
  const markers = store.state.markerInfo.coverMap
  const paths = store.state.markerInfo.pathMap

  let trackLine = null as any
  function getTrackLineInstance () {
    if (!trackLine) {
      trackLine = new AMap.Polyline({
        map: root.$map,
        strokeColor: '#939393' // 线颜色
      })
    }
    return trackLine
  }

  function initIcon (type: number) {
    return new AMap.Icon({
      image: icons.get(type),
      imageSize: new AMap.Size(40, 40),
      size: new AMap.Size(40, 40)
    })
  }

  function initMarker (type: number, name: string, sn: string, lng?: number, lat?: number) {
    if (markers[sn]) {
      // console.log('我在执行绘制返回')
      // return
      root.$map.remove(markers[sn])
      delete markers[sn]
    }
    if (root.$aMap === undefined) {
      return
    }
    AMap = root.$aMap
    markers[sn] = new AMap.Marker({
      position: new AMap.LngLat(lng || 121.371499, lat || 37.521124),
      icon: initIcon(type),
      title: name,
      anchor: 'top-center',
      offset: [0, -20],
    })
    root.$map.add(markers[sn])
    // markers[sn].on('moving', function (e: any) {
    //   let path = paths[sn]
    //   if (!path) {
    //     paths[sn] = e.passedPath
    //     return
    //   }
    //   path.push(e.passedPath[0])
    //   path.push(e.passedPath[1])
    //   getTrackLineInstance().setPath(path)
    // })
  }
  // 添加航线站点图标
  function initMarker1 (type: number, name: string, sn: string, lng?: number, lat?: number) {
    AMap = root.$aMap
    const tt = new AMap.Marker({
      position: new AMap.LngLat(lng || 121.371499, lat || 37.521124),
      icon: initIcon(type),
      title: name,
      anchor: 'top-center',
      offset: [0, -20],
    })
    root.$map.add(tt)
    // markers[sn].on('moving', function (e: any) {
    //   let path = paths[sn]
    //   if (!path) {
    //     paths[sn] = e.passedPath
    //     return
    //   }
    //   path.push(e.passedPath[0])
    //   path.push(e.passedPath[1])
    //   getTrackLineInstance().setPath(path)
    // })
  }
  function removeAllMarker () {
    if (root.$map && markers.length > 0) {
      markers.forEach(marker => {
        root.$map.remove(marker) // 移除每个标记
      })
      // 清空 markers 数组
      markers.length = 0
    }
  }
  function removeMarker (sn: string) {
    if (!markers[sn]) {
      return
    }
    root.$map.remove(markers[sn])
    getTrackLineInstance().setPath([])
    delete markers[sn]
    delete paths[sn]
  }

  function addMarker (sn: string, lng?: number, lat?: number) {
    getDeviceBySn(localStorage.getItem(ELocalStorageKey.WorkspaceId)!, sn)
      .then(data => {
        if (data.code !== 0) {
          message.error(data.message)
          return
        }
        initMarker(data.data.domain, data.data.nickname, sn, lng, lat)
        console.log('飞行器标记')
        // console.log("xxxddf",data.data.domain)
      })
  }

  function moveTo (sn: string, lng: number, lat: number) {
    let marker = markers[sn]
    if (!marker) {
      addMarker(sn, lng, lat)
      marker = markers[sn]

      return
    }
    marker.moveTo([lng, lat], {
      duration: 1800,
      autoRotation: true
    })
  }

  return {
    marker: markers,
    initMarker,
    initMarker1,
    removeAllMarker,
    removeMarker,
    moveTo
  }
}
