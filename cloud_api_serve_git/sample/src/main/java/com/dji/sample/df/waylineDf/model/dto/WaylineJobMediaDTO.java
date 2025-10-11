package com.dji.sample.df.waylineDf.model.dto;

import com.dji.sample.media.model.MediaFileDTO;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sean
 * @version 1.1
 * @date 2022/6/1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WaylineJobMediaDTO {

    private WaylineJobDTO waylineJobDTO;

    private MediaFileDTO mediaFileDTO;

}
