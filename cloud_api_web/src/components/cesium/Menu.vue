<template>
  <div id="contextMenu" v-show="isContextMenuVisible" :style="{ top: contextMenuStyle.top, left: contextMenuStyle.left }"
       class="context-menu">
    <div v-for="(option, index) in options" :key="index" @click="handleOptionClick(option.action)"
         class="context-menu-option">
      <span class="icon">{{ option.icon }}</span> <!-- 增加图标 -->
      <span>{{ option.name }}</span>
      <span class="shortcut">{{ option.shortcut }}</span> <!-- 增加快捷键提示 -->
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'

export default {
  props: {
    options: {
      type: Array,
      required: true,
      default: () => []
    },
    isVisible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:isVisible'], // 用于 v-model 的双向绑定
  setup (props, { emit }) {
    // 状态管理
    const isContextMenuVisible = ref(props.isVisible)
    const contextMenuStyle = reactive({
      top: '0px',
      left: '0px'
    })

    // 监听 isVisible 的变化
    watch(() => props.isVisible, (newVal) => {
      isContextMenuVisible.value = newVal
    })

    // 事件处理
    const handleClickOutside = (event) => {
      if (!event.target.closest('#contextMenu')) {
        hideContextMenu()
      }
    }

    const showContextMenu = (event) => {
      isContextMenuVisible.value = true
      emit('update:isVisible', true) // 更新父组件的状态

      const menuWidth = 280 // 计算弹窗的宽度，可以根据实际情况获取
      const windowWidth = window.innerWidth
      const maxLeft = windowWidth - menuWidth // 弹窗最大允许的 left 值

      let left = event.clientX
      if (left > maxLeft) {
        left = maxLeft
      }

      contextMenuStyle.top = `${event.clientY}px`
      contextMenuStyle.left = `${left}px`
    }

    const hideContextMenu = () => {
      isContextMenuVisible.value = false
      emit('update:isVisible', false) // 更新父组件的状态
    }

    const handleOptionClick = (action) => {
      hideContextMenu()
      action() // 执行传入的方法
    }

    // 生命周期钩子
    onMounted(() => {
      window.addEventListener('click', handleClickOutside)
    })

    onBeforeUnmount(() => {
      window.removeEventListener('click', handleClickOutside)
    })

    // 返回暴露的响应式变量和方法
    return {
      isContextMenuVisible,
      contextMenuStyle,
      showContextMenu,
      hideContextMenu,
      handleOptionClick,
    }
  }
}
</script>

  <style>
  .context-menu {
    position: fixed;
    z-index: 1000;
    min-width: 150px;
    max-width: 300px;
    background-color: white;
    border: none;
    border-radius: 3px;
    box-shadow: 0 0 5px #ccc;
  }

  .context-menu-option {
    display: flex;
    font-size: 12px;
    align-items: center;
    justify-content: center;
    padding: 10px 5px;
    cursor: pointer;
  }

  .context-menu-option:not(:last-child) {
    border-bottom: 1px solid #eee;
  }

  .icon {
    margin-right: 20px;
    /* 控制图标与文字之间的间距 */
  }

  .shortcut {
    margin-right: 10px;
    margin-left: 40px;
    /* 将快捷键提示放置在右侧 */
    text-align: right;
    /* 文字靠右显示 */
  }

  .context-menu-option:hover {
    background-color: #f0f0f0;
  }
  </style>
