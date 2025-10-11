<template>
  <div style="margin: 20px 0; padding: 0 15px;">
    <video ref="vid" autoplay controls :style="{ width: '100%', height: '400px' }"></video>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'

export default {
  setup () {
    const vid = ref(null)
    const btn1Disabled = ref(false)
    const btn2Disabled = ref(true)
    let pc = null
    let xmlhttp = null

    onMounted(() => {
      // vid.value = document.querySelector('video')
      // const vid = ref(null)
      start()
    })

    const start = () => {
      const servers = null
      pc = new RTCPeerConnection(servers)
      console.log('Created remote peer connection object pc')
      pc.onicecandidate = iceCallback
      pc.ontrack = gotRemoteStream

      xmlhttp = new XMLHttpRequest()
      const addr = 'http://172.20.63.108:8004/webrtc'
      xmlhttp.onreadystatechange = () => {
        if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
          const res = xmlhttp.responseText
          gotoffer(res)
        }
      }
      xmlhttp.open('GET', addr, true)
      xmlhttp.send()
    }

    const onCreateSessionDescriptionError = (error) => {
      console.log('Failed to create session description: ' + error.toString())
      stop()
    }

    const onCreateAnswerError = (error) => {
      console.log('Failed to set createAnswer: ' + error.toString())
      stop()
    }

    const onSetLocalDescriptionError = (error) => {
      console.log('Failed to set setLocalDescription: ' + error.toString())
      stop()
    }

    const onSetLocalDescriptionSuccess = () => {
      console.log('localDescription success.')
    }

    const gotoffer = (offer) => {
      console.log('Offer from server \n' + offer)
      const desc = new RTCSessionDescription({
        type: 'offer',
        sdp: offer
      })
      pc.setRemoteDescription(desc)
      pc.createAnswer().then(
        gotDescription,
        onCreateSessionDescriptionError
      )
    }

    const gotDescription = (desc) => {
      pc.setLocalDescription(desc).then(
        onSetLocalDescriptionSuccess,
        onSetLocalDescriptionError
      )
      console.log('Pranswer from pc \n' + desc.sdp)
    }

    const stop = () => {
      console.log('Ending Call' + '\n\n')
      pc.close()
      pc = null
      btn1Disabled.value = false
      btn2Disabled.value = true
    }

    const gotRemoteStream = (e) => {
      vid.value.srcObject = e.streams[0]
      console.log('Received remote stream')
    }

    const iceCallback = (event) => {
      if (event.candidate) {
        console.log('Remote ICE candidate: \n ' + event.candidate.candidate)
      } else {
        // All ICE candidates have been sent
      }
    }

    return {
      btn1Disabled,
      btn2Disabled,
      start,
      stop,
      vid
    }
  },
}
</script>

<style scoped>
video {
  border: 5px solid black;
  width: 100%;
  height: 100%;
}
</style>
