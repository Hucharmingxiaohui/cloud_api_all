package com.dji.sample.df.constructionDf;
import com.dji.sample.common.model.CustomClaim;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.*;
public class ConstructionParkingChecks {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args) {
        Map<String,Map<String,Object>> rows=new HashMap<>();
        JdbcTemplate db=new JdbcTemplate(){
            @Override public List<Map<String,Object>> queryForList(String sql,Object... a){List<Map<String,Object>> found=new ArrayList<>();rows.values().stream().filter(p->p.get("workspace_id").equals(a[0])).forEach(p->found.add(new HashMap<>(p)));return found;}
            @Override public int update(String sql,Object... a){
                if(sql.startsWith("INSERT")){rows.put((String)a[0],new HashMap<>(Map.of("id",a[0],"workspace_id",a[1],"name",a[2],"lng",a[3],"lat",a[4],"radius_m",a[5],"version",1)));return 1;}
                boolean delete=sql.startsWith("DELETE");int offset=delete?0:4;Map<String,Object> p=rows.get(a[offset+1]);
                if(p==null||!p.get("workspace_id").equals(a[offset])||!p.get("version").equals(a[offset+2]))return 0;
                if(delete)rows.remove(a[1]);else {p.put("name",a[0]);p.put("lng",a[1]);p.put("lat",a[2]);p.put("radius_m",a[3]);p.put("version",(Integer)p.get("version")+1);}return 1;
            }
        };
        CustomClaim claim=new CustomClaim();claim.setWorkspaceId("w");
        HttpServletRequest r=(HttpServletRequest)Proxy.newProxyInstance(ConstructionParkingChecks.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->claim);
        ConstructionParkingController c=new ConstructionParkingController(db);
        Map<String,Object> body=new HashMap<>(Map.of("name"," Parking ","lng",120.0,"lat",36.0,"radius_m",2000));
        Map saved=(Map)c.create(r,body).getData();String id=(String)saved.get("id");check(saved.get("name").equals("Parking"));check(((List)c.list(r).getData()).size()==1);
        body.put("version",1);body.put("radius_m",5000);check(c.update(r,id,body).getCode()==0);check(c.update(r,id,body).getCode()==409);check(c.delete(r,id,1).getCode()==409);
        claim.setWorkspaceId("foreign");check(((List)c.list(r).getData()).isEmpty());body.put("version",2);check(c.update(r,id,body).getCode()==409);check(c.delete(r,id,2).getCode()==409);check(rows.size()==1);
        claim.setWorkspaceId("w");
        for(Object radius:List.of(99,5001,100.5,"100",Double.NaN)){body.put("radius_m",radius);check(c.create(r,body).getCode()==400);}body.put("radius_m",100);
        for(Object lng:List.of(181,Double.NaN,Double.POSITIVE_INFINITY,"120")){body.put("lng",lng);check(c.create(r,body).getCode()==400);}body.put("lng",120);
        body.put("name","  ");check(c.create(r,body).getCode()==400);body.put("name","P");body.put("lat",86);check(c.create(r,body).getCode()==400);
        check(c.delete(r,id,2).getCode()==0);check(rows.isEmpty());claim.setWorkspaceId(null);check(c.list(r).getCode()==401);check(c.create(r,body).getCode()==401);
        System.out.println("Parking checks passed: persistence, workspace isolation, coordinate/radius validation, stale update/delete rejection and authorization. In-memory database.");
    }
}
