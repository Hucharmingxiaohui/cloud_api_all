package com.dji.sample.df.commonDf.util;

import java.util.Map;

//分页工具
public class PageUtil {

    public static void setPageArgs(Map map){
        if (map != null && map.containsKey("page")){
            int page = Integer.parseInt(map.get("page").toString());
            int pageSize = Integer.parseInt(map.get("pageSize").toString());
            map.put("startPos", (page-1) * pageSize);
        }
    }
}
//使用时只需PageUtil.setPageArgs(map);
