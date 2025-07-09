package dev.kmfg.musicbot.core.songrecommender.youtubeapi;

import com.fasterxml.jackson.databind.*;
        import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.*;
        import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.*;

public class Trial {
    private static final String VIDEO_ID = "hKX9rEEgb0I";
    private static final ObjectMapper M = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        HttpClient http = HttpClient.newHttpClient();

        String html = http.send(
                HttpRequest.newBuilder(
                                URI.create("https://www.youtube.com/watch?v=" + VIDEO_ID))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();

        String apiKey    = group(html, "\"INNERTUBE_API_KEY\":\"([^\"]+)\"");
        String clientVer = group(html, "\"INNERTUBE_CONTEXT_CLIENT_VERSION\":\"([^\"]+)\"");

        System.out.printf("[DEBUG] Using key %s… and clientVersion %s%n",
                apiKey.substring(0, 8), clientVer);

        ObjectNode payload = M.createObjectNode();
        payload.put("videoId", VIDEO_ID);
        payload.put("racyCheckOk", true);
        payload.put("contentCheckOk", true);

        ObjectNode ctxCli = M.createObjectNode();
        ctxCli.put("clientName", "WEB");
        ctxCli.put("clientVersion", clientVer);
        ctxCli.put("hl", "en");
        ctxCli.put("gl", "US");

        ObjectNode ctx = M.createObjectNode();
        ctx.set("client", ctxCli);
        payload.set("context", ctx);

        String body = post(http, "next", apiKey, clientVer,
                M.writeValueAsString(payload));

        JsonNode root = M.readTree(body);
        int count = traverseAndPrint(root);

        if (count == 0) {
            System.err.println("No compactVideoRenderer found. First 600 bytes of reply:");
            System.err.println(body.substring(0, Math.min(600, body.length())));
        }
    }

    private static int traverseAndPrint(JsonNode n) {
        int found = 0;
        if (n.has("compactVideoRenderer")) {
            JsonNode vid = n.get("compactVideoRenderer");
            String id    = vid.path("videoId").asText();
            String title = vid.at("/title/simpleText").asText();
            if (!id.isEmpty()) {
                System.out.printf("%s — https://youtu.be/%s%n", title, id);
                ++found;
            }
        }
        if (n.isArray()) {
            for (JsonNode child : n) found += traverseAndPrint(child);
        } else if (n.isObject()) {
            Iterator<Entry<String, JsonNode>> it = n.fields();
            while (it.hasNext()) found += traverseAndPrint(it.next().getValue());
        }
        return found;
    }

    private static String post(HttpClient c, String endpoint,
                               String key, String clientVer,
                               String json) throws Exception {

        HttpRequest req = HttpRequest.newBuilder(
                        URI.create("https://www.youtube.com/youtubei/v1/" + endpoint + "?key=" + key))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", clientVer)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = c.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            System.err.printf("YouTube replied %d:\n%s\n", resp.statusCode(),
                    resp.body().substring(0, Math.min(600, resp.body().length())));
        }
        return resp.body();
    }

    private static String group(String haystack, String regex) {
        Matcher m = Pattern.compile(regex).matcher(haystack);
        if (!m.find())
            throw new IllegalStateException("Pattern not found: " + regex);
        return m.group(1);
    }
}

