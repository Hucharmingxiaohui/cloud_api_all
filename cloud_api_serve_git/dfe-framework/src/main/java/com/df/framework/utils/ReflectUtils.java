package com.df.framework.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.poi.ss.usermodel.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;

/**
 * @author lianyc
 */
@SuppressWarnings("rawtypes")
public class ReflectUtils {

    private static Logger logger = LoggerFactory.getLogger(ReflectUtils.class);

    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";


    /**
     * 字段值为空转为字符串
     *
     * @param obj
     */
    public static void nullToEmptyEntity(Object obj) {
        if (Object.class == obj.getClass()) {
            // 无需处理
            return;
        }
        Class clazz = obj.getClass();
        Field[] fs = clazz.getDeclaredFields();
        // 属性值
        Object val = null;
        for (Field f : fs) {
            boolean isStatic = Modifier.isStatic(f.getModifiers());
            if (isStatic) {
                // 静态常量，忽视
                continue;
            }
            f.setAccessible(true);
            try {
                val = f.get(obj);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (f.getType().equals(String.class)) {
                // String类型
                if (null == val) {
                    try {
                        f.set(obj, "");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * 字段值为空转为字符串
     *
     * @param list
     */
    public static void nullToEmptyListEntity(List<?> list) {
        for (Object obj : list) {
            nullToEmptyEntity(obj);
        }
    }

    /**
     * 获取值
     *
     * @param obj 请求对象
     * @return
     */
    public static String val(Object obj) {
        if (null == obj) {
            return "";
        } else {
            String str = obj.toString().trim();
            return (StringUtils.isBlank(str) ? "" : str);
        }
    }

    /**
     * 调用Setter方法, 仅匹配方法名。
     * 支持多级，如：对象名.对象名.方法
     */
    public static <E> void invokeSetter(Object obj, String propertyName, E value) {
        Object object = obj;
        String[] names = StringUtils.split(propertyName, ".");
        for (int i = 0; i < names.length; i++) {
            if (i < names.length - 1) {
                String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(names[i]);
                object = invokeMethod(object, getterMethodName, new Class[]{}, new Object[]{});
            } else {
                String setterMethodName = SETTER_PREFIX + StringUtils.capitalize(names[i]);
                invokeMethodByName(object, setterMethodName, new Object[]{value});
            }
        }
    }

    /**
     * 直接调用对象方法, 无视private/protected修饰符.
     * 用于一次性调用的情况，否则应使用getAccessibleMethod()函数获得Method后反复调用.
     * 同时匹配方法名+参数类型，
     */
    @SuppressWarnings("unchecked")
    public static <E> E invokeMethod(final Object obj, final String methodName, final Class<?>[] parameterTypes,
                                     final Object[] args) {
        if (obj == null || methodName == null) {
            return null;
        }
        Method method = getAccessibleMethod(obj, methodName, parameterTypes);
        if (method == null) {
            logger.debug("在 [" + obj.getClass() + "] 中，没有找到 [" + methodName + "] 方法 ");
            return null;
        }
        try {
            return (E) method.invoke(obj, args);
        } catch (Exception e) {
            logger.error("{}", e);
            throw convertReflectionExceptionToUnchecked("反射出错", e);
        }
    }

    /**
     * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问.
     * 如向上转型到Object仍无法找到, 返回null.
     * 匹配函数名+参数类型。
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)
     */
    public static Method getAccessibleMethod(final Object obj, final String methodName,
                                             final Class<?>... parameterTypes) {
        // 为空不报错。直接返回 null
        if (obj == null) {
            return null;
        }
        Validate.notBlank(methodName, "methodName can't be blank");
        for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType.getSuperclass()) {
            try {
                Method method = searchType.getDeclaredMethod(methodName, parameterTypes);
                makeAccessible(method);
                return method;
            } catch (NoSuchMethodException e) {
                logger.error("{}", e);
                continue;
            }
        }
        return null;
    }

    /**
     * 改变private/protected的方法为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
     */
    public static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }

    /**
     * 将反射时的checked exception转换为unchecked exception.
     */
    public static RuntimeException convertReflectionExceptionToUnchecked(String msg, Exception e) {
        if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException
                || e instanceof NoSuchMethodException) {
            return new IllegalArgumentException(msg, e);
        } else if (e instanceof InvocationTargetException) {
            return new RuntimeException(msg, ((InvocationTargetException) e).getTargetException());
        }
        return new RuntimeException(msg, e);
    }

    /**
     * 直接调用对象方法, 无视private/protected修饰符，
     * 用于一次性调用的情况，否则应使用getAccessibleMethodByName()函数获得Method后反复调用.
     * 只匹配函数名，如果有多个同名函数调用第一个。
     */
    @SuppressWarnings("unchecked")
    public static <E> E invokeMethodByName(final Object obj, final String methodName, final Object[] args) {
        Method method = getAccessibleMethodByName(obj, methodName, args.length);
        if (method == null) {
            // 如果为空不报错，直接返回空。
            logger.debug("在 [" + obj.getClass() + "] 中，没有找到 [" + methodName + "] 方法 ");
            return null;
        }
        try {
            // 类型转换（将参数数据类型转换为目标方法参数类型）
            Class<?>[] cs = method.getParameterTypes();
            for (int i = 0; i < cs.length; i++) {
                if (args[i] != null && !args[i].getClass().equals(cs[i])) {
                    if (cs[i] == String.class) {
                        args[i] = ConvertUtils.toStr(args[i]);
                        if (StringUtils.endsWith((String) args[i], ".0")) {
                            args[i] = StringUtils.substringBefore((String) args[i], ".0");
                        }
                    } else if (cs[i] == Integer.class) {
                        args[i] = ConvertUtils.toInt(args[i]);
                    } else if (cs[i] == Long.class) {
                        args[i] = ConvertUtils.toLong(args[i]);
                    } else if (cs[i] == Double.class) {
                        args[i] = ConvertUtils.toDouble(args[i]);
                    } else if (cs[i] == Float.class) {
                        args[i] = ConvertUtils.toFloat(args[i]);
                    } else if (cs[i] == Date.class) {
                        if (args[i] instanceof String) {
                            args[i] = DateUtils.parseDate(args[i].toString());
                        } else {
                            args[i] = DateUtil.getJavaDate((Double) args[i]);
                        }
                    } else if (cs[i] == boolean.class || cs[i] == Boolean.class) {
                        args[i] = ConvertUtils.toBool(args[i]);
                    }
                }
            }
            return (E) method.invoke(obj, args);
        } catch (Exception e) {
            logger.error("{}", e);
            throw convertReflectionExceptionToUnchecked("反射出错", e);
        }
    }

    /**
     * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问.
     * 如向上转型到Object仍无法找到, 返回null.
     * 只匹配函数名。
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)
     */
    public static Method getAccessibleMethodByName(final Object obj, final String methodName, int argsNum) {
        // 为空不报错。直接返回 null
        if (obj == null) {
            return null;
        }
        Validate.notBlank(methodName, "methodName can't be blank");
        for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType.getSuperclass()) {
            Method[] methods = searchType.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.getParameterTypes().length == argsNum) {
                    makeAccessible(method);
                    return method;
                }
            }
        }
        return null;
    }
}
