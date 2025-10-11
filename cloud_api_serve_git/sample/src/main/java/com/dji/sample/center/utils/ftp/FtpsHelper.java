package com.dji.sample.center.utils.ftp;

import com.dji.sample.center.utils.FilePathCheckUtils;
import com.dji.sample.center.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
public class FtpsHelper implements Closeable {
    private FTPSClient ftp = null;
    boolean _isLogin = false;
    boolean _isUsing = true;

    @Override
    public void close() {
        closeConnection();
    }

    /**
     * 杨西明 判断是否连接还在
     */
    public boolean isUsing() {
        return this._isUsing;
    }

    /**
     * 杨西明 判断是否连接还在
     */
    public boolean isLogin() {
        if (this.ftp != null) {
            return this._isLogin;
        }
        return false;
    }

    /**
     * 关闭链接
     */
    private void closeConnection() {
        this._isLogin = false;
        if (this.ftp != null && this.ftp.isConnected()) {
            try {
                this.ftp.logout();
                this.ftp.disconnect();
            } catch (IOException e) {
                log.error("FTP关闭连接异常：{}", e.getMessage());
            }
            this.ftp = null;
        }
    }

    /**
     * 判断ftp是否连接
     *
     * @return
     */
    public boolean isConnect() {
        return this.ftp != null && this.ftp.isConnected();
    }

    /**
     * 登录到FTP服务
     *
     * @param ip
     * @param port
     * @param uname
     * @param pass
     * @param implicit false显式，true隐式
     * @return
     */
    public boolean login(String ip, int port, String uname, String pass, boolean implicit) {
        this.ftp = new SSLSessionReuseFTPSClient(implicit);
        FTPClientConfig config = new FTPClientConfig();
        this.ftp.configure(config);
        this.ftp.setAuthValue("TLS");
        this.ftp.setConnectTimeout(10000);//tcp握手连接超时，单位毫秒
        // SoTimeout读超时，服务端flush到输出流的间隔时间，也就是服务端多久时间内必须flush(或者close)一次到输出流。
        // 传输数据不会发送一个数据包，SoTimeout不是指整个传输时间，而是指两个数据包之间的间隔时间。
        this.ftp.setDefaultTimeout(10000);//传输控制命令的SoTimeout，一个命令等待响应结果的超时时间，单位毫秒
        this.ftp.setDataTimeout(10000);//传输数据的SoTimeout，单位毫秒
        //下面2个一般成对出现
        this.ftp.setControlKeepAliveTimeout(60);//传输数据这个过程中，传输控制命令的心跳(保活)时间，单位秒,tcp网络编程中一般设置心跳为300秒
        this.ftp.setControlKeepAliveReplyTimeout(10000);//传输数据这个过程中，暂时替换掉传输控制命令的SoTimeout，单位毫秒
        this.ftp.setRemoteVerificationEnabled(false);//取消服务器获取自身Ip地址和提交的host进行匹配
        try {
            this.ftp.connect(ip, port);
            this._isLogin = this.ftp.login(uname, pass);
            this.ftp.setSoTimeout(10000);
            log.info("FTPS：{}", this._isLogin ? "登录成功" : "登录失败");
            int reply = this.ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                log.error("FTPS：服务器拒绝连接");
                this.ftp.disconnect();
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.error("FTPS：登录发生异常：{} {}", ex.getMessage(),ex);
            return false;
        }
    }

    /**
     * 上传文件到服务器
     *
     * @param localFile    本地文件路径
     * @param destDir      目的地路径
     * @param destFileName 上传到服务器文件名
     * @return
     */
    public boolean uploadFile(String localFile, String destDir, String destFileName) {
        this._isUsing = true;

        if (StringUtil.isEmpty(destFileName)) {
            this._isUsing = false;
            throw new RuntimeException("上传到服务器的文件名不能为空！");
        }

        //安全扫描要求上传文件路径必须经过校验
        localFile = FilePathCheckUtils.cleanString(localFile);
        File srcFile = new File(localFile);
        if (!srcFile.exists()) {
            this._isUsing = false;
            throw new RuntimeException("FTPS：要上传的本地文件不存在，文件：" + localFile);
        }

        try (FileInputStream fis = new FileInputStream(srcFile)) {
            boolean flag = enterOrCreateDir(destDir);
            if (!flag) {
                this._isUsing = false;
                throw new RuntimeException("FTPS：切入FTP目录失败，目录：" + destDir);
            }
            log.info("FTPS：准备上传文件流，文件：{}/{}", destDir, destFileName);
            this.ftp.execPBSZ(0L);
            this.ftp.execPROT("P");
            this.ftp.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            this.ftp.enterLocalPassiveMode(); //被动模式
            this.ftp.setBufferSize(1024 * 512);
            this.ftp.setControlEncoding("UTF-8");
            this.ftp.setFileType(FTP.BINARY_FILE_TYPE);
            /*System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,SSLv3");
            this.ftp.setEnabledProtocols(new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3"});
            System.setProperty("jdk.tls.useExtendedMasterSecret", "false");*/
            String fileName = new String(destFileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            fileName = FilePathCheckUtils.cleanString(fileName);
            if (this.ftp.storeFile(fileName, fis)) {
                log.info("FTPS：文件上传成功，文件：{}/{}", destDir, destFileName);
                this._isUsing = false;
                return true;
            }
            this._isUsing = false;
            return false;
        } catch (Exception e) {
            log.info("FTPS：文件上传失败，异常信息  {} {} ", e.getMessage(),e);
            return false;
        }
    }

    /**
     * 本地上传文件到服务器
     *
     * @param localFile    本地文件
     * @param destDir      目的地路径
     * @param destFileName 上传到服务器文件名
     * @return
     */
    public boolean localFileUpload(File localFile, String destDir, String destFileName) {
        this._isUsing = true;
        if (StringUtil.isEmpty(destFileName)) {
            this._isUsing = false;
            throw new RuntimeException("上传到服务器的文件名不能为空！");
        }

        //安全扫描要求上传文件路径必须经过校验
        if (!localFile.exists()) {
            this._isUsing = false;
            throw new RuntimeException("FTPS：要上传的本地文件不存在，文件：" + localFile);
        }

        try (FileInputStream fis = new FileInputStream(localFile)) {
            boolean flag = enterOrCreateDir(destDir);
            if (!flag) {
                this._isUsing = false;
                throw new RuntimeException("FTPS：切入FTP目录失败，目录：" + destDir);
            }
            log.info("FTPS：准备上传文件流，文件：{}/{}", destDir, destFileName);
            this.ftp.execPBSZ(0L);
            this.ftp.execPROT("P");
            this.ftp.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            this.ftp.enterLocalPassiveMode(); //被动模式
            this.ftp.setBufferSize(1024 * 512);
            this.ftp.setControlEncoding("UTF-8");
            this.ftp.setFileType(FTP.BINARY_FILE_TYPE);
            /*System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,SSLv3");
            this.ftp.setEnabledProtocols(new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3"});
            System.setProperty("jdk.tls.useExtendedMasterSecret", "false");*/

            String fileName = new String(destFileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            fileName = FilePathCheckUtils.cleanString(fileName);
            if (this.ftp.storeFile(fileName, fis)) {
                log.info("FTPS：文件上传成功，文件：{}/{}", destDir, destFileName);
                this._isUsing = false;
                return true;
            }
            this._isUsing = false;
            return false;
        } catch (Exception e) {
            log.info("FTPS：文件上传失败，异常信息 {} {} ", e.getMessage(),e);
            return false;
        }
    }

    /**
     * 上传文件到服务器，主动/被动模式可配置
     *
     * @param localFile    本地文件路径
     * @param destDir      目的地路径
     * @param destFileName 上传到服务器文件名
     * @param passiveMode  true被动模式，false主动模式
     * @return
     */
    public boolean uploadFile(String localFile, String destDir, String destFileName, String passiveMode) {
        this._isUsing = true;
        if (StringUtil.isEmpty(destFileName)) {
            this._isUsing = false;
            throw new RuntimeException("上传到服务器的文件名不能为空！");
        }

        //安全扫描要求上传文件路径必须经过校验
        localFile = FilePathCheckUtils.cleanString(localFile);
        File srcFile = new File(localFile);
        if (!srcFile.exists()) {
            this._isUsing = false;
            throw new RuntimeException("FTPS：要上传的本地文件不存在，文件：" + localFile);
        }

        try (FileInputStream fis = new FileInputStream(srcFile)) {
            boolean flag = enterOrCreateDir(destDir);
            if (!flag) {
                this._isUsing = false;
                throw new RuntimeException("FTPS：切入FTP目录失败，目录：" + destDir);
            }
            log.info("FTPS：准备上传文件流，文件：{}/{}", destDir, destFileName);
            this.ftp.execPBSZ(0L);
            this.ftp.execPROT("P");
            this.ftp.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            this.ftp.enterLocalPassiveMode(); //被动模式
            this.ftp.setBufferSize(1024 * 512);
            this.ftp.setControlEncoding("UTF-8");
            this.ftp.setFileType(FTP.BINARY_FILE_TYPE);
            if ("true".equals(passiveMode)) {
                this.ftp.enterLocalPassiveMode(); //被动模式
            } else {
                this.ftp.enterLocalActiveMode(); //主动模式
            }
            String fileName = new String(destFileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            fileName = FilePathCheckUtils.cleanString(fileName);
            if (this.ftp.storeFile(fileName, fis)) {
                log.info("FTPS：文件上传成功，文件：{}/{}", destDir, destFileName);
                this._isUsing = false;
                return true;
            }
            this._isUsing = false;
            return false;
        } catch (Exception e) {
            log.info("FTPS：文件上传失败，异常信息： {} {} ", e.getMessage(),e);
            return false;
        }
    }

    /**
     * 切入或创建文件夹
     *
     * @param dir
     * @return
     */
    public boolean enterOrCreateDir(String dir) {
        if (StringUtil.isEmpty(dir)) {
            return true;
        }
        try {
            String d = new String(dir.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            if (this.ftp.changeWorkingDirectory(d)) {
                return true;
            }
            dir = StringUtil.trimStart(dir, "/");
            dir = StringUtil.trimEnd(dir, "/");
            String[] arr = dir.split("/");
            StringBuilder sbfDir = new StringBuilder("/");
            byte b;
            int i;
            String[] arrayOfString1;
            for (i = (arrayOfString1 = arr).length, b = 0; b < i; ) {
                String s = arrayOfString1[b];
                sbfDir.append(s);
                d = new String(sbfDir.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

                String dChecked = FilePathCheckUtils.cleanString(d);
                if (this.ftp.changeWorkingDirectory(dChecked)) {
                    //sbfDir = new StringBuffer(d+"/");
                    sbfDir = new StringBuilder(sbfDir + "/");
                } else {
                    if (!this.ftp.makeDirectory(dChecked)) {
                        log.error("FTPS：创建目录[失败]：" + sbfDir);
                        return false;
                    }
                    sbfDir.append("/");
                }
                b++;
            }
            return this.ftp.changeWorkingDirectory(d);
        } catch (Exception e) {
            log.error("FTPS：创建目录发生异常： {} {} ", e.getMessage(),e);
            return false;
        }
    }


    /**
     * 文件下载
     *
     * @param remotePath ftp上的地址，如：100002/Model/device_model_33.xml
     * @param localPath  本地存放文件的地址，如：D:/tmp/device_model_33.xml
     */
    public boolean download(String remotePath, String localPath) throws IOException {
        this._isUsing = true;
        File localFile = new File(localPath);
        File dir = localFile.getParentFile();
        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (SecurityException | NullPointerException e1) {
                log.error("FTPS：创建文件夹异常，异常信息：{}", e1.getMessage());
            } finally {
                this._isUsing = false;
            }
        }

        try (OutputStream os = Files.newOutputStream(localFile.toPath())) {
            this.ftp.execPBSZ(0L);
            this.ftp.setBufferSize(1024 * 512);
            // 数据传输需要加密——以下代码段，否则会出522错误。
            ftp.setControlEncoding("UTF-8");
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            ftp.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            ftp.execPROT("P");
            boolean flag = this.ftp.retrieveFile(remotePath, os);
            this._isUsing = false;
            return flag;
        } catch (IOException e) {
            this._isUsing = false;
            return false;
        }
    }
}

