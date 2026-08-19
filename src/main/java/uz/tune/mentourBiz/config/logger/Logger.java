package uz.tune.mentourBiz.config.logger;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uz.tune.mentourBiz.config.GlobalVar;

@Log4j2
public class Logger {

    private static final String excFormat = "[user: %s] : [reqId: %s] : [message: %s]";
    private static final String excFormatWithThrowable = "[user: %s] : [reqId: %s] : [message: %s]";

    private static final String reqFormat = "[REQ] %s %s | User: %s | Body: %s";
    private static final String resFormat = "[RES] %s | Status: %s (%sms) | Body: %s";
    private static final String infoFormat = "[INFO] %s";

    public static void reqMain(HttpMethod httpMethod, String uri, String headers, String requestBody) {
        log.info(String.format(reqFormat, httpMethod, uri, GlobalVar.getUsername(), truncate(requestBody)));
    }

    public static void resMain(HttpStatus status, Long time, String headers, String body) {
        log.info(String.format(resFormat, GlobalVar.getRequestId(), status.value(), time, truncate(body)));
    }

    public static void logAi(String type, String url, String payload) {
        log.info(String.format("[AI-LOG] [%s] | URL: %s | Payload: %s", type, url, payload));
    }

    public static void exception(Throwable th) {
        log.error(
                String.format(
                        excFormat,
                        GlobalVar.getUsername(),
                        GlobalVar.getRequestId(),
                        th.getMessage()
                ),
                th
        );
    }

    public static void exception(String message) {
        // [USER-ID] : [LOG-ID] : [MESSAGE]
        log.error(
                String.format(
                        excFormat,
                        GlobalVar.getUsername(),
                        GlobalVar.getRequestId(),
                        message
                )
        );
    }

    public static void exception(String message, Throwable th) {
        log.error(
                String.format(
                        excFormatWithThrowable,
                        GlobalVar.getUsername(),
                        GlobalVar.getRequestId(),
                        message
                ),
                th
        );
    }

    private static String truncate(String str) {
        if (str == null) return "{}";
        if (str.length() > 200) {
            return str.substring(0, 200) + "... [TRUNCATED]";
        }
        return str;
    }

    public static void logInfo(String message) {
        log.info(String.format(infoFormat, message));
    }

    public static void logWarn(String message) {
        log.warn(String.format("[WARN] [user: %s] [req: %s] : %s",
                GlobalVar.getUsername() != null ? GlobalVar.getUsername() : "SYSTEM",
                GlobalVar.getRequestId() != null ? GlobalVar.getRequestId() : "N/A",
                message));
    }

    private static void reqInfo(HttpMethod method, String url, String headers, String body) {
        log.info(
                String.format(
                        reqFormat,
                        GlobalVar.getUsername(),
                        GlobalVar.getRequestId(),
                        "REQ",
                        method,
                        url,
                        headers,
                        body
                )
        );
    }

    private static void resInfo(HttpStatus status, Long time, String headers, String body) {
        // [USER-ID] : [LOG-ID] : [SERVICE (TYPE)] : [STATUS (TIME)] : [HEADERS] : [BODY]
        log.info(
                String.format(
                        resFormat,
                        GlobalVar.getUsername(),
                        GlobalVar.getRequestId(),
                        "RES",
                        status.value(),
                        time,
                        headers,
                        body
                )
        );
    }
}