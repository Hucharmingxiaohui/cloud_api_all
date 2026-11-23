package com.dji.sample.media.service.impl;

import com.dji.sample.media.dao.IFileMapper;
import com.dji.sample.media.model.MediaFileEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

@Service
public class MediaFilePersistence {
    private static final Object SAVE_LOCK = new Object();
    private final IFileMapper mapper;
    private final TransactionTemplate transaction;

    public MediaFilePersistence(IFileMapper mapper, PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int save(String workspaceId, String jobId, String objectKey,
                    Consumer<String> setFileId, IntSupplier insert) {
        // Commit before releasing the shared JVM lock, even when called inside an outer transaction.
        synchronized (SAVE_LOCK) {
            return transaction.execute(status -> {
                MediaFileEntity existing = StringUtils.hasText(objectKey)
                        ? mapper.findUploadedFile(workspaceId, jobId, objectKey) : null;
                if (existing != null) {
                    setFileId.accept(existing.getFileId());
                    return 1;
                }
                return insert.getAsInt();
            });
        }
    }
}
