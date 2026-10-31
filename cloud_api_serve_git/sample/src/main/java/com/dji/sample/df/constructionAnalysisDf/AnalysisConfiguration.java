package com.dji.sample.df.constructionAnalysisDf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.nio.file.Paths;

@Configuration
public class AnalysisConfiguration {
    @Bean public AnalysisService folderAnalysisService(AnalysisMetadata metadata,
            @Value("${construction-analysis.input-root:./data/construction-analysis/input}") String input,
            @Value("${construction-analysis.result-root:./data/construction-analysis/output}") String output,
            @Value("${construction-analysis.algorithm-url:}") String url,
            @Value("${construction-analysis.algorithm-token:}") String token,
            @Value("${construction-analysis.connect-timeout-ms:5000}") int connect,
            @Value("${construction-analysis.read-timeout-ms:30000}") int read,
            @Value("${construction-analysis.max-photos:100}") int photos,
            @Value("${construction-analysis.max-file-bytes:20971520}") long file,
            @Value("${construction-analysis.max-total-bytes:104857600}") long total) {
        return new AnalysisService(new AnalysisStore(Paths.get(input),Paths.get(output)),metadata,
                new AnalysisClient(url,token,connect,read,4*((file+2)/3)+1024*1024),photos,file,total);
    }
}
