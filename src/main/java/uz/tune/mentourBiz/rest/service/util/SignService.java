package uz.tune.mentourBiz.rest.service.util;


import jakarta.servlet.http.HttpServletRequest;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqSignIn;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqTokenRefresh;
import uz.tune.mentourBiz.rest.payload.res.ai.ResIntrospect;
import uz.tune.mentourBiz.rest.payload.res.auth.ResSignIn;

public interface SignService {
    ResSignIn signIn(ReqSignIn request);
    ResSignIn tokenRefresh(ReqTokenRefresh request);
    ResIntrospect introspect(HttpServletRequest request);

}
