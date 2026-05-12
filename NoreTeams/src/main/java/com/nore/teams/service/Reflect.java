package com.nore.teams.service;

import java.lang.reflect.Method;
import java.util.Optional;

final class Reflect {
    private Reflect() {
    }

    static Optional<Object> call(Object target, String name, Object... args) {
        if (target == null) {
            return Optional.empty();
        }
        Method method = findMethod(target instanceof Class<?> c ? c : target.getClass(), name, args.length);
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

    static Method findMethod(Class<?> type, String name, int argCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        Class<?> parent = type.getSuperclass();
        return parent == null ? null : findMethod(parent, name, argCount);
    }
}
