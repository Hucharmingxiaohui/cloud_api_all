package com.dji.sample.df.mediaDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@TableName(value = "media_thumbnail_df")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaThumbnailEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("file_id")
    private String fileId;

    @TableField("thumbnail_id")
    private String thumbnailId;

    @TableField("workspace_id")
    private String workspaceId;

    @TableField("object_key")
    private String objectKey;
}


