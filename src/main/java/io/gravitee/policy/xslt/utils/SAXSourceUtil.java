/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.xslt.utils;

import io.gravitee.gateway.api.buffer.Buffer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class SAXSourceUtil {

    private SAXSourceUtil() {}

    public static SAXSource createSAXSource(Buffer input, boolean secureProcessing) throws ParserConfigurationException, SAXException {
        XMLReader xmlReader = secureProcessing ? createSecureXMLReader() : createUnsecureXMLReader();
        InputStream xslInputStream = new ByteArrayInputStream(input.getBytes());
        return new SAXSource(xmlReader, new InputSource(xslInputStream));
    }

    /**
     * Create a SAXSource from a Buffer using a SAXParserFactory with a secure configuration, based on best practices explained on:
     * <ul>
     *   <li>
     *     <a href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxp-documentbuilderfactory-saxparserfactory-and-dom4j">OWASP Website</a>
     *   </li>
     *   <li>
     *     <a href="https://research.nccgroup.com/2014/05/19/xml-schema-dtd-and-entity-attacks-a-compendium-of-known-techniques/">The whitepaper – XML Schema, DTD, and Entity Attacks: A Compendium of Known Techniques</a>
     *   </li>
     * </ul>
     * For instance, DTDs and external entities are disabled.
     *
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws SAXException for SAX errors.
     */
    private static XMLReader createSecureXMLReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);

        // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented
        // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
        saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        // As stated in the documentation "Feature for Secure Processing (FSP)" is the central mechanism to help safeguard
        // XML processing. It instructs XML processors, such as parsers, validators, and transformers, to try and process XML securely.
        saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // If you can't completely disable DTDs, then at least do the following:
        // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
        // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
        // JDK7+ - http://xml.org/sax/features/external-general-entities
        // This feature has to be used together with the following one, otherwise it will not protect you from XXE for sure
        saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);

        // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
        // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
        // JDK7+ - http://xml.org/sax/features/external-parameter-entities
        // This feature has to be used together with the previous one, otherwise it will not protect you from XXE for sure
        saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        // Disable external DTDs as well
        saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
        // https://research.nccgroup.com/2014/05/19/xml-schema-dtd-and-entity-attacks-a-compendium-of-known-techniques/
        saxParserFactory.setXIncludeAware(false);

        return saxParserFactory.newSAXParser().getXMLReader();
    }

    /**
     * Create a SAXSource from a Buffer using a SAXParserFactory with default configuration.
     *
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws SAXException for SAX errors.
     */
    private static XMLReader createUnsecureXMLReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        return saxParserFactory.newSAXParser().getXMLReader();
    }
}
