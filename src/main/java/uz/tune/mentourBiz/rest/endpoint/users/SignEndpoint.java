package uz.tune.mentourBiz.rest.endpoint.users;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqSignIn;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqTokenRefresh;
import uz.tune.mentourBiz.rest.payload.res.auth.ResSignIn;
import uz.tune.mentourBiz.rest.service.util.SignService;


@RestController
@RequestMapping(BaseURI.API1 + BaseURI.AUTH)
@RequiredArgsConstructor
public class SignEndpoint {

    private final SignService signService;

    @PostMapping(BaseURI.SIGN)
    public ResponseEntity<uz.tune.mentourBiz.rest.payload.res.auth.ResSignIn> signIn(@RequestBody ReqSignIn request) {
        return ResponseEntity.ok(signService.signIn(request));
    }

    @PostMapping(BaseURI.TOKEN + BaseURI.REFRESH)
    public ResponseEntity<ResSignIn> tokenRefresh(@RequestBody ReqTokenRefresh request) {
        return new ResponseEntity<>(signService.tokenRefresh(request), HttpStatus.OK);
    }

    // TODO KEYCLOACK TIME
}
