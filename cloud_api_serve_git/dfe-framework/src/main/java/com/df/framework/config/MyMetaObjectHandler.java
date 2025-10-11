//package com.df.framework.config;
//
//import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
//import org.apache.ibatis.reflection.MetaObject;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//
///**
// * 自动填充处理器
// *
// * @author lyc
// **/
//@Component
//public class MyMetaObjectHandler implements MetaObjectHandler {
//
//    @Override
//    public void insertFill(MetaObject metaObject) {
//        this.strictInsertFill(metaObject, "createTime", Date::new, Date.class);
//    }
//
//    @Override
//    public void updateFill(MetaObject metaObject) {
//        this.strictUpdateFill(metaObject, "updateTime", Date::new, Date.class);
//    }
//}
