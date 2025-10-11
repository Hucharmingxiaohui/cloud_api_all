//// sample: com.dji.sample.config.WaylineMvcConfig
//package com.dji.sample.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.MediaType;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import javax.annotation.Resource; // 你是 javax 版
//import javax.servlet.http.HttpServletRequest;
//import java.util.List;
//
//@Configuration
//public class WaylineMvcConfig implements WebMvcConfigurer {
//
//    @Resource(name = "waylineObjectMapper")  // 用 dfe-wayline 提供的 OM
//    private ObjectMapper waylineObjectMapper;
//
//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        // 放到最前，优先匹配
//        converters.add(0, new PathAwareJacksonConverter(
//                waylineObjectMapper,
//                // 把 dfe-wayline 的接口路径都列上（你现在的控制器方法）
//                "/buildKmz", "/updateKmz", "/parseKmz"
//                // 如果以后统一加了前缀，比如 /wayline/**，就改成一个 "/wayline/**" 即可
//        ));
//    }
//
//    /** 只在匹配到给定路径时启用的 Converter */
//    static class PathAwareJacksonConverter extends MappingJackson2HttpMessageConverter {
//        private final AntPathMatcher matcher = new AntPathMatcher();
//        private final String[] patterns;
//
//        PathAwareJacksonConverter(ObjectMapper om, String... patterns) {
//            super(om);
//            this.patterns = patterns;
//            setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
//        }
//
//        private boolean match() {
//            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//            if (attrs == null) return false;
//            HttpServletRequest req = attrs.getRequest();
//            String uri = req.getRequestURI();
//            for (String p : patterns) {
//                if (matcher.match(p, uri)) return true;
//            }
//            return false;
//        }
//
//        @Override public boolean canRead(Class<?> clazz, MediaType mediaType)  { return match() && super.canRead(clazz, mediaType); }
//        @Override public boolean canWrite(Class<?> clazz, MediaType mediaType) { return match() && super.canWrite(clazz, mediaType); }
//    }
//}
