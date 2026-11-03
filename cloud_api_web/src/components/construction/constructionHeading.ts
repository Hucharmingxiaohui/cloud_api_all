export interface HeadingWaypoint {
  lng: number
  lat: number
  placemark?: { waypointHeadingParam?: { waypointHeadingMode?: string; waypointHeadingAngle?: number } }
}

function normalizeHeading (angle: number) {
  return ((angle % 360) + 360) % 360
}

function bearing (from: HeadingWaypoint, to: HeadingWaypoint) {
  const radians = Math.PI / 180
  const lat1 = from.lat * radians; const lat2 = to.lat * radians
  const deltaLng = (to.lng - from.lng) * radians
  const y = Math.sin(deltaLng) * Math.cos(lat2)
  const x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLng)
  return normalizeHeading(Math.atan2(y, x) / radians)
}

export function resolveWaypointHeadings (waypoints: HeadingWaypoint[]): number[] {
  return waypoints.map((waypoint, index) => {
    const heading = waypoint.placemark?.waypointHeadingParam
    const configured = Number(heading?.waypointHeadingAngle)
    if (['manually', 'smoothTransition'].includes(String(heading?.waypointHeadingMode)) && Number.isFinite(configured)) return normalizeHeading(configured)
    const next = waypoints[index + 1] || waypoints[index - 1]
    return next ? bearing(index === waypoints.length - 1 ? next : waypoint, index === waypoints.length - 1 ? waypoint : next) : 0
  })
}
