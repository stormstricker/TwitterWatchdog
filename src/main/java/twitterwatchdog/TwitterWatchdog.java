package twitterwatchdog;


import io.github.nandandesai.twitterscraper4j.models.ReplyTweet;
import io.github.nandandesai.twitterscraper4j.models.Tweet;

import java.util.*;
import java.util.concurrent.*;


public class TwitterWatchdog {
    private ScheduledExecutorService MAIN_EXECUTOR;
    private boolean started;

    private TwitterListener twitter = new TwitterListener(this);
    private DiscordListener discord = new DiscordListener(this);

    private String twitterUsername =
            Utils.getAllLinesFromFile("TwitterUser.setup").get(0);
    private String lastTweetId;
    private Queue<Tweet> tweetsToSend = new ConcurrentLinkedQueue<>();

    private ScheduledFuture<?> twitterTaskHandle;
    private ScheduledFuture<?> discordTaskHandle;
    private Random random = new Random();

    private Runnable twitterTask = new Runnable()  {
        @Override
        public void run()  {
            System.out.println("Inside twitterTask");
            System.out.println("lastTweetId: " + lastTweetId);

            try  {
                int sleepTime = (random.nextInt(5) + 1) * 1000;
                System.out.println("Sleeping " + sleepTime + "ms before newTweets");
                Thread.sleep(sleepTime);  //1s - 5s

                List<Tweet> newTweets = twitter.getNewTweets(
                    twitterUsername, lastTweetId);

                if (newTweets != null && newTweets.size() > 0)  {
                    setLastTweetId(newTweets.get(0).getTweetID());
                    Collections.reverse(newTweets);
                    tweetsToSend.addAll(newTweets);
                    System.out.println();
                }
            }
            catch (Exception e)  {
                e.printStackTrace();
            }
        }
    };

    private Runnable discordTask = new Runnable()  {
        @Override
        public void run() {
            System.out.println("Inside discordTask");

            try {
                Tweet tweetToSend = tweetsToSend.poll();
                while (tweetToSend != null)  {
                    if (!(tweetToSend instanceof ReplyTweet))  {
                        discord.notify(tweetToSend);
                    }
                    else  {
                        System.out.println(tweetToSend);
                        System.err.println("It is a reply tweet, not notifying");
                    }

                    int sleepTime = (random.nextInt(5) + 1) * 1000;  //1s - 5s
                    System.out.println("Sleeping " + sleepTime + "ms before notify");
                    Thread.sleep(sleepTime);

                    tweetToSend = tweetsToSend.poll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public TwitterWatchdog()  {
        List<String> lines = Utils.getAllLinesFromFile("LastTweet.setup");
        lastTweetId = lines.size() > 0 ? lines.get(0) : "";

        MAIN_EXECUTOR = Executors.newScheduledThreadPool(5);

        start();
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;

        if (started)  {
            start();
        }
        else  {
            stop();
        }
    }

    public void start()  {
        twitterTaskHandle = MAIN_EXECUTOR.scheduleAtFixedRate(twitterTask,
                0, 30, TimeUnit.SECONDS);
        discordTaskHandle = MAIN_EXECUTOR.scheduleAtFixedRate(discordTask,
                0, 5, TimeUnit.SECONDS);

        /*updateReadyProxiesTaskHandle = MAIN_EXECUTOR.scheduleAtFixedRate(
                updateReadyProxiesTask, 0, 10, TimeUnit.MINUTES);
        purgeDeadProxiesTaskHandle = MAIN_EXECUTOR.scheduleAtFixedRate(
                purgeDeadProxiesTask, 0, 5, TimeUnit.MINUTES);*/

        started = true;
    }

    public void stop()  {
        twitterTaskHandle.cancel(false);
        discordTaskHandle.cancel(false);

       /* updateReadyProxiesTaskHandle.cancel(true);
        purgeDeadProxiesTaskHandle.cancel(true);*/
        started = false;
    }

    public void setLastTweetId(String lastTweetId)  {
        this.lastTweetId = lastTweetId;
        Utils.clearFileFromPath("LastTweet.setup");
        Utils.addLineToFileFromPath("LastTweet.setup", lastTweetId);
    }

    public void setTwitterUsername(String username)  {
        this.twitterUsername = username;
        Utils.clearFileFromPath("TwitterUser.setup");
        Utils.addLineToFileFromPath("TwitterUser.setup",
                username.equalsIgnoreCase("") ? "realDonaldTrump" : username);
    }

    public static void main(String[] args) throws Exception {
        /*TwitterScraper twitterScraper = TwitterScraper.getInstance();
        List<Tweet> tweets=twitterScraper.getUserTimeline("realDonaldTrump");

        for(Tweet tweet:tweets){
            System.out.println(tweet);
        }

        System.out.println();*/

        TwitterWatchdog twitterWatchdog = new TwitterWatchdog();

    }
}
