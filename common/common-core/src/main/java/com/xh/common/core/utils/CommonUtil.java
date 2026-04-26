package com.xh.common.core.utils;

import com.google.common.base.CaseFormat;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 通用工具类
 * sunxh 2023/4/16
 */
@Slf4j
public class CommonUtil {
    // 全局ip查询对象
    final static Ip2Region ip2Region = initIpRegionSearch();

    /**
     * 获取字符串，null返回空串
     */
    public static String getString(Object object) {
        return object == null ? "" : object.toString();
    }

    /**
     * 判断不为null且不为为空串
     */
    public static boolean isNotEmpty(Object object) {
        return !isEmpty(object);
    }

    /**
     * 判断为null或者为空串
     */
    public static boolean isEmpty(Object object) {
        return object == null || object.toString().isEmpty();
    }

    /**
     * 驼峰转小写下划线
     */
    public static String toLowerUnderscore(String str) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
    }

    /**
     * 驼峰转大写下划线
     */
    public static String toUpperUnderscore(String str) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, str);
    }

    /**
     * 下划线转小写驼峰
     */
    public static String toLowerCamel(String str) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, str);
    }

    /**
     * 下划线转大写驼峰
     */
    public static String toUpperCamel(String str) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str);
    }

    /**
     * 获取所有的field包括父级继承的，子类会覆盖父类的field
     */
    public static Collection<Field> getAllFields(Class<?> clazz) {
        if (clazz != null) {
            LinkedList<Class<?>> list = new LinkedList<>();

            Class<?> currentClass = clazz;
            while (currentClass != null) {
                list.addFirst(currentClass);
                currentClass = currentClass.getSuperclass();
            }
            Map<String, Field> fieldMap = new LinkedHashMap<>();
            for (Class<?> aClass : list) {
                Field[] declaredFields = aClass.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    fieldMap.put(declaredField.getName(), declaredField);
                }
            }
            return fieldMap.values();
        }
        return null;
    }

    /**
     * 获取field包括父级继承的，子类会覆盖父类的field
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz != null) {
            Class<?> currentClass = clazz;
            while (currentClass != null) {
                try {
                    return currentClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    currentClass = currentClass.getSuperclass();
                }
            }
        }
        return null;
    }

    /**
     * 获取文件的后缀名
     */
    public static String getFileSuffix(String filename) {
        if (filename == null) return null;
        int index = filename.lastIndexOf(".");
        if (index == -1) return null;
        return filename.substring(index + 1);
    }

    /**
     * 获取文件摘要sha1
     */
    public static String getFileSha1(InputStream inputStream) {
        try (inputStream) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[1024 * 1024 * 10];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
            StringBuilder sha1 = new StringBuilder(new BigInteger(1, digest.digest()).toString(16));
            int length = 40 - sha1.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    sha1.insert(0, "0");
                }
            }
            return sha1.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回异常的堆栈文本信息
     */
    public static String getThrowString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw);
        throwable.printStackTrace(printWriter);
        return sw.toString();
    }


    /**
     * 离线解析ip地址，支持ipv4和ipv6
     */
    public static String getIpRegion2(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip)) return null;
        try {
            return ip2Region.search(ip).replace("0", "");
        } catch (Exception e) {
            log.error("解析ip属地异常", e);
            return "";
        }
    }


    public static Ip2Region initIpRegionSearch() {
        try {

            InputStream v4InputStream = new ClassPathResource("/ip2region_v4.xdb").getInputStream();
            // 1, 创建 v4 的配置：指定缓存策略和 v4 的 xdb 文件路径
            final Config v4Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)     // 指定缓存策略:  NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                    // .setCacheSliceBytes(int)             // 设置缓存的分片字节数，默认为 50MiB
                    .setXdbInputStream(v4InputStream)      // 设置 v4 xdb 文件的 inputstream 对象
                    // .setXdbFile(File)                    // 设置 v4 xdb File 对象
                    // .setFairLock(boolean)                // 设置 ReentrantLock 是否使用公平锁
//                    .setXdbPath("ip2region v4 xdb path")    // 设置 v4 xdb 文件的路径
                    .asV4();    // 指定为 v4 配置

//            InputStream v6InputStream = new ClassPathResource("/ip2region_v6.xdb").getInputStream();
//            // 2, 创建 v6 的配置：指定缓存策略和 v6 的 xdb 文件路径
//            final Config v6Config = Config.custom()
//                    .setCachePolicy(Config.BufferCache)     // 指定缓存策略: NoCache / VIndexCache / BufferCache
//                    .setSearchers(15)                       // 设置初始化的查询器数量
//                    // .setCacheSliceBytes(int)             // 设置缓存的分片字节数，默认为 50MiB
//                     .setXdbInputStream(v6InputStream)      // 设置 v6 xdb 文件的 inputstream 对象
//                    // .setXdbFile(File)                    // 设置 v6 xdb File 对象
//                    // .setFairLock(boolean)                // 设置 ReentrantLock 是否使用公平锁
//                    .setXdbPath("ip2region v6 xdb path")    // 设置 v6 xdb 文件的路径
//                    .asV6();    // 指定为 v6 配置

            // 备注：Xdb 三种初始化输入的优先级：XdbInputStream -> XdbFile -> XdbPath
            // setXdbInputStream 仅方便使用者从 jar 包中加载 xdb 文件内容，这时 cachePolicy 只能设置为 Config.BufferCache
            return Ip2Region.create(v4Config, null);
        } catch (Exception e) {
            log.error("ipRegion init error", e);
            throw new RuntimeException(e);
        }
    }
}
