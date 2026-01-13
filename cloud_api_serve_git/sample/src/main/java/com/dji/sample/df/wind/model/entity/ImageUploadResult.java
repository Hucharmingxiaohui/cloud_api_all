package com.dji.sample.df.wind.model.entity;

/**
 * 图片上传结果类
 */
public class ImageUploadResult {
    private boolean success;
    private String message;
    private String originalName;
    private String savedName;
    private String filePath;
    private String fileUrl;
    private long fileSize;
    private Integer width;
    private Integer height;
    private int index;

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getSavedName() { return savedName; }
    public void setSavedName(String savedName) { this.savedName = savedName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
}

