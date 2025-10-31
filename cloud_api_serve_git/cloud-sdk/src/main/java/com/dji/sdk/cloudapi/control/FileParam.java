package com.dji.sdk.cloudapi.control;

public class FileParam {

    private String fingerprint;

    private String url;

        // 无参构造方法
    public FileParam() {
        }

        // 全参构造方法
     public FileParam(String fingerprint, String url) {
         this.fingerprint = fingerprint;
         this.url = url;
     }

     // Getter方法
     public String getFingerprint() {
         return fingerprint;
     }

     // Setter方法
     public void setFingerprint(String fingerprint) {
         this.fingerprint = fingerprint;
     }

     // Getter方法
     public String getUrl() {
         return url;
     }

     // Setter方法
     public void setUrl(String url) {
         this.url = url;
     }

     // toString方法
     @Override
     public String toString() {
         return "FileParam{" +
                 "fingerprint='" + fingerprint + '\'' +
                 ", url='" + url + '\'' +
                 '}';
     }
}
