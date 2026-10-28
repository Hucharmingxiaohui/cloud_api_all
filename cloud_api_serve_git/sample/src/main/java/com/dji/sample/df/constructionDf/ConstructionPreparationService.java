package com.dji.sample.df.constructionDf;

import com.dji.sample.wayline.service.IWaylineFileService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Service
public class ConstructionPreparationService {
    private final JdbcTemplate db;private final IWaylineFileService files;
    public ConstructionPreparationService(JdbcTemplate db,IWaylineFileService files){this.db=db;this.files=files;}
    private static RuntimeException bad(String text){return new ConstructionAreaGeometry.RuleException(409,text);}
    public byte[] bytes(String w,String id){
        try{var c=files.getObjectUrl(w,id).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(15000);try(InputStream in=c.getInputStream()){return ConstructionKmz.readBounded(in);}}
        catch(Exception e){throw bad("航线读取失败："+e.getMessage());}
    }
    public Map<String,Object> lineage(String w,String id){
        var rows=db.queryForList("SELECT source_wayline_id,source_sign,prepared_sign FROM construction_wayline_preparation WHERE workspace_id=? AND prepared_wayline_id=?",w,id);
        return rows.isEmpty()?Map.of():rows.get(0);
    }
    @Transactional(rollbackFor=Exception.class)
    public String prepare(String w,String source,String expectedSign,String user){
        // Serialize preparation for a source route across retries and application instances.
        if(db.queryForList("SELECT wayline_id FROM wayline_file WHERE workspace_id=? AND wayline_id=? FOR UPDATE",w,source).isEmpty())throw bad("源航线不存在");
        var original=files.getWaylineByWaylineId(w,source).orElseThrow(()->bad("源航线不存在"));
        if(!Objects.equals(original.getSign(),expectedSign))throw bad("航线已变化，请重新解析");
        byte[] input=bytes(w,source);
        if(!Objects.equals(expectedSign,DigestUtils.md5DigestAsHex(input)))throw bad("源航线内容与版本标识不一致");
        var normalized=ConstructionPhotoNaming.prepare(input);
        if(!normalized.changed())return source;
        var existing=db.queryForList("SELECT prepared_wayline_id,prepared_sign FROM construction_wayline_preparation WHERE workspace_id=? AND source_wayline_id=? AND source_sign=?",w,source,expectedSign);
        if(!existing.isEmpty()){
            var row=existing.get(0);String id=(String)row.get("prepared_wayline_id");
            var saved=files.getWaylineByWaylineId(w,id).orElseThrow(()->bad("已生成的执行版被删除，请恢复该航线或上传新的源版本"));
            if(!Objects.equals(saved.getSign(),row.get("prepared_sign"))||!Objects.equals(saved.getSign(),DigestUtils.md5DigestAsHex(bytes(w,id))))throw bad("已生成的执行版被修改，请恢复该航线或上传新的源版本");
            return id;
        }
        byte[] output=normalized.bytes();String stem=original.getName();if(stem==null)stem="工地航线";if(stem.length()>55)stem=stem.substring(0,55);
        final String name=stem+"-工地执行版-"+expectedSign.substring(0,8)+".kmz";
        MultipartFile upload=new MultipartFile(){
            public String getName(){return "file";}public String getOriginalFilename(){return name;}public String getContentType(){return "application/vnd.google-earth.kmz";}
            public boolean isEmpty(){return false;}public long getSize(){return output.length;}public byte[] getBytes(){return output.clone();}
            public InputStream getInputStream(){return new ByteArrayInputStream(output);}public void transferTo(File f)throws IOException{Files.write(f.toPath(),output);}
        };
        String id=files.importKmzFileWithId(upload,w,user);
        String sign=DigestUtils.md5DigestAsHex(output);
        db.update("INSERT INTO construction_wayline_preparation (workspace_id,source_wayline_id,source_sign,prepared_wayline_id,prepared_sign) VALUES (?,?,?,?,?)",w,source,expectedSign,id,sign);
        return id;
    }
}
