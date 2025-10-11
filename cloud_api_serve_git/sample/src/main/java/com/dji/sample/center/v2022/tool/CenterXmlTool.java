package com.dji.sample.center.v2022.tool;

import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandSimple;
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

public class CenterXmlTool {
    private static final Logger logger = LoggerFactory.getLogger(CenterXmlTool.class);

    public static String serialize(PatrolHostCommand commandData, Class<?> cls) {
        try {
            JAXBContext context = null;

            /*
             * 如果传参cls也是PatrolHostCommand.class，jaxb不知道使用何种类型序列化List<BaseItem>
             * 此时，使用EmptyItem.class, EmptyItem extends BaseItem
             */
            if (cls.getName().equals(PatrolHostCommand.class.getName())) {
                context = JAXBContext.newInstance(new Class[]{PatrolHostCommand.class, EmptyItem.class});
            } else {
                context = JAXBContext.newInstance(new Class[]{PatrolHostCommand.class, cls});
            }
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            String title = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

            StringWriter sw = new StringWriter();
            marshaller.marshal(commandData, sw);

            String msg = title + sw;
            if (msg.indexOf("<Code></Code>") > 0) {
                msg = msg.replace("<Code></Code>", "<Code/>");
            }
            if (msg.indexOf("<Command></Command>") > 0) {
                msg = msg.replace("<Command></Command>", "<Command/>");
            }
            return msg;
        } catch (JAXBException e) {
            logger.error("序列化XML失败", e);
            return "";
        }
    }

    public static PatrolHostCommand deserialize(String xmlStr, Class<?> cls) {
        try {
            JAXBContext context = null;
            /*
             * 如果传参cls也是PatrolHostCommand.class，jaxb不知道使用何种类型处理List<BaseItem>
             * 此时，使用EmptyItem.class, EmptyItem extends BaseItem
             */
            if (cls.getName().equals(PatrolHostCommand.class.getName())) {
                context = JAXBContext.newInstance(new Class[]{PatrolHostCommand.class, EmptyItem.class});
            } else {
                context = JAXBContext.newInstance(new Class[]{PatrolHostCommand.class, cls});
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            PatrolHostCommand commandData = (PatrolHostCommand) unmarshaller.unmarshal(sr);
            return commandData;
        } catch (JAXBException e) {
            logger.error("反序列化XML失败{}, {}", xmlStr, e);
            return null;
        }
    }

    public static PatrolHostCommand deserialize(String xmlStr, Class... cls) {
        try {
            JAXBContext context = JAXBContext.newInstance(cls);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            PatrolHostCommand commandData = (PatrolHostCommand) unmarshaller.unmarshal(sr);
            return commandData;
        } catch (JAXBException e) {
            logger.error("反序列化XML失败", e);
            return null;
        }
    }


    public static PatrolHostCommandSimple deserialize(String xmlStr) {
        try {
            JAXBContext context = JAXBContext.newInstance(new Class[]{PatrolHostCommandSimple.class});
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            PatrolHostCommandSimple commandData = (PatrolHostCommandSimple) unmarshaller.unmarshal(sr);
            return commandData;
        } catch (JAXBException e) {
            logger.error("反序列化XML失败: {} {}", xmlStr, e);
            return null;
        }
    }


    public static <T> void toFile(String filename, T t) {
        JAXBContext context = null;
        try {
            context = JAXBContext.newInstance(t.getClass());
            StringWriter writer = new StringWriter();
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n    ");

            Marshaller marshaller = context.createMarshaller();
            //格式化xml格式
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //去掉生成xml的默认报文头
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

            //java 对象生成到writer
            marshaller.marshal(t,writer);

            //转化为String类型
            String stringXML = writer.toString();
            FileWriter fWriter=new FileWriter(new File(filename));
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

}
