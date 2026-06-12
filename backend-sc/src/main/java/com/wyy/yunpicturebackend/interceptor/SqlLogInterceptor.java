package com.wyy.yunpicturebackend.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
@Slf4j
@Component
public class SqlLogInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 统一提取参数
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];

        // 2. 针对不同的 query 方法提取 BoundSql
        BoundSql boundSql;
        if (args.length == 6) {
            // 如果是 6 参数的 query，直接拿现成的 boundSql，效率更高
            boundSql = (BoundSql) args[5];
        } else {
            // 否则手动生成
            boundSql = mappedStatement.getBoundSql(parameter);
        }

        Configuration configuration = mappedStatement.getConfiguration();
        long startTime = System.currentTimeMillis();

        Object result = null;
        try {
            result = invocation.proceed(); // 执行 SQL
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String traceId = MDC.get("traceId");
            if (traceId == null) traceId = "SYSTEM";

            // 计算条数（逻辑保持不变）
            String resultCount = "0";
            if (result instanceof Collection) {
                resultCount = String.valueOf(((Collection<?>) result).size());
            } else if (result instanceof Integer) {
                resultCount = String.valueOf(result);
            } else if (result != null) {
                resultCount = "1";
            }

            // 还原完整 SQL (使用你之前的 showSql 逻辑)
            String sql = showSql(configuration, boundSql);

            // 打印日志
            log.info("\n[SQL Log] [User: {}] [Trace: {}]\n" +
                            "Mapper: {}\n" +
                            "Time: {} ms | Results: {}\n" +
                            "SQL: {}\n" +
                            "--------------------------------------------------",
                    MDC.get("userId"), traceId, mappedStatement.getId(), duration, resultCount, sql);
        }
    }

    private String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    }
                }
            }
        }
        return sql;
    }

    private String getParameterValue(Object obj) {
        if (obj instanceof String) return "'" + obj + "'";
        if (obj instanceof Date) return "'" + DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA).format(obj) + "'";
        return obj != null ? obj.toString() : "null";
    }

    @Override
    public Object plugin(Object target) { return Plugin.wrap(target, this); }
}