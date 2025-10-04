package io.kestra.plugin.youtube.helpers;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for safely parsing and rendering enums with case-insensitive
 * support
 */
public class EnumHelper {

    /**
     * Safely parse enum from string, case-insensitive
     */
    public static <T extends Enum<T>> T parseEnumCaseInsensitive(String value, Class<T> enumClass) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // Try exact match first
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            // Try case-insensitive match
            for (T enumConstant : enumClass.getEnumConstants()) {
                if (enumConstant.name().equalsIgnoreCase(value)) {
                    return enumConstant;
                }
            }
            return null;
        }
    }

    /**
     * Safely render enum property with case-insensitive parsing
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T safeRenderEnum(RunContext runContext, Property property, Class<T> enumClass) {
        if (property == null) {
            return null;
        }
        try {
            // First try direct enum parsing
            Object rendered = runContext.render(property).as(enumClass).orElse(null);
            if (rendered != null && enumClass.isInstance(rendered)) {
                return (T) rendered;
            }
        } catch (Exception e) {
            // Fall through to string parsing
        }

        // Fallback to string parsing with case-insensitive matching
        try {
            Object rendered = runContext.render(property).as(String.class).orElse(null);
            if (rendered instanceof String) {
                return parseEnumCaseInsensitive((String) rendered, enumClass);
            }
        } catch (Exception ex) {
            // Ignore and return null
        }
        return null;
    }

    /**
     * Safely render list of enums with case-insensitive parsing
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> List<T> safeRenderEnumList(RunContext runContext, Property property,
            Class<T> enumClass) {
        if (property == null) {
            return null;
        }
        try {
            // First try direct list parsing
            List renderedList = (List) runContext.render(property).asList(enumClass);
            if (renderedList != null && !renderedList.isEmpty()) {
                List<T> result = new ArrayList<>();
                for (Object item : renderedList) {
                    if (enumClass.isInstance(item)) {
                        result.add((T) item);
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            // Fall through to string parsing
        }

        // Fallback to string list parsing with case-insensitive matching
        try {
            List renderedList = (List) runContext.render(property).asList(String.class);
            if (renderedList != null && !renderedList.isEmpty()) {
                List<T> result = new ArrayList<>();
                for (Object item : renderedList) {
                    if (item instanceof String) {
                        T enumValue = parseEnumCaseInsensitive((String) item, enumClass);
                        if (enumValue != null) {
                            result.add(enumValue);
                        }
                    }
                }
                return result.isEmpty() ? null : result;
            }
        } catch (Exception ex) {
            // Ignore and return null
        }
        return null;
    }
}
