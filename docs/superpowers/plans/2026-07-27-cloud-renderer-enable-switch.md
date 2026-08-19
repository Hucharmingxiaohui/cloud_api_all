# Cloud Renderer Enable Switch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a deployment/runtime switch for outdoor cloud rendering so sites without a rendering host default to 2D maps and show a clear unavailable state in the cloud 3D wayline editor.

**Architecture:** Extend the existing `CloudRendererConfig` with an `enabled` boolean whose missing-value default is `true` for backward compatibility. All cloud-rendering entry points read this shared config before mounting a renderer; disabling it closes the shared outdoor session, defaults monitoring pages to 2D, and renders a static disabled-state page for cloud wayline planning without creating WebRTC or HTTP sessions.

**Tech Stack:** Vue 3 Composition API, TypeScript, Element Plus, Vite runtime config, existing `CloudRendererClient` singleton.

---

## File Structure

- Modify `cloud_api_web/public/config.js`: deployment default for `cloudRenderer.enabled`.
- Modify `cloud_api_web/src/types/runtime-config.ts`: type the new `enabled` property.
- Modify `cloud_api_web/src/components/cloudRenderer/cloudRendererConfig.ts`: merge and expose backward-compatible enable state.
- Modify `cloud_api_web/src/components/variableMgt/cloudRenderingMgt/index.vue`: add the enable switch and implement save/close/reconnect behavior.
- Modify `cloud_api_web/src/pages/page-web/projects/livestream.vue`: default to 2D and suppress the 3D switch when disabled.
- Modify `cloud_api_web/src/components/task/waylineVideo.vue`: default to 2D and suppress the 3D switch when disabled.
- Modify `cloud_api_web/src/components/cloudRenderer/CloudWaylineEditor.vue`: show the disabled-state message and avoid creating/starting a wayline session.

### Task 1: Extend Runtime Configuration

**Files:**
- Modify: `cloud_api_web/public/config.js`
- Modify: `cloud_api_web/src/types/runtime-config.ts`
- Modify: `cloud_api_web/src/components/cloudRenderer/cloudRendererConfig.ts`

- [ ] **Step 1: Add the typed enable property**

Add `enabled: boolean` to `CloudRendererConfig` in `runtime-config.ts`.

- [ ] **Step 2: Add the deployment setting**

Add the following before `baseURL` in `public/config.js`:

```js
cloudRenderer: {
  enabled: true,
  baseURL: 'http://127.0.0.1:3000',
  // existing settings
}
```

- [ ] **Step 3: Preserve compatibility with old configurations**

Set `enabled: true` in `DEFAULT_CLOUD_RENDERER_CONFIG`. Keep merge precedence as local override > deployment config > default so existing installations with no `enabled` field remain enabled.

- [ ] **Step 4: Add a shared enable helper**

Export:

```ts
export function isCloudRendererEnabled () {
  return getCloudRendererConfig().enabled !== false
}
```

- [ ] **Step 5: Run focused lint**

Run:

```bash
npx eslint "src/components/cloudRenderer/cloudRendererConfig.ts" "src/types/runtime-config.ts"
```

Expected: exit code `0`.

### Task 2: Add the Outdoor Configuration Switch

**Files:**
- Modify: `cloud_api_web/src/components/variableMgt/cloudRenderingMgt/index.vue`

- [ ] **Step 1: Add the switch at the top of the outdoor form**

Use an Element Plus switch bound to `form.enabled`:

```vue
<el-form-item label="启用室外云渲染">
  <el-switch v-model="form.enabled" active-text="已启用" inactive-text="未启用" />
</el-form-item>
```

- [ ] **Step 2: Disable connection-only actions when switched off**

Disable “读取文件” and “测试连接” when `!form.enabled`. Keep service address and model fields editable so deployment values can be prepared before enabling.

- [ ] **Step 3: Save without reconnecting when disabled**

In `saveOutdoor`, save the form first. If disabled, call `cloudRendererClient.close()`, set connection status to `未启用`, show `室外云渲染已关闭`, and return without calling `restart`.

- [ ] **Step 4: Reconnect only when enabled**

Preserve the existing `restart('outdoor')` path when `form.enabled` is true.

- [ ] **Step 5: Apply the same rule when restoring deployment config**

After `resetCloudRendererConfig()`, close the client when disabled; otherwise restart it.

- [ ] **Step 6: Run focused lint**

Run:

```bash
npx eslint "src/components/variableMgt/cloudRenderingMgt/index.vue"
```

Expected: exit code `0`.

### Task 3: Default the Video Live Page to 2D

**Files:**
- Modify: `cloud_api_web/src/pages/page-web/projects/livestream.vue`

- [ ] **Step 1: Read enable state once during page setup**

Import `isCloudRendererEnabled`, define `const cloudRendererEnabled = isCloudRendererEnabled()`, and initialize:

```ts
const isFlatMap = ref(!cloudRendererEnabled)
```

- [ ] **Step 2: Guard renderer mounting**

Render `OutdoorRenderer` only when `cloudRendererEnabled && !isFlatMap && isMounted`.

- [ ] **Step 3: Hide the switch when 3D is unavailable**

Add `v-if="cloudRendererEnabled"` to the map switch. This leaves the 2D map as the only valid view for non-rendering sites.

- [ ] **Step 4: Run focused lint**

Run:

```bash
npx eslint "src/pages/page-web/projects/livestream.vue"
```

Expected: exit code `0`.

### Task 4: Default the Console Page to 2D

**Files:**
- Modify: `cloud_api_web/src/components/task/waylineVideo.vue`

- [ ] **Step 1: Read the shared enable state**

Import `isCloudRendererEnabled`, define `const cloudRendererEnabled = isCloudRendererEnabled()`, and initialize `isFlatMap` to `!cloudRendererEnabled`.

- [ ] **Step 2: Guard renderer mounting**

Require `cloudRendererEnabled` in the `OutdoorRenderer` condition.

- [ ] **Step 3: Hide the unavailable switch**

Render the map-switch control only when cloud rendering is enabled.

- [ ] **Step 4: Run focused lint**

Run:

```bash
npx eslint "src/components/task/waylineVideo.vue"
```

Expected: exit code `0`.

### Task 5: Add the Disabled State to Cloud Wayline Planning

**Files:**
- Modify: `cloud_api_web/src/components/cloudRenderer/CloudWaylineEditor.vue`

- [ ] **Step 1: Resolve enable state before creating the client**

Import `isCloudRendererEnabled`, define `const cloudRendererEnabled = isCloudRendererEnabled()`, and create `CloudRendererClient` only in the enabled branch. Avoid registering signal listeners when disabled.

- [ ] **Step 2: Add a full-page disabled state**

At the template root, branch on the flag. The disabled state must contain:

```vue
<div v-if="!cloudRendererEnabled" class="cloud-disabled">
  <div class="cloud-disabled__card">
    <h2>当前现场未开启三维云渲染服务</h2>
    <p>请使用二维航线规划，或联系管理员在云渲染管理中启用室外云渲染。</p>
    <el-button type="primary" @click="router.back()">返回</el-button>
  </div>
</div>
```

- [ ] **Step 3: Prevent disabled lifecycle work**

Ensure no renderer session, WebSocket listener, KMZ wayline operation, or cleanup call assumes a client exists when disabled. Use a no-op listener cleanup or optional chaining consistently.

- [ ] **Step 4: Style the disabled page**

Use the existing cyan/blue panel language, center the card, and keep it responsive without changing the enabled editor layout.

- [ ] **Step 5: Run focused lint**

Run:

```bash
npx eslint "src/components/cloudRenderer/CloudWaylineEditor.vue"
```

Expected: exit code `0`.

### Task 6: Integration Verification

**Files:**
- Verify all modified files.

- [ ] **Step 1: Verify enabled deployment behavior**

Set `cloudRenderer.enabled: true`, reload, and confirm:

```text
视频直播默认三维，可切换二维
控制台默认三维，可切换二维
云渲染三维航线正常创建 wayline session
```

- [ ] **Step 2: Verify disabled deployment behavior**

Set `cloudRenderer.enabled: false`, clear the local `cloud-renderer-config` override if present, reload, and confirm:

```text
视频直播默认二维且无三维切换按钮
控制台默认二维且无三维切换按钮
云渲染三维航线显示未开启提示
浏览器网络面板无云渲染 session/WebSocket 请求
```

- [ ] **Step 3: Verify runtime management switching**

Disable and save in cloud-rendering management; confirm the current shared client closes. Enable and save; confirm the client reconnects and monitoring pages show 3D after reload.

- [ ] **Step 4: Run all focused lint checks**

Run:

```bash
npx eslint "src/types/runtime-config.ts" "src/components/cloudRenderer/cloudRendererConfig.ts" "src/components/variableMgt/cloudRenderingMgt/index.vue" "src/pages/page-web/projects/livestream.vue" "src/components/task/waylineVideo.vue" "src/components/cloudRenderer/CloudWaylineEditor.vue"
```

Expected: exit code `0`.

- [ ] **Step 5: Run production build**

Run:

```bash
npm run build
```

Expected: Vite build succeeds; existing Sass and deep-selector deprecation warnings are acceptable.
