package converter;


import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;


public class Parser {


    public static Element parseXml(XMLTokenizer tokenizer) throws IOException {
        return parseRootElement(null, tokenizer);
    }

    public static String convertToJson(String xml) {
        String jsonFromXML = "";
        try (XMLTokenizer tokenizer = new XMLTokenizer(xml)) {
            Element e = parseXml(tokenizer);
            jsonFromXML = String.format("{%s}", e.toJson());
        } catch (IOException ignored) {

        }
        return jsonFromXML;
    }

    public static String convertToXml(String json) {
        try (JSonTokenizer tokenizer = new JSonTokenizer(json)) {
            return Parser.jsonToElement(tokenizer).toXML();
        } catch (IOException ignored) {

        }
        return "";
    }

    public static Element jsonToElement(JSonTokenizer tokenizer) throws IOException {
        String token = tokenizer.nextToken();
        Deque<Element> openElementsStack = new ArrayDeque<>();
        Deque<Element> openArrayStack = new ArrayDeque<>();
        List<Element> elements = new ArrayList<>();
        String arrayElementsName = "element";
        while (token != null) {
            switch (token) {
                case "{":
                    if (openElementsStack.isEmpty()) {
                        openElementsStack.offerLast(new Element(tokenizer.nextToken()));
                        elements.add(openElementsStack.peekLast());
                    } else if (!openArrayStack.isEmpty()) {
                        Element element = new Element(arrayElementsName, "");
                        assert openArrayStack.peekLast() != null;
                        openArrayStack.peekLast().addChild(element);
                        openArrayStack.offerLast(element);

                    }
                    break;
                case ":":
                    token = tokenizer.nextToken();
                    if ("{".equals(token)) break;
                    if ("[".equals(token)) {
                        openArrayStack.offerLast(openElementsStack.pollLast());
                    } else requireNonNull(openElementsStack.pollLast()).setValue(token);
                    break;
                case "[":
                    Element element = new Element(arrayElementsName, "");
                    if (!openArrayStack.isEmpty()) {
                        openArrayStack.peekLast().addChild(element);
                    } else if (!openElementsStack.isEmpty()) openElementsStack.peekLast().addChild(element);
                    openArrayStack.offerLast(element);
                    break;
                case "]": {
                    setValueToEmptyIfChildLess(openArrayStack.pollLast());
                }
                break;
                case "}":
                    if (!openArrayStack.isEmpty()) {
                        setValueToEmptyIfChildLess(openArrayStack.pollLast());
                    } else {
                        setValueToEmptyIfChildLess(openElementsStack.pollLast());
                    }
                    break;
                case ",":
                    break;
                default:
                    if (!openArrayStack.isEmpty()) {
                        String newToken = tokenizer.nextToken();
                        if (",".equals(newToken)) {
                            assert openArrayStack.peekLast() != null;
                            openArrayStack.peekLast().addChild(new Element(arrayElementsName, token));
                        }
                        else if ("]".equals(newToken)) {
                            if (!token.isBlank()) {
                                assert openArrayStack.peekLast() != null;
                                openArrayStack.peekLast().addChild(new Element(arrayElementsName, token));
                            }
                            Element element1 = openArrayStack.pollLast();
                            setValueToEmptyIfChildLess(element1);
                            assert element1 != null;
                            if (element1.getName().startsWith("@")) {
                                Element e = new Element(arrayElementsName);
                                setValueToEmptyIfChildLess(e);
                                element1.addChild(e);
                            }
                        } else {
                            String thirdToken = tokenizer.nextToken();
                            if ("{".equals(thirdToken)) {
                                Element element1 = new Element(token);
                                requireNonNull(openArrayStack.peekLast()).addChild(element1);
                                openArrayStack.offerLast(element1);
                            } else if ("[".equals(thirdToken)) {
                                Element e = new Element(token);
                                requireNonNull(openArrayStack.peekLast()).addChild(e);
                                openArrayStack.offerLast(e);
                            } else {
                                requireNonNull(openArrayStack.peekLast()).addChild(new Element(token, thirdToken));
                            }
                        }
                    } else {
                        if (!openElementsStack.isEmpty()) {
                            Element element1 = new Element(token);
                            requireNonNull(openElementsStack.peekLast()).addChild(element1);
                            openElementsStack.offerLast(element1);
                        } else {
                            Element c = new Element(token);
                            openElementsStack.offerLast(c);
                            elements.add(c);
                        }
                    }


            }
            token = tokenizer.nextToken();
        }
        elements.forEach(Parser::fixKids);
        if (elements.size() > 1) {
            Element root = new Element("root", "");
            elements.forEach(root::addChild);
            return root;
        } else return elements.get(0);
    }

    private static void setValueToEmptyIfChildLess(Element e) {
        if (e != null && e.getChildren().isEmpty() && e.getValue() == null) e.setValue("");
    }

    private static void fixKids(Element element) {
        element.getChildren().forEach(Parser::fixKids);
        analyseChildren(element);
    }

    private static Element parseJson(Element parent, JSonTokenizer tokenizer) throws IOException {

        while (parent.isOpen()) {
            String token = tokenizer.nextToken();
            if (isClosingToken(token)) {
                analyseChildren(parent);
                if (isBlankObject(parent)) {
                    parent.setValue("");
                }
                parent.setOpen(false);
                return parent;
            } else {
                String nextToken = tokenizer.nextToken();
                if (isOpeningToken(nextToken)) {
                    recurse(parent, tokenizer, token);
                } else {
                    parent.addChild(new Element(token, nextToken));
                }
            }

        }
        if (!parent.isOpen()) {
            String token = tokenizer.nextToken();
            if (token.equals("{")) {
                parent.setOpen(true);
                parseJson(parent, tokenizer);
            } else {
                token = token.equals("null") ? null : token;
                parent.setValue(token);
            }
        }

        return parent;
    }

    private static boolean isValueName(String name, String parentName) {
        return name.matches("^[#]" + parentName);
    }

    private static void recurse(Element parent, JSonTokenizer tokenizer, String token) throws IOException {
        parent.addChild(parseJson(createOpenElement(token), tokenizer));
    }

    private static boolean isBlankObject(Element parent) {
        return !hasChildren(parent) && parent.getValue() == null && !parent.hasAttributes();
    }

    private static void analyseChildren(Element parent) {
        boolean hasAttributes = hasAttributes(parent);

        Iterator<Element> listIterator = parent.getChildren().listIterator();
        while (listIterator.hasNext()) {
            Element child = listIterator.next();
            String childName = child.getName();

            if (removedBlank(listIterator, childName, parent)) continue;

            if (hasAttributes && isAttribute(childName)) {
                changeChildToAttribute(parent, listIterator, child);
            } else if (hasAttributes && isValueName(childName, parent.getName())) {
                setParentValue(parent, listIterator, child);
            } else if (!hasAttributes && isDuplicateChild(parent, childName)) {
                listIterator.remove();
            } else {
                child.setName(trim(childName));
            }
        }
//        if (isBlankObject(parent)) {
//            parent.setValue("");
//        }
    }

    private static boolean isDuplicateChild(Element parent, String childName) {
        return childName.matches("^[#@].+") && parent.getChildren().stream().anyMatch(e -> e.getName().equals(trim(childName)));
    }

    private static void setParentValue(Element parent, Iterator<Element> listIterator, Element child) {
        parent.setValue(child.getValue());
        listIterator.remove();
    }

    private static void changeChildToAttribute(Element parent, Iterator<Element> listIterator, Element child) {
        String value = child.getValue();
        value = "null".equals(value) || value == null ? "" : value;
        parent.addAttribute(new Attribute(trim(child.getName()), value));
        listIterator.remove();
    }

    private static boolean removedBlank(Iterator<Element> listIterator, String childName, Element parent) {
        if (isBlank(childName)) {
            listIterator.remove();
            if (parent.getChildren().isEmpty() && parent.getValue() == null) parent.setValue("");
            return true;
        }
        return false;
    }

    private static boolean hasAttributes(Element parent) {
        boolean hasAttributes = true;
        Iterator<Element> iterator = parent.getChildren().listIterator();
        while (iterator.hasNext()) {
            Element child = iterator.next();
            if (isAttributeInvalid(parent, child)) {
                if (isBlank(child.getName())) iterator.remove();
                hasAttributes = false;
                break;
            }
        }

        if (hasAttributes) {
            Element valueChild = getValueChild(parent);
            if (valueChild != null) {
                parent.getChildren().remove(valueChild);
                valueChild.getChildren().forEach(parent::addChild);
            } else {
                hasAttributes = isAnyChildJsonObjectValue(parent);
            }
        }
        return hasAttributes;
    }

    private static Element createOpenElement(String token) {
        Element child = new Element();
        child.setName(token);
        child.setOpen(true);
        return child;
    }

    private static boolean isOpeningToken(String nextToken) {
        return "{".equals(nextToken);
    }

    private static boolean isClosingToken(String token) {
        return "}".equals(token);
    }

    private static boolean isAttributeInvalid(Element parent, Element child) {
        String childName = child.getName();
        return isBlank(childName) || !isAttributeOrValue(childName)
                || (isAttribute(childName) && hasChildren(child))
                || valueDoesNotMatchParentName(parent, childName);
    }

    private static boolean isBlank(String name) {
        return name.matches("[#@\\s]") || name.isBlank();
    }

    private static boolean isAttributeOrValue(String name) {
        return name.matches("^[#@].+");
    }

    private static boolean hasChildren(Element child) {
        return !child.getChildren().isEmpty();
    }

    private static boolean isAttribute(String name) {
        return name.matches("^[@].+");
    }

    private static Element getValueChild(Element element) {
        List<Element> valueChildren = element.getChildren().stream().filter(e -> hasJsonObjectValue(element, e)).collect(Collectors.toList());
        return valueChildren.isEmpty() ? null : valueChildren.get(0);
    }

    private static boolean isAnyChildJsonObjectValue(Element parent) {
        return parent.getChildren().stream().anyMatch(e -> isValueName(e.getName(), parent.getName()));
    }

    private static boolean hasJsonObjectValue(Element parent, Element child) {
        return isValueName(child.getName(), parent.getName()) && hasChildren(child);
    }

    private static boolean valueDoesNotMatchParentName(Element element, String name) {
        return name.startsWith("#") && !isValueName(name, element.getName());
    }

    private static String trim(String token) {
        return token.replaceAll("^[@#]", "");
    }

    private static Element parseRootElement(Element element, XMLTokenizer tokenizer) throws IOException {
        if (isRootElementNotCreated(element)) {
            element = buildElement(tokenizer.nextToken());
            parseRootElement(element, tokenizer);
        } else {
            while (element.isOpen()) {
                String token = tokenizer.nextToken();

                if (isContentToken(token)) {
                    element.setValue(token);
                    tokenizer.nextToken();
                    element.setOpen(false);
                    return element;
                }
                if (isClosingTag(element.getName(), token)) {
                    if (element.getValue() == null) element.setValue("");
                    element.setOpen(false);
                    return element;
                }
                element.addChild(parseRootElement(buildElement(token), tokenizer));
            }
        }
        return element;

    }

    private static Element buildElement(String token) {
        Element element = new Element();
        Matcher matcher = Pattern.compile("<\\s*(?<tag>[^<>\\s/]+)\\s*(?<attribute>[^<>/]+)?/?>").matcher(token);
        if (matcher.find()) {
            element.setName(matcher.group("tag"));
            String attribute = matcher.group("attribute");
            if (attribute != null) parseAttributes(attribute).forEach(element::addAttribute);
            element.setOpen(!isSelfClosing(token));

        }
        return element;
    }

    private static boolean isRootElementNotCreated(Element element) {
        return element == null;
    }

    private static boolean isClosingTag(String name, String token) {
        return token.matches("</\\s*" + name + "\\s*>");
    }

    private static boolean isContentToken(String token) {
        return !token.startsWith("<");
    }

    private static boolean isSelfClosing(String token) {
        return token.contains("/>");
    }

    private static List<Attribute> parseAttributes(String attributes) {
        Matcher matcher = Pattern.compile("(?<key>.+?)\\s*=\\s*[\"'](?<value>.*?)[\"']").matcher(attributes);
        List<Attribute> attributesList = new ArrayList<>();
        while (matcher.find()) {
            attributesList.add(new Attribute(matcher.group("key").strip(), matcher.group("value").strip()));
        }
        return attributesList;
    }
}


