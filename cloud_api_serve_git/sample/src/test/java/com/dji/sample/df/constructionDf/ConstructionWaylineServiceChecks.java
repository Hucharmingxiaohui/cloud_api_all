package com.dji.sample.df.constructionDf;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.model.dto.WaylineFileDTO;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sdk.cloudapi.wayline.GetWaylineListResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ConstructionWaylineServiceChecks {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    static void rejects(Runnable call,int code) { try { call.run(); } catch(RuleException e) { check(e.getCode()==code);return; } throw new AssertionError("Expected "+code); }
    public static void run(byte[] bytes) throws Exception {
        Map<String,Object> area=new HashMap<>();
        area.put("version",1);area.put("enabled",true);area.put("archived",false);
        area.put("boundary",List.of(Map.of("lng",120d,"lat",36d),Map.of("lng",120.01,"lat",36d),Map.of("lng",120.01,"lat",36.01),Map.of("lng",120d,"lat",36.01)));
        ConstructionAreaService areas=new ConstructionAreaService(null,null) {
            @Override public Map<String,Object> detail(String w,String a) { if(!"w".equals(w)||!"a".equals(a)) throw new RuleException(404,"missing");return new HashMap<>(area); }
        };
        List<Map<String,Object>> rows=new ArrayList<>();
        JdbcTemplate db=new JdbcTemplate() {
            @Override public List<Map<String,Object>> queryForList(String sql,Object... args) {
                if(sql.contains("FOR UPDATE")) return "w".equals(args[0])&&"a".equals(args[1])?List.of(Map.of("id","a")):List.of();
                List<Map<String,Object>> result=new ArrayList<>();
                for(Map<String,Object> r:rows) if(Objects.equals(r.get("workspace_id"),args[0])&&Objects.equals(r.get("area_id"),args[1])&&(args.length==2||Objects.equals(r.get(sql.contains("request_id=?")?"request_id":"wayline_id"),args[2]))) result.add(r);
                return result;
            }
            @Override public int update(String sql,Object... args) {
                if(sql.startsWith("INSERT")) {
                    String[] keys={"id","workspace_id","area_id","wayline_id","area_version","file_sign","source","request_id","request_hash","points_json","height_mode"};
                    Map<String,Object> row=new HashMap<>();for(int i=0;i<keys.length;i++)row.put(keys[i],args[i]);rows.add(row);
                } else if(sql.startsWith("UPDATE")) { rows.get(0).put("area_version",args[0]);rows.get(0).put("file_sign",args[1]);rows.get(0).put("points_json",args[2]);rows.get(0).put("height_mode",args[3]); }
                else if(sql.startsWith("DELETE")) rows.clear();
                return 1;
            }
        };
        Map<String,GetWaylineListResponse> catalog=new HashMap<>();
        IWaylineFileService files=(IWaylineFileService)Proxy.newProxyInstance(IWaylineFileService.class.getClassLoader(),new Class[]{IWaylineFileService.class},(p,m,args)-> {
            if(m.getName().equals("saveWaylineFile")) {
                WaylineFileDTO dto=(WaylineFileDTO)args[1];check(args[0].equals("w"));
                catalog.put("real-wayline-id",new GetWaylineListResponse().setId("real-wayline-id").setName(dto.getName()).setSign(dto.getSign()).setObjectKey(dto.getObjectKey()));return 42;
            }
            if(m.getName().equals("getWaylineByWaylineId")) return Optional.ofNullable(catalog.get(args[1]));
            throw new UnsupportedOperationException(m.getName());
        });
        IWaylineFileMapper mapper=(IWaylineFileMapper)Proxy.newProxyInstance(IWaylineFileMapper.class.getClassLoader(),new Class[]{IWaylineFileMapper.class},(p,m,args)-> {
            check(m.getName().equals("selectById")&&args[0].equals(42));return WaylineFileEntity.builder().waylineId("real-wayline-id").build();
        });
        OssConfiguration.enable=false;
        Map<String,byte[]> objects=new HashMap<>();
        OssServiceContext oss=new OssServiceContext(List.of(),new OssConfiguration()) {
            @Override public void putObject(String bucket,String key,InputStream in) { try { objects.put(key,in.readAllBytes()); } catch(IOException e) { throw new RuntimeException(e); } }
            @Override public InputStream getObject(String bucket,String key) { return objects.containsKey(key)?new ByteArrayInputStream(objects.get(key)):null; }
            @Override public Boolean deleteObject(String bucket,String key) { objects.remove(key);return true; }
        };
        ConstructionWaylineService service=new ConstructionWaylineService(areas,db,files,mapper,oss);
        TransactionSynchronizationManager.initSynchronization();
        try {
            Map<String,Object> result=service.upload("w","a",1,"request-00000000001","航线",bytes,"user");
            check(result.get("wayline_id").equals("real-wayline-id"));check(result.get("status").equals("VALID"));check(rows.size()==1&&objects.size()==1);
            check(service.upload("w","a",1,"request-00000000001","航线",bytes,"user").get("wayline_id").equals("real-wayline-id"));check(rows.size()==1&&objects.size()==1);
            rejects(()->service.upload("w","a",1,"request-00000000001","different",bytes,"user"),409);
            rejects(()->service.list("other","a"),404);
            area.put("version",2);check(service.detail("w","a","real-wayline-id").get("status").equals("STALE"));check(!service.detail("w","a","real-wayline-id").containsKey("points"));
            check(service.validate("w","a","real-wayline-id").get("status").equals("VALID"));
            area.put("enabled",false);check(service.list("w","a").get(0).get("status").equals("DISABLED"));
            rejects(()->service.upload("w","a",2,"request-00000000002","second",bytes,"user"),400);
            area.put("enabled",true);catalog.get("real-wayline-id").setSign("changed");check(service.list("w","a").get(0).get("status").equals("STALE"));
            try { service.validate("w","a","real-wayline-id");throw new AssertionError(); } catch(RuleException expected) { check(expected.getMessage().contains("签名")); }
            service.unlink("w","a","real-wayline-id");check(rows.isEmpty()&&catalog.size()==1&&objects.size()==1);
            System.out.println("Construction service checks passed: shared ID, idempotency, cross-workspace denial, stale version/sign, revalidation, disabled area and unlink preserves shared file. In-memory collaborators; no database integration.");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
