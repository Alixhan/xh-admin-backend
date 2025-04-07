package com.xh.generator.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.base.CaseFormat;
import com.xh.common.core.dto.OnlineUserDTO;
import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.generator.client.dto.GenTableColumnDTO;
import com.xh.generator.client.entity.GenTable;
import com.xh.generator.client.vo.GenCodeResult;
import com.xh.generator.client.vo.GenTableVO;
import com.xh.generator.client.vo.TableColumnMateDataVO;
import com.xh.generator.client.vo.TableMateDataVO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成service
 * sunxh 2023/4/22
 */
@Service
@Slf4j
public class CodeGenService extends BaseServiceImpl {
    /**
     * 系统代码生成查询
     */
    @Transactional(readOnly = true)
    public PageResult<GenTable> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select id, table_name, table_comment, entity_name, name, service, module, author, " +
                     " create_time " +
                     "from gen_table a where deleted is false ";
        if (CommonUtil.isNotEmpty(param.get("tableName"))) {
            sql += " and table_name like '%' ? '%'";
            pageQuery.addArg(param.get("tableName"));
        }
        if (CommonUtil.isNotEmpty(param.get("tableComment"))) {
            sql += " and table_comment like '%' ? '%'";
            pageQuery.addArg(param.get("tableComment"));
        }
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and name like '%' ? '%'";
            pageQuery.addArg(param.get("name"));
        }
        if (CommonUtil.isNotEmpty(param.get("service"))) {
            sql += " and service like '%' ? '%'";
            pageQuery.addArg(param.get("service"));
        }
        if (CommonUtil.isNotEmpty(param.get("module"))) {
            sql += " and module like '%' ? '%'";
            pageQuery.addArg(param.get("module"));
        }
        if (CommonUtil.isNotEmpty(param.get("author"))) {
            sql += " and author like '%' ? '%'";
            pageQuery.addArg(param.get("author"));
        }
        sql += " order by create_time desc";
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(GenTable.class, pageQuery);
    }

    @Transactional
    public GenTable save(GenTable genTable) {
        List<GenTableColumnDTO> columns = genTable.getColumns();
        genTable.setColumnsJson(JSONArray.from(columns).toJSONString());
        if (genTable.getId() == null) baseJdbcDao.insert(genTable);
        else baseJdbcDao.update(genTable);
        return genTable;
    }

    /**
     * id获取代码生成详情
     */
    @Transactional(readOnly = true)
    public GenTable getById(Serializable id) {
        GenTable genTable = baseJdbcDao.findById(GenTable.class, id);
        String columnsJson = genTable.getColumnsJson();
        List<GenTableColumnDTO> columns = JSONArray.parseArray(columnsJson, GenTableColumnDTO.class);
        genTable.setColumns(columns);
        genTable.setColumnsJson(null);
        return genTable;
    }

    /**
     * 批量id删除代码生成
     */
    public void del(List<Integer> ids) {
        log.info("批量id删除代码生成--");
        String sql = "delete from gen_table where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }

    /**
     * 获取表字段详情
     */
    public TableMateDataVO getTableDetail(Map<String, Object> param) throws SQLException {
        String tableName = CommonUtil.getString(param.get("tableName"));
        try (Connection connection = Objects.requireNonNull(primaryJdbcTemplate.getDataSource()).getConnection()) {
            DatabaseMetaData dbMetaData = connection.getMetaData();
            List<TableColumnMateDataVO> columns = new ArrayList<>();
            String tableSchema = connection.getSchema();

            TableMateDataVO tableMateDataVO = this.getTableList(tableName).getFirst();

            // 查询表主键信息
            ResultSet rs = dbMetaData.getPrimaryKeys(connection.getCatalog(), tableSchema, tableName);
            Set<String> pks = new HashSet<>();
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");//列名
                pks.add(columnName);
            }

            // 表的字段信息
            ResultSet tableResult = dbMetaData.getColumns(connection.getCatalog(), tableSchema, tableName, null);

            while (tableResult.next()) {
                JSONObject column = new JSONObject();
                ResultSetMetaData metaData = tableResult.getMetaData();
                // 是否主键
                if (pks.contains(tableResult.getString("COLUMN_NAME"))) {
                    column.put("primaryKey", true);
                }
                int columnCount = metaData.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    String columnLabel = metaData.getColumnLabel(i + 1);
                    Object object = tableResult.getObject(columnLabel);
                    column.put(CommonUtil.toLowerCamel(columnLabel), object);
                }
                TableColumnMateDataVO tableColumnMateDataVO = column.to(TableColumnMateDataVO.class);
                columns.add(tableColumnMateDataVO);
            }
            tableMateDataVO.setColumns(columns);
            return tableMateDataVO;
        }
    }

    /**
     * 获取所有表信息
     */
    public List<TableMateDataVO> getTableList(String tableNamePattern) {
        try (Connection connection = Objects.requireNonNull(primaryJdbcTemplate.getDataSource()).getConnection();) {
            DatabaseMetaData dbMetaData = connection.getMetaData();
            List<TableMateDataVO> tables = new ArrayList<>();
            String tableSchema = connection.getSchema();

            // 获取表列表
            ResultSet resultSet = dbMetaData.getTables(connection.getCatalog(), tableSchema, tableNamePattern, new String[]{"TABLE"});
            while (resultSet.next()) {
                JSONObject tableInfo = new JSONObject();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    String columnLabel = metaData.getColumnLabel(i + 1);
                    Object object = resultSet.getObject(columnLabel);
                    tableInfo.put(CommonUtil.toLowerCamel(columnLabel), object);
                }
                TableMateDataVO tableMateDataVO = tableInfo.to(TableMateDataVO.class);
                tables.add(tableMateDataVO);
            }
            return tables;
        } catch (SQLException e) {
            throw new MyException(e);
        }
    }

    @Resource
    Configuration configuration;

    /**
     * 代码生成
     */
    @Transactional
    public void generate(Serializable id) throws IOException, TemplateException {
        GenTableVO vo = this.getGenTableVO(getById(id));
        if (CommonUtil.isEmpty(vo.getFrontendPath())) {
            throw new MyException("前端项目路径未配置！");
        }
        if (CommonUtil.isEmpty(vo.getBackendPath())) {
            throw new MyException("后端项目路径未配置！");
        }
        List<GenCodeResult> results = this.genCodeResults(vo);
        for (GenCodeResult result : results) {
            if (CommonUtil.isNotEmpty(result.getErrorMsg())) {
                throw new MyException(result.getErrorMsg());
            }
        }
        for (GenCodeResult result : results) {
            if ("1".equals(vo.getDesignType()) && "sql".equals(result.getType())) {
                primaryJdbcTemplate.execute(result.getCode());
            } else {
                new File(result.getAbsoluteDirPath()).mkdirs();
                File file = new File(result.getAbsolutePath());
                file.createNewFile();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(result.getCode().getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    /**
     * 代码zip下载
     */
    @Transactional(readOnly = true)
    public void getCodeZipFile(Serializable id, OutputStream outputStream) throws TemplateException, IOException {
        GenTableVO vo = this.getGenTableVO(getById(id));
        List<GenCodeResult> results = this.genCodeResults(vo).stream().filter(i -> CommonUtil.isNotEmpty(i.getZipPath())).toList();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (GenCodeResult result : results) {
                ZipEntry entry = new ZipEntry(result.getZipPath());
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(result.getCode().getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
    }

    /**
     * 获取代码结果集合
     */
    public List<GenCodeResult> genCodeResults(GenTableVO vo) throws TemplateException, IOException {
        List<GenCodeResult> results = new ArrayList<>();
        results.add(this.genCodeResult("frontend/api.ts.ftl", vo));
        results.add(this.genCodeResult("frontend/form.vue.ftl", vo));
        results.add(this.genCodeResult("frontend/index.vue.ftl", vo));
        if (Boolean.TRUE.equals(vo.getHasImport())) {
            results.add(this.genCodeResult("frontend/import.vue.ftl", vo));
        }
        results.add(this.genCodeResult("backend/Controller.java.ftl", vo));
        results.add(this.genCodeResult("backend/DTO.java.ftl", vo));
        results.add(this.genCodeResult("backend/Entity.java.ftl", vo));
        results.add(this.genCodeResult("backend/Service.java.ftl", vo));
        results.add(this.genCodeResult("sql/createTable.sql.ftl", vo));
        if (Boolean.TRUE.equals(vo.getIsCreateMenu())) {
            results.add(this.genCodeResult("sql/createSysMenu.sql.ftl", vo));
        }
        if (Boolean.TRUE.equals(vo.getIsDataPermission())) {
            results.add(this.genCodeResult("sql/createSysDataEntity.sql.ftl", vo));
        }
        return results;
    }

    /**
     * 生成源码结果
     */
    public GenCodeResult genCodeResult(String templatePath, GenTableVO vo) throws IOException, TemplateException {
        GenCodeResult genCodeResult = new GenCodeResult();
        String type = templatePath.split("/")[0];
        genCodeResult.setTemplatePath(templatePath);
        String bcp = "%s/client/src/main/java".formatted(vo.getService());
        String bsp = "%s/service/src/main/java".formatted(vo.getService());
        String bfa = Stream.of("src/api", vo.getService(), vo.getModule()).filter(CommonUtil::isNotEmpty).collect(Collectors.joining("/"));
        String bfv = Stream.of("src/views", vo.getService(), vo.getModule(), vo.getEntityVarName()).filter(CommonUtil::isNotEmpty).collect(Collectors.joining("/"));
        var middleStr = "";
        if ("backend/Controller.java.ftl".equals(templatePath)) {
            genCodeResult.setFileName("%sController.java".formatted(vo.getEntityName()));
            middleStr = "%s/%s".formatted(bsp, vo.getControllerPackage().replaceAll("\\.", "/"));
        }
        if ("backend/DTO.java.ftl".equals(templatePath)) {
            genCodeResult.setFileName(vo.getDtoName() + ".java");
            middleStr = "%s/%s".formatted(bcp, vo.getDtoPackage().replaceAll("\\.", "/"));
        }
        if ("backend/Entity.java.ftl".equals(templatePath)) {
            genCodeResult.setFileName(vo.getEntityName() + ".java");
            middleStr = "%s/%s".formatted(bcp, vo.getEntityPackage().replaceAll("\\.", "/"));
        }
        if ("backend/Service.java.ftl".equals(templatePath)) {
            genCodeResult.setFileName(vo.getServiceName() + ".java");
            middleStr = "%s/%s".formatted(bsp, vo.getServicePackage().replaceAll("\\.", "/"));
        }
        if ("frontend/api.ts.ftl".equals(templatePath)) {
            genCodeResult.setFileName(vo.getEntityVarName() + ".ts");
            middleStr = bfa;
        }
        if ("frontend/form.vue.ftl".equals(templatePath)) {
            genCodeResult.setFileName(vo.getEntityVarName() + "Form.vue");
            middleStr = bfv;
        }
        if ("frontend/import.vue.ftl".equals(templatePath)) {
            genCodeResult.setFileName(vo.getEntityVarName() + "Import.vue");
            middleStr = bfv;
        }
        if ("frontend/index.vue.ftl".equals(templatePath)) {
            genCodeResult.setFileName("index.vue");
            middleStr = bfv;
        }
        if ("sql/createTable.sql.ftl".equals(templatePath)) {
            List<TableMateDataVO> tableList = getTableList(vo.getTableName());
            if (!tableList.isEmpty()) {
                genCodeResult.setErrorMsg("数据表 %s 已存在！".formatted(vo.getTableName()));
            }
            genCodeResult.setFileName("createTable.sql");
        }
        if ("sql/createSysMenu.sql.ftl".equals(templatePath)) {
            String sql2 = "select count(1) from sys_menu where deleted is false and name = ?";
            Integer count = primaryJdbcTemplate.queryForObject(sql2, Integer.class, vo.getPermissionPrefix());
            if (count == null || count > 0) {
                genCodeResult.setErrorMsg("系统菜单name “%s” 已存在！".formatted(vo.getPermissionPrefix()));
            }
            String sql = "select max(id) from sys_menu";
            Integer menuId = primaryJdbcTemplate.queryForObject(sql, Integer.class);
            if (menuId == null) menuId = 0;
            menuId++;
            vo.setMenuId(menuId);
            genCodeResult.setFileName("createSysMenu.sql");
        }
        if ("sql/createSysDataEntity.sql.ftl".equals(templatePath)) {
            String sql = "select * from sys_data_entity where id = ?";
            List<Map<String, Object>> list = primaryJdbcTemplate.queryForList(sql, vo.getTableName());
            if (!list.isEmpty()) {
                genCodeResult.setErrorMsg("系统数据实体 “%s” 已存在！".formatted(vo.getTableName()));
            }
            genCodeResult.setFileName("createSysDataEntity.sql");
        }
        if ("frontend".equals(type)) {
            genCodeResult.setZipPath("%s/%s/%s".formatted("frontend", middleStr, genCodeResult.getFileName()));
            genCodeResult.setAbsolutePath("%s/%s/%s".formatted(vo.getFrontendPath(), middleStr, genCodeResult.getFileName()));
            genCodeResult.setAbsoluteDirPath("%s/%s".formatted(vo.getFrontendPath(), middleStr));
        } else if ("backend".equals(type)) {
            genCodeResult.setZipPath("%s/%s/%s".formatted("backend", middleStr, genCodeResult.getFileName()));
            genCodeResult.setAbsolutePath("%s/%s/%s".formatted(vo.getBackendPath(), middleStr, genCodeResult.getFileName()));
            genCodeResult.setAbsoluteDirPath("%s/%s".formatted(vo.getBackendPath(), middleStr));
        } else if ("sql".equals(type)) {
            genCodeResult.setZipPath("%s/%s".formatted("sql", genCodeResult.getFileName()));
        }
        if (CommonUtil.isNotEmpty(genCodeResult.getAbsolutePath())) {
            File file = new File(genCodeResult.getAbsolutePath());
            if (file.exists()) {
                genCodeResult.setErrorMsg("目标文件\"%s\"已存在！".formatted(genCodeResult.getAbsolutePath()));
            }
        }
        genCodeResult.setCode(genCode(templatePath, vo));
        genCodeResult.setType(type);
        return genCodeResult;
    }

    public GenTableVO getGenTableVO(GenTable genTable) {
        GenTableVO vo = new GenTableVO();
        OnlineUserDTO onlineUserInfo = LoginUtil.getOnlineUserInfo();
        vo.setUserId(onlineUserInfo.getUserId());
        BeanUtils.copyProperties(genTable, vo);
        String packageFormat = Stream.of(
                "com.xh",
                vo.getService(),
                "%s",
                CommonUtil.getString(vo.getModule().replaceAll("/", "."))
        ).filter(CommonUtil::isNotEmpty).collect(Collectors.joining("."));

        vo.setDate(LocalDate.now().toString());
        vo.setControllerPackage(packageFormat.formatted("controller"));
        vo.setEntityPackage(packageFormat.formatted("client.entity"));
        vo.setEntityVarName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, vo.getEntityName()));
        if (CommonUtil.isNotEmpty(vo.getExtend())) {
            vo.setEntityExtendClass(" extends %s<Integer>".formatted(vo.getExtend()));
            vo.setDtoExtendClass(" extends %sDTO<Integer>".formatted(vo.getExtend()));
        }
        vo.setServicePackage(packageFormat.formatted("service"));
        vo.setServiceName(vo.getEntityName() + "Service");
        vo.setServiceVarName(vo.getEntityVarName() + "Service");
        vo.setPermissionPrefix(
                Stream.of(vo.getService(), CommonUtil.getString(vo.getModule()).replaceAll("/", ""), vo.getEntityVarName())
                        .filter(CommonUtil::isNotEmpty).collect(Collectors.joining(":"))
        );
        vo.setDtoPackage(packageFormat.formatted("client.dto"));
        vo.setDtoName(vo.getEntityName() + "DTO");
        vo.setDtoVarName(vo.getEntityVarName() + "DTO");
        vo.setQueryFun("query%sList".formatted(vo.getEntityName()));
        vo.setSaveFun("postSave%s".formatted(vo.getEntityName()));
        vo.setGetFun("get%sById".formatted(vo.getEntityName()));
        vo.setDelFun("del%sByIds".formatted(vo.getEntityName()));
        vo.setImportFun(vo.getEntityVarName() + "Import");
        vo.setMappingPath(Stream.of("/api", vo.getService(), vo.getModule(), vo.getEntityVarName()).filter(CommonUtil::isNotEmpty).collect(Collectors.joining("/")));
        vo.setApiPath("@" + vo.getMappingPath());
        vo.setIndexViewPath(Stream.of("/src/views", vo.getService(), vo.getModule(), vo.getEntityVarName(), "index.vue").filter(CommonUtil::isNotEmpty).collect(Collectors.joining("/")));
        this.genColumns(vo);
        GenTableColumnDTO colDTO = vo.getColumns().stream().filter(i -> Boolean.TRUE.equals(i.getPrimaryKey())).findFirst().get();
        vo.setOrderBy(" order by %s desc ".formatted(colDTO.getColumnName()));
        vo.setIdProp(colDTO.getProp());
        vo.setPrimaryKeyGet("get%s".formatted(CommonUtil.toUpperCamel(vo.getIdProp())));
        vo.setHasImport(vo.getColumns().stream().anyMatch(i -> Boolean.TRUE.equals(i.getIsImport())));
        vo.setHasId(vo.getColumns().stream().filter(i -> !Boolean.TRUE.equals(i.getIsExtend())).anyMatch(i -> Boolean.TRUE.equals(i.getPrimaryKey())));
        vo.setHasLocalDate(vo.getColumns().stream().filter(i -> !Boolean.TRUE.equals(i.getIsExtend())).anyMatch(i -> "LocalDate".equals(i.getJavaType())));
        vo.setHasLocalDateTime(vo.getColumns().stream().filter(i -> !Boolean.TRUE.equals(i.getIsExtend())).anyMatch(i -> "LocalDateTime".equals(i.getJavaType())));
        vo.setHasBigDecimal(vo.getColumns().stream().filter(i -> !Boolean.TRUE.equals(i.getIsExtend())).anyMatch(i -> "BigDecimal".equals(i.getJavaType())));

        for (GenTableColumnDTO column : vo.getColumns()) {
            this.genQueryColStr(column, vo);
            this.genTableColStr(column, vo);
            this.genFormColStr(column, vo);
            this.genImportColStr(column, vo);
            this.genSqlColStr(column, vo);
        }
        return vo;
    }

    /**
     * 如果继承了基础实体类，则需要添加这些基础列
     */
    public void genColumns(GenTableVO vo) {
        var columns = vo.getColumns();
        if (CommonUtil.isNotEmpty(vo.getExtend())) {
            if (columns.stream().noneMatch(i -> "id".equals(i.getColumnName()))) {
                var col = new GenTableColumnDTO("number", "id", "ID", true, "auto", "id", "int", "ID", false, true, true, "Integer", null, true);
                columns.addFirst(col);
            }
            if ("DataPermissionEntity".equals(vo.getExtend())) {
                if (columns.stream().noneMatch(i -> "sys_org_id".equals(i.getColumnName()))) {
                    var col = new GenTableColumnDTO("number", "sysOrgId", "机构ID", null, null, "sys_org_id", "int", "机构ID", false, null, null, "Integer", null, true);
                    columns.addLast(col);
                }
                if (columns.stream().noneMatch(i -> "sys_role_id".equals(i.getColumnName()))) {
                    var col = new GenTableColumnDTO("number", "sysRoleId", "角色ID", null, null, "sys_role_id", "int", "角色ID", false, null, null, "Integer", null, true);
                    columns.addLast(col);
                }
            }
            if (columns.stream().noneMatch(i -> "create_time".equals(i.getColumnName()))) {
                var col = new GenTableColumnDTO("datetime", "createTime", "创建时间", false, null, "create_time", "datetime", "创建时间", true, true, true, "LocalDateTime", null, true);
                columns.addLast(col);
            }
            if (columns.stream().noneMatch(i -> "update_time".equals(i.getColumnName()))) {
                var col = new GenTableColumnDTO("datetime", "updateTime", "修改时间", false, null, "update_time", "datetime", "修改时间", false, false, false, "LocalDateTime", null, true);
                columns.addLast(col);
            }
            if (columns.stream().noneMatch(i -> "create_by".equals(i.getColumnName()))) {
                var col = new GenTableColumnDTO("number", "createBy", "创建人", false, null, "create_by", "int", "创建人", true, true, true, "Integer", null, true);
                columns.addLast(col);
            }
            if (columns.stream().noneMatch(i -> "update_by".equals(i.getColumnName()))) {
                var col = new GenTableColumnDTO("number", "updateBy", "修改人", false, null, "update_by", "int", "修改人", false, false, false, "Integer", null, true);
                columns.addLast(col);
            }
            if (columns.stream().noneMatch(i -> "deleted".equals(i.getColumnName()))) {
                var col = new GenTableColumnDTO("select", "deleted", "是否已删除", false, null, "deleted", "bit", "是否已删除", false, false, null, "Boolean", 1, true);
                columns.addLast(col);
            }
        }
    }

    public void genQueryColStr(GenTableColumnDTO col, GenTableVO vo) {
        if (Boolean.TRUE.equals(col.getIsQuery())) {
            var querySql = switch (col.getJavaType()) {
                case "String" -> " and " + col.getColumnName() + " like '%' ? '%'";
                default -> "and " + col.getColumnName() + " = ?";
            };
            col.setQuerySql(querySql);

            JSONObject json = new JSONObject();
            json.put("prop", col.getProp());
            json.put("label", col.getLabel());
            if (Arrays.asList("date", "datetime", "number", "rate").contains(col.getFormType())) {
                json.put("type", col.getFormType());
            }
            if (Arrays.asList("radio-group", "checkbox-group", "select").contains(col.getFormType())) {
                json.put("type", "select");
                json.put("itemList", "useDictDetails(%s)".formatted(col.getDictType()));
                vo.getHasDict().add("index");
            }
            this.transition(col, json);
            this.genRules(col, json, "query");
            col.setQueryColStr(this.genColCode(json));
        }
    }

    public void genTableColStr(GenTableColumnDTO col, GenTableVO vo) {
        if (Boolean.TRUE.equals(col.getIsTable())) {
            JSONObject json = new JSONObject();
            json.put("prop", col.getProp());
            json.put("label", col.getLabel());
            if (Arrays.asList("radio-group", "checkbox-group", "select").contains(col.getFormType())) {
                json.put("type", "select");
                json.put("itemList", "useDictDetails(%s)".formatted(col.getDictType()));
                vo.getHasDict().add("index");
            } else {
                json.put("type", col.getFormType());
            }
            if (Boolean.FALSE.equals(col.getIsExport())) json.put("notExport", true);
            this.transition(col, json);
            col.setTableColStr(this.genColCode(json));
        }
    }

    public void genFormColStr(GenTableColumnDTO col, GenTableVO vo) {
        if (Boolean.TRUE.equals(col.getIsForm())) {
            JSONObject json = new JSONObject();
            json.put("prop", col.getProp());
            json.put("label", col.getLabel());
            json.put("type", col.getFormType());
            this.transition(col, json);
            this.genRules(col, json, "form");
            if (Arrays.asList("radio-group", "checkbox-group", "select").contains(col.getFormType())) {
                json.put("itemList", "useDictDetails(%s)".formatted(col.getDictType()));
                vo.getHasDict().add("form");
            }
            col.setFormColStr(this.genColCode(json));
        }
    }

    public void genImportColStr(GenTableColumnDTO col, GenTableVO vo) {
        if (Boolean.TRUE.equals(col.getIsImport())) {
            vo.setHasImport(true);
            JSONObject json = new JSONObject();
            json.put("prop", col.getProp());
            json.put("label", col.getLabel());
            if (Arrays.asList("radio-group", "select").contains(col.getFormType())) {
                json.put("type", "select");
                json.put("itemList", "useDictDetails(%s)".formatted(col.getDictType()));
                if (col.getRules() == null) col.setRules(new ArrayList<>());
                col.getRules().add("itemList");
                vo.getHasDict().add("import");
            }
            this.transition(col, json);
            this.genRules(col, json, "import");
            col.setImportColStr(this.genColCode(json));
        }
    }

    public void genRules(GenTableColumnDTO col, JSONObject object, String type) {
        if (col.getRules() != null && !col.getRules().isEmpty()) {
            var rules = col.getRules().stream().map(i -> {
                JSONObject json = new JSONObject();
                if ("required".equals(i)) {
                    if (!"query".equals(type)) json.put("required", true);
                } else if ("itemList".equals(i)) {
                    json.put("itemList", "useDictDetails(%s)".formatted(col.getDictType()));
                } else json.put("type", i);
                return json;
            }).collect(Collectors.toSet());
            object.put("rules", rules);
        }
    }

    public void transition(GenTableColumnDTO col, JSONObject object) {
        if (Arrays.asList("int", "double", "float").contains(col.getFormType())) {
            object.put("type", "number");
        } else if (List.of("rate").contains(col.getFormType())) {
            object.put("type", "text");
        }
    }

    public void genSqlColStr(GenTableColumnDTO column, GenTableVO vo) {
        if (!Boolean.TRUE.equals(column.getIsVirtual())) {
            var colType = column.getColType();
            String sizeStr = Stream.of(column.getColumnSize(), column.getDecimalDigits()).map(CommonUtil::getString).filter(CommonUtil::isNotEmpty).collect(Collectors.joining(","));
            if (CommonUtil.isNotEmpty(sizeStr)) colType += "(%s)".formatted(sizeStr);
            var str = "`%s` %s comment '%s'".formatted(column.getColumnName(), colType, column.getLabel());
            if (Boolean.TRUE.equals(column.getPrimaryKey())) {
                str += " not null primary key";
                if ("auto".equals(column.getPrimaryKeyType())) str += " auto_increment";
            }
            column.setSqlColStr(str);
        }
    }

    /**
     * 生成列代码
     */
    public String genColCode(JSONObject object) {
        String jsonString = object.toJSONString();
        Pattern pattern = Pattern.compile("\"([a-zA-Z0-9]+)\":");
        Matcher matcher = pattern.matcher(jsonString);
        StringBuilder str = new StringBuilder();
        while (matcher.find()) {
            String prop = matcher.group(1);
            matcher.appendReplacement(str, prop + ": ");
        }
        matcher.appendTail(str);

        Pattern pattern2 = Pattern.compile("\"(useDictDetails\\([0-9]+\\))\"");
        Matcher matcher2 = pattern2.matcher(str);
        StringBuilder str2 = new StringBuilder();
        while (matcher2.find()) {
            String prop = matcher2.group(1);
            matcher2.appendReplacement(str2, prop);
        }
        matcher2.appendTail(str2);
        return str2.toString().replaceAll("\"", "'");
    }

    /**
     * freemarker 模板代码生成
     */
    public String genCode(String templatePath, GenTableVO vo) throws IOException, TemplateException {
        StringWriter writer = new StringWriter();
        Template template = configuration.getTemplate(templatePath);
        template.process(vo, writer);
        return writer.toString();
    }
}
