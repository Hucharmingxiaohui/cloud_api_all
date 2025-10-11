<template>
  <span>
    <a-tree
      draggable
      :defaultExpandAll="true"
      class="device-map-layers"
      @drop="onDrop"
      v-bind="$attrs"
    >
      <a-tree-node
        :title="layer.name"
        :id="layer.id"
        v-for="layer in getTreeData"
        :key="layer.id"
      >
        <template v-if="layer.elements">
          <a-tree-node
            v-for="resource in layer.elements"
            :id="getLayerTreeKey('resource', resource.id)"
            :key="getLayerTreeKey('resource', resource.id)"
          >
            <template #title>
              {{ resource.name }}
              <el-icon @click.stop="onDelete(resource.id, layer.id)" style="color: #8C939A; margin-left: 10px; line-height: 100%;height: 100%;">
                <DeleteFilled />
              </el-icon>
            </template>
          </a-tree-node>
        </template>
      </a-tree-node>
    </a-tree>
  </span>
</template>

<script lang="ts" setup>
import { computed, defineProps, PropType, reactive, defineEmits } from 'vue'
import { useMyStore } from '/@/store'
import { DropEvent, mapLayer } from '/@/types/mapLayer'
import { getLayerTreeKey } from '/@/utils/layer-tree'

const store = useMyStore()
const props = defineProps({
  layerData: Array as PropType<mapLayer[]>
})
const emit = defineEmits(['delete', 'update:layerData'])
const state = reactive({
  checkedKeys: [] as string[],
  expandedKeys: [] as string[]
})
const getTreeData = computed(() => JSON.parse(JSON.stringify(props.layerData)))

const shareId = computed(() => store.state.layerBaseInfo.share)
const defaultId = computed(() => store.state.layerBaseInfo.default)

async function onDrop ({ node, dragNode, dropPosition, dropToGap }: DropEvent) {
  const _treeData = JSON.parse(JSON.stringify(props.layerData))
  const dragKey = dragNode.eventKey.replaceAll('resource__', '')
  const dropKey = node.eventKey.replaceAll('resource__', '')

  const findNodeByKey = (data: mapLayer[], key: string): { item: mapLayer; parent: mapLayer | null } | null => {
    for (const item of data) {
      if (item.id === key) return { item, parent: null }
      if (item.elements) {
        const result = findNodeByKey(item.elements, key)
        if (result) return { item: result.item, parent: item }
      }
    }
    return null
  }

  const dragObj = findNodeByKey(_treeData, dragKey)
  const dropObj = findNodeByKey(_treeData, dropKey)

  if (dragObj && dropObj) {
    // Remove dragObj from its original location
    const dragIndex = dragObj.parent
      ? dragObj.parent.elements.findIndex(item => item.id === dragObj.item.id)
      : _treeData.findIndex(item => item.id === dragObj.item.id)
    if (dragObj.parent) {
      dragObj.parent.elements.splice(dragIndex, 1)
    } else {
      _treeData.splice(dragIndex, 1)
    }
    // Check if the drag object and drop object are the same
    // Insert dragObj into the new location
    if (!dropToGap) {
      // dropObj.item.elements = dropObj.item.elements || []
      // dropObj.item.elements.push(dragObj.item)
      return
    } else {
      const dropIndex = dropObj.parent
        ? dropObj.parent.elements.findIndex(item => item.id === dropObj.item.id)
        : _treeData.findIndex(item => item.id === dropObj.item.id)

      if (dropPosition < 0) {
        dropObj.parent ? dropObj.parent.elements.splice(dropIndex, 0, dragObj.item) : _treeData.splice(dropIndex, 0, dragObj.item)
      } else {
        dropObj.parent ? dropObj.parent.elements.splice(dropIndex + 1, 0, dragObj.item) : _treeData.splice(dropIndex + 1, 0, dragObj.item)
      }
    }
  }

  emit('update:layerData', _treeData)
}

function onDelete (id: string, parentId?: string) {
  emit('delete', { id, parentId })
}
</script>
<style lang="scss">
$antPrefix: 'ant';
.device-map-layers.#{$antPrefix}-tree {
color: #fff;

.#{$antPrefix}-tree-checkbox:not(.#{$antPrefix}-tree-checkbox-checked)
    .#{$antPrefix}-tree-checkbox-inner {
    background-color: unset;
}

.anticon {
    font-size: 16px;
}

// 第一个层级的 li，有左边距 16px
> li {
    padding-left: 16px;
    padding-right: 16px;
}

li {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    padding-top: 0;
    padding-bottom: 0;

    &:first-child {
    padding-top: 4px;
    }

    &.#{$antPrefix}-tree-treenode-disabled
    > .#{$antPrefix}-tree-node-content-wrapper {
    height: 20px;
    span {
        color: #fff;
    }
    }
    > ul {
    width: 100%;
    }

    .#{$antPrefix}-tree-switcher {
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    }

    .#{$antPrefix}-tree-checkbox {
    z-index: 1;
    }
    .#{$antPrefix}-tree-checkbox:hover::after,
    .#{$antPrefix}-tree-checkbox-wrapper:hover
    .#{$antPrefix}-tree-checkbox::after {
    visibility: collapse;
    }

    .#{$antPrefix}-tree-title {
    display: block;
    }

    .#{$antPrefix}-tree-node-content-wrapper {
    color: #fff;
    width: calc(100% - 46px);
    flex: 1;
    box-sizing: content-box;
    height: 20px;
    min-width: 0; // 解决文字溢出不会省略的问题
    padding-right: 0;

    &:not([draggable='true']) {
        border-top: 2px transparent solid;
        border-bottom: 2px transparent solid;
    }

    &:hover {
        background-color: transparent;
    }

    > span {
        &::before {
        // position: absolute;
        // right: 0;
        // left: 0;
        height: 28px;
        transition: all 0.3s;
        content: '';
        }

        // 进度条组件需要相对最外层定位，进度条组件的position不能设置为relative
        > *:not(.progress-wrapper) {
        position: relative;
        z-index: 1;
        }
    }

    &.#{$antPrefix}-tree-node-selected {
        background-color: transparent;
        color: #2d8cf0;
        > span {
        &::before {
            background-color: #4f4f4f;
        }
        }
    }
    }
}
span.#{$antPrefix}-tree-switcher.#{$antPrefix}-tree-switcher_open
    .#{$antPrefix}-tree-switcher-icon {
    transform: rotate(0deg) !important;
}
span.#{$antPrefix}-tree-switcher.#{$antPrefix}-tree-switcher_close
    .#{$antPrefix}-tree-switcher-icon {
    transform: rotate(0deg) !important;
}
}
</style>
