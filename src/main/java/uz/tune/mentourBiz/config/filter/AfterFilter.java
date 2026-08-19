package uz.tune.mentourBiz.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.utils.ConfigUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AfterFilter extends OncePerRequestFilter {

    private final Set<String> URLS_NOT_TO_SAVE = new HashSet<>() {{
        add("logs");
        add("swagger");
        add("favicon");
        add("report");
        add("download");
        add("upload");
        add("/v3/api-docs");
        add("/login");
    }};

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println(">>> BeforeFilter running for: " + request.getMethod() + " " + request.getRequestURI());
        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            doFilterWrapped(ConfigUtils.wrapRequest(request), ConfigUtils.wrapResponse(response), filterChain);
        }
        GlobalVar.clearContext();
    }

    private void doFilterWrapped(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            final String URI = request.getRequestURI();
            if (allowToSave(URI)) {
                Logger.reqMain(
                        HttpMethod.valueOf(request.getMethod()),
                        URI,
                        ConfigUtils.getHeaders(request),
                        ConfigUtils.getRequestBody(request)
                );
                Long endTime = System.currentTimeMillis();

                String contentType = response.getContentType();
                boolean isJson = contentType != null && contentType.contains("application/json");

                if (response.getStatus() >= 300 && response.getStatus() < 400) {
                    Logger.resMain(
                            HttpStatus.valueOf(response.getStatus()),
                            (endTime - GlobalVar.getStartTime()),
                            ConfigUtils.getHeaders(response),
                            "[REDIRECT RESPONSE - BODY SKIPPED FOR LOGGING]"
                    );
                } else if (!isJson) {
                    // Excel, PDF, image va boshqa binary — JSON parse qilinmaydi
                    Logger.resMain(
                            HttpStatus.valueOf(response.getStatus()),
                            (endTime - GlobalVar.getStartTime()),
                            ConfigUtils.getHeaders(response),
                            "[BINARY RESPONSE - Content-Type: " + contentType + "]"
                    );
                } else {
                    Logger.resMain(
                            HttpStatus.valueOf(response.getStatus()),
                            (endTime - GlobalVar.getStartTime()),
                            ConfigUtils.getHeaders(response),
                            ConfigUtils.getResponseBody(response)
                    );
                }
            }
            if (!response.isCommitted()) {
                response.copyBodyToResponse();
            }
        }
    }

    private boolean allowToSave(String URI) {
        for (String url : URLS_NOT_TO_SAVE) {
            if (URI.contains(url))
                return false;
        }

        return true;
    }
}