
package uz.tune.mentourBiz.rest.endpoint.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.model.ResAttachmentModel;
import uz.tune.mentourBiz.rest.payload.res.ResUploadResult;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.service.util.FileService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.FILE)
@RequiredArgsConstructor
public class FileEndpoint {

    private final FileService fileService;

    @PostMapping(value = BaseURI.UPLOAD, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResAttachmentModel> upload(MultipartFile file) {
        return ResponseEntity.ok(fileService.upload(file));
    }

    @PostMapping(value = "/upload/{type}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResAttachmentModel> uploadStructured(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file,
            @RequestParam("book") String book,
            @RequestParam("level") String level,
            @RequestParam(value = "fileName", required = false) String fileName) {

        return ResponseEntity.ok(fileService.uploadStructured(file, type, book, level, fileName));
    }

    @PostMapping(value = "/upload/excel", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseMessage> uploadStructured(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "groupUuid", required = false) UUID groupUuid,
            @RequestParam(value = "fileName", required = false) String fileName) {
        return ResponseEntity.ok(fileService.excelUpload(groupUuid, fileName, file));
    }

    @PostMapping(value = "/upload/excel-res/{schoolUuid}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResUploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName) {
        return ResponseEntity.ok(fileService.excelUploadRes(fileName, file));
    }
    @GetMapping("/excel-template")
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        byte[] templateBytes = fileService.generateExcelTemplate();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"students_template.xlsx\"")
                .body(templateBytes);
    }

}
