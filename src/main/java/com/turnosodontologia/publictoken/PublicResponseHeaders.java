package com.turnosodontologia.publictoken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
public class PublicResponseHeaders implements ResponseBodyAdvice<Object> {

    private final List<String> prefijos;

    @Autowired
    public PublicResponseHeaders(@Value("${public.surface.prefixes:}") String prefijos) {
        this(dividir(prefijos));
    }

    public PublicResponseHeaders(List<String> prefijos) {
        this.prefijos = prefijos;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                returnType.getContainingClass(), RequestMapping.class);
        if (mapping == null) {
            return false;
        }
        return Arrays.stream(mapping.value())
                .anyMatch(ruta -> prefijos.stream().anyMatch(ruta::startsWith));
    }

    @Override
    public Object beforeBodyWrite(Object cuerpo, MethodParameter returnType, MediaType medio,
                                  Class<? extends HttpMessageConverter<?>> convertidor,
                                  ServerHttpRequest solicitud, ServerHttpResponse respuesta) {
        respuesta.getHeaders().set("Referrer-Policy", "no-referrer");
        respuesta.getHeaders().set("Cache-Control", "no-store");
        return cuerpo;
    }

    private static List<String> dividir(String valor) {
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(parte -> !parte.isEmpty())
                .toList();
    }
}
