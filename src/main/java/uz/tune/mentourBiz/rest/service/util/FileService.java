package uz.tune.mentourBiz.rest.service.util;

import org.springframework.web.multipart.MultipartFile;
import uz.tune.mentourBiz.rest.model.ResAttachmentModel;
import uz.tune.mentourBiz.rest.payload.res.ResUploadResult;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.UUID;

public interface FileService {
    ResAttachmentModel upload(MultipartFile file);
    ResAttachmentModel uploadStructured(MultipartFile file, String type, String book, String level, String fileName);
    ResAttachmentModel uploadSpeakingAudio(MultipartFile file);
    byte[] getFileBytes(String fullPath);

    ResponseMessage excelUpload(UUID groupUuid, String fileName, MultipartFile file);

    ResUploadResult excelUploadRes(String fileName, MultipartFile file);

    byte[] generateExcelTemplate();
}