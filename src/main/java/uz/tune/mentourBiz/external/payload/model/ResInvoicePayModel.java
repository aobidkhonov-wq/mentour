package uz.tune.mentourBiz.external.payload.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResInvoicePayModel {

    private boolean isSuccess = false;

    private String errorMessage;
}
