package converter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class JSonTokenizer implements AutoCloseable {
    private final Reader reader;

    public JSonTokenizer(String json) {
        reader = new StringReader(removeWhiteSpaces(json));
    }

    public String nextToken() throws IOException {

        StringBuilder token = new StringBuilder();

        int data = reader.read();
        if (data == -1) return null;
        while (true) {
            String c = String.valueOf((char) data);
            token.append(c);
            if ("[]{}:,".contains(c)) {
                if (token.length() > 1) {
                    reader.reset();
                    token = new StringBuilder(token.substring(0, token.length() - 1));
                }
                break;
            }
            reader.mark(10000);
            data = reader.read();
            if (data == -1) return null;
        }
        return token.toString().replaceAll("^\"|\"$", "");
    }

    public void close() throws IOException {
        this.reader.close();
    }

    private String removeWhiteSpaces(String json) {
        return (json.replaceAll("[\\n\\t]|\\s{2,}", "").replaceAll("\\s*(?<braces>[:{}\\[\\],])\\s*", "$1"));
    }

}
