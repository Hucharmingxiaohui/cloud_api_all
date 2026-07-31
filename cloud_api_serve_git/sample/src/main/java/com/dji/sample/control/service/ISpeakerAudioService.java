package com.dji.sample.control.service;

import com.dji.sample.control.model.dto.SpeakerAudioUploadDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ISpeakerAudioService {

    SpeakerAudioUploadDTO uploadPcm(String workspaceId, String dockSn, MultipartFile file);

    Boolean delete(String objectKey);
}
