package com.df.framework.utils.file;

import com.df.framework.exception.FastException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * 文件处理工具类
 *
 * @author ruoyi
 */
public class FileUtils extends org.apache.commons.io.FileUtils {
    public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 输出指定文件的byte数组
     *
     * @param filePath 文件路径
     * @param os       输出流
     * @return
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } catch (IOException e) {
            log.error("{}", e);
            throw e;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    //e1.printStackTrace();
                    log.error("{}", e1);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    // e1.printStackTrace();
                    log.error("{}", e1);
                }
            }
        }
    }

    /**
     * 本地服务器文件下载
     *
     * @param fileFullPath 本地文件绝对路径
     * @param response     前端请求
     */
    public static void downloadLocalFile(String fileFullPath, HttpServletResponse response) {
        File file = new File(fileFullPath);
        if (!file.exists()) {
            throw new FastException("文件不存在");
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try {
            String fileName = fileFullPath.substring(fileFullPath.lastIndexOf("/") + 1);
            setAttachmentResponseHeader(response, fileName);
        } catch (UnsupportedEncodingException e) {
            log.error("{}", e);
            return;
        }
        try (
                FileInputStream fis = new FileInputStream(file);
                ServletOutputStream outputStream = response.getOutputStream()
        ) {
            byte[] b = new byte[1024 * 1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                outputStream.write(b, 0, length);
            }
        } catch (IOException e) {
            log.error("{}", e);
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件
     * @return
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {

            try {
                file.delete();
            } catch (SecurityException | NullPointerException e1) {
                log.error("删除文件夹异常：{}", e1.getMessage());
            }
            flag = true;
        }
        return flag;
    }

    /**
     * 项目工程代码静态文件下载，默认目录 static/excel/XXXX.xx
     *
     * @param fileFullPath 文件目录  static/excel/XXXX.xx
     * @param response     前端请求
     */
    public static void downloadProjectFile(String fileFullPath, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try {
            String fileName = fileFullPath.substring(fileFullPath.lastIndexOf("/") + 1);
            setAttachmentResponseHeader(response, fileName);
        } catch (UnsupportedEncodingException e) {
            log.error("{}", e);
            return;
        }
        try (
                InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(fileFullPath);
                ServletOutputStream outputStream = response.getOutputStream()
        ) {
            byte[] b = new byte[1024];
            int length;
            while ((length = in.read(b)) > 0) {
                outputStream.write(b, 0, length);
            }
        } catch (IOException e) {
            log.error("{}", e);
        }
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * 检查文件是否可下载
     *
     * @param resource 需要下载的文件
     * @return true 正常 false 非法
     */
    public static boolean checkAllowDownload(String resource) {
        // 禁止目录上跳级别
        if (StringUtils.contains(resource, "..")) {
            return false;
        }

        // 检查允许下载的文件规则
        if (ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, FileTypeUtils.getFileType(resource))) {
            return true;
        }

        // 不在允许下载的文件规则
        return false;
    }

    /**
     * 检查文件是否可下载（相比上面的方法，多允许了一个xml文件下载）
     *
     * @param resource 需要下载的文件
     * @return true 正常 false 非法
     */
    public static boolean checkAllowDownloadNew(String resource) {
        // 禁止目录上跳级别
        if (StringUtils.contains(resource, "..")) {
            return false;
        }

        // 检查允许下载的文件规则
        if (ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION_ADD_XML, FileTypeUtils.getFileType(resource))) {
            return true;
        }

        // 不在允许下载的文件规则
        return false;
    }

    /**
     * 下载文件名重新编码
     *
     * @param request  请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 下载文件名重新编码
     *
     * @param response     响应对象
     * @param realFileName 真实文件名
     * @return
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; filename=")
                .append(percentEncodedFileName)
                .append(";")
                .append("filename*=")
                .append("utf-8''")
                .append(percentEncodedFileName);

        response.setHeader("Content-disposition", contentDispositionValue.toString());
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        return encode.replaceAll("\\+", "%20");
    }

    /**
     * 删除指定文件夹下文件
     *
     * @param filePath
     */
    public static void deleteFolders(String filePath) {
        Path path = Paths.get(filePath);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    //log.info("删除文件: {}", file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException exc) throws IOException {
                    Files.delete(dir);
                    //log.info("文件夹被删除: {}", dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFilesByLimitDate(String filePath, long LimitMillis) {
        Path path = Paths.get(filePath);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    FileTime lastModifiedTime = Files.getLastModifiedTime(file);
                    long l = lastModifiedTime.toMillis();
                    if (LimitMillis > l) {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {

                        }
                        log.info("删除文件: {}", file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException exc) throws IOException {
                    /*Files.delete(dir);
                    log.info("文件夹被删除: {}", dir);*/
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getFileLocalSize(final String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return 0;
        }
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            return 0;
        }
        return file.length();

    }
}
