import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
public class Test {
    public static void main(String[] args) throws Exception {
        String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("result.json")), "UTF-16LE");
        System.out.println("JSON length: " + json.length());
        System.out.println("Starts with: " + json.substring(0, Math.min(20, json.length())));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode choices = root.path("output").path("choices");
        System.out.println("Choices is array? " + choices.isArray());
        if (choices.isArray()) {
            JsonNode choice = choices.get(0);
            System.out.println("content: " + choice.path("message").path("content").asText(null));
        }
    }
}
