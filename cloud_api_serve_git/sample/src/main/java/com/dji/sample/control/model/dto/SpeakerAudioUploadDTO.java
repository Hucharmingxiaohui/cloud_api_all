package com.dji.sample.control.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Temporary audio object for speaker playback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerAudioUploadDTO {

    private String name;

    private String url;

    private String md5;

    private String format;

    private String objectKey;
}
