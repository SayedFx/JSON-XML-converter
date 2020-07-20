package converter;

import java.util.ArrayList;
import java.util.List;

public class Element {
    private final List<Element> children;
    private String name;
    private List<Attribute> attributes;
    private String value;
    private Element parent;
    private boolean isOpen;
    private boolean isArrayMember;
    private boolean isArrayParent;

    public Element() {
        children = new ArrayList<>();
        attributes = new ArrayList<>();
    }

    public Element(String name, String value) {
        this();
        this.name = name;
        this.value = "null".equals(value) ? null : value;
    }

    public Element(String name) {
        this();
        this.name = name;
    }

    public String getPath() {
        String path = parent == null ? "" : parent.getPath() + ", ";
        return path + name;
    }

    public void addChild(Element element) {
        element.setParent(this);
        children.add(element);
    }

    public List<Element> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Element:\n");
        appendPath(sb);
        if (children.isEmpty()) {
            appendValue(sb);
        }
        if (!attributes.isEmpty()) {
            appendAttributes(sb);
        }
        this.children.forEach(element -> sb.append("\n").append(element.toString()));
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = "null".equals(value) ? null : value;
    }

    public void addAttribute(Attribute attribute) {
        if (attributes == null) attributes = new ArrayList<>();
        attributes.add(attribute);
    }

    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    public String toJson() {
        children.forEach(element -> {
            if (children.stream().filter(e -> e.getName().equals(element.getName())).count() > 1) {
                this.isArrayParent = true;
                children.forEach(e -> e.isArrayMember = true);
            }
        });
        StringBuilder sb = new StringBuilder();
        if (!isArrayMember) sb.append(String.format("\"%s\":", name));
        if (!attributes.isEmpty()) {
            sb.append("{");
            attributes.forEach(a -> sb.append(String.format("\"@%s\":%s,", a.getName(), String.format("\"%s\"", a.getValue()))));
            if (!children.isEmpty()) {
                if (this.isArrayParent) {
                    sb.append(String.format("\"#%s\":[", name));
                    for (int i = 0; i < children.size(); i++) {
                        sb.append(children.get(i).toJson());
                        if (i != children.size() - 1) sb.append(",");
                    }
                    sb.append("]}");
                } else {
                    sb.append(String.format("\"#%s\":{", name));
                    for (int i = 0; i < children.size(); i++) {
                        sb.append(children.get(i).toJson());
                        if (i != children.size() - 1) sb.append(",");
                    }
                    sb.append("}}");
                }
            } else {
                String value = this.value;
                if (value != null) value = String.format("\"%s\"", value);
                sb.append(String.format("\"#%s\":%s", name, value)).append("}");
            }
        } else if (!children.isEmpty()) {
            if (isArrayParent) {
                sb.append("[");
                for (int i = 0; i < children.size(); i++) {
                    sb.append(children.get(i).toJson());
                    if (i != children.size() - 1) sb.append(",");
                }
                sb.append("]");
            } else {
                sb.append("{");
                for (int i = 0; i < children.size(); i++) {
                    sb.append(children.get(i).toJson());
                    if (i != children.size() - 1) sb.append(",");
                }
                sb.append("}");
            }
        } else {
            String value = this.value;
            if (value != null) value = String.format("\"%s\"", value);
            sb.append(value);
        }
        return sb.toString();
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<%s", name));
        if (!attributes.isEmpty()) {
            attributes.forEach(a -> sb.append(String.format(" %s=\"%s\"", a.getName(), a.getValue())));
        }
        if (!children.isEmpty()) {
            sb.append(">");
            children.forEach(c -> sb.append(c.toXML()));
            sb.append(String.format("</%s>", name));
        } else if (value == null) sb.append(" />");
        else sb.append(String.format(">%s</%s>", value, name));

        return sb.toString();
    }

    private void setParent(Element element) {
        this.parent = element;
    }

    private void appendAttributes(StringBuilder sb) {
        sb.append("attributes: \n");
        attributes.forEach(attribute -> sb.append(attribute.getName())
                .append(" = ")
                .append("\"")
                .append(attribute.getValue())
                .append("\"")
                .append("\n"));
    }

    private void appendValue(StringBuilder sb) {
        sb.append("value = ");
        if (value == null) sb.append("null").append("\n");
        else sb.append("\"").append(value).append("\"").append("\n");
    }

    private void appendPath(StringBuilder sb) {
        sb.append("path = ").append(getPath()).append("\n");
    }
}
