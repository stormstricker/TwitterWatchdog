package twitterwatchdog;


import io.github.nandandesai.twitterscraper4j.TwitterScraper;
import io.github.nandandesai.twitterscraper4j.models.Tweet;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class TwitterListener {
    private TwitterWatchdog twitterWatchdog;

    public TwitterListener(TwitterWatchdog twitterWatchdog)  {this.twitterWatchdog = twitterWatchdog;  }

    public List<Tweet> getNewTweets(String username, String lastTweetId)  {
        try {
            List<Tweet> result = new ArrayList<>();

            List<Tweet> tweets = null;
            try  {
                TwitterScraper twitterScraper = Utils.createTwtter();
                tweets = twitterScraper.getUserTimeline(username);

                if (tweets.size() == 0)  {
                    return result;
                }
            }
            catch (Exception e)  {
                return result;
            }

            System.out.println("tweets size: " + tweets.size());

            if (lastTweetId.equalsIgnoreCase(""))  {
                //first tweet
                System.out.println("First run, lastTweetId = \"\" !sending the last tweet");
                twitterWatchdog.setLastTweetId(tweets.get(0).getTweetID());
                //result.add(tweets.get(0));
                return result;
            }

            for (Tweet tweet : tweets) {
                if (tweet.getTweetID().equalsIgnoreCase(lastTweetId))   {
                    return result;
                }

                result.add(tweet);
               // System.out.println(tweet);
            }

           // System.out.println();

            System.out.println("result tweets size: " + result.size());
            return result;
        }
        catch (Exception e)  {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {

    }
}

