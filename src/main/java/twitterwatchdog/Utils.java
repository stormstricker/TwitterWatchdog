package twitterwatchdog;

import io.github.nandandesai.twitterscraper4j.TwitterScraper;
import io.github.nandandesai.twitterscraper4j.exceptions.TwitterException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.print.Doc;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static boolean isAvailable(int portNr) {
        boolean portFree;
        try (ServerSocket ignored = new ServerSocket(portNr)) {
            portFree = true;
        } catch (IOException e) {
            portFree = false;
        }
        return portFree;
    }

    public static boolean addLineToFileFromPath(String filename, String line) {
        try {
            Writer output;
            System.out.println(Paths.get("").toString());
            output = new BufferedWriter(
                    new FileWriter(Paths.get("setups", filename).toFile(), true));
            output.append(line + System.lineSeparator());
            output.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean clearFileFromPath(String filename)  {
        try {
            Writer output;
            System.out.println(Paths.get("").toString());
            output = new BufferedWriter(new FileWriter(Paths.get("setups", filename).toFile()));

            output.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getAllLinesFromFile(String filename)  {
        try  {
            BufferedReader br = new BufferedReader(
                    new FileReader(Paths.get("setups", filename).toFile()));
            List<String> result = new ArrayList<>();
            String line;
            while ((line = br.readLine())!=null)  {
                result.add(line);
            }
            br.close();

            return result;
        }
        catch (Exception e)  {
            e.printStackTrace();
            return null;
        }
    }

    //returns HTTP headers
    static Map<String, String> getHttpHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Charset", "utf-8");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        return headers;
    }

    static Document getDocument(String url)
            throws IOException, TwitterException {
        Connection.Response response = Jsoup.connect(url)
                .headers(getHttpHeaders()).ignoreHttpErrors(true).followRedirects(true)
                .method(Connection.Method.POST)
                .header("Referer", "https://mobile.twitter.com/")
                 .execute();
        int statusCode = response.statusCode();
        if (statusCode == 404) {
            throw new TwitterException(404, "No tweet(s) found.");
        } else if (statusCode != 200) {
            throw new TwitterException(statusCode, response.statusMessage());
        }
        return response.parse();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(isAvailable(9150));
        /* Document document = Utils.getDocument("https://mobile.twitter.com/" +
                "i/nojs_router?path=/FatKidDeals/status/1216534839752581120/photo/1");
        System.out.println();*/
    }

    public static TwitterScraper createTwtter() throws Exception  {
        TwitterScraper twitterScraper;

        if (!Utils.isAvailable(9150)) {
            System.out.println("connecting via TOR:9150");
            Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress("127.0.0.1", 9150));
            twitterScraper = TwitterScraper.builder().proxy(proxy).build();
        }
        else if (!Utils.isAvailable(9050)) {
            System.out.println("connecting via TOR:9050");
            Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress("127.0.0.1", 9050));
            twitterScraper = TwitterScraper.builder().proxy(proxy).build();
        }
        else  {
            System.out.println("connecting without TOR");
            twitterScraper = TwitterScraper.builder().build();
        }

        return twitterScraper;
    }
}
