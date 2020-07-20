package converter;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class XMLTokenizer implements AutoCloseable {
    private final Reader reader;
    private TokenType tokenType = TokenType.TAG;

    public XMLTokenizer(String xml) {
        reader = new StringReader(removeXmlHeader(xml));
    }

    public void close() throws IOException {
        this.reader.close();
    }

    public String nextToken() throws IOException {
        reader.mark(100000);
        int data = reader.read();
        StringBuilder token = new StringBuilder();
        outerLoop:
        while (data != -1) {
            char c = (char) data;
            switch (tokenType) {
                case TAG -> {
                    token.append(c);
                    if (c == '>') {
                        tokenType = TokenType.CONTENT;
                        break outerLoop;
                    }
                }
                case CONTENT -> {
                    if (c == '<') {
                        reader.reset();
                        tokenType = TokenType.TAG;
                        break outerLoop;
                    }
                    token.append(c);
                }
            }
            reader.mark(100000);
            data = reader.read();

        }
        token = new StringBuilder(token.toString().strip());
        if (data != -1 && token.toString().isBlank()) return nextToken();
        return token.toString();
    }

    private String removeXmlHeader(String xml) {
        return xml.replaceAll("<\\?.*?\\?>", "");
    }

    private enum TokenType {
        CONTENT, TAG
    }
}
