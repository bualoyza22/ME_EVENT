package com.bakban9.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Removes active content from uploaded SVG files: scripts, event handlers, foreign HTML,
 * animation that can rewrite links, and links that leave the document.
 * DOCTYPE declarations are rejected so entity expansion and XXE cannot happen.
 */
public final class SvgSanitizer {

    private static final Set<String> FORBIDDEN_ELEMENTS = Set.of(
            "script", "foreignobject", "iframe", "embed", "object", "handler", "listener",
            "set", "animate", "animatemotion", "animatetransform", "animatecolor");

    private SvgSanitizer() {}

    public static class InvalidSvgException extends Exception {
        public InvalidSvgException(String message) {
            super(message);
        }
    }

    public static byte[] sanitize(byte[] input) throws InvalidSvgException {
        Document doc;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder builder = f.newDocumentBuilder();
            builder.setErrorHandler(null);
            doc = builder.parse(new ByteArrayInputStream(input));
        } catch (Exception e) {
            throw new InvalidSvgException("ไฟล์ SVG ไม่ถูกต้อง");
        }
        Element root = doc.getDocumentElement();
        if (root == null || !"svg".equalsIgnoreCase(localName(root))) {
            throw new InvalidSvgException("ไฟล์ SVG ไม่ถูกต้อง");
        }
        clean(root);
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            t.transform(new DOMSource(doc), new StreamResult(out));
            return out.toByteArray();
        } catch (Exception e) {
            throw new InvalidSvgException("ไฟล์ SVG ไม่ถูกต้อง");
        }
    }

    private static void clean(Element el) {
        List<Node> remove = new ArrayList<>();
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element c = (Element) child;
                String name = localName(c).toLowerCase(Locale.ROOT);
                if (FORBIDDEN_ELEMENTS.contains(name) || (name.equals("style") && isDangerousCss(c.getTextContent()))) {
                    remove.add(c);
                } else {
                    clean(c);
                }
            } else if (child.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                remove.add(child);
            }
        }
        remove.forEach(el::removeChild);

        NamedNodeMap attrs = el.getAttributes();
        List<Attr> badAttrs = new ArrayList<>();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            String name = (a.getLocalName() != null ? a.getLocalName() : a.getName()).toLowerCase(Locale.ROOT);
            String value = a.getValue() == null ? "" : a.getValue().trim().toLowerCase(Locale.ROOT);
            boolean handler = name.startsWith("on");
            boolean link = name.equals("href") || name.equals("src") || name.equals("action") || name.equals("formaction");
            boolean externalLink = link && !value.startsWith("#");
            boolean scriptValue = value.contains("javascript:") || value.contains("vbscript:");
            boolean badStyle = name.equals("style") && isDangerousCss(value);
            if (handler || externalLink || scriptValue || badStyle) {
                badAttrs.add(a);
            }
        }
        badAttrs.forEach(el::removeAttributeNode);
    }

    private static boolean isDangerousCss(String css) {
        String v = css == null ? "" : css.toLowerCase(Locale.ROOT);
        if (v.contains("javascript:") || v.contains("expression(") || v.contains("@import") || v.contains("behavior:")) {
            return true;
        }
        // only allow url(#fragment) references
        return v.replaceAll("url\\(\\s*['\"]?#", "").contains("url(");
    }

    private static String localName(Node n) {
        return n.getLocalName() != null ? n.getLocalName() : n.getNodeName();
    }
}
