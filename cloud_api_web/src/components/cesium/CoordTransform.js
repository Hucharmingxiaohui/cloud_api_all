class CoordTransform {
  /**
     * WGS84 转 GCJ02（火星坐标系）
     * @param lng WGS84 经度
     * @param lat WGS84 纬度
     * @returns [GCJ02经度, GCJ02纬度]
     */
  static WGS84ToGCJ02 (lng, lat) {
    if (this.outOfChina(lng, lat)) {
      return [lng, lat]
    }
    const d = this.delta(lng, lat)
    return [lng + d[0], lat + d[1]]
  }

  /**
     * GCJ02（火星坐标系）转 WGS84
     * @param lng GCJ02 经度
     * @param lat GCJ02 纬度
     * @returns [WGS84经度, WGS84纬度]
     */
  static GCJ02ToWGS84 (lng, lat) {
    if (this.outOfChina(lng, lat)) {
      return [lng, lat]
    }
    const d = this.delta(lng, lat)
    return [lng - d[0], lat - d[1]]
  }

  /**
     * 计算偏移量
     * @param lng 经度
     * @param lat 纬度
     * @returns [经度偏移量, 纬度偏移量]
     */
  static delta (lng, lat) {
    const a = 6378245.0 // 长半轴
    const ee = 6.693421622965943e-3 // 扁率（修正后的值）

    let dLat = this.transformLat(lng - 105.0, lat - 35.0)
    let dLng = this.transformLng(lng - 105.0, lat - 35.0)

    const radLat = (lat / 180.0) * Math.PI
    let magic = Math.sin(radLat)
    magic = 1 - ee * magic * magic

    const sqrtMagic = Math.sqrt(magic)
    dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI)
    dLng = (dLng * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * Math.PI)

    return [dLng, dLat]
  }

  /**
     * 判断是否在中国境外
     * @param lng 经度
     * @param lat 纬度
     * @returns {boolean}
     */
  static outOfChina (lng, lat) {
    // 经度范围
    if (lng < 72.004 || lng > 137.8347) {
      return true
    }
    // 纬度范围
    if (lat < 0.8293 || lat > 55.8271) {
      return true
    }
    return false
  }

  /**
     * 纬度转换
     * @param lng 经度
     * @param lat 纬度
     * @returns {number}
     */
  static transformLat (lng, lat) {
    let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
    ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
    ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0
    ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320.0 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0
    return ret
  }

  /**
     * 经度转换
     * @param lng 经度
     * @param lat 纬度
     * @returns {number}
     */
  static transformLng (lng, lat) {
    let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
    ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
    ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0
    ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0
    return ret
  }
}

export default CoordTransform
