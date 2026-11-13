package com.dji.sample.df.temperatureMeasurementDF.thermal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "thermal")
public class ThermalProperties {
    public enum Provider { sdk, cli }
    private Provider provider = Provider.sdk;
    private Cli cli = new Cli();

    @Data
    public static class Cli {
        private String executable = "/opt/python3.12.6/python-headless-3.12.6-linux-aarch64/bin/dji-thermal-cli";
        private List<String> arguments = new ArrayList<>();
        private String tempDir = System.getProperty("java.io.tmpdir");
        private int timeoutSeconds = 60;
        private long maxFileBytes = 64L * 1024 * 1024;
        private long maxPixels = 4L * 1024 * 1024;
    }

    @PostConstruct
    public void validate() {
        if (provider == null) throw new IllegalArgumentException("thermal.provider must be sdk or cli");
        if (provider != Provider.cli) return;
        if (cli == null || cli.executable == null || cli.executable.isBlank()
                || cli.tempDir == null || cli.tempDir.isBlank() || cli.timeoutSeconds < 1 || cli.timeoutSeconds > 600
                || cli.maxFileBytes < 1 || cli.maxFileBytes > 512L * 1024 * 1024
                || cli.maxPixels < 1 || cli.maxPixels > 16L * 1024 * 1024
                || cli.arguments == null || cli.arguments.isEmpty() || cli.arguments.stream().anyMatch(a -> a == null || a.indexOf('\0') >= 0)
                || cli.arguments.stream().noneMatch(a -> a.contains("{inputDir}"))
                || cli.arguments.stream().noneMatch(a -> a.contains("{outputDir}"))) {
            throw new IllegalArgumentException("Invalid thermal.cli configuration: require executable, temp-dir, bounded limits and arguments containing {inputDir}/{outputDir}; verify dji-thermal-cli process --help");
        }
    }
}
