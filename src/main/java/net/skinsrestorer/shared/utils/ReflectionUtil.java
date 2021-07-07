/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package net.skinsrestorer.shared.utils;

import li.cock.ie.reflect.DuckBypass;
import net.skinsrestorer.shared.exception.EnumNotFoundException;
import net.skinsrestorer.shared.exception.FieldNotFoundException;
import net.skinsrestorer.shared.exception.ReflectionException;
import net.skinsrestorer.shared.serverinfo.ServerVersion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionUtil {
    public static final String SERVER_VERSION_STRING = ServerVersion.getNMSVersion();
    public static final ServerVersion SERVER_VERSION;
    private static final DuckBypass reflect = new DuckBypass();
    private static final Map<Class<?>, Class<?>> builtInMap = new HashMap<>();

    static {
        builtInMap.put(Integer.class, Integer.TYPE);
        builtInMap.put(Long.class, Long.TYPE);
        builtInMap.put(Double.class, Double.TYPE);
        builtInMap.put(Float.class, Float.TYPE);
        builtInMap.put(Boolean.class, Boolean.TYPE);
        builtInMap.put(Character.class, Character.TYPE);
        builtInMap.put(Byte.class, Byte.TYPE);
        builtInMap.put(Short.class, Short.TYPE);

        if (SERVER_VERSION_STRING == null) {
            SERVER_VERSION = null;
        } else {
            String[] strings = SERVER_VERSION_STRING.replace("v", "").replace("R", "").split("_");

            SERVER_VERSION = new ServerVersion(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]));
        }
    }

    private ReflectionUtil() {
    }

    public static Class<?> getBukkitClass(String clazz) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + SERVER_VERSION_STRING + "." + clazz);
    }

    public static Class<?> getNMSClass(String clazz, String fullClassName) throws ReflectionException {
        try {
            return forNameWithFallback(clazz, fullClassName);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException(e);
        }
    }

    private static Class<?> forNameWithFallback(String clazz, String fullClassName) throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.server." + SERVER_VERSION_STRING + "." + clazz);
        } catch (ClassNotFoundException ignored) {
            return Class.forName(fullClassName);
        }
    }

    public static Enum<?> getEnum(Class<?> clazz, String constant) throws EnumNotFoundException {
        Enum<?>[] enumConstants = (Enum<?>[]) clazz.getEnumConstants();

        for (Enum<?> e : enumConstants)
            if (e.name().equalsIgnoreCase(constant))
                return e;

        throw new EnumNotFoundException("Enum constant not found " + constant);
    }

    public static Enum<?> getEnum(Class<?> clazz, String enumName, String constant) throws EnumNotFoundException, ClassNotFoundException {
        return getEnum(getSubClass(clazz, enumName), constant);
    }

    private static Class<?> getSubClass(Class<?> clazz, String className) throws ClassNotFoundException {
        for (Class<?> subClass : clazz.getDeclaredClasses()) {
            if (subClass.getSimpleName().equals(className))
                return subClass;
        }

        for (Class<?> subClass : clazz.getClasses()) {
            if (subClass.getSimpleName().equals(className))
                return subClass;
        }

        throw new ClassNotFoundException("Sub class " + className + " of " + clazz.getSimpleName() + " not found!");
    }

    public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field f;

        try {
            f = clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            f = clazz.getField(fieldName);
        }

        setFieldAccessible(f);

        return f;
    }

    private static Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        Method m;
        try {
            m = clazz.getDeclaredMethod(methodName);
        } catch (Exception e) {
            m = clazz.getMethod(methodName);
        }

        m.setAccessible(true);
        return m;
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... args) throws NoSuchMethodException {
        Method m;
        try {
            m = clazz.getDeclaredMethod(methodName, args);
        } catch (Exception e) {
            m = clazz.getMethod(methodName, args);
        }

        m.setAccessible(true);
        return m;
    }

    public static <T> Field getField(Class<?> target, String name, Class<T> fieldType, int index) throws FieldNotFoundException {
        for (final Field field : target.getDeclaredFields()) {
            if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType()) && index-- <= 0) {
                field.setAccessible(true);
                return field;
            }
        }

        if (target.getSuperclass() != null)
            return getField(target.getSuperclass(), name, fieldType, index);

        throw new FieldNotFoundException("Cannot find field with type " + fieldType + " in " + target.getSimpleName());
    }

    public static Object getObject(Object obj, String fieldName) throws ReflectionException {
        try {
            return getField(obj.getClass(), fieldName).get(obj);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object getFieldByType(Object obj, String typeName) throws ReflectionException {
        return getFieldByType(obj, obj.getClass(), typeName);
    }

    private static Object getFieldByType(Object obj, Class<?> superClass, String typeName) throws ReflectionException {
        return getFieldByTypeList(obj, superClass, typeName).get(0);
    }

    public static List<Object> getFieldByTypeList(Object obj, String typeName) throws ReflectionException {
        return getFieldByTypeList(obj, obj.getClass(), typeName);
    }

    public static List<Object> getFieldByTypeList(Object obj, Class<?> superClass, String typeName) throws ReflectionException {
        List<Object> fields = new ArrayList<>();

        try {
            for (Field f : superClass.getDeclaredFields()) {
                if (f.getType().getSimpleName().equalsIgnoreCase(typeName)) {
                    setFieldAccessible(f);

                    fields.add(f.get(obj));
                }
            }

            if (superClass.getSuperclass() != null) {
                fields.addAll(getFieldByTypeList(obj, superClass.getSuperclass(), typeName));
            }

            if (fields.isEmpty() && obj.getClass() == superClass) {
                throw new FieldNotFoundException("Could not find field of type " + typeName + " in " + obj.getClass().getSimpleName());
            } else {
                return fields;
            }
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeConstructor(Class<?> clazz, Class<?>[] args, Object... initArgs) throws ReflectionException {
        try {
            return getConstructor(clazz, args).newInstance(initArgs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeConstructor(Class<?> clazz, Object... initArgs) throws ReflectionException {
        try {
            return getConstructorByArgs(clazz, initArgs).newInstance(initArgs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Class<?>... args) throws NoSuchMethodException {
        Constructor<?> c = clazz.getConstructor(args);
        c.setAccessible(true);

        return c;
    }

    private static Constructor<?> getConstructorByArgs(Class<?> clazz, Object... args) throws ReflectionException {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterTypes().length != args.length)
                continue;

            int i = 0;
            for (Class<?> parameter : constructor.getParameterTypes()) {
                if (!isAssignable(parameter, args[i]))
                    break;

                i++;
            }

            if (i == args.length)
                return constructor;
        }

        throw new ReflectionException("Could not find constructor with args " + Arrays.stream(args).map(Object::getClass).map(Class::getSimpleName).collect(Collectors.joining(", ")) + " in " + clazz.getSimpleName());
    }

    private static boolean isAssignable(Class<?> clazz, Object obj) {
        clazz = convertToPrimitive(clazz);

        return clazz.isInstance(obj) || clazz == convertToPrimitive(obj.getClass());
    }

    private static Class<?>[] toClassArray(Object[] args) {
        return Stream.of(args).map(Object::getClass).map(ReflectionUtil::convertToPrimitive).toArray(Class<?>[]::new);
    }

    private static Class<?> convertToPrimitive(Class<?> clazz) {
        return builtInMap.getOrDefault(clazz, clazz);
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(clazz, method)).invoke(obj);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, Object... initArgs) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(clazz, method, args)).invoke(obj, initArgs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Object... initArgs) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(clazz, method)).invoke(obj, initArgs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Object obj, String method) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(obj.getClass(), method)).invoke(obj);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Object obj, String method, Object[] initArgs) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(obj.getClass(), method)).invoke(obj, initArgs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    private static void setFieldAccessible(Field f) {
        reflect.setEditable(f);
        f.setAccessible(true);
    }

    public static void setObject(Class<?> clazz, Object obj, String fieldName, Object value) {
        // getField(clazz, fieldName).set(obj, value);
        reflect.setValue(clazz, fieldName, obj, value);
    }
}
