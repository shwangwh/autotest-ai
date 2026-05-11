import java.nio.file.Files;
import java.nio.file.Paths;

public class Extractor {
    public static void main(String[] args) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("result.json")), "UTF-16LE");
        String output = json.substring(json.indexOf(""content":") + 11);
        System.out.println(output.substring(0, Math.min(200, output.length())));
    }
}
