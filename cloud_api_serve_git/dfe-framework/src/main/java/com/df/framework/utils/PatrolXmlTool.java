package com.df.framework.utils;


import com.df.framework.vo.EmptyItem;
import com.df.framework.vo.PatrolCommand;
import com.df.framework.vo.PatrolCommandSimple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;

public class PatrolXmlTool {
    private static final Logger logger = LoggerFactory.getLogger(PatrolXmlTool.class);

    public static String serialize(PatrolCommand patrolCommand, Class<?> cls) {
        try {
            JAXBContext context = null;
            /*
             * 如果cls也是PatrolCommand.class，jaxb不知道使用何种类型序列化List<BaseItem>
             * 此时使用EmptyItem.class, EmptyItem extends BaseItem
             */
            if (cls.getName().equals(PatrolCommand.class.getName())) {
                context = JAXBContext.newInstance(new Class[]{PatrolCommand.class, EmptyItem.class});
            } else {
                context = JAXBContext.newInstance(new Class[]{PatrolCommand.class, cls});
            }

            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n");
            marshaller.marshal(patrolCommand, writer);
            return writer.toString();
        } catch (JAXBException e) {
            logger.error("序列化XML失败", e);
            return "";
        }
    }

    public static PatrolCommand deserialize(String xmlStr, Class<?> cls) {
        try {
            JAXBContext context = null;

            /*
             * 如果cls也是RobotCommand.class，jaxb不知道使用何种类型序列化List<BaseItem>
             * 此时，使用EmptyItem.class, EmptyItem extends BaseItem
             */
            if (cls.getName().equals(PatrolCommand.class.getName())) {
                context = JAXBContext.newInstance(new Class[]{PatrolCommand.class, EmptyItem.class});
            } else {
                context = JAXBContext.newInstance(new Class[]{PatrolCommand.class, cls});
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            return (PatrolCommand) unmarshaller.unmarshal(sr);

        } catch (JAXBException e) {
            logger.error("序列化XML失败", e);
            return null;
        }
    }

    public static PatrolCommand deserialize(String xmlStr, Class... cls) {
        try {
            JAXBContext context = JAXBContext.newInstance(cls);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            return (PatrolCommand) unmarshaller.unmarshal(sr);
        } catch (JAXBException e) {
            logger.error("序列化XML失败", e);
            return null;
        }
    }

    public static PatrolCommandSimple deserialize(String xmlStr) {
        try {
            JAXBContext context = JAXBContext.newInstance(new Class[]{PatrolCommandSimple.class});
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            return (PatrolCommandSimple) unmarshaller.unmarshal(sr);

        } catch (JAXBException e) {
            logger.error("序列化XML失败{}, {}", xmlStr, e);
            return null;
        }
    }

    public static <T> void toFile(String filename, T t) {
        JAXBContext context = null;
        try {
            context = JAXBContext.newInstance(t.getClass());
            Marshaller marshaller = context.createMarshaller();
            //格式化xml格式
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //去掉生成xml的默认报文头
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

            StringWriter writer = new StringWriter();
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n");
            //Java对象生成到writer
            marshaller.marshal(t, writer);

            //转化为String类型
            String stringXML = writer.toString();
            FileWriter fWriter = new FileWriter(filename);
            fWriter.write(stringXML);
            fWriter.close();

        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    public static <T> T parse(String filename, Class<?> cls) {
        T t = null;
        try {
            JAXBContext context = JAXBContext.newInstance(cls);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            t = (T) unmarshaller.unmarshal(new File(filename));
        } catch (JAXBException e) {
            logger.error("序列化XML失败", e);
            return null;
        }
        return t;
    }

    public static <T> T parseNew(File file, Class<?> cls) {
        T t = null;
        try {
            JAXBContext context = JAXBContext.newInstance(cls);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            t = (T) unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            logger.error("序列化XML失败", e);
            return null;
        }
        return t;
    }

}
