package uz.tune.mentourBiz.rest.endpoint.mobile;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.VERIFY + BaseURI.VERSION)
@RequiredArgsConstructor
public class CheckVersionApi {



}
