package com.dji.sample.center.v2022.tool;

import com.dji.sample.center.v2022.command.base.EdgeNodeCommand;
import com.dji.sample.center.v2022.command.base.EdgeNodeCommandSimple;
import com.dji.sample.center.v2022.command.base.SMConfigCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

public class PatrolHostXmlTool {
    private static final Logger logger = LoggerFactory.getLogger(PatrolHostXmlTool.class);

    public static String serialize(EdgeNodeCommand commandData, Class<?> cls) {
        try {
            JAXBContext context = null;

            /*
             * 如果传参cls也是PatrolHostCommand.class，jaxb不知道使用何种类型序列化List<BaseItem>
             * 此时，使用EmptyItem.class, EmptyItem extends BaseItem
             */
            if (cls.getName().equals(EdgeNodeCommand.class.getName())) {
                context = JAXBContext.newInstance(new Class[]{EdgeNodeCommand.class, EmptyItem.class});
            } else {
                context = JAXBContext.newInstance(new Class[]{EdgeNodeCommand.class, cls});
            }
            Marshaller marshaller = context.createMarshaller();
            //格式化xml格式
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
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

    public static EdgeNodeCommand deserialize(String xmlStr, Class<?> cls) {
        try {
            JAXBContext context = null;
            /*
             * 如果传参cls也是PatrolHostCommand.class，jaxb不知道使用何种类型处理List<BaseItem>
             * 此时，使用EmptyItem.class, EmptyItem extends BaseItem
             */
            if (cls.getName().equals(EdgeNodeCommand.class.getName())) {
                context = JAXBContext.newInstance(new Class[]{EdgeNodeCommand.class, EmptyItem.class});
            } else {
                context = JAXBContext.newInstance(new Class[]{EdgeNodeCommand.class, cls});
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            EdgeNodeCommand commandData = (EdgeNodeCommand) unmarshaller.unmarshal(sr);
            return commandData;
        } catch (JAXBException e) {
            logger.error("反序列化XML失败{}, {}", xmlStr, e);
            return null;
        }
    }


    public static SMConfigCommand smDeserialize(String xmlStr, Class<?> cls) {
        try {
            JAXBContext context = null;
            /*
             * 如果传参cls也是PatrolHostCommand.class，jaxb不知道使用何种类型处理List<BaseItem>
             * 此时，使用EmptyItem.class, EmptyItem extends BaseItem
             */
            if (cls.getName().equals(SMConfigCommand.class.getName())) {
                context = JAXBContext.newInstance(new Class[]{SMConfigCommand.class, EmptyItem.class});
            } else {
                context = JAXBContext.newInstance(new Class[]{SMConfigCommand.class, cls});
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            SMConfigCommand commandData = (SMConfigCommand) unmarshaller.unmarshal(sr);
            return commandData;
        } catch (JAXBException e) {
            logger.error("反序列化XML失败{}, {}", xmlStr, e);
            return null;
        }
    }

    public static EdgeNodeCommand deserialize(String xmlStr, Class... cls) {
        try {
            JAXBContext context = JAXBContext.newInstance(cls);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            EdgeNodeCommand commandData = (EdgeNodeCommand) unmarshaller.unmarshal(sr);
            return commandData;
        } catch (JAXBException e) {
            logger.error("反序列化XML失败", e);
            return null;
        }
    }


    public static EdgeNodeCommandSimple deserialize(String xmlStr) {
        try {
            JAXBContext context = JAXBContext.newInstance(new Class[]{EdgeNodeCommandSimple.class});
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(xmlStr);
            EdgeNodeCommandSimple commandData = (EdgeNodeCommandSimple) unmarshaller.unmarshal(sr);
            return commandData;
        } catch (JAXBException e) {
            logger.error("反序列化XML失败: {} {}", xmlStr, e);
            return null;
        }
    }
}
