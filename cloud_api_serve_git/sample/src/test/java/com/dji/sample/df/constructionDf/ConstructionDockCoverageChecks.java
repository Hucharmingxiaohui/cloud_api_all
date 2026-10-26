package com.dji.sample.df.constructionDf;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.*;
public class ConstructionDockCoverageChecks {
    static void check(boolean value) {if(!value) throw new AssertionError();}
    public static void main(String[] args) {
        Map<String,Integer> stored=new HashMap<>();
        JdbcTemplate db=new JdbcTemplate() {
            @Override public List<Map<String,Object>> queryForList(String sql,Object... values) {Integer n=stored.get(values[0]+":"+values[1]);return n==null?List.of():List.of(Map.of("radius_m",n));}
            @Override public int update(String sql,Object... values) {stored.put(values[0]+":"+values[1],(Integer)values[2]);return 1;}
        };
        FrogJumpLandingRecommendationService docks=new FrogJumpLandingRecommendationService(null,null,null) {
            @Override public boolean isWorkspaceDock(String w,String sn) {return w.equals("w")&&List.of("dock1","dock2").contains(sn);}
        };
        CustomClaim claim=new CustomClaim();claim.setWorkspaceId("w");
        HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(ConstructionDockCoverageChecks.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->claim);
        ConstructionDockCoverageController c=new ConstructionDockCoverageController(db,docks);
        check(((Map)c.get(request,"dock1").getData()).get("radius_m")==null);
        for(int radius:List.of(100,2000,5000)) {check(c.save(request,"dock1",Map.of("radius_m",radius)).getCode()==0);check(((Map)c.get(request,"dock1").getData()).get("radius_m").equals(radius));}
        c.save(request,"dock2",Map.of("radius_m",100));check(stored.get("w:dock1")==5000&&stored.get("w:dock2")==100);
        for(Object invalid:List.of(0,99,5001,-1,100.5,Double.NaN,Double.POSITIVE_INFINITY,"2000")) check(c.save(request,"dock1",Map.of("radius_m",invalid)).getCode()==400);
        check(stored.get("w:dock1")==5000);
        claim.setWorkspaceId("other");check(c.get(request,"dock1").getCode()==404);check(c.save(request,"dock1",Map.of("radius_m",100)).getCode()==404);
        claim.setWorkspaceId(null);check(c.get(request,"dock1").getCode()==401);
        System.out.println("Dock coverage checks passed: unset, per-dock persistence, radius limits, invalid inputs and workspace isolation; in-memory database.");
    }
}
