
package com.jokati.invoice.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class ListToMapReadingConverter implements Converter<List<?>, Map<String, Object>> {
    @Override
    public Map<String, Object> convert(List<?> source) {
        // Only for empty arrays: map [] -> {}
        return Collections.emptyMap();
    }
}
