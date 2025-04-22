package ${servicePackage};

import com.xh.common.core.service.BaseServiceImpl;
<#if isDataPermission!false>
import com.xh.common.core.service.CommonService;
</#if>
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import ${entityPackage}.${entityName};
import ${dtoPackage}.${dtoName};
<#if isDataPermission!false>
import jakarta.annotation.Resource;
</#if>
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
<#if hasImport>
import org.springframework.transaction.interceptor.TransactionAspectSupport;
</#if>

import java.io.Serializable;
<#if hasImport>
import java.util.ArrayList;
</#if>
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ${name} Service
 *
 * @author ${author}
 * @since ${date}
 */
@Slf4j
@Service
public class ${serviceName} extends BaseServiceImpl {
<#if isDataPermission!false>
    @Resource
    private CommonService commonService;

</#if>
    /**
     * ${name}查询
     */
    @Transactional(readOnly = true)
    public PageResult<${entityName}> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        if (param == null) param = new HashMap<>();

        String sql = "select * from ${tableName} where 1=1<#if extend??> and deleted is false</#if> ";

    <#list columns as field>
    <#if field.isQuery!false>
        if (CommonUtil.isNotEmpty(param.get("${field.prop}"))) {
            sql += " ${field.querySql}";
            pageQuery.addArg(param.get("${field.prop}"));
        }
    </#if>
    </#list>

    <#if isDataPermission!false>
        // 数据权限
        String permissionSql = commonService.getPermissionSql("${tableName}", "create_by", "sys_role_id", "sys_org_id");
        if (CommonUtil.isNotEmpty(permissionSql)) {
            sql += " and %s".formatted(permissionSql);
        }

    </#if>
    <#if orderBy??>
        sql += " ${orderBy}";

    </#if>
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(${entityName}.class, pageQuery);
    }

    /**
     * ${name}新增
     */
    @Transactional
    public ${entityName} insert(${dtoName} ${dtoVarName}) {
        ${entityName} ${entityVarName} = new ${entityName}();
        BeanUtils.copyProperties(${dtoVarName}, ${entityVarName});
        baseJdbcDao.insert(${entityVarName});
        return ${entityVarName};
    }

    /**
     * ${name}修改
     */
    @Transactional
    public ${entityName} update(${dtoName} ${dtoVarName}) {
        ${entityName} ${entityVarName} = new ${entityName}();
        BeanUtils.copyProperties(${dtoVarName}, ${entityVarName});
        baseJdbcDao.update(${entityVarName});
        return ${entityVarName};
    }

    /**
     * id获取${name}详情
     */
    @Transactional(readOnly = true)
    public ${entityName} getById(${idJavaType} id) {
        return baseJdbcDao.findById(${entityName}.class, id);
    }

    /**
     * ids批量删除
     */
    @Transactional
    public void del(List<${idJavaType}> ids) {
        log.info("批量删除${name}--");
        <#if extend??>
        String sql = "update ${tableName} set deleted = 1 where ${idProp} in (:ids)";
        <#else>
        String sql = "delete from ${tableName} where ${idProp} in (:ids)";
        </#if>
        Map<String, Object> paramMap = new HashMap<>(){{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }
<#if hasImport>

    /**
     * ${name}导入
     */
    @Transactional
    public List<Map<String, Object>> imports(List<${dtoName}> data) {
        List<Map<String, Object>> res = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            try {
                insert(data.get(i));
            } catch (MyException e) {
                Map<String, Object> resMap = new HashMap<>();
                resMap.put("rowIndex", i);
                resMap.put("error", e.getMessage());
                res.add(resMap);
            }
        }
        //有错误信息直接手动回滚整个事务
        if (!res.isEmpty()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return res;
        }
        return null;
    }
</#if>
}
