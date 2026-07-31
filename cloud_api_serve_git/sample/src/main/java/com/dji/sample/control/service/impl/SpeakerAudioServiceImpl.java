package com.dji.sample.control.service.impl;

import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.control.model.dto.SpeakerAudioUploadDTO;
import com.dji.sample.control.service.ISpeakerAudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SpeakerAudioServiceImpl implements ISpeakerAudioService {

    private static final long MAX_PCM_BYTES = 64L * 1024 * 1024;
    private static final String OBJECT_PREFIX = "temp/as1-audio";
    private static final String FORMAT = "pcm";

    @Autowired
    private OssServiceContext ossService;

    @Override
    public SpeakerAudioUploadDTO uploadPcm(String workspaceId, String dockSn, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The speaker audio file is empty.");
        }
        if (file.getSize() > MAX_PCM_BYTES) {
            throw new IllegalArgumentException("The speaker audio file exceeds the 64MB limit.");
        }
        if (!StringUtils.hasText(dockSn)) {
            throw new IllegalArgumentException("dock_sn is required.");
        }

        String name = "as1-record-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String objectKey = OBJECT_PREFIX + "/" + dockSn + "/" + date + "/" + name + ".pcm";

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new IllegalArgumentException("The speaker audio file is empty.");
            }
            String md5 = md5Hex(new ByteArrayInputStream(bytes));
            ossService.putObject(OssConfiguration.bucket, objectKey, new ByteArrayInputStream(bytes));
            URL objectUrl = ossService.getObjectUrl(OssConfiguration.bucket, objectKey);
            if (objectUrl == null) {
                ossService.deleteObject(OssConfiguration.bucket, objectKey);
                throw new IllegalStateException("Failed to generate speaker audio download url.");
            }
            log.info("Uploaded AS1 speaker audio. dockSn={}, objectKey={}, size={}, md5={}", dockSn, objectKey, bytes.length, md5);
            return SpeakerAudioUploadDTO.builder()
                    .name(name)
                    .url(objectUrl.toString())
                    .md5(md5)
                    .format(FORMAT)
                    .objectKey(objectKey)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload speaker audio file.", e);
        }
    }

    @Override
    public Boolean delete(String objectKey) {
        if (!StringUtils.hasText(objectKey) || !objectKey.startsWith(OBJECT_PREFIX + "/")) {
            throw new IllegalArgumentException("Invalid speaker audio object key.");
        }
        return ossService.deleteObject(OssConfiguration.bucket, objectKey);
    }

    private String md5Hex(InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = input.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to calculate speaker audio md5.", e);
        }
    }
}
