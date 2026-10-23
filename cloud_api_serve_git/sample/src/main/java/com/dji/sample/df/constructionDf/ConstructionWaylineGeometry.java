package com.dji.sample.df.constructionDf;

import java.util.*;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;

/** Checks every interval split by a polygon edge, including concave boundaries. */
public final class ConstructionWaylineGeometry {
    private static final double EPS = 1e-10;
    private static Point sub(Point a, Point b) { return new Point(a.lng-b.lng, a.lat-b.lat); }
    private static double cross(Point a, Point b) { return a.lng*b.lat-a.lat*b.lng; }
    public static boolean inside(Point p, List<Point> polygon) {
        boolean inside = false;
        for (int i=0,j=polygon.size()-1;i<polygon.size();j=i++) {
            Point a=polygon.get(j), b=polygon.get(i);
            if (Math.abs(cross(sub(b,a),sub(p,a))) < EPS*Math.hypot(b.lng-a.lng,b.lat-a.lat)
                && p.lng>=Math.min(a.lng,b.lng)-EPS && p.lng<=Math.max(a.lng,b.lng)+EPS
                && p.lat>=Math.min(a.lat,b.lat)-EPS && p.lat<=Math.max(a.lat,b.lat)+EPS) return true;
            if ((a.lat>p.lat)!=(b.lat>p.lat) && p.lng<(b.lng-a.lng)*(p.lat-a.lat)/(b.lat-a.lat)+a.lng) inside=!inside;
        }
        return inside;
    }
    public static void validate(List<Point> points, List<Point> polygon) {
        ConstructionAreaGeometry.validated(polygon);
        if (points.size()<2 || points.size()>2000) throw new RuleException(400,"航线需包含 2–2000 个航点");
        for (int i=0;i<points.size();i++) {
            Point b=points.get(i);
            if (!Double.isFinite(b.lng)||!Double.isFinite(b.lat)||!inside(b,polygon)) throw new RuleException(400,"航点 "+(i+1)+" 超出区域边界");
            if(i==0) continue;
            Point a=points.get(i-1),r=sub(b,a);
            List<Double> cuts=new ArrayList<>(Arrays.asList(0d,1d));
            for(int j=0;j<polygon.size();j++) {
                Point c=polygon.get(j),s=sub(polygon.get((j+1)%polygon.size()),c);
                double den=cross(r,s);
                if(Math.abs(den)<1e-20) continue;
                double t=cross(sub(c,a),s)/den,u=cross(sub(c,a),r)/den;
                if(t>0&&t<1&&u>=0&&u<=1) cuts.add(t);
            }
            Collections.sort(cuts);
            for(int k=1;k<cuts.size();k++) {
                double t=(cuts.get(k-1)+cuts.get(k))/2;
                if(!inside(new Point(a.lng+r.lng*t,a.lat+r.lat*t),polygon)) throw new RuleException(400,"航段 "+i+" → "+(i+1)+" 穿出区域边界");
            }
        }
    }
}
