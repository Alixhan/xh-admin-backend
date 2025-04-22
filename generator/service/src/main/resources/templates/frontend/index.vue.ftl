<template>
  <div class="root">
    <m-table
      class="m-table"
      ref="tableRef"
      is-filter-table
      is-complex-filter
      :filter-param="filterParam"
      :filter-columns="topFilterColumns"
      :columns="columns"
      :fetch-data="${queryFun}"
      selection="multiple"
      @selection-change="(rows) => (selectRows = rows)"
      v-model:data="data"
    >
      <template #right-action>
      <#if hasImport>
        <el-button v-auth="'${permissionPrefix}:import'" type="primary" icon="upload" @click="importRef.open()">
          {{ $t('common.imports') }}
        </el-button>
      </#if>
        <el-button v-auth="'${permissionPrefix}:add'" type="primary" icon="plus" @click="openForm('add', null)">
          {{ $t('common.add') }}
        </el-button>
        <el-button
            v-auth="'${permissionPrefix}:del'"
            type="danger"
            icon="delete"
            :disabled="selectRows.length === 0"
            @click="del(selectRows)"
        >{{ $t('common.del') }}
        </el-button>
      </template>
    </m-table>
    <el-dialog
      :title="handleType && $t('common.' + handleType)"
      v-model="formVisible"
      draggable
      destroy-on-close
      append-to-body
      align-center
      :close-on-click-modal="false"
    >
      <${entityName}Form :handle-type="handleType" :model-value="row" @close="close" style="height: 100%" />
    </el-dialog>
<#if hasImport>
    <${entityName}Import ref="importRef" @close="close" />
</#if>
  </div>
</template>
<script setup lang="tsx">
import { type Ref, computed, reactive, ref } from 'vue'
import { ${delFun}, ${queryFun} } from '${apiPath}'
import ${entityName}Form from './${entityVarName}Form.vue'
import { useI18n } from 'vue-i18n'
<#if hasDict?seq_contains('index')>
import useDictDetails from '@/utils/dict'
</#if>
<#if hasImport>
import ${entityName}Import from './${entityVarName}Import.vue'
</#if>

const { t } = useI18n()
const tableRef = ref()
const data = ref([])
const selectRows = ref([])

const filterParam = reactive({})
<#if hasImport>

const importRef = ref()
</#if>

const topFilterColumns = computed(() => [
<#list columns as field>
<#if field.isQuery!false>
  ${field.queryColStr}<#if field_has_next>,</#if>
</#if>
</#list>
])

const columns: Ref<CommonTableColumn[]> = computed(() => [
  { type: 'index', width: 90 },
<#list columns as field>
<#if field.isTable!false>
  ${field.tableColStr},
</#if>
</#list>
  {
    type: 'operation',
    fixed: 'right',
    align: 'center',
    buttons: [
      { label: t('common.edit'), auth: '${permissionPrefix}:edit', icon: 'edit', onClick: (row) => openForm('edit', row) },
      {
        label: t('common.detail'),
        auth: '${permissionPrefix}:detail',
        icon: 'document',
        onClick: (row) => openForm('detail', row)
      },
      { label: t('common.del'), auth: '${permissionPrefix}:del', icon: 'delete', type: 'danger', onClick: (row) => del([row]) }
    ]
  }
])

const formVisible = ref(false)
const handleType = ref()
const row = ref()

function openForm(type: FormHandleType, r) {
  row.value = r
  formVisible.value = true
  handleType.value = type
}

function del(rows: any[]) {
  ${delFun}(rows.map((i) => i.id).join(','), {
    showLoading: true,
    showBeforeConfirm: true,
    showSuccessMsg: true,
    confirmMsg: t('common.confirmDelete')
  }).then(() => {
    tableRef.value.fetchQuery()
  })
}

function close(type) {
  formVisible.value = false
  if (type === 'refresh') {
    tableRef.value.fetchQuery()
  }
}
</script>
<style lang="scss" scoped>
.m-table {
  height: 100%;
}
</style>
