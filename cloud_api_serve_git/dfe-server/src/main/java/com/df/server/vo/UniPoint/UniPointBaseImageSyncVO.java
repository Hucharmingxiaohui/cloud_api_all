package com.df.server.vo.UniPoint;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class UniPointBaseImageSyncVO {

    List<ImageSyncVO> resImageData = new ArrayList<>();

    @NoArgsConstructor
    @Data
    public static class ImageSyncVO {
        private String pointCode;
        private String url;
    }
}
