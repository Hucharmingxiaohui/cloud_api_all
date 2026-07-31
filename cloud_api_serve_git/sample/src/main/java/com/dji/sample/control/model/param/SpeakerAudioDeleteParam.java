package com.dji.sample.control.model.param;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SpeakerAudioDeleteParam {

    @NotBlank
    private String objectKey;
}
