package com.dji.sample.df.waylineDf.kmz.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class MyMultipartFile implements MultipartFile {
    private final File file;

    public MyMultipartFile(String filePath) throws FileNotFoundException {
        this.file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getOriginalFilename() {
        return file.getName();
    }

    @Override
    public String getContentType() {
        return null; // You can implement this method based on your requirements
    }

    @Override
    public boolean isEmpty() {
        return file.length() == 0;
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public byte[] getBytes() throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (InputStream inputStream = new FileInputStream(file)) {
            inputStream.read(bytes);
        }
        return bytes;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (InputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}

