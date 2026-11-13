package com.dji.sample.df.cloudRendererDf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dji.sample.common.model.CustomClaim;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Collectors;

public class CloudRendererSettingsChecks {
    interface Work { void run() throws Exception; }
    static void check(boolean ok,String text) { if(!ok) throw new AssertionError(text); }
    static void rejects(Work work,int status) throws Exception {
        try { work.run(); } catch(ResponseStatusException e) { check(e.getStatus().value()==status,"expected HTTP "+status);return; }
        throw new AssertionError("expected rejection");
    }
    public static void main(String[] args) throws Exception {
        Path root=Files.createTempDirectory("cloud-renderer-settings-");
        try {
            ObjectMapper json=new ObjectMapper();
            Path file=root.resolve("settings.json");
            var service=new CloudRendererSettingsService(file.toString());
            check(!service.read().path("configured").asBoolean(),"initially absent");
            ObjectNode config=(ObjectNode)json.readTree("{\"enabled\":false,\"baseURL\":\"http://127.0.0.1:3000\",\"renderer\":\"outdoor\",\"pointCloudFile\":\"3dgs/site.ply\",\"rendererParams\":{\"alignSplatFile\":\"\",\"alignReferenceTileset\":\"\"},\"iceServers\":[]}");
            ObjectNode request=json.createObjectNode().put("revision",0);request.set("config",config);
            service.save(request);
            var restarted=new CloudRendererSettingsService(file.toString());
            check(!restarted.read().path("config").path("enabled").asBoolean(),"disabled survives backend restart");
            rejects(()->restarted.save(request),409);
            request.put("revision",1);config.put("enabled",true);restarted.save(request);
            check(service.read().path("config").path("enabled").asBoolean(),"other service sees saved config");
            request.put("revision",2);config.put("enabled","false");rejects(()->service.save(request),400);
            config.put("enabled",false);config.put("baseURL","file:///tmp/test");rejects(()->service.save(request),400);
            check(service.read().path("revision").asInt()==2,"invalid writes preserve old file");
            var controller=new CloudRendererSettingsController(service);
            CustomClaim[] claim={null};
            HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->m.getName().equals("getAttribute")?claim[0]:null);
            HttpServletResponse res=(HttpServletResponse)Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->null);
            rejects(()->controller.read(req,res),401);rejects(()->controller.save(request,req,res),401);
            claim[0]=new CustomClaim();claim[0].setWorkspaceId("workspace");claim[0].setUserType(2);
            rejects(()->controller.read(req,res),403);
            claim[0].setUserType(1);controller.read(req,res);
            Files.writeString(file,"broken");
            boolean failed=false;try { service.read(); } catch(Exception expected) { failed=true; }
            check(failed,"corrupt storage never resets defaults");
            System.out.println("Cloud renderer backend checks passed: restart persistence, shared reads, stale revision, validation, auth and corrupt-file rejection.");
        } finally {try(var walk=Files.walk(root)){for(Path path:walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList()))Files.deleteIfExists(path);}}
    }
}
