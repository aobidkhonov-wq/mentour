package uz.tune.mentourBiz.config;

import uz.tune.mentourBiz.config.security.AppUserDetails;
import uz.tune.mentourBiz.rest.enums.Lang;

public class GlobalVar {

    private final static GlobalVar INSTANCE = new GlobalVar();
    private final static ThreadLocal<Long> START_TIME = ThreadLocal.withInitial(System::currentTimeMillis);
    private final static ThreadLocal<Lang> LANG = ThreadLocal.withInitial(() -> Lang.UZB);
    private final static ThreadLocal<String> IP_ADDRESS = ThreadLocal.withInitial(String::new);
    private final static ThreadLocal<String> REQUEST_ID = ThreadLocal.withInitial(String::new);
    private final static ThreadLocal<String> USERNAME = ThreadLocal.withInitial(String::new);
    private final static ThreadLocal<String> SECRET_KEY = ThreadLocal.withInitial(String::new);
    private final static ThreadLocal<AppUserDetails> USER_DETAILS = ThreadLocal.withInitial(()-> null);
    private final static ThreadLocal<String> SCHOOL_SCOPE = ThreadLocal.withInitial(String::new);


    private GlobalVar() {
    }

    public static GlobalVar getINSTANCE() {return INSTANCE;}

    private static void setSecretKey(String secretKey){
        GlobalVar.SECRET_KEY.set(secretKey);
    }

    public static String getSecretKey(){
        return GlobalVar.SECRET_KEY.get();
    }

    public static void initStartTime() {
        GlobalVar.START_TIME.set(System.currentTimeMillis());
    }

    public static void setLang(Lang lang) {
        GlobalVar.LANG.set(lang);
    }

    public static Lang getLang() {
        return GlobalVar.LANG.get();
    }

    public static void setRequestId(String requestId) {
        GlobalVar.REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return GlobalVar.REQUEST_ID.get();
    }

    public static void setIpAddress(String ipAddress) {
        GlobalVar.IP_ADDRESS.set(ipAddress);
    }

    public static String getIpAddress() {
        return GlobalVar.IP_ADDRESS.get();
    }

    public static void setUsername(String username) {
        GlobalVar.USERNAME.set(username);
    }

    public static String getUsername() {
        return GlobalVar.USERNAME.get();
    }

    public static void setUserDetails(AppUserDetails userDetails) {
        GlobalVar.USER_DETAILS.set(userDetails);
    }

    public static AppUserDetails getUserDetails() {
        return GlobalVar.USER_DETAILS.get();
    }

    public static void setSchoolScope(String schoolScope) {
        GlobalVar.SCHOOL_SCOPE.set(schoolScope);
    }

    public static String getSchoolScope() {
        return GlobalVar.SCHOOL_SCOPE.get();
    }

    public static Long getStartTime() {
        return GlobalVar.START_TIME.get();
    }

    public static void clearContext() {
        // start time
        GlobalVar.START_TIME.remove();
        // header
        GlobalVar.LANG.remove();
        GlobalVar.IP_ADDRESS.remove();
        GlobalVar.REQUEST_ID.remove();
        GlobalVar.SECRET_KEY.remove();
        GlobalVar.USER_DETAILS.remove();
        GlobalVar.USERNAME.remove();
        GlobalVar.SCHOOL_SCOPE.remove();
        org.apache.logging.log4j.ThreadContext.clearAll();
    }
}
