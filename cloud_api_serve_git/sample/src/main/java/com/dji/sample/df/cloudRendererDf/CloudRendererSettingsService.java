package com.dji.sample.df.cloudRendererDf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.Set;

/** One configuration per deployed backend; no browser storage or workspace override. */
@Service
public class CloudRendererSettingsService {
    private final Path file;
    private final ObjectMapper json = new ObjectMapper();
    public CloudRendererSettingsService(@Value("${platform.cloud-renderer-config-file:./data/platform/cloud-renderer.json}") String path) {
        file = Paths.get(path).toAbsolutePath().normalize();
    }
    private static ResponseStatusException bad(String reason) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
    private static String text(JsonNode node, String key, int limit) {
        JsonNode value = node.get(key);
        if(value == null || !value.isTextual() || value.asText().length() > limit) throw bad("云渲染配置字段无效：" + key);
        return value.asText();
    }
    private static void fields(JsonNode node, Set<String> allowed) {
        if(node == null || !node.isObject()) throw bad("云渲染配置必须为对象");
        node.fieldNames().forEachRemaining(key -> { if(!allowed.contains(key)) throw bad("未知云渲染配置字段：" + key); });
    }
    private void validate(JsonNode config) {
        fields(config, Set.of("enabled","baseURL","renderer","pointCloudFile","rendererParams","iceServers"));
        if(!config.path("enabled").isBoolean()) throw bad("enabled 必须为布尔值");
        try {
            URI uri = URI.create(text(config,"baseURL",2048));
            if(!Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null) throw new IllegalArgumentException();
        } catch(IllegalArgumentException e) { throw bad("云渲染服务地址必须为有效的 HTTP 或 HTTPS 地址"); }
        if(!Set.of("outdoor","align","wayline").contains(text(config,"renderer",20))) throw bad("渲染模式无效");
        text(config,"pointCloudFile",4096);
        JsonNode params=config.get("rendererParams");fields(params,Set.of("alignSplatFile","alignReferenceTileset"));
        text(params,"alignSplatFile",4096);text(params,"alignReferenceTileset",4096);
        JsonNode servers=config.get("iceServers");
        if(servers==null || !servers.isArray() || servers.size()>16) throw bad("ICE 服务配置无效");
        for(JsonNode server:servers) {
            fields(server,Set.of("urls","username","credential"));
            String url=text(server,"urls",2048);
            if(!url.matches("(?i)^(stun|stuns|turn|turns):\\S+$")) throw bad("ICE 服务地址无效");
            if(server.has("username")) text(server,"username",1024);
            if(server.has("credential")) text(server,"credential",4096);
        }
        if(config.toString().length()>65536) throw bad("云渲染配置过大");
    }
    public synchronized ObjectNode read() throws IOException {
        if(!Files.exists(file)) return json.createObjectNode().put("configured",false).put("revision",0);
        if(Files.size(file)>262144) throw new IOException("云渲染配置文件过大");
        JsonNode stored=json.readTree(file.toFile());
        if(stored==null || !stored.isObject() || !stored.path("revision").isIntegralNumber() || stored.path("revision").asLong()<1 || !stored.path("configured").asBoolean()) throw new IOException("云渲染配置文件损坏");
        validate(stored.get("config"));
        return (ObjectNode)stored;
    }
    public synchronized ObjectNode save(JsonNode request) throws IOException {
        fields(request,Set.of("revision","config"));
        JsonNode revision=request.get("revision");
        if(revision==null || !revision.isIntegralNumber() || !revision.canConvertToLong() || revision.asLong()<0 || revision.asLong()==Long.MAX_VALUE) throw bad("配置版本无效");
        validate(request.get("config"));
        Files.createDirectories(file.getParent());
        try(FileChannel channel=FileChannel.open(file.resolveSibling(file.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            FileLock ignored=channel.lock()) {
            ObjectNode current=read();
            if(current.path("revision").asLong()!=revision.asLong()) throw new ResponseStatusException(HttpStatus.CONFLICT,"云渲染配置已被修改，请重新读取后再保存");
            ObjectNode result=json.createObjectNode().put("configured",true).put("revision",revision.asLong()+1);
            result.set("config",request.get("config").deepCopy());
            Path temp=Files.createTempFile(file.getParent(),"cloud-renderer-",".tmp");
            try {
                json.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(),result);
                Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temp); }
            return result;
        }
    }
}
