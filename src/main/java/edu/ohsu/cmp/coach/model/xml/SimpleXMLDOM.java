package edu.ohsu.cmp.coach.vsac.xml;

import org.w3c.dom.Document;

public class SimpleXMLDOM {
    private SimpleXMLElement root;

    public SimpleXMLDOM(Document document) {
        root = new SimpleXMLElement(document.getFirstChild());
    }

    public SimpleXMLElement getRootElement() {
        return root;
    }
}
