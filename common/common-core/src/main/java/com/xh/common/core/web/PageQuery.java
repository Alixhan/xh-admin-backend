package com.xh.common.core.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xh.common.core.utils.CommonUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 页面查询
 * sunxh 2023/3/16
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PageQuery<T> {

    /**
     * 是否分页
     */
    private Boolean isPage = true;
    /**
     * 当前页
     */
    private int currentPage = 1;
    /**
     * 页面大小
     */
    private int pageSize = 20;
    /**
     * 可以携带自定义参数
     */
    private T param;
    /**
     * 高级组合查询
     */
    private List<FilterRow> filters = new ArrayList<>();

    /**
     * 排序字段
     */
    private String orderProp;

    /**
     * 排序方向
     */
    private OrderDirection orderDirection;

    /**
     * 查询基础sql
     */
    @Schema(hidden = true)
    private String baseSql;

    /**
     * 查询sql的占位符参数
     */
    @Schema(hidden = true)
    private LinkedList<Object> args = new LinkedList<>();

    /**
     * 尾部添加参数
     */
    public void addArg(Object... arg) {
        this.args.addAll(Arrays.asList(arg));
    }

    /**
     * 头部添加参数
     */
    public void addFirstArg(Object... arg) {
        this.args.addAll(0, Arrays.asList(arg));
    }

    @Schema(hidden = true)
    public String getSql() {
        var whereCon = getWhereCon(this.filters);
        if (CommonUtil.isNotEmpty(whereCon)) whereCon = " where " + whereCon;
        if (CommonUtil.isNotEmpty(orderProp) && CommonUtil.isNotEmpty(orderDirection)) {
            whereCon += " order by `%s` %s".formatted(CommonUtil.toLowerUnderscore(orderProp), orderDirection);
        }
        if (CommonUtil.isNotEmpty(whereCon)) {
            return "select * from (%s) QUERY %s".formatted(baseSql, whereCon);
        }
        return baseSql;
    }

    public String getWhereCon(List<FilterRow> filters) {
        if (filters == null) return "";
        StringBuilder whereCon = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            FilterRow filter = filters.get(i);
            String str = "";
            if (i != 0) {
                str += (" " + filter.getLogic() + " ");
            }
            List<FilterRow> children = filter.getChildren();
            if (children != null && !children.isEmpty()) {
                String childrenWhereCon = getWhereCon(children);
                if (!CommonUtil.isEmpty(childrenWhereCon)) {
                    whereCon.append(str).append("(").append(childrenWhereCon).append(") ");
                }
            } else {
                String prop = filter.getAlias();
                if (CommonUtil.isEmpty(prop)) {
                    prop = CommonUtil.toLowerUnderscore(filter.getProp());
                }
                ComparatorEnum condition = filter.getCondition();
                str +=  "`%s` ".formatted(prop);
                switch (condition) {
                    case eq:
                        str += "= " + filter.getValue(1);
                        break;
                    case ne:
                        str += "<> " + filter.getValue(1);
                        break;
                    case gt:
                        str += "> " + filter.getValue(1);
                        break;
                    case ge:
                        str += ">= " + filter.getValue(1);
                        break;
                    case lt:
                        str += "< " + filter.getValue(1);
                        break;
                    case le:
                        str += "<= " + filter.getValue(1);
                        break;
                    case in:
                        str += "in (" + filter.getInValues() + ")";
                        break;
                    case ct:
                        str += "like '%" + filter.getValue1() + "%'";
                        break;
                    case nct:
                        str += "not like '%" + filter.getValue1() + "%'";
                        break;
                    case bt:
                        str += "between " + filter.getValue(1) + " and " + filter.getValue(2);
                        break;
                }
                whereCon.append(str);
            }
        }
        return whereCon.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class FilterRow {
        private Boolean enabled;
        private String type;
        private String logic;
        private String prop;
        private String alias;
        private ComparatorEnum condition;
        private Object value1;
        private Object value2;
        private List<FilterRow> children;

        private Object getValue(int flag) {
            Object value = this.value1;
            if (flag == 2) {
                value = this.value2;
            }
            if (value == null) return "null";
            if ("number".equals(type) || value instanceof Boolean) {
                return value;
            }
            return "'" + value + "'";
        }

        private String getInValues() {
            String[] values = CommonUtil.getString(this.value1).split(",");
            StringBuilder sb = new StringBuilder();
            if ("number".equals(type)) {
                for (String value : values) {
                    sb.append(",").append(value);
                }
            } else {
                for (String value : values) {
                    sb.append(",'").append(value).append("'");
                }
            }
            return sb.substring(1);
        }
    }


    public enum ComparatorEnum {
        /**
         * 等于
         */
        eq,
        /**
         * 不等于
         */
        ne,
        /**
         * 大于
         */
        gt,
        /**
         * 大于等于
         */
        ge,
        /**
         * 小于
         */
        lt,
        /**
         * 小于等于
         */
        le,
        /**
         * 含哪几个值
         */
        in,
        /**
         * not like
         */
        nct,
        /**
         * like
         */
        ct,
        /**
         * between ... and ...
         */
        bt
    }

    /**
     * 排序方向
     */
    public enum OrderDirection {
        asc,
        desc
    }
}
