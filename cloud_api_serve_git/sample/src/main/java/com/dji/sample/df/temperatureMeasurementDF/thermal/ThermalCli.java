package com.dji.sample.df.temperatureMeasurementDF.thermal;

import com.dji.sample.df.temperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
public class ThermalCli {
    private final ThermalProperties properties;

    public ThermalCli(ThermalProperties properties) { this.properties = properties; }

    public TemResultEntity measure(Path original, TemParamEntity parameters) {
        try (InputStream input = Files.newInputStream(original)) { return measure(input, parameters); }
        catch (IOException e) { return failure("无法读取本地红外图片"); }
    }

    public TemResultEntity measure(InputStream source, TemParamEntity parameters) {
        return measure(source, parameters, false);
    }

    public TemResultEntity measureWholeImage(Path original) {
        try (InputStream input = Files.newInputStream(original)) { return measure(input, null, true); }
        catch (IOException e) { return failure("无法读取本地红外图片"); }
    }

    private TemResultEntity measure(InputStream source, TemParamEntity parameters, boolean wholeImage) {
        Path work = null;
        try {
            properties.validate();
            ThermalProperties.Cli config = properties.getCli();
            Path root = Path.of(config.getTempDir()); Files.createDirectories(root);
            work = Files.createTempDirectory(root, "thermal-");
            Path input = Files.createDirectory(work.resolve("input")), output = Files.createDirectory(work.resolve("output"));
            try (OutputStream target = Files.newOutputStream(input.resolve("source.jpg"), StandardOpenOption.CREATE_NEW)) {
                byte[] buffer = new byte[8192]; long count = 0; int length;
                while ((length = source.read(buffer)) != -1) {
                    count += length;
                    if (count > config.getMaxFileBytes()) throw new IOException("红外图片超过 thermal.cli.max-file-bytes");
                    target.write(buffer, 0, length);
                }
                if (count == 0) throw new IOException("红外图片为空");
            }
            List<String> command = new ArrayList<>(); command.add(config.getExecutable());
            for (String argument : config.getArguments()) command.add(argument.replace("{inputDir}", input.toAbsolutePath().toString())
                    .replace("{outputDir}", output.toAbsolutePath().toString()));
            run(command, work, config.getTimeoutSeconds());
            Path tiff = output.resolve("source_temp_c.tiff");
            try (var outputs = Files.list(output)) {
                if (outputs.filter(p -> p.getFileName().toString().endsWith("_temp_c.tiff")).count() != 1
                        || !Files.isRegularFile(tiff, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(output))
                    throw new IOException("CLI 未输出唯一 source_temp_c.tiff；请核对 process --help 和工具版本");
            }
            TemperatureMatrix matrix = TemperatureMatrix.read(tiff, config.getMaxFileBytes(), config.getMaxPixels());
            return wholeImage ? matrix.measureWholeImage() : matrix.measure(parameters);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return failure("CLI 测温被中断");
        } catch (IOException | IllegalArgumentException e) {
            log.warn("CLI 测温失败: {}", e.getMessage());
            return failure(e.getMessage());
        } finally {
            if (work != null) try { remove(work); }
            catch (IOException e) { log.warn("无法清理 CLI 临时目录 {}", work, e); }
        }
    }

    private static void run(List<String> command, Path work, int timeout) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).directory(work.toFile()).redirectErrorStream(true).start();
        ExecutorService drain = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "thermal-cli-output"); t.setDaemon(true); return t; });
        Future<String> diagnostic = drain.submit(() -> {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            try (InputStream stream = process.getInputStream()) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = stream.read(buffer)) != -1) {
                    int keep = Math.min(n, 8192 - captured.size());
                    if (keep > 0) captured.write(buffer, 0, keep);
                }
            }
            return captured.toString(StandardCharsets.UTF_8);
        });
        try {
            process.getOutputStream().close();
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) throw new IOException("CLI 测温超时 (" + timeout + "s)");
            String text;
            try { text = diagnostic.get(1, TimeUnit.SECONDS); }
            catch (ExecutionException | TimeoutException e) { throw new IOException("CLI 输出读取失败", e); }
            if (process.exitValue() != 0) {
                log.warn("Thermal CLI exit {}: {}", process.exitValue(), text);
                throw new IOException("CLI 测温失败，退出码 " + process.exitValue() + "；请检查服务日志");
            }
        } finally {
            List<ProcessHandle> descendants = new ArrayList<>(); process.descendants().forEach(descendants::add);
            descendants.forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
            try { process.getInputStream().close(); } finally { diagnostic.cancel(true); drain.shutdownNow(); }
        }
    }

    private static void remove(Path work) throws IOException {
        Files.walkFileTree(work, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }

    private static TemResultEntity failure(String message) {
        TemResultEntity result = new TemResultEntity(); result.setError(message); return result;
    }
}
