package edu.ohsu.cmp.coach.model.xml;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

public class SimpleXMLElement {
    private String name;
    private Map<String, String> attributes = null;
    private String text;
    private Map<String, List<SimpleXMLElement>> children = null;

    SimpleXMLElement(Node node) {
        name = node.getNodeName();

        NamedNodeMap nnm = node.getAttributes();
        for (int i = 0; i < nnm.getLength(); i ++) {
            Node n = nnm.item(i);
            if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
                if (attributes == null) {
                    attributes = new LinkedHashMap<String, String>();
                }
                attributes.put(n.getNodeName(), n.getNodeValue());
            }
        }

        text = node.getTextContent() != null && node.getTextContent().trim().length() > 0 ?
                node.getTextContent() :
                null;

        NodeList nl = node.getChildNodes();
        for (int j = 0; j < nl.getLength(); j ++) {
            Node n = nl.item(j);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (children == null) {
                    children = new LinkedHashMap<String, List<SimpleXMLElement>>();
                }
                if ( ! children.containsKey(n.getNodeName()) ) {
                    children.put(n.getNodeName(), new ArrayList<SimpleXMLElement>());
                }
                children.get(n.getNodeName()).add(new SimpleXMLElement(n));
            }
        }
    }

    public SimpleXMLElement findFirst(String path) {
        List<SimpleXMLElement> list = findAll(path);
        return list != null && list.size() > 0 ?
                list.get(0) :
                null;
    }

    public SimpleXMLElement findLast(String path) {
        List<SimpleXMLElement> list = findAll(path);
        return list != null && list.size() > 0 ?
                list.get(list.size() - 1) :
                null;
    }

    public List<SimpleXMLElement> findAll(String path) {
        if (path.startsWith("/")) path = path.substring(1); // strip leading slash
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        int pos = path.indexOf('/');
        String searchName = pos == -1 ? path : path.substring(0, pos);
        if (hasChildren(searchName)) {
            List<SimpleXMLElement> list = new ArrayList<SimpleXMLElement>();
            if (pos == -1) {
                list.addAll(getChildren(searchName));

            } else {
                String remainingPath = path.substring(pos + 1);
                for (SimpleXMLElement child : getChildren(searchName)) {
                    list.addAll(child.findAll(remainingPath));
                }
            }
            return list;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public boolean hasAttribute(String name) {
        return attributes != null && attributes.containsKey(name);
    }

    public Collection<String> getAttributeNames() {
        return attributes != null ? attributes.keySet() : new ArrayList<String>();
    }

    public String getAttribute(String name) {
        return hasAttribute(name) ? attributes.get(name) : null;
    }

    public boolean hasText() {
        return text != null;
    }

    public String getText() {
        return text;
    }

    public boolean hasChildren(String name) {
        return children != null && children.containsKey(name);
    }

    public Collection<String> getChildrenNames() {
        return children != null ? children.keySet() : new ArrayList<String>();
    }

    public List<SimpleXMLElement> getChildren(String name) {
        return hasChildren(name) ? children.get(name) : null;
    }
}
