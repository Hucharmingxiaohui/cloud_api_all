<template>
  <div>
    <a-tree
      :tree-data="treeData"
      :defaultExpandAll="false"
      :selectedKeys="[selectedKeys]"
      :expandedKeys="expandedKeys"
      show-line
      @select="onSelect"
      @expand="onExpand"
    >
      <template #title="{ title, key }">
        <span :style="{ color: selectedKeys === key ? 'blue' : 'inherit' }">
          {{ title }}
        </span>
      </template>
    </a-tree>
  </div>
</template>

<script>
import { ref, watch, onMounted } from 'vue'

export default {
  name: 'CustomTree',
  props: {
    treeData: {
      type: Object,
      required: true
    }
  },
  emits: ['change'],
  setup (props, { emit }) {
    const selectedKeys = ref('') // 用来保存选中的节点 key
    const expandedKeys = ref([]) // 用来保存展开的节点 key

    // 处理节点选择事件
    const onSelect = (newSelectedKeys, { node }) => {
      if (node.isLeaf) {
        highlightSelectedNodeAndParents(node) // 高亮选中的节点及其父节点
      }
    }

    const onExpand = (newExpandedKeys) => {
      expandedKeys.value = newExpandedKeys // 更新展开的节点
    }

    // 递归查找节点的父节点
    const findParentNode = (nodeKey, treeData, parent = null) => {
      for (const node of treeData) {
        if (node.key === nodeKey) {
          return parent
        }
        if (node.children && node.children.length > 0) {
          const foundParent = findParentNode(nodeKey, node.children, node)
          if (foundParent) {
            return foundParent
          }
        }
      }
      return null
    }

    // 高亮选中的节点及其父节点
    const highlightSelectedNodeAndParents = (node) => {
      const keysToHighlight = []
      let currentNode = node

      while (currentNode) {
        keysToHighlight.push(currentNode.key)
        currentNode = findParentNode(currentNode.key, props.treeData)
      }

      selectedKeys.value = node.key || node.dataRef?.key
      const key = node.key || node.dataRef?.key
      if (key) {
        localStorage.setItem('workspace_id', key) // 将 key 替换为 workspace_id
      }
      emit('change', node)
    }

    // 如果 treeData 发生变化，清空选中和展开状态
    watch(() => props.treeData, () => {
      selectedKeys.value = []
      expandedKeys.value = []
    })

    // 在组件挂载时选择第一个节点并展开
    onMounted(() => {
      setTimeout(() => {
        if (props.treeData.length > 0) {
          const firstNode = props.treeData[0]
          expandedKeys.value = [firstNode.key] // 展开第一个节点
          const defaultWid = localStorage.getItem('workspace_id')
          // 选择第一个节点的第一个子节点
          if (firstNode.children && firstNode.children.length > 0 && defaultWid) {
            selectedKeys.value = defaultWid
          }
        }
      }, 500)
    })

    return {
      selectedKeys,
      expandedKeys,
      onSelect,
      onExpand
    }
  }
}
</script>

<style scoped>
/* 树节点的通用样式 */
:deep(.ant-tree li .ant-tree-node-content-wrapper) {
  color: #d4d4ed !important; /* 节点文字颜色 */
  font-size: 16px; /* 调整字体大小 */
  font-weight: 500; /* 增加字体粗细 */
  padding: 8px 12px; /* 增加内边距 */
  background-color: #1a1a2e !important; /* 背景颜色 */
  border-radius: 8px; /* 圆角边框 */
  transition: all 0.3s ease; /* 添加过渡效果 */
  display: inline-flex;
  align-items: center;
}

/* 节点悬停时的样式 */
:deep(.ant-tree li .ant-tree-node-content-wrapper:hover) {
  background-color: #25274d !important; /* 悬停背景色 */
  color: #ffffff !important; /* 悬停文字颜色 */
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); /* 阴影效果 */
}

/* 选中节点的样式 */
:deep(.ant-tree li .ant-tree-node-selected .ant-tree-node-content-wrapper) {
  background-color: #f36412 !important; /* 选中背景色，橙色 */
  color: #ffffff !important; /* 选中文字颜色为白色 */
  font-weight: 700 !important; /* 选中节点的字体加粗 */
  border: 2px solid #f39c12; /* 添加选中项的边框颜色 */
  box-shadow: 0px 0px 10px rgba(243, 156, 18, 0.6); /* 添加发光效果 */
}

/* 选中节点图标的样式 */
:deep(.ant-tree li.ant-tree-node-selected .ant-tree-iconEle) {
  color: #ff0000 !important; /* 选中节点图标颜色 */
}

/* 自定义展开/收起图标 */
:deep(.ant-tree.ant-tree-show-line li span.ant-tree-switcher) {
  background: #d4d4ed !important;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  color: #1a1a2e;
}

:deep(.ant-tree li .ant-tree-node-content-wrapper span.ant-tree-title) {
  margin-left: 8px;
}

:deep(.ant-tree li .ant-tree-node-content-wrapper .ant-tree-iconEle) {
  color: #9b59b6 !important; /* 自定义节点图标颜色 */
  margin-right: 8px; /* 图标和文字的间距 */
}

/* 调整树形结构的线条样式 */
:deep(.ant-tree.ant-tree-show-line li .ant-tree-switcher-line-icon) {
  color: #6c757d;
}
</style>
