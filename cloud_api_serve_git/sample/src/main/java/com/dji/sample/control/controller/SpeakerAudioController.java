package com.dji.sample.control.controller;

import com.dji.sample.control.model.dto.SpeakerAudioUploadDTO;
import com.dji.sample.control.model.param.SpeakerAudioDeleteParam;
import com.dji.sample.control.service.ISpeakerAudioService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("${url.control.prefix}${url.control.version}")
public class SpeakerAudioController {

    @Autowired
    private ISpeakerAudioService speakerAudioService;

    @PostMapping("/workspaces/{workspace_id}/speaker/audio/upload")
    public HttpResultResponse<SpeakerAudioUploadDTO> upload(@PathVariable("workspace_id") String workspaceId,
                                                            @RequestParam("dock_sn") String dockSn,
                                                            @RequestParam("file") MultipartFile file) {
        return HttpResultResponse.success(speakerAudioService.uploadPcm(workspaceId, dockSn, file));
    }

    @DeleteMapping("/workspaces/{workspace_id}/speaker/audio")
    public HttpResultResponse delete(@PathVariable("workspace_id") String workspaceId,
                                     @Valid @RequestBody SpeakerAudioDeleteParam param) {
        speakerAudioService.delete(param.getObjectKey());
        return HttpResultResponse.success();
    }
}
