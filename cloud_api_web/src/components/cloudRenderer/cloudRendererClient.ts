import { CURRENT_CONFIG } from '/@/api/http/config'

type SignalMessage = {
  type: string
  sdp?: string
  candidate?: any
  [key: string]: any
}

type StatusListener = (status: string) => void

const DEFAULT_RENDERER_CONFIG = {
  baseURL: 'http://127.0.0.1:3000',
  pointCloudFile: '3dgs/7.1.1.ply',
  rendererParams: {
    alignSplatFile: '3dgs/7.1.1.ply',
    alignReferenceTileset: '3dgs/dfelanqiuchang/tileset.json'
  },
  iceServers: [
    {
      urls: 'turn:172.20.13.53:3478?transport=udp',
      username: 'cloudrender',
      credential: 'CloudRender@123456'
    }
  ]
}

class CloudRendererClient {
  private sessionId = ''
  private ws: WebSocket | null = null
  private pc: RTCPeerConnection | null = null
  private mediaStream: MediaStream | null = null
  private startPromise: Promise<void> | null = null
  private reconnectTimer: number | null = null
  private signalReconnectTimer: number | null = null
  private pendingIceCandidates: any[] = []
  private statusListeners = new Set<StatusListener>()
  private videoElements = new Set<HTMLVideoElement>()
  private closed = false

  private get config () {
    const userConfig = CURRENT_CONFIG.cloudRenderer || {}
    return {
      ...DEFAULT_RENDERER_CONFIG,
      ...userConfig,
      rendererParams: {
        ...DEFAULT_RENDERER_CONFIG.rendererParams,
        ...(userConfig.rendererParams || {})
      }
    }
  }

  start () {
    this.closed = false
    if (this.startPromise) return this.startPromise
    if (this.sessionId && this.ws) return Promise.resolve()

    this.setStatus('云渲染连接中...')
    this.startPromise = this.createSession()
      .then(() => {
        if (!this.closed) {
          this.connectSignal()
        }
      })
      .catch(error => {
        if (!this.closed) {
          console.error('Cloud renderer start failed:', error)
          this.setStatus('云渲染启动失败')
        }
      })
      .finally(() => {
        this.startPromise = null
      })

    return this.startPromise
  }

  attachVideo (video: HTMLVideoElement | null) {
    if (video) {
      this.videoElements.add(video)
      video.srcObject = this.mediaStream
      this.playVideo(video)
    }
  }

  detachVideo (video: HTMLVideoElement | null) {
    if (video) {
      this.videoElements.delete(video)
      video.srcObject = null
    }
  }

  onStatusChange (listener: StatusListener) {
    this.statusListeners.add(listener)
    listener(this.mediaStream ? '' : '云渲染连接中...')
    return () => {
      this.statusListeners.delete(listener)
    }
  }

  sendInput (action: 'orbit' | 'pan' | 'zoom', payload: Record<string, any>) {
    this.sendSignal({ type: 'input', action, ...payload })
  }

  pick (x: number, y: number) {
    this.sendSignal({ type: 'viewport-double-click', x, y })
  }

  clearPath () {
    this.sendSignal({ type: 'drone-control', action: 'clear-path' })
  }

  async close () {
    this.closed = true
    this.clearTimers()
    this.closePeer()
    this.ws?.close()
    this.ws = null
    this.mediaStream = null
    this.videoElements.forEach(video => {
      video.srcObject = null
    })
    this.videoElements.clear()
    if (this.sessionId) {
      const currentSession = this.sessionId
      this.sessionId = ''
      try {
        await fetch(this.buildHttpUrl(`/api/session/${encodeURIComponent(currentSession)}/close`), { method: 'POST' })
      } catch (error) {
        console.warn('Cloud renderer close session failed:', error)
      }
    }
  }

  private buildHttpUrl (path: string) {
    return new URL(path, this.config.baseURL).toString()
  }

  private buildWsUrl (path: string) {
    const url = new URL(path, this.config.baseURL)
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    return url.toString()
  }

  private async createSession () {
    const res = await fetch(this.buildHttpUrl('/api/session'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pointCloudFile: this.config.pointCloudFile,
        rendererParams: this.config.rendererParams
      })
    })
    if (!res.ok) {
      throw new Error(`创建云渲染会话失败：${res.status}`)
    }
    const data = await res.json()
    this.sessionId = data.id
  }

  private connectSignal () {
    if (this.closed || !this.sessionId) return
    this.ws = new WebSocket(this.buildWsUrl(`/signal?role=client&session=${encodeURIComponent(this.sessionId)}`))
    this.ws.onopen = () => this.setStatus(this.mediaStream ? '' : '等待渲染服务...')
    this.ws.onmessage = event => {
      let msg: SignalMessage
      try {
        msg = JSON.parse(event.data)
      } catch (error) {
        console.warn('Invalid cloud renderer signal:', event.data, error)
        return
      }
      this.handleSignalMessage(msg).catch(error => {
        console.error('Cloud renderer signal handling failed:', error)
        this.setStatus('云渲染信令处理失败')
        this.reconnectPeer()
      })
    }
    this.ws.onerror = () => this.setStatus('云渲染信令异常')
    this.ws.onclose = () => {
      this.ws = null
      if (!this.closed) {
        this.setStatus('云渲染连接已断开')
        this.scheduleSignalReconnect()
      }
    }
  }

  private async handleSignalMessage (msg: SignalMessage) {
    if (msg.type === 'renderer-ready') {
      await this.createPeerAndOffer()
      return
    }

    if (msg.type === 'answer' && msg.sdp && this.pc) {
      await this.pc.setRemoteDescription({ type: 'answer', sdp: msg.sdp })
      await this.flushPendingIceCandidates()
      this.setStatus('')
      return
    }

    if (msg.type === 'ice' && msg.candidate && this.pc) {
      if (this.pc.remoteDescription) {
        await this.pc.addIceCandidate(msg.candidate)
      } else {
        this.pendingIceCandidates.push(msg.candidate)
      }
    }
  }

  private async createPeerAndOffer () {
    this.closePeer()
    this.pendingIceCandidates = []
    this.pc = new RTCPeerConnection({
      iceServers: this.config.iceServers || [],
      iceTransportPolicy: 'all'
    })
    this.pc.addTransceiver('video', { direction: 'recvonly' })
    this.pc.ontrack = event => {
      this.mediaStream = event.streams[0]
      this.videoElements.forEach(video => {
        video.srcObject = this.mediaStream
        this.playVideo(video)
      })
      this.setStatus('')
    }
    this.pc.onicecandidate = event => {
      this.sendSignal({ type: 'ice', candidate: event.candidate })
    }
    this.pc.oniceconnectionstatechange = () => {
      if (!this.pc) return
      if (this.pc.iceConnectionState === 'failed') {
        this.reconnectPeer()
      }
      if (this.pc.iceConnectionState === 'disconnected') {
        this.schedulePeerReconnect()
      }
    }

    const offer = await this.pc.createOffer({ offerToReceiveVideo: true, offerToReceiveAudio: false })
    await this.pc.setLocalDescription(offer)
    this.sendSignal({ type: 'offer', sdp: offer.sdp })
  }

  private schedulePeerReconnect () {
    if (this.reconnectTimer || this.closed) return
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      if (this.pc?.iceConnectionState === 'disconnected') {
        this.reconnectPeer()
      }
    }, 10000)
  }

  private scheduleSignalReconnect () {
    if (this.signalReconnectTimer || this.closed || !this.sessionId) return
    this.closePeer()
    this.signalReconnectTimer = window.setTimeout(() => {
      this.signalReconnectTimer = null
      this.setStatus('云渲染信令重连中...')
      this.connectSignal()
    }, 2000)
  }

  private reconnectPeer () {
    if (this.closed) return
    this.setStatus('云渲染重连中...')
    this.createPeerAndOffer().catch(error => {
      console.error('Cloud renderer reconnect failed:', error)
      this.setStatus('云渲染重连失败')
    })
  }

  private async flushPendingIceCandidates () {
    if (!this.pc || !this.pc.remoteDescription) return
    const candidates = this.pendingIceCandidates
    this.pendingIceCandidates = []
    for (const candidate of candidates) {
      await this.pc.addIceCandidate(candidate)
    }
  }

  private sendSignal (payload: Record<string, any>) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload))
    }
  }

  private closePeer () {
    if (this.pc) {
      this.pc.ontrack = null
      this.pc.onicecandidate = null
      this.pc.oniceconnectionstatechange = null
      this.pc.close()
      this.pc = null
    }
  }

  private clearTimers () {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.signalReconnectTimer) {
      window.clearTimeout(this.signalReconnectTimer)
      this.signalReconnectTimer = null
    }
  }

  private setStatus (status: string) {
    this.statusListeners.forEach(listener => listener(status))
  }

  private playVideo (video: HTMLVideoElement) {
    if (!video.srcObject) return
    video.play().catch(error => {
      console.warn('Cloud renderer video play failed:', error)
    })
  }
}

export const cloudRendererClient = new CloudRendererClient()

window.addEventListener('beforeunload', () => {
  cloudRendererClient.close()
})
