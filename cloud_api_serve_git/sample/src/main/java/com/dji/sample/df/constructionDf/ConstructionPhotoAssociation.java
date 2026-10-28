package com.dji.sample.df.constructionDf;

import java.util.*;

/** Association decisions are scoped to a job snapshot, never current area configuration. */
public final class ConstructionPhotoAssociation {
    @SuppressWarnings("unchecked")
    public static Map<String,Object> resolve(Map<String,Object> snapshot,String fileId,String name,Map<String,Object> decisions){
        Object decision=decisions.get(fileId);
        if(!(decision instanceof Map))return ConstructionOverallService.matchPhoto(snapshot,name);
        Map<String,Object> d=(Map<String,Object>)decision;
        if(Boolean.TRUE.equals(d.get("excluded")))return Map.of("association","EXCLUDED","excluded",true,"association_message",String.valueOf(d.get("reason")));
        for(Map<String,Object> b:(List<Map<String,Object>>)snapshot.getOrDefault("bindings",List.of()))if(Objects.equals(b.get("area_id"),d.get("area_id")))
            return Map.of("association","MANUAL","area_id",b.get("area_id"),"area_name",b.get("area_name"),"association_message","人工确认工作面，未推定拍摄航点");
        return Map.of("association","UNMATCHED","association_message","人工归属不在任务快照中，请重新确认");
    }
    @SuppressWarnings("unchecked")
    public static Map<String,Object> decision(Map<String,Object> snapshot,Map<String,Object> body,String user){
        if(snapshot.isEmpty())throw new ConstructionAreaGeometry.RuleException(409,"该任务缺少总体航线区域快照，无法人工归属");
        Map<String,Object> out=new LinkedHashMap<>();
        if(Boolean.TRUE.equals(body.get("excluded"))){
            String reason=body.get("reason") instanceof String?(String)body.get("reason"):"";
            if(reason.isBlank()||reason.length()>200)throw new ConstructionAreaGeometry.RuleException(400,"排除照片必须填写1–200字原因");
            out.put("excluded",true);out.put("reason",reason.trim());
        }else{
            Object id=body.get("area_id");
            if(!(id instanceof String)||((List<Map<String,Object>>)snapshot.getOrDefault("bindings",List.of())).stream().noneMatch(b->id.equals(b.get("area_id"))))throw new ConstructionAreaGeometry.RuleException(400,"只能选择本次任务快照中的工作面");
            out.put("area_id",id);out.put("excluded",false);
        }
        out.put("updated_by",user==null?"":user);out.put("updated_at",System.currentTimeMillis());return out;
    }
}
