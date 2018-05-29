/*
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.core.conditions;

import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.core.enums.SqlKeyword.*;


/**
 * <p>
 * 查询条件封装
 * </p>
 *
 * @author hubin
 * @since 2017-05-26
 */
public abstract class AbstractWrapper<T, R, This extends AbstractWrapper<T, R, This>> extends Wrapper<T> {

    private List<ISqlSegment> expression = new ArrayList<>();
    private static final String MP_GENERAL_PARAMNAME = "MPGENVAL";
    private final AtomicInteger paramNameSeq = new AtomicInteger(0);
    private final Map<String, Object> paramNameValuePairs = new HashMap<>();
    protected String paramAlias = null;
    private static final String DEFAULT_PARAM_ALIAS = "ew";

    /**
     * 占位符
     */
    private static final String PLACE_HOLDER = "{%s}";

    private static final String MYBATIS_PLUS_TOKEN = "#{%s.paramNameValuePairs.%s}";

    public abstract String columnToString(R column);

    public This apply(String condition) {
        expression.add(() -> condition);
        return typedThis();
    }

    public This notIn(String condition) {
        return not().in(condition);
    }

    /**
     * LIKE '%值%'
     */
    public This like(R column, Object val) {
        return like(true, column, val);
    }

    /**
     * LIKE '%值%'
     */
    public This like(boolean condition, R column, Object val) {
        return doIt(condition, () -> columnToString(column), LIKE, () -> "'%", () -> formatSql("{0}", val), () -> "%'");
    }

    /**
     * LIKE '%值'
     */
    public This likeLeft(R column, Object val) {
        return likeLeft(true, column, val);
    }

    /**
     * LIKE '%值'
     */
    public This likeLeft(boolean condition, R column, Object val) {
        return doIt(condition, () -> columnToString(column), LIKE, () -> "'%", () -> formatSql("{0}", val), () -> "'");
    }

    /**
     * LIKE '值%'
     */
    public This likeRight(R column, Object val) {
        return likeRight(true, column, val);
    }

    /**
     * LIKE '值%'
     */
    public This likeRight(boolean condition, R column, Object val) {
        return doIt(condition, () -> columnToString(column), LIKE, () -> "'", () -> formatSql("{0}", val), () -> "%'");
    }

    /**
     * 等于 =
     */
    public This eq(R column, Object val) {
        return eq(true, column, val);
    }

    /**
     * 等于 =
     */
    public This eq(boolean condition, R column, Object val) {
        return addCondition(condition, column, EQ, val);
    }

    /**
     * 不等于 <>
     */
    public This ne(R column, Object val) {
        return ne(true, column, val);
    }

    /**
     * 不等于 <>
     */
    public This ne(boolean condition, R column, Object val) {
        return addCondition(condition, column, NE, val);
    }

    /**
     * 大于 >
     */
    public This gt(R column, Object val) {
        return gt(true, column, val);
    }

    /**
     * 大于 >
     */
    public This gt(boolean condition, R column, Object val) {
        return addCondition(condition, column, GT, val);
    }

    /**
     * 大于等于 >=
     */
    public This ge(R column, Object val) {
        return ge(true, column, val);
    }

    /**
     * 大于等于 >=
     */
    public This ge(boolean condition, R column, Object val) {
        return addCondition(condition, column, GE, val);
    }

    /**
     * 小于 <
     */
    public This lt(R column, Object val) {
        return lt(true, column, val);
    }

    /**
     * 小于 <
     */
    public This lt(boolean condition, R column, Object val) {
        return addCondition(condition, column, LT, val);
    }

    /**
     * 小于等于 <=
     */
    public This le(R column, Object val) {
        return le(true, column, val);
    }

    /**
     * 小于等于 <=
     */
    public This le(boolean condition, R column, Object val) {
        return addCondition(condition, column, LE, val);
    }

    /**
     * BETWEEN 值1 AND 值2
     */
    public This between(R column, Object val1, Object val2) {
        return between(true, column, "val1", "val2");
    }

    /**
     * BETWEEN 值1 AND 值2
     */
    public This between(boolean condition, R column, Object val1, Object val2) {
        return doIt(condition, () -> columnToString(column), BETWEEN, () -> "val1", AND, () -> "val2");
    }

    /**
     * 字段 IS NULL
     */
    public This isNull(R column) {
        return isNull(true, column);
    }

    /**
     * 字段 IS NULL
     */
    public This isNull(boolean condition, R column) {
        return doIt(condition, () -> columnToString(column), IS_NULL);
    }

    /**
     * 字段 IS NOT NULL
     */
    public This isNotNull(R column) {
        return isNotNull(true, column);
    }

    /**
     * 字段 IS NOT NULL
     */
    public This isNotNull(boolean condition, R column) {
        return doIt(condition, () -> columnToString(column), IS_NOT_NULL);
    }

    /**
     * 分组：GROUP BY 字段, ...
     */
    public This groupBy(R column) {
        return doIt(true, GROUP_BY, () -> columnToString(column));
    }

    /**
     * 排序：ORDER BY 字段, ...
     */
    public This orderBy(R column) {//todo 产生的sql有bug
        return doIt(true, ORDER_BY, () -> columnToString(column));
    }

    /**
     * HAVING 关键词
     */
    public This having() {
        return doIt(true, HAVING);
    }

    /**
     * exists ( sql 语句 )
     */
    public This exists(String condition) {
        return this.addNestedCondition(condition, EXISTS);
    }

    /**
     * LAST 拼接在 SQL 末尾
     */
    public This last(String condition) {
        return doIt(true, () -> condition);
    }

    /**
     * NOT 关键词
     */
    protected This not() {//todo 待考虑
        return doIt(true, NOT);
    }

    public This and() {
        expression.add(AND);
        return typedThis();
    }

    public This and(Function<This, This> func) {
        return addNestedCondition(func, AND);
    }

    public This or(Function<This, This> func) {
        return addNestedCondition(func, OR);
    }

    public This in(String condition) {//todo 待动
        return addNestedCondition(condition, IN);
    }

    public This or(R column, Object val) {
        //todo 待动
        return addCondition(true, column, OR, val);
    }

    /**
     * <p>
     * 普通查询条件
     * </p>
     *
     * @param condition  是否执行
     * @param column     属性
     * @param sqlKeyword SQL 关键词
     * @param val        条件值
     * @return this
     */
    protected This addCondition(boolean condition, R column, SqlKeyword sqlKeyword, Object val) {
        return doIt(condition, () -> columnToString(column),
            sqlKeyword, () -> this.formatSql("{0}", val));
    }

    /**
     * <p>
     * 嵌套查询条件
     * </p>
     *
     * @param val        查询条件值
     * @param sqlKeyword SQL 关键词
     * @return this
     */
    protected This addNestedCondition(Object val, SqlKeyword sqlKeyword) {
        return doIt(true, sqlKeyword, () -> this.formatSql("({0})", val));
    }

    /**
     * <p>
     * 多重嵌套查询条件
     * </p>
     *
     * @param condition  查询条件值
     * @param sqlKeyword SQL 关键词
     * @return
     */
    protected This addNestedCondition(Function<This, This> condition, SqlKeyword sqlKeyword) {
//        return doIt(true, sqlKeyword, () -> "(",
//            condition.apply(instance(paramNameValuePairs, paramNameSeq)), () -> ")");
        return null;//todo 待处理
    }

    /**
     * <p>
     * 格式化SQL
     * </p>
     *
     * @param sqlStr SQL语句部分
     * @param params 参数集
     * @return this
     */
    protected String formatSql(String sqlStr, Object... params) {
        return formatSqlIfNeed(true, sqlStr, params);
    }

    /**
     * <p>
     * 根据需要格式化SQL<BR>
     * <BR>
     * Format SQL for methods: EntityQ<T>.where/and/or...("name={0}", value);
     * ALL the {<b>i</b>} will be replaced with #{MPGENVAL<b>i</b>}<BR>
     * <BR>
     * ew.where("sample_name=<b>{0}</b>", "haha").and("sample_age &gt;<b>{0}</b>
     * and sample_age&lt;<b>{1}</b>", 18, 30) <b>TO</b>
     * sample_name=<b>#{MPGENVAL1}</b> and sample_age&gt;#<b>{MPGENVAL2}</b> and
     * sample_age&lt;<b>#{MPGENVAL3}</b><BR>
     * </p>
     *
     * @param need   是否需要格式化
     * @param sqlStr SQL语句部分
     * @param params 参数集
     * @return this
     */
    protected String formatSqlIfNeed(boolean need, String sqlStr, Object... params) {
        if (!need || StringUtils.isEmpty(sqlStr)) {
            return null;
        }
        if (ArrayUtils.isNotEmpty(params)) {
            for (int i = 0; i < params.length; ++i) {
                String genParamName = MP_GENERAL_PARAMNAME + paramNameSeq.incrementAndGet();
                sqlStr = sqlStr.replace(String.format(PLACE_HOLDER, i),
                    String.format(MYBATIS_PLUS_TOKEN, getParamAlias(), genParamName));
                paramNameValuePairs.put(genParamName, params[i]);
            }
        }
        return sqlStr;
    }

    /**
     * <p>
     * 对sql片段进行组装
     * </p>
     *
     * @param condition   是否执行
     * @param sqlSegments sql片段数组
     * @return this
     */
    protected This doIt(boolean condition, ISqlSegment... sqlSegments) {
        if (condition) {
            expression.addAll(Arrays.asList(sqlSegments));
        }
        return typedThis();
    }

    public String getParamAlias() {
        return StringUtils.isEmpty(paramAlias) ? DEFAULT_PARAM_ALIAS : paramAlias;
    }

    @SuppressWarnings("unchecked")
    protected This typedThis() {
        return (This) this;
    }

    @Override
    public String getSqlSegment() {
        return String.join(" ", expression.stream()
            .map(ISqlSegment::getSqlSegment)
            .collect(Collectors.toList()));
    }

}