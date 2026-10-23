package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.kmzDf.entity.wayline.Wayline;
import com.dji.sample.df.kmzDf.utils.CreateKmz;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.model.dto.WaylineFileDTO;
import com.dji.sdk.cloudapi.device.*;
import com.dji.sdk.cloudapi.wayline.GetWaylineListResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.DigestUtils;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ConstructionWaylineService {
    private final ConstructionAreaService areas; private final JdbcTemplate db;
    private final IWaylineFileService files; private final IWaylineFileMapper mapper; private final OssServiceContext oss;
    private final ObjectMapper json=new ObjectMapper();
    public ConstructionWaylineService(ConstructionAreaService areas,JdbcTemplate db,IWaylineFileService files,IWaylineFileMapper mapper,OssServiceContext oss) {
        this.areas=areas;this.db=db;this.files=files;this.mapper=mapper;this.oss=oss;
    }
    private RuleException bad(String message) { return new RuleException(400,message); }
    private Map<String,Object> lock(String workspace,String id) {
        if(db.queryForList("SELECT id FROM construction_area WHERE workspace_id=? AND id=? FOR UPDATE",workspace,id).isEmpty()) throw new RuleException(404,"区域不存在");
        return areas.detail(workspace,id);
    }
    private void writable(Map<String,Object> area,int version) {
        if(Boolean.TRUE.equals(area.get("archived"))||!Boolean.TRUE.equals(area.get("enabled"))) throw bad("区域已归档或暂停，不能创建或校验航线");
        if(((Number)area.get("version")).intValue()!=version) throw new RuleException(409,"区域边界已更新，请返回区域页面刷新后重试");
    }
    @SuppressWarnings("unchecked") private List<Point> boundary(Map<String,Object> area) {
        List<Point> result=new ArrayList<>();
        for(Map<String,Object> p:(List<Map<String,Object>>)area.get("boundary")) result.add(new Point(((Number)p.get("lng")).doubleValue(),((Number)p.get("lat")).doubleValue()));
        return result;
    }
    private String encode(Object value) { try { return json.writeValueAsString(value); } catch(Exception e) { throw bad("航线数据序列化失败"); } }
    private String hash(byte[] bytes) { return DigestUtils.md5DigestAsHex(bytes); }
    private String requestHash(Object value) { return hash(encode(value).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private Map<String,Object> retry(String w,String a,String request,String digest) {
        if(request==null||!request.matches("[a-zA-Z0-9-]{16,64}")) throw bad("提交标识无效");
        List<Map<String,Object>> rows=db.queryForList("SELECT * FROM construction_area_wayline WHERE workspace_id=? AND area_id=? AND request_id=?",w,a,request);
        if(rows.isEmpty()) return null;
        if(!digest.equals(rows.get(0).get("request_hash"))) throw new RuleException(409,"提交标识已用于另一份航线，请重新开始创建");
        return present(w,areas.detail(w,a),rows.get(0),true);
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> upload(String w,String a,int version,String request,String name,byte[] bytes,String user) {
        Map<String,Object> area=lock(w,a);
        String digest=requestHash(List.of(name,version,hash(bytes)));
        Map<String,Object> old=retry(w,a,request,digest); if(old!=null) return old;
        writable(area,version);
        return save(w,a,area,request,digest,name,bytes,user,"upload");
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> create(String w,String a,Map<String,Object> body,String user) throws IOException {
        Map<String,Object> area=lock(w,a);
        if(!(body.get("expected_version") instanceof Number)) throw bad("缺少区域版本");
        int version=((Number)body.get("expected_version")).intValue(); String request=(String)body.get("request_id");
        String digest=requestHash(body.get("wayline"));
        Map<String,Object> old=retry(w,a,request,digest); if(old!=null) return old;
        writable(area,version);
        Wayline wayline;
        try { wayline=json.convertValue(body.get("wayline"),Wayline.class); } catch(Exception e) { throw bad("航线参数无效"); }
        if(wayline==null||wayline.getMissionConfig()==null||wayline.getFolder()==null||wayline.getFolder().getPlaceMarks()==null) throw bad("航线参数不完整");
        if(wayline.getFolder().getPlaceMarks().size()<2||wayline.getFolder().getPlaceMarks().size()>2000) throw bad("航线需包含 2–2000 个航点");
        String name=wayline.getMissionConfig().getFileName();
        wayline.getMissionConfig().setFileName("route");
        Path tmp=Files.createTempDirectory("construction-kmz-");
        try {
            Files.createDirectories(tmp.resolve("wpmz"));
            String path=CreateKmz.craeteKmz(wayline,tmp.toString());
            byte[] bytes;
            try(InputStream in=Files.newInputStream(Path.of(path))) { bytes=ConstructionKmz.readBounded(in); }
            return save(w,a,area,request,digest,name,bytes,user,"manual");
        } finally {
            try(java.util.stream.Stream<Path> paths=Files.walk(tmp)) {
                for(Path p:paths.sorted(Comparator.reverseOrder()).toArray(Path[]::new)) Files.deleteIfExists(p);
            }
        }
    }
    private Map<String,Object> save(String w,String a,Map<String,Object> area,String request,String digest,String name,byte[] bytes,String user,String source) {
        if(name==null||name.isBlank()||name.length()>100) throw bad("航线名称需为1–100字");
        ConstructionKmz parsed=ConstructionKmz.parse(bytes);
        ConstructionWaylineGeometry.validate(parsed.points,boundary(area));
        String drone,payload;
        try {
            drone=DeviceEnum.find(DeviceDomainEnum.DRONE,DeviceTypeEnum.find(parsed.drone),DeviceSubTypeEnum.find(parsed.droneSub)).getDevice();
            payload=DeviceEnum.find(DeviceDomainEnum.PAYLOAD,DeviceTypeEnum.find(parsed.payload),DeviceSubTypeEnum.find(parsed.payloadSub)).getDevice();
        } catch(Exception e) { throw bad("平台暂不支持 KMZ 指定的机型或负载"); }
        String key=OssConfiguration.objectDirPrefix+"/"+w+"/construction/"+UUID.randomUUID()+".kmz";
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) { try { oss.deleteObject(OssConfiguration.bucket,key); } catch(RuntimeException ignored) { } } }
        });
        oss.putObject(OssConfiguration.bucket,key,new ByteArrayInputStream(bytes));
        try(InputStream read=oss.getObject(OssConfiguration.bucket,key)) {
            if(read==null||!hash(bytes).equals(hash(ConstructionKmz.readBounded(read)))) throw bad("航线文件保存校验失败，请重试");
        } catch(IOException e) { throw bad("航线文件保存失败"); }
        Integer id=files.saveWaylineFile(w,WaylineFileDTO.builder().name(name.trim()).objectKey(key).sign(hash(bytes)).username(user)
            .droneModelKey(drone).payloadModelKeys(List.of(payload)).templateTypes(List.of(0)).favorited(false).build());
        if(id==null||id<=0) throw bad("航线入库失败");
        String waylineId=mapper.selectById(id).getWaylineId();
        db.update("INSERT INTO construction_area_wayline (id,workspace_id,area_id,wayline_id,area_version,file_sign,source,request_id,request_hash,points_json,height_mode) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
            UUID.randomUUID().toString(),w,a,waylineId,area.get("version"),hash(bytes),source,request,digest,encode(parsed.preview),parsed.heightMode);
        return detail(w,a,waylineId);
    }
    public List<Map<String,Object>> list(String w,String a) {
        Map<String,Object> area=areas.detail(w,a); List<Map<String,Object>> out=new ArrayList<>();
        for(Map<String,Object> row:db.queryForList("SELECT * FROM construction_area_wayline WHERE workspace_id=? AND area_id=? ORDER BY created_at DESC",w,a)) out.add(present(w,area,row,false));
        return out;
    }
    private Map<String,Object> require(String w,String a,String id) {
        areas.detail(w,a);
        List<Map<String,Object>> rows=db.queryForList("SELECT * FROM construction_area_wayline WHERE workspace_id=? AND area_id=? AND wayline_id=?",w,a,id);
        if(rows.isEmpty()) throw new RuleException(404,"区域未关联该航线"); return rows.get(0);
    }
    public Map<String,Object> detail(String w,String a,String id) { return present(w,areas.detail(w,a),require(w,a,id),true); }
    private Map<String,Object> present(String w,Map<String,Object> area,Map<String,Object> row,boolean points) {
        Optional<GetWaylineListResponse> file=files.getWaylineByWaylineId(w,(String)row.get("wayline_id"));
        String status="VALID",message="区域边界校验通过";
        if(file.isEmpty()) { status="MISSING";message="航线已从航线库删除"; }
        else if(((Number)area.get("version")).intValue()!=((Number)row.get("area_version")).intValue()||!Objects.equals(file.get().getSign(),row.get("file_sign"))) { status="STALE";message="区域或航线已变化，请重新校验"; }
        else if(Boolean.TRUE.equals(area.get("archived"))||!Boolean.TRUE.equals(area.get("enabled"))) { status="DISABLED";message="区域已归档或暂停"; }
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("wayline_id",row.get("wayline_id"));out.put("name",file.isPresent()?file.get().getName():"已删除航线");
        out.put("source",row.get("source"));out.put("area_version",row.get("area_version"));out.put("status",status);out.put("message",message);out.put("height_mode",row.get("height_mode"));
        if(points&&"VALID".equals(status)) try { out.put("points",json.readValue((String)row.get("points_json"),new TypeReference<List<Map<String,Object>>>(){})); } catch(Exception e) { throw bad("航线预览数据损坏，请重新校验"); }
        return out;
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> validate(String w,String a,String id) throws IOException {
        Map<String,Object> area=lock(w,a);require(w,a,id);writable(area,((Number)area.get("version")).intValue());
        GetWaylineListResponse file=files.getWaylineByWaylineId(w,id).orElseThrow(()->new RuleException(404,"航线已删除"));
        byte[] bytes;try(InputStream in=oss.getObject(OssConfiguration.bucket,file.getObjectKey())) { if(in==null) throw bad("航线文件缺失");bytes=ConstructionKmz.readBounded(in); }
        if(!hash(bytes).equals(file.getSign())) throw bad("航线文件与记录签名不一致");
        ConstructionKmz parsed=ConstructionKmz.parse(bytes);ConstructionWaylineGeometry.validate(parsed.points,boundary(area));
        db.update("UPDATE construction_area_wayline SET area_version=?,file_sign=?,points_json=?,height_mode=? WHERE workspace_id=? AND area_id=? AND wayline_id=?",area.get("version"),file.getSign(),encode(parsed.preview),parsed.heightMode,w,a,id);
        return detail(w,a,id);
    }
    @Transactional public void unlink(String w,String a,String id) { lock(w,a); require(w,a,id);db.update("DELETE FROM construction_area_wayline WHERE workspace_id=? AND area_id=? AND wayline_id=?",w,a,id); }
}
