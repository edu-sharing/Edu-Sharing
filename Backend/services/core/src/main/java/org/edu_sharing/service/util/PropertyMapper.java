package org.edu_sharing.service.util;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PropertyMapper {
    private final Map<String, Object> properties;

    @NotNull
    public List<String> getStringList(String key) {
        return getAsList(key);
    }

    @NotNull
    public List<String> getStringList(String key, List<String> defaultValue) {
        return getAsList(key, defaultValue);
    }



    public String getString(String name){
        return getAsSingleValue(name, null);
    }

    public String getString(String name, String defaultValue){
        return getAsSingleValue(name, defaultValue);
    }

    public Integer getInteger(String name){
        return getAsSingleValue(name, null);
    }

    public Integer getInteger(String name, int defaultValue){
        return getAsSingleValue(name, defaultValue);
    }

    public Long getLong(String name){
        return getAsSingleValue(name, null);
    }

    public Long getLong(String name, long defaultValue){
        return getAsSingleValue(name, defaultValue);
    }

    public Boolean getBoolean(String name){
        return getAsSingleValue(name, null);
    }

    public Boolean getBoolean(String name, boolean defaultValue){
        return getAsSingleValue(name, defaultValue);
    }

    @NotNull
    private <T> List<T> getAsList(String name) {
        return getAsList(name, null);
    }

    @NotNull
    private <T> List<T> getAsList(String name, List<T> defaultValue) {
        Object value = properties.getOrDefault(name, defaultValue);
        if(value == null){
            return Collections.emptyList();
        }

        if(value instanceof List) {
            return (List<T>) value;
        }
        return Collections.singletonList((T)value);
    }



    private <T> T getAsSingleValue(String name, T defaultValue){
        Object value = properties.getOrDefault(name, defaultValue);
        if(value instanceof List) {
            return ((List<?>)properties.get(name))
                    .stream()
                    .findFirst()
                    .map(x -> (T)x)
                    .orElse(defaultValue);
        }
        return (T) value;
    }

}
