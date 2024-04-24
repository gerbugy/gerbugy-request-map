package com.gerbugy.springframework.web.request;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import jakarta.servlet.http.HttpServletRequest;

class RequestMapResolver implements HandlerMethodArgumentResolver {

    private final ApplicationContext applicationContext;
    private final MultipartResolver multipartResolver;

    private RequestResponseBodyMethodProcessor bodyProcessor;

    RequestMapResolver(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.multipartResolver = applicationContext.getBean(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        RequestMap annotation = parameter.getParameterAnnotation(RequestMap.class);
        return annotation != null && Map.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        RequestMap annotation = parameter.getParameterAnnotation(RequestMap.class);
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        resolvePath(multiValueMap, request);
        resolveParameter(multiValueMap, request);
        resolveMultipart(multiValueMap, request);
        resolveBody(multiValueMap, request, parameter, mavContainer, webRequest, binderFactory);
        doStrip(multiValueMap, annotation.allowStrip());
        doDistinct(multiValueMap, annotation.allowDistinct());
        // mavContainer.getModel().put("test", 1234); // for jstl
        return MultiValueMap.class.isAssignableFrom(parameter.getParameterType()) ? multiValueMap : toHashMap(multiValueMap);
    }

    @SuppressWarnings("unchecked")
    private void resolvePath(MultiValueMap<String, Object> map, HttpServletRequest request) {
        Map<String, String> pathMap = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        pathMap.forEach(map::add);
    }

    private void resolveParameter(MultiValueMap<String, Object> map, HttpServletRequest request) {
        request.getParameterMap().forEach((key, values) -> map.addAll(key, Arrays.asList(values)));
    }

    private void resolveMultipart(MultiValueMap<String, Object> map, HttpServletRequest request) {
        if (request.getContentLength() > 0) {
            if (request.getContentType().contains("multipart/")) {
                MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(request);
                MultiValueMap<String, MultipartFile> fileMap = multipartRequest.getMultiFileMap();
                fileMap.forEach((key, values) -> {
                    values.forEach(value -> {
                        map.add(key, value);
                    });
                });
            }
        }
    }

    private void resolveBody(MultiValueMap<String, Object> map, HttpServletRequest request, MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        if (request.getContentLength() > 0) {
            String contentType = request.getContentType();
            if (contentType.contains(MediaType.APPLICATION_JSON_VALUE) || contentType.contains(MediaType.APPLICATION_XML_VALUE)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) getBodyProcessor().resolveArgument(parameter, mavContainer, webRequest, binderFactory);
                if (body != null) {
                    body.forEach((key, value) -> {
                        map.add(key, value);
                    });
                }
            }
        }
    }

    private RequestResponseBodyMethodProcessor getBodyProcessor() {
        if (bodyProcessor == null) {
            RequestMappingHandlerAdapter adapter = applicationContext.getBean("requestMappingHandlerAdapter", RequestMappingHandlerAdapter.class);
            List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
            bodyProcessor = new RequestResponseBodyMethodProcessor(converters);
        }
        return bodyProcessor;
    }

    private <V> void doStrip(Map<?, V> map, boolean allowTrim) {
        if (allowTrim) {
            RequestMapUtils.strip(map);
        }
    }

    private void doDistinct(MultiValueMap<String, Object> map, boolean allowDistinct) {
        if (allowDistinct) {
            RequestMapUtils.distinct(map);
        }
    }

    private Map<String, Object> toHashMap(MultiValueMap<String, Object> multiValueMap) {
        Map<String, Object> hashMap = new LinkedHashMap<>();
        multiValueMap.forEach((key, values) -> {
            hashMap.put(key, values.size() == 1 ? values.get(0) : values);
        });
        return hashMap;
    }

    private static final String DATATABLE_SEARCH_KEY = "_datatable";

//    private void resolveLogin(MultiValueMap<String, Object> map, HttpServletRequest request) {
//        HttpSession session = request.getSession();
//        Map<String, Object> loginMap = SessionUtils.getLoginMap(session);
//        if (loginMap != null) {
//            map.set("request_user_id", loginMap.get("user_id"));
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private void resolveDatatable(MultiValueMap<String, Object> map) {
//        if (map.containsKey(DATATABLE_SEARCH_KEY)) {
//            Map<String, Object> datatableMap = ObjectMapperUtils.readValueAsMap(map.getFirst(DATATABLE_SEARCH_KEY).toString());
//            map.setAll(datatableMap);
//            map.remove(DATATABLE_SEARCH_KEY);
//
//            Map<String, Object> searches = MapUtils.getMap(datatableMap, "search", "value");
//            map.set("searches", searches);
//            searches.entrySet().forEach(set -> {
//                set.setValue(Arrays.stream(set.getValue().toString().split("\\s+")).filter(a -> a.length() > 0).collect(Collectors.toCollection(ArrayList::new)));
//            });
//
//            List<Map<String, Object>> orders = (List<Map<String, Object>>) datatableMap.get("order");
//            if (orders != null) {
//                List<Map<String, Object>> columns = (List<Map<String, Object>>) datatableMap.get("columns");
//                for (Map<String, Object> order : orders) {
//                    order.put("data", MapUtils.getString(columns.get(MapUtils.getIntValue(order, "column")), "data"));
//                }
//            }
//        }
//    }

}
