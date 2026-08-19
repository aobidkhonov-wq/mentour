package uz.tune.mentourBiz.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

public class ConfigUtils {

    public static String getHeaders(ContentCachingRequestWrapper request) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> headers = new HashMap<>();
        Iterator<String> iter = request.getHeaderNames().asIterator();
        while (iter.hasNext()) {
            String header = iter.next();
            headers.put(header, request.getHeader(header));
        }
        return mapper.writeValueAsString(headers);
    }

    public static String getHeaders(ContentCachingResponseWrapper response) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> headers = new HashMap<>();
        for (String header : response.getHeaderNames()) {
            headers.put(header, response.getHeader(header));
        }
        return mapper.writeValueAsString(headers);
    }

    public static String getRequestBody(ContentCachingRequestWrapper request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {

            return new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        }

        if (request.getContentAsByteArray().length > 0) {
            try {
                ObjectReader reader = mapper.reader();
                return reader.readTree(new ByteArrayInputStream(request.getContentAsByteArray())).toString();
            } catch (JsonProcessingException e) {
                return "[Non-JSON Body]: " + new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
            }
        }
        if (request.getParameterMap().size() > 0) {
            ObjectNode parameters = mapper.createObjectNode();
            request.getParameterMap().forEach((key, value) -> parameters.put(key, getParameter(value)));
            return parameters.toString();
        }
        ObjectNode node = mapper.createObjectNode();
        return node.toString();
    }

    public static String getResponseBody(ContentCachingResponseWrapper response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        if (response.getContentAsByteArray().length > 0) {
            ObjectReader reader = mapper.reader();
            return reader.readTree(new ByteArrayInputStream(response.getContentAsByteArray())).toString();
        }
        ObjectNode node = mapper.createObjectNode();
        return node.toString();
    }

    private static String getParameter(String[] value) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : value) {
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }

    public static ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        } else {
            return new ContentCachingRequestWrapper(request);
        }
    }

    public static ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        } else {
            return new ContentCachingResponseWrapper(response);
        }
    }

}
