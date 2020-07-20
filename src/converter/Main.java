package converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        String testString = Files.readString(Path.of("test.txt"));
        if (isValidJSON(testString)) {
            System.out.println(Parser.convertToXml(testString));
        } else if (isValidXml(testString)) {
            System.out.println(Parser.convertToJson(testString));
        }
    }

    private static boolean isValidJSON(String text) {
        return !text.isBlank() && text.charAt(0) == '{';
    }

    private static boolean isValidXml(String text) {
        return !text.isBlank() && text.charAt(0) == '<';
    }

}






