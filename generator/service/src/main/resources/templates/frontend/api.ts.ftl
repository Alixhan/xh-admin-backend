import createAxios from '@/utils/request'

const baseUrl = import.meta.env.VITE_${service?upper_case}_BASE_URL

// ${name}列表查询
export function ${queryFun}(params: PageQuery, option?: RequestOption) {
    return createAxios(option).post(`${r'${baseUrl}'}${mappingPath}/query`, params)
}

// 新增${name}
export function ${insertFun}(params = {}, option?: RequestOption) {
    return createAxios(option).post(`${r'${baseUrl}'}${mappingPath}/insert`, params)
}

// 修改${name}
export function ${updateFun}(params = {}, option?: RequestOption) {
    return createAxios(option).put(`${r'${baseUrl}'}${mappingPath}/update`, params)
}

// 获取${name}详情
export function ${getFun}(id: number) {
    return createAxios().get(`${r'${baseUrl}'}${mappingPath}/get/${r'${id}'}`)
}

// 批量删除${name}
export function ${delFun}(ids: string, option?: RequestOption) {
    return createAxios(option).delete(`${r'${baseUrl}'}${mappingPath}/del`, { params: { ids } })
}
<#if hasImport>

// ${name}导入
export function ${importFun}(params: object[], option?: RequestOption) {
    return createAxios(option).post(`${r'${baseUrl}'}${mappingPath}/imports`, params)
}
</#if>
