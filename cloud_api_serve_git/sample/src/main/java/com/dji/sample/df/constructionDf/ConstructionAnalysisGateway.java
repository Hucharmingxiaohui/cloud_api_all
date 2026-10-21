package com.dji.sample.df.constructionDf;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Implement as a Spring bean when the construction algorithm protocol is supplied.
 * No wind/solar endpoint is used as a fallback. Implementations must enforce timeouts.
 * Transfer original files (or verify shared access) before accepting a batch.
 * The batchId is an idempotency key on the analysis host, including ambiguous retries.
 * query must resolve by batchId even when submit timed out before returning a remoteId.
 * Result JSON and referenced evidence must be persisted locally before SUCCEEDED.
 */
public interface ConstructionAnalysisGateway {
    boolean checkConnection() throws Exception;
    String submit(String batchId, String jobId, List<Path> originals) throws Exception;
    // Keys: status (QUEUED/RUNNING/SUCCEEDED/FAILED), result (JSON object), message.
    Map<String, Object> query(String batchId, String remoteId) throws Exception;
}
