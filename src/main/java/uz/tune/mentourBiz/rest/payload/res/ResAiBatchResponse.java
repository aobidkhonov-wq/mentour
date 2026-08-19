package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;

import java.util.List;

@Data
public class ResAiBatchResponse {
    private List<ResAiBatchItem> results;
}
