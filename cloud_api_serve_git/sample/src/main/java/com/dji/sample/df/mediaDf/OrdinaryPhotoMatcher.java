package com.dji.sample.df.mediaDf;

import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import java.util.*;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.regex.*;

public final class OrdinaryPhotoMatcher {
    private OrdinaryPhotoMatcher() { }
    private static final Pattern PHOTO = Pattern.compile("^DJI_\\d{14}_(\\d+)_([VT])_(.+)\\.(?i:jpg|jpeg|png)$");
    public static String pointName(String name) { return name == null ? "" : name; }
    private static Matcher parse(String name) {
        if (name == null || name.contains("/") || name.contains("\\")) throw new IllegalArgumentException("图片名称无效：" + name);
        Matcher m=PHOTO.matcher(name);
        if (!m.matches() || m.group(3).isBlank()) throw new IllegalArgumentException("图片文件名不符合 DJI_14位时间_序号_V/T_完整点位名称.jpg/jpeg/png：" + name);
        return m;
    }
    public static String photoKey(String name) {
        Matcher m=parse(name); return ("V".equals(m.group(2)) ? "0:" : "1:") + m.group(3);
    }
    private static BigDecimal preset(UniPoint p) {
        String value=p.getWaylinePointPos();
        if (value==null || !value.matches("[+-]?\\d+(?:\\.\\d+)?"))
            throw new IllegalArgumentException("点位匹配冲突：重复名称组缺少数值航点预置位，点位="+p.getPointCode()+"，名称="+p.getPointName());
        return new BigDecimal(value);
    }
    public static void validatePoints(String route, List<UniPoint> points) {
        for (List<UniPoint> group : groups(route,points).values()) if (group.size()>1) {
            Set<BigDecimal> positions=new TreeSet<>();
            for (UniPoint p:group) if (!positions.add(preset(p))) throw new IllegalArgumentException("点位匹配冲突：同完整名称和类型的航点预置位数值重复："+p.getPointName());
        }
    }
    private static Map<String,List<UniPoint>> groups(String route,List<UniPoint> points) {
        Map<String,List<UniPoint>> groups=new LinkedHashMap<>();
        for (UniPoint p:points) if (Objects.equals(route,p.getWaylineId()) && p.getPointName()!=null && !p.getPointName().isBlank()
                && (Integer.valueOf(0).equals(p.getPicType()) || Integer.valueOf(1).equals(p.getPicType())))
            groups.computeIfAbsent(p.getPicType()+":"+p.getPointName(), k->new ArrayList<>()).add(p);
        return groups;
    }
    public static Map<MediaFileEntity, UniPoint> match(String route, List<UniPoint> points, List<MediaFileEntity> photos) {
        Map<String,List<UniPoint>> candidates=groups(route,points);
        Map<String,List<MediaFileEntity>> groups=new LinkedHashMap<>();
        Map<String,MediaFileEntity> ids=new HashMap<>();
        for (MediaFileEntity photo:photos) {
            String key=photoKey(photo.getFileName());
            if (photo.getFileId()!=null && !photo.getFileId().isBlank()) {
                MediaFileEntity previous=ids.putIfAbsent(photo.getFileId(),photo);
                if (previous!=null) {
                    if (Objects.equals(previous.getFileName(),photo.getFileName()) && Objects.equals(previous.getJobId(),photo.getJobId())
                            && Objects.equals(previous.getWorkspaceId(),photo.getWorkspaceId())) continue;
                    throw new IllegalArgumentException("同一文件ID对应不同媒体："+photo.getFileId());
                }
            }
            groups.computeIfAbsent(key,k->new ArrayList<>()).add(photo);
        }
        Map<MediaFileEntity,UniPoint> result=new LinkedHashMap<>();
        for (Map.Entry<String,List<MediaFileEntity>> entry:groups.entrySet()) {
            List<UniPoint> group=candidates.get(entry.getKey());
            if (group==null) continue;
            List<MediaFileEntity> images=entry.getValue();
            Set<BigInteger> sequences=new HashSet<>();
            for (MediaFileEntity image:images) if (!sequences.add(new BigInteger(parse(image.getFileName()).group(1))))
                throw new IllegalArgumentException("照片序号重复，无法匹配："+entry.getKey());
            if (group.size()!=images.size()) throw new IllegalArgumentException("点位匹配数量不一致：航线="+route+"，完整名称/类型="+entry.getKey()+"，点位="+group.size()+"，照片="+images.size()+"；拒绝按部分照片顺移分配");
            validatePoints(route,group);
            if (group.size()>1) group.sort(Comparator.comparing(OrdinaryPhotoMatcher::preset));
            images.sort(Comparator.comparing(p->new BigInteger(parse(p.getFileName()).group(1))));
            for (int i=0;i<group.size();i++) result.put(images.get(i),group.get(i));
        }
        return Collections.unmodifiableMap(result);
    }
    public static ReportMatchResult matchForReport(String route, List<UniPoint> points, List<MediaFileEntity> photos) {
        Map<String,List<UniPoint>> candidates=groups(route,points);
        Map<String,List<MediaFileEntity>> photoGroups=new LinkedHashMap<>();
        Map<MediaFileEntity,UniPoint> matched=new LinkedHashMap<>();
        List<String> issues=new ArrayList<>();
        Map<String,MediaFileEntity> ids=new HashMap<>();
        Set<String> conflicts=new HashSet<>();
        for (MediaFileEntity photo:photos) {
            String key;
            try { key=photoKey(photo.getFileName()); }
            catch (IllegalArgumentException e) {
                issues.add("航线="+route+"；完整名称/point_code/picType=无法解析；预期=无法确定；实际=1；文件名="+photo.getFileName()+"；原因="+e.getMessage());
                continue;
            }
            if (photo.getFileId()!=null && !photo.getFileId().isBlank()) {
                MediaFileEntity previous=ids.putIfAbsent(photo.getFileId(),photo);
                if (previous!=null) {
                    if (Objects.equals(previous.getFileName(),photo.getFileName()) && Objects.equals(previous.getJobId(),photo.getJobId())
                            && Objects.equals(previous.getWorkspaceId(),photo.getWorkspaceId())) continue;
                    conflicts.add(key); conflicts.add(photoKey(previous.getFileName()));
                }
            }
            photoGroups.computeIfAbsent(key,k->new ArrayList<>()).add(photo);
        }
        for (Map.Entry<String,List<MediaFileEntity>> entry:photoGroups.entrySet()) {
            String key=entry.getKey(); List<MediaFileEntity> images=entry.getValue();
            List<UniPoint> group=candidates.getOrDefault(key,Collections.emptyList());
            String reason;
            try {
                if (group.isEmpty()) throw new IllegalArgumentException("未匹配完整点位名称和照片类型");
                if (conflicts.contains(key)) throw new IllegalArgumentException("同一文件ID对应不同媒体");
                // Reuse the strict group algorithm; a failed group never contributes partial rows.
                matched.putAll(match(route,group,images));
                continue;
            } catch (IllegalArgumentException e) { reason=e.getMessage(); }
            String codes=group.stream().map(p->String.valueOf(p.getPointCode())).collect(java.util.stream.Collectors.joining("、"));
            for (MediaFileEntity image:images) issues.add("航线="+route+"；完整名称="+key.substring(2)+"；picType="+key.charAt(0)
                    +"；point_code="+(codes.isEmpty()?"无匹配点位":codes)+"；预期="+group.size()+"；实际="+images.size()
                    +"；文件名="+image.getFileName()+"；原因="+reason+"；已跳过整组");
        }
        return new ReportMatchResult(matched,issues);
    }

    public static ReportMatchResult matchForReportBySequence(String route, List<UniPoint> points, List<MediaFileEntity> photos) {
        Map<Integer,List<UniPoint>> candidates=new LinkedHashMap<>();
        Map<Integer,List<MediaFileEntity>> photoGroups=new LinkedHashMap<>();
        Map<Integer,String> invalidTypes=new HashMap<>();
        Map<MediaFileEntity,BigInteger> sequences=new HashMap<>();
        Map<String,MediaFileEntity> ids=new HashMap<>();
        Map<MediaFileEntity,UniPoint> matched=new LinkedHashMap<>();
        List<String> issues=new ArrayList<>();
        for (UniPoint point:points) if (Objects.equals(route,point.getWaylineId())
                && (Integer.valueOf(0).equals(point.getPicType()) || Integer.valueOf(1).equals(point.getPicType())))
            candidates.computeIfAbsent(point.getPicType(),k->new ArrayList<>()).add(point);
        for (MediaFileEntity photo:photos) {
            Integer type;
            BigInteger sequence=null;
            String invalid=null;
            try {
                Matcher parsed=parse(photo.getFileName());
                type="V".equals(parsed.group(2)) ? 0 : 1;
                sequence=new BigInteger(parsed.group(1));
            } catch (IllegalArgumentException e) {
                String name=photo.getFileName();
                // A recognizable type must fail closed, otherwise a bad sequence could shift every ordinal.
                boolean visible=name!=null && name.contains("_V_");
                boolean thermal=name!=null && name.contains("_T_");
                if (visible==thermal) {
                    issues.add("航线="+route+"；picType=无法解析；预期=无法确定；实际=1；文件名="+name+"；原因="+e.getMessage());
                    continue;
                }
                type=visible ? 0 : 1;
                invalid="图片文件名或序号无法解析";
            }
            if (photo.getFileId()!=null && !photo.getFileId().isBlank()) {
                MediaFileEntity previous=ids.putIfAbsent(photo.getFileId(),photo);
                if (previous!=null) {
                    if (Objects.equals(previous.getFileName(),photo.getFileName()) && Objects.equals(previous.getJobId(),photo.getJobId())
                            && Objects.equals(previous.getWorkspaceId(),photo.getWorkspaceId())) continue;
                    invalidTypes.put(type,"同一文件ID对应不同媒体");
                    for (Map.Entry<Integer,List<MediaFileEntity>> entry:photoGroups.entrySet())
                        if (entry.getValue().contains(previous)) invalidTypes.put(entry.getKey(),"同一文件ID对应不同媒体");
                }
            }
            photoGroups.computeIfAbsent(type,k->new ArrayList<>()).add(photo);
            if (invalid!=null) invalidTypes.putIfAbsent(type,invalid);
            else sequences.put(photo,sequence);
        }
        for (int type=0;type<=1;type++) {
            List<UniPoint> group=candidates.getOrDefault(type,Collections.emptyList());
            List<MediaFileEntity> images=photoGroups.getOrDefault(type,Collections.emptyList());
            if (group.isEmpty() && images.isEmpty()) continue;
            String reason;
            try {
                if (invalidTypes.containsKey(type)) throw new IllegalArgumentException(invalidTypes.get(type));
                Set<BigInteger> uniqueSequences=new HashSet<>();
                for (MediaFileEntity image:images) if (!uniqueSequences.add(sequences.get(image)))
                    throw new IllegalArgumentException("同类型照片数值序号重复，无法确定序位");
                Set<BigDecimal> positions=new TreeSet<>();
                for (UniPoint point:group) {
                    String value=point.getWaylinePointPos();
                    if (value==null || !value.matches("[+-]?\\d+(?:\\.\\d+)?"))
                        throw new IllegalArgumentException("航点预置位为空或非数值，点位="+point.getPointCode());
                    if (!positions.add(new BigDecimal(value)))
                        throw new IllegalArgumentException("同类型航点预置位数值重复，无法确定序位");
                }
                if (group.size()!=images.size()) throw new IllegalArgumentException("点位匹配数量不一致，拒绝按部分照片顺移分配");
                group.sort(Comparator.comparing(OrdinaryPhotoMatcher::preset).reversed());
                images.sort(Comparator.comparing((MediaFileEntity image)->sequences.get(image)).reversed());
                for (int i=0;i<images.size();i++) matched.put(images.get(i),group.get(i));
                continue;
            } catch (IllegalArgumentException e) { reason=e.getMessage(); }
            String codes=group.stream().map(p->String.valueOf(p.getPointCode())).collect(java.util.stream.Collectors.joining("、"));
            String detail="航线="+route+"；picType="+type+"；point_code="+(codes.isEmpty()?"无匹配点位":codes)
                    +"；预期="+group.size()+"；实际="+images.size();
            if (images.isEmpty()) issues.add(detail+"；文件名=无；原因="+reason+"；已跳过整组");
            else for (MediaFileEntity image:images) issues.add(detail+"；文件名="+image.getFileName()+"；原因="+reason+"；已跳过整组");
        }
        return new ReportMatchResult(matched,issues);
    }

    public static final class ReportMatchResult {
        private final Map<MediaFileEntity,UniPoint> matchedMap;
        private final List<String> issues;
        private ReportMatchResult(Map<MediaFileEntity,UniPoint> matchedMap,List<String> issues) {
            this.matchedMap=Collections.unmodifiableMap(new LinkedHashMap<>(matchedMap));
            this.issues=Collections.unmodifiableList(new ArrayList<>(issues));
        }
        public Map<MediaFileEntity,UniPoint> getMatchedMap() { return matchedMap; }
        public List<String> getIssues() { return issues; }
    }
}
