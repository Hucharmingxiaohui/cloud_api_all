package com.dji.sample.center.v2022.tool;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;

final class PatrolXmlReader {
    private PatrolXmlReader() {
    }

    static <T> T unmarshal(JAXBContext context, String xml, Class<T> type) throws JAXBException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            XMLFilterImpl reader = new XMLFilterImpl(factory.newSAXParser().getXMLReader()) {
                private boolean root = true;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if (root) {
                        root = false;
                        if (!uri.isEmpty() || (!"PatrolHost".equals(localName)
                                && !"PatrolDevice".equals(localName))) {
                            throw new SAXException("XML root must be PatrolHost or PatrolDevice without a namespace");
                        }
                    }
                    super.startElement(uri, localName, qName, attributes);
                }
            };
            return context.createUnmarshaller().unmarshal(
                    new SAXSource(reader, new InputSource(new StringReader(xml))), type).getValue();
        } catch (ParserConfigurationException | SAXException e) {
            throw new JAXBException("Cannot create secure patrol XML reader", e);
        }
    }
}
