package com.dji.sample.df.constructionAnalysisDf;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.math.BigInteger;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

final class AnalysisReport {
    static byte[] render(AnalysisService service,String workspace,String task,Map<String,Object> batch)throws Exception {
        try(XWPFDocument doc=new XWPFDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            setup(doc);
            cover(doc,task,batch);
            heading(doc,"1  报告概述",true);
            line(doc,"任务ID："+task+"　分析批次："+batch.get("batchId"));
            line(doc,"批次状态："+status(batch.get("status"))+"　已处理/照片总数："+batch.get("finished")+"/"+batch.get("total"));
            line(doc,"说明：批次完成仅表示结果已取回，不代表未发现隐患；须结合逐项检测与证据人工复核。");
            if(batch.get("validationMessage")!=null)line(doc,"协议校验警告：区域汇总与逐图统计存在不一致，不能将区域人数作为完整可信结果。" );
            line(doc,"统计口径：工作面人数为本批照片的算法跨图去重结果，不累加单图人数，不将不同工作面人数相加作为全工地去重总人数。失败、不可用或证据缺失不能据此认定无隐患。");
            heading(doc,"2  工作面人数汇总",false);
            XWPFTable areas=table(doc,"工作面ID","状态","去重人数","说明");
            if(batch.get("areas") instanceof List)for(Object item:(List<?>)batch.get("areas"))if(item instanceof Map){
                Map<?,?> area=(Map<?,?>)item;row(areas,text(area.get("area_id")),status(area.get("status")),text(area.get("count")),text(area.get("message")));
            }
            if(areas.getNumberOfRows()==1)line(doc,"暂无工作面汇总；未指定工作面的照片不并入其他工作面人数。");
            heading(doc,"3  逐图检测与证据",true);
            long embedded=0;
            Map<String,List<Map<String,Object>>> grouped=new LinkedHashMap<>();
            for(Map<String,Object> photo:AnalysisService.photos(batch))
                grouped.computeIfAbsent(text(photo.get("areaId")),key->new ArrayList<>()).add(photo);
            int areaNumber=0, photoNumber=0;
            for(Map.Entry<String,List<Map<String,Object>>> group:grouped.entrySet()) {
                heading(doc,"3."+(++areaNumber)+"  工作面："+group.getKey(),areaNumber>1);
                line(doc,"本工作面纳入本批分析照片："+group.getValue().size()+" 张");
            for(Map<String,Object> photo:group.getValue()) {
                heading(doc,"照片 "+(++photoNumber)+"  "+text(photo.get("fileName")),false);
                if(photo.get("waypoint_index") instanceof Number)
                    line(doc,"航点："+(((Number)photo.get("waypoint_index")).intValue()+1));
                line(doc,"照片ID："+photo.get("id")+"　工作面："+text(photo.get("areaId"))+"　镜头："+lens(photo.get("type")));
                line(doc,"照片状态："+status(photo.get("status")));
                XWPFTable checks=table(doc,"检测项目","状态","人数/检测框数","明细与说明");
                Map<?,?> result=photo.get("result") instanceof Map?(Map<?,?>)photo.get("result"):Map.of();
                Set<Object> received=new HashSet<>();
                if(result.get("checks") instanceof List)for(Object item:(List<?>)result.get("checks"))if(item instanceof Map){
                    Map<?,?> check=(Map<?,?>)item;Object code=check.get("code");received.add(code);
                    String count="person_count".equals(code)?"人数："+text(check.get("count")):check.get("boxes") instanceof List?"检测框："+((List<?>)check.get("boxes")).size():"—";
                    row(checks,checkName(code),status(check.get("status")),count,details(check));
                }
                if(photo.get("checks") instanceof List)for(Object code:(List<?>)photo.get("checks"))if(!received.contains(code))row(checks,checkName(code),"结果未取回","—","缺少该项目结果，不能据此认定无隐患");
                if(photo.get("infrared_temperature") instanceof Map){
                    Map<?,?> thermal=(Map<?,?>)photo.get("infrared_temperature");
                    line(doc,"本地红外测温："+status(thermal.get("status"))+"；"+text(thermal.get("message")));
                    if("success".equals(thermal.get("status"))){line(doc,"最高温："+thermal.get("max_temperature_c")+"℃；最低温："+thermal.get("min_temperature_c")+"℃；平均温："+thermal.get("average_temperature_c")+"℃");line(doc,"温度阈值："+thermal.get("threshold_c")+"℃；最小连通热点像素数："+thermal.get("min_hotspot_pixels")+"；符合条件的热点区域："+thermal.get("hotspot_count")+"；热点位置："+thermal.get("hotspots"));line(doc,"结果图为平台根据温度矩阵生成的热力图，白框为符合阈值的热点；温度异常不直接等同失火。");}
                }
                line(doc,"原图证据："+photo.get("originalUrl"));
                Path original=service.original(workspace,task,(String)photo.get("id"));
                byte[] bytes=service.store.bytes(original,service.maxFile);
                if(!AnalysisService.digest(bytes).equals(photo.get("sha256")))line(doc,"警告：原图已在提交后发生变化，未嵌入，不能作为本批次原始证据。");
                else if(embedded+bytes.length<=service.maxTotal){embedded+=bytes.length;embed(doc,bytes,"原图");}
                else line(doc,"原图嵌入容量已达上限，请通过上方授权地址查看。");
                if(photo.get("imageUrl")!=null) {
                    line(doc,"结果图证据："+photo.get("imageUrl"));
                    bytes=service.store.bytes(service.result(workspace,task,(String)batch.get("batchId"),(String)photo.get("id")),service.maxFile);
                    if(embedded+bytes.length<=service.maxTotal){embedded+=bytes.length;embed(doc,bytes,"结果图");}
                    else line(doc,"结果图嵌入容量已达上限，请通过上方授权地址查看。");
                }else line(doc,"结果图缺失："+text(result.get("image_error"))+"。已保留逐项检测结果，不能据此认定无隐患。");
            }
            }
            heading(doc,"4  复核与处理记录",true);
            line(doc,"请结合原图、算法标注及现场情况复核检测结果，填写处理记录。以下内容留待人工确认，不代表已完成处置。");
            XWPFTable review=table(doc,"复核项目","记录");
            row(review,"异常复核与处理意见","待填写");
            row(review,"复核人员 / 日期","待填写");
            row(review,"处理人员 / 完成日期","待填写");
            line(doc,"编制单位：东方电子股份有限公司");
            doc.write(out);return out.toByteArray();
        }
    }
    private static final String COMPANY="东方电子股份有限公司";
    private static void setup(XWPFDocument doc){
        CTSectPr section=doc.getDocument().getBody().addNewSectPr();
        CTPageSz size=section.addNewPgSz();
        size.setW(BigInteger.valueOf(11906));size.setH(BigInteger.valueOf(16838));
        CTPageMar margin=section.addNewPgMar();
        margin.setTop(BigInteger.valueOf(1134));margin.setBottom(BigInteger.valueOf(1134));
        margin.setLeft(BigInteger.valueOf(1134));margin.setRight(BigInteger.valueOf(1134));
        margin.setHeader(BigInteger.valueOf(567));margin.setFooter(BigInteger.valueOf(567));
        doc.getProperties().getCoreProperties().setTitle("工地智能巡检结果分析报告");
        doc.getProperties().getCoreProperties().setCreator(COMPANY);
        XWPFParagraph header=doc.createHeader(HeaderFooterType.DEFAULT).createParagraph();
        header.setBorderBottom(Borders.SINGLE);header.setAlignment(ParagraphAlignment.RIGHT);
        font(header.createRun(),COMPANY+"  |  工地智能巡检",9,false,"64748B");
        XWPFParagraph footer=doc.createFooter(HeaderFooterType.DEFAULT).createParagraph();
        footer.setAlignment(ParagraphAlignment.CENTER);
        font(footer.createRun(),"工地巡检分析报告  ·  第 ",9,false,"64748B");
        footer.getCTP().addNewFldSimple().setInstr("PAGE");
        font(footer.createRun()," 页",9,false,"64748B");
    }
    private static void cover(XWPFDocument doc,String task,Map<String,Object> batch){
        XWPFParagraph brand=doc.createParagraph();brand.setSpacingBefore(700);
        font(brand.createRun(),COMPANY,14,true,"174A70");
        XWPFParagraph title=doc.createParagraph();title.setAlignment(ParagraphAlignment.CENTER);
        title.setSpacingBefore(1500);title.setSpacingAfter(400);
        font(title.createRun(),"工地智能巡检",28,true,"174A70");
        XWPFParagraph subtitle=doc.createParagraph();subtitle.setAlignment(ParagraphAlignment.CENTER);
        subtitle.setSpacingAfter(900);font(subtitle.createRun(),"结果分析报告",24,true,"174A70");
        XWPFTable info=table(doc,"报告信息","内容");
        row(info,"巡检任务编号",text(task));
        row(info,"分析批次编号",text(batch.get("batchId")));
        row(info,"分析提交时间",date(batch.get("createdAt")));
        row(info,"报告生成时间",date(java.time.Instant.now().toString()));
        row(info,"分析照片数量",text(batch.get("total"))+" 张");
        XWPFParagraph company=doc.createParagraph();company.setAlignment(ParagraphAlignment.CENTER);
        company.setSpacingBefore(1100);font(company.createRun(),COMPANY,16,true,"174A70");
        XWPFParagraph note=doc.createParagraph();note.setAlignment(ParagraphAlignment.CENTER);
        font(note.createRun(),"可见光智能识别 · 工作面人数统计 · 红外测温",11,false,"64748B");
    }
    private static String date(Object value){
        if(value==null)return "—";
        try{return java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
            .withZone(java.time.ZoneId.of("Asia/Shanghai")).format(java.time.Instant.parse(value.toString()));}
        catch(java.time.format.DateTimeParseException e){return text(value);}
    }
    private static void font(XWPFRun run,String text,int size,boolean bold,String color){
        run.setFontFamily("宋体");run.setFontFamily("宋体",XWPFRun.FontCharRange.eastAsia);
        run.setFontSize(size);run.setBold(bold);run.setColor(color);run.setText(text);
    }
    private static void heading(XWPFDocument doc,String text,boolean page){
        XWPFParagraph p=doc.createParagraph();p.setPageBreak(page);p.setKeepNext(true);
        p.setSpacingBefore(280);p.setSpacingAfter(160);
        font(p.createRun(),text,15,true,"174A70");
    }
    private static void line(XWPFDocument doc,String text){
        XWPFParagraph p=doc.createParagraph();p.setSpacingAfter(120);p.setSpacingBetween(1.35);
        font(p.createRun(),text,11,false,"263445");
    }
    private static String text(Object value){return value==null||String.valueOf(value).isBlank()?"—":String.valueOf(value);}
    private static void cell(XWPFTableCell cell,String value,boolean header,boolean alternate){
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        cell.setColor(header?"174A70":alternate?"F0F5FA":"FFFFFF");
        XWPFParagraph p=cell.getParagraphs().get(0);p.setSpacingBefore(70);p.setSpacingAfter(70);
        font(p.createRun(),value,10,header,header?"FFFFFF":"263445");
    }
    private static XWPFTable table(XWPFDocument doc,String... headers){
        XWPFTable table=doc.createTable(1,headers.length);table.setWidth("100%");
        table.getRow(0).setRepeatHeader(true);table.setCellMargins(80,100,80,100);
        for(int i=0;i<headers.length;i++)cell(table.getRow(0).getCell(i),headers[i],true,false);
        return table;
    }
    private static void row(XWPFTable table,String... values){
        XWPFTableRow row=table.createRow();
        for(int i=0;i<values.length;i++)cell(row.getCell(i),values[i],false,table.getNumberOfRows()%2==0);
    }
    private static String lens(Object value){return "visible".equals(value)?"可见光":"infrared".equals(value)?"红外":"未确认";}
    private static String status(Object value){
        switch(String.valueOf(value)){
            case "success":return "检测成功";case "SUCCEEDED":return "结果已完整取回（非无隐患结论）";
            case "PARTIAL":return "部分结果或证据缺失";case "unavailable":return "模型或能力不可用";
            case "not_applicable":return "不适用";case "unassessable":return "无法判断";
            case "not_requested":return "未请求人数统计";case "failed":case "FAILED":return "失败";
            case "RUNNING":case "running":return "处理中或结果待取回";case "RETRIEVAL_PENDING":return "结果待重新取回";
            case "QUEUED":case "queued":return "排队中";case "SUBMITTING":return "提交中";case "UNKNOWN":return "提交结果待确认，禁止重复提交";
            default:return "未知状态（"+text(value)+"）";
        }
    }
    private static String checkName(Object code){int i=AnalysisService.CHECKS.indexOf(code);String[] names={"未佩戴安全帽","违规玩手机","工作场所吸烟","人员倒地","未穿着安全服","人员数量统计","火灾烟雾","恶劣天气","动物入侵","人员违规跨栏","塔吊吊物下方站人","防脱钩缺失"};return (i<0?"未知检测项目":names[i])+"（"+text(code)+"）";}
    private static String details(Map<?,?> check){
        StringJoiner parts=new StringJoiner("；");
        if(check.get("message")!=null)parts.add("算法说明："+check.get("message"));
        if(check.get("label")!=null)parts.add("识别类别："+check.get("label"));
        if(check.get("score")!=null)parts.add("置信度："+check.get("score"));
        if(check.get("boxes") instanceof List){int index=0;for(Object box:(List<?>)check.get("boxes"))if(box instanceof Map){Map<?,?> b=(Map<?,?>)box;parts.add("框"+(++index)+"：位置[x,y,宽,高]="+text(b.get("rect"))+"，置信度="+text(b.get("score"))+(b.get("label")==null?"":"，类别="+b.get("label")));}}
        if(parts.length()==0)parts.add("success".equals(check.get("status"))?"该项目检测已完成；请结合标注图复核":"该项目未成功，不作无异常结论");
        return parts.toString();
    }
    private static void embed(XWPFDocument doc,byte[] bytes,String name)throws Exception {
        // Only small JPEG/PNG evidence is embedded; other original formats remain linked untouched.
        int format=bytes.length>2&&(bytes[0]&255)==255&&(bytes[1]&255)==216?Document.PICTURE_TYPE_JPEG:
                bytes.length>8&&(bytes[0]&255)==137&&bytes[1]==80?Document.PICTURE_TYPE_PNG:-1;
        if(format<0){line(doc,name+"格式不支持直接嵌入，请通过授权证据地址查看原文件。");return;}
        try(javax.imageio.stream.ImageInputStream input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<javax.imageio.ImageReader> readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()){line(doc,name+"无法预览，请通过授权证据地址查看。");return;}
            javax.imageio.ImageReader reader=readers.next();
            try {
                reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<1||h<1||(long)w*h>25_000_000){line(doc,name+"预览超过像素上限，请通过授权证据地址查看。");return;}
                BufferedImage image=reader.read(0);if(image==null){line(doc,name+"无法预览，请通过授权证据地址查看。");return;}
                double scale=Math.min(480.0/w,320.0/h);
                XWPFParagraph picture=doc.createParagraph();picture.setAlignment(ParagraphAlignment.CENTER);picture.setKeepNext(true);
                picture.createRun().addPicture(new ByteArrayInputStream(bytes),format,name,Units.toEMU(w*scale),Units.toEMU(h*scale));
                XWPFParagraph caption=doc.createParagraph();caption.setAlignment(ParagraphAlignment.CENTER);caption.setSpacingAfter(200);
                font(caption.createRun(),name,10,false,"64748B");
            }catch(IOException e){line(doc,name+"无法预览，请通过授权证据地址查看。");}finally{reader.dispose();}
        }
    }
}
