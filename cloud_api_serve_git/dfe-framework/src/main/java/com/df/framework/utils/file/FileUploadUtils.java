package com.df.framework.utils.file;

import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.framework.utils.DateUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 文件保存本地工具类
 */
@Component
public class FileUploadUtils {

    private static FileConfig fileConfig;

    @Autowired
    public void setComponent(FileConfig fileConfig) {
        FileUploadUtils.fileConfig = fileConfig;
    }

    /**
     * 将上传的图片文件保存至本地服务器，返回数据库应保存的路径  注意：请提前检查文件是否有效，按照后缀检查
     *
     * @param file
     * @param baseDir 路径
     * @return
     */
    public static String saveImageReturnDBSavePath(MultipartFile file, String baseDir) {
        String extension = FileUploadUtils.getExtension(file);
        String newFileName = DateUtils.getNowDateTimeStrMil() + "." + extension;
        if (StringUtils.isBlank(baseDir)) {
            throw new FastException("保存文件路径不能为空");
        }
        while (baseDir.startsWith("/")) {
            baseDir = baseDir.substring(1);
        }
        if (!baseDir.startsWith("/")) {
            baseDir = "/" + baseDir;
        }
        if (!baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        String dbSavePath = baseDir + newFileName;
        String fileFullPath = fileConfig.getFileSavePath() + dbSavePath;
        File absoluteFile = getAbsoluteFile(fileFullPath);
        try {
            file.transferTo(absoluteFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new FastException("文件写入失败");
        }
        return dbSavePath;
    }


    /**
     * 将上传的文件保存至本地服务器，返回数据库应保存的路径  注意：请提前检查文件是否有效，按照后缀检查
     * 原文件名保存
     *
     * @param file
     * @param baseDir 路径
     * @return
     */
    public static String saveFileReturnDBSavePath(MultipartFile file, String baseDir) {
        String newFileName = file.getOriginalFilename();
        if (StringUtils.isBlank(baseDir)) {
            throw new FastException("保存文件路径不能为空");
        }
        while (baseDir.startsWith("/")) {
            baseDir = baseDir.substring(1);
        }
        if (!baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        String dbSavePath = baseDir + newFileName;
        String fileFullPath = fileConfig.getFileSavePath() + "/" + dbSavePath;
        File absoluteFile = getAbsoluteFile(fileFullPath);
        try {
            file.transferTo(absoluteFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new FastException("文件写入失败");
        }
        return dbSavePath;
    }

    public static File getAbsoluteFile(String fileFullPath) {
        File desc = new File(fileFullPath);
        if (!desc.exists()) {
            if (!desc.getParentFile().exists()) {
                desc.getParentFile().mkdirs();
            }
        }
        return desc;
    }

    /**
     * 获取文件名的后缀
     *
     * @param file 表单文件
     * @return 后缀名
     */
    public static String getExtension(MultipartFile file) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isEmpty(extension)) {
            extension = MimeTypeUtils.getExtension(Objects.requireNonNull(file.getContentType()));
        }
        return extension;
    }

    /**
     * 检查文件只能是JPG格式
     *
     * @param file
     */
    public static void checkJPGFileExtension(MultipartFile file) {
        String extension = getExtension(file);
        if (!"jpg".equalsIgnoreCase(extension)) {
            throw new FastException("只允许上传jpg");
        }
    }

    /**
     * 检查文件只能是excel格式
     *
     * @param file
     */
    public static void checkExcelFileExtension(MultipartFile file) {
        String extension = getExtension(file);
        if (!"xlsx".equalsIgnoreCase(extension) && !"xls".equalsIgnoreCase(extension)) {
            throw new FastException("只允许上传Excel");
        }
    }

}
