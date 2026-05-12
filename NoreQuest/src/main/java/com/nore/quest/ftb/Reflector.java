package com.nore.quest.ftb;

import java.lang.reflect.Method;
import java.util.Optional;

public final class Reflector {
    private Reflector() {
    }

    public static Optional<Object> call(Object target, String methodName, Object... args) {
        if (target == null) {
            return Optional.empty();
        }
        Method method = find(target instanceof Class<?> c ? c : target.getClass(), methodName, args);
        if (method == null) {
            return Optional.empty();
        }
        try {
            method.setAccessible(true);
            return Optional.ofNullable(method.invoke(target instanceof Class<?> ? null : target, args));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static boolean invoke(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }
        Method method = find(target instanceof Class<?> c ? c : target.getClass(), methodName, args);
        if (method == null) {
            return false;
        }
        try {
            method.setAccessible(true);
            method.invoke(target instanceof Class<?> ? null : target, args);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Method find(Class<?> type, String name, Object[] args) {
        for (Method method : type.getMethods()) {
            if (matches(method, name, args)) {
                return method;
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (matches(method, name, args)) {
                return method;
            }
        }
        Class<?> parent = type.getSuperclass();
        return parent == null ? null : find(parent, name, args);
    }

    private static boolean matches(Method method, String name, Object[] args) {
        if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
            return false;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isCompatible(parameterTypes[i], args[i])) {
                return false;
            }
        }

        return true;
    }

    private static boolean isCompatible(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }

        Class<?> argType = arg.getClass();
        if (!parameterType.isPrimitive()) {
            return parameterType.isAssignableFrom(argType);
        }

        if (parameterType == boolean.class) {
            return argType == Boolean.class;
        } else if (parameterType == byte.class) {
            return argType == Byte.class;
        } else if (parameterType == short.class) {
            return argType == Short.class || argType == Byte.class;
        } else if (parameterType == int.class) {
            return argType == Integer.class || argType == Short.class || argType == Byte.class;
        } else if (parameterType == long.class) {
            return argType == Long.class || argType == Integer.class || argType == Short.class || argType == Byte.class;
        } else if (parameterType == float.class) {
            return argType == Float.class || argType == Long.class || argType == Integer.class || argType == Short.class || argType == Byte.class;
        } else if (parameterType == double.class) {
            return argType == Double.class || argType == Float.class || argType == Long.class || argType == Integer.class || argType == Short.class || argType == Byte.class;
        } else if (parameterType == char.class) {
            return argType == Character.class;
        }

        return false;
    }
}
