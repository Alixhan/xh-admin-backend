<template>
  <div class="form-view">
    <el-scrollbar class="m-form-scroll" max-height="75vh">
      <m-form ref="formRef" :columns="columns" :model="formData" :handleType="handleType" :loading="formLoading" />
    </el-scrollbar>
    <div class="m-footer">
      <el-button icon="close" @click="close()">{{ $t('common.cancel') }}</el-button>
      <template v-if="!formLoading">
        <el-button
          v-if="['add', 'edit'].includes(handleType)"
          v-auth="['${permissionPrefix}:add', '${permissionPrefix}:edit']"
          icon="check"
          type="primary"
          :loading="saveLoading"
          @click="save"
        >
          {{ $t('common.save') }}
        </el-button>
      </template>
    </div>
  </div>
</template>
<script setup lang="tsx">
import type { PropType } from 'vue'
import { ref, watchEffect } from 'vue'
import { ${getFun}, ${insertFun}, ${updateFun} } from '${apiPath}'
import { useI18n } from 'vue-i18n'
<#if hasDict?seq_contains('form')>
import useDictDetails from '@/utils/dict'
</#if>

const props = defineProps({
  handleType: {
    type: String as PropType<FormHandleType>,
    default: 'add'
  },
  modelValue: {
    type: Object as PropType<{ id: number }>
  }
})

const { t } = useI18n()

const emits = defineEmits<{
  (e: 'close', type: 'refresh' | string): void
}>()

const formRef = ref()
const formLoading = ref(false)
const saveLoading = ref(false)
const formData = ref({})

init()

async function init() {
  formLoading.value = true
  if (props.handleType !== 'add') {
    await ${getFun}(props.modelValue!.id!).then(res => {
      formData.value = res.data
    })
  }
  formLoading.value = false
}

// 表单列定义
const columns = ref<CommonFormColumn<typeof formData.value> []>([])
watchEffect(() => {
  columns.value = [
<#list columns as field>
<#if field.isForm!false>
    ${field.formColStr}<#if field_has_next>,</#if>
</#if>
</#list>
  ]
})

// 保存方法
function save() {
  formRef.value.submit().then(() => {
    const fun = props.handleType === 'add' ? ${insertFun} : ${updateFun}
    fun(formData.value, {
      loadingRef: saveLoading,
      showSuccessMsg: true,
      successMsg: t('common.saveSuccess')
    }).then(() => close('refresh'))
  })
}

function close(type?: any) {
  emits('close', type)
}
</script>
<style lang="scss" scoped>
.form-view {
  height: 100%;
  display: flex;
  flex-direction: column;

  .m-form-scroll {
    flex-grow: 1;
    padding-right: 10px;
    margin-right: -10px;
  }
}
</style>
