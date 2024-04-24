package com.gerbugy.springframework.web.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.MultiValueMap;

final class RequestMapUtils {

    private RequestMapUtils() {

    }

    static MultiValueMap<String, Object> distinct(MultiValueMap<String, Object> source) {
        source.entrySet().forEach(entry -> {
            entry.setValue(distinct(entry.getValue()));
        });
        return source;
    }

    static Map<String, Object> distinct(HashMap<String, Object> source) {
        source.entrySet().forEach(entry -> {
            entry.setValue(distinct(entry.getValue()));
        });
        return source;
    }

    private static Object[] distinct(Object[] source) {
        List<Object> answer = new ArrayList<>();
        for (int i = 0; i < source.length; i++) {
            Object obj = distinct(source[i]);
            if (!answer.contains(obj)) {
                answer.add(obj);
            }
        }
        return answer.toArray();
    }

    private static List<Object> distinct(List<Object> source) {
        List<Object> answer = new ArrayList<>();
        source.forEach(element -> {
            Object obj = distinct(element);
            if (!answer.contains(obj)) {
                answer.add(obj);
            }
        });
        return answer;
    }

    @SuppressWarnings("unchecked")
    private static Object distinct(Object obj) {
        if (obj instanceof MultiValueMap multiValueMap) {
            return distinct(multiValueMap);
        } else if (obj instanceof HashMap hashMap) {
            return distinct(hashMap);
        } else if (obj instanceof List list) {
            return distinct(list);
        } else if (obj instanceof Object[] array) {
            return distinct(array);
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    static <V> void strip(Map<?, V> map) {
        map.entrySet().forEach(e -> {
            V value = e.getValue();
            if (value != null) {
                if (value instanceof String str) {
                    if (isStrippable(str)) {
                        e.setValue((V) str.strip());
                    }
                } else {
                    strip(value);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> void strip(T[] array) {
        for (int i = 0; i < array.length; i++) {
            T element = array[i];
            if (element != null) {
                if (element instanceof String str) {
                    if (isStrippable(str)) {
                        array[i] = (T) str.strip();
                    }
                } else {
                    strip(element);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void strip(List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            T element = list.get(i);
            if (element != null) {
                if (element instanceof String str) {
                    if (isStrippable(str)) {
                        list.set(i, (T) str.strip());
                    }
                } else {
                    strip(element);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void strip(Set<T> set) {
        Set<T> strippedSet = new HashSet<>();
        Iterator<T> it = set.iterator();
        while (it.hasNext()) {
            T element = it.next(); // Set cannot have Null
            if (element instanceof String str) {
                String stripped = str.strip();
                if (!strippedSet.contains(stripped)) {
                    strippedSet.add((T) stripped);
                }
                it.remove();
            } else {
                strip(element);
            }
        }
        set.addAll(strippedSet);
    }

    @SuppressWarnings("unchecked")
    private static <T> void strip(T t) {
        if (t instanceof Map map) {
            strip(map);
        } else if (t instanceof List list) {
            strip(list);
        } else if (t instanceof Set set) {
            strip(set);
        } else if (t instanceof Object[] array) {
            strip(array);
        }
    }

    private static boolean isStrippable(String str) {
        return str != null && (str.startsWith(" ") || str.endsWith(" "));
    }
}
