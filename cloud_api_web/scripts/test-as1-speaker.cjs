const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const sourcePath = path.resolve(__dirname, '../src/components/devices/speaker/as1-speaker.ts')
const source = fs.readFileSync(sourcePath, 'utf8')
const output = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020,
  },
}).outputText

const moduleUnderTest = { exports: {} }
// eslint-disable-next-line no-new-func
new Function('require', 'module', 'exports', output)(require, moduleUnderTest, moduleUnderTest.exports)

const {
  buildSpeakerCommand,
  clampSpeakerSpeed,
  clampSpeakerValue,
  createSpeakerFileName,
  findAs1State,
  getSpeakerErrorMessage,
  getSpeakerStatusText,
  md5Utf8,
} = moduleUnderTest.exports

assert.equal(md5Utf8('hello'), '5d41402abc4b2a76b9719d911017c592')
assert.equal(md5Utf8('请注意安全'), '67aa3ac6289adb31e8c0fccb32c569d0')

const state = findAs1State({
  psdk_index: 7,
  psdk_type: 5,
  psdk_name: 'Speaker',
  psdk_sn: 'AS1-001',
  psdk_version: '01.00',
  speaker: {
    system_state: 2,
    play_mode: 0,
    tts_volume: 80,
    tts_type: 1,
    tts_language: 0,
    tts_speed: 50,
  },
})
assert.equal(state.psdk_index, 7)
assert.equal(state.psdk_sn, 'AS1-001')
assert.equal(findAs1State({ psdk_index: 2, psdk_name: 'Searchlight' }), null)

assert.deepEqual(buildSpeakerCommand('drc_speaker_tts_set', 12, {
  psdk_index: 7,
  volume: 80,
  type: 1,
  language: 0,
  speed: 50,
}), {
  method: 'drc_speaker_tts_set',
  seq: 12,
  data: {
    psdk_index: 7,
    volume: 80,
    type: 1,
    language: 0,
    speed: 50,
  },
})

assert.equal(getSpeakerStatusText(0), '空闲')
assert.equal(getSpeakerStatusText(4), 'TTS 文本转换中')
assert.equal(getSpeakerStatusText(99), '正在下载')
assert.equal(getSpeakerErrorMessage(327071), 'TTS 文本 MD5 校验失败')
assert.match(createSpeakerFileName(1722067200000), /^as1-1722067200000$/)
assert.equal(clampSpeakerValue(0), 1)
assert.equal(clampSpeakerValue(101), 100)
assert.equal(clampSpeakerSpeed(0), 0)
assert.equal(clampSpeakerSpeed(101), 100)

assert.deepEqual(buildSpeakerCommand('drc_speaker_play_volume_set', 13, {
  psdk_index: 7,
  play_volume: 80,
}), {
  method: 'drc_speaker_play_volume_set',
  seq: 13,
  data: {
    psdk_index: 7,
    play_volume: 80,
  },
})

console.log('AS1 speaker protocol tests passed')
