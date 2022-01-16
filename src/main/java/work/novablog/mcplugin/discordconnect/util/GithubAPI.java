package work.novablog.mcplugin.discordconnect.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GithubAPI {
    /**
     * プラグインの最新バージョン番号を取得する
     * @return バージョン番号
     */
    public static String getLatestVersionNum() {
        try {
            StringBuilder result = new StringBuilder();

            URL url = new URL("https://api.github.com/repos/nova-27/DiscordConnect/releases/latest");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }

            in.close();
            con.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result.toString());
            return root.get("tag_name").asText().replace("v", "");
        } catch(Exception e) {
            return null;
        }
    }
}
