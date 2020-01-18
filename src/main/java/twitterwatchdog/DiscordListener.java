package twitterwatchdog;

import io.github.nandandesai.twitterscraper4j.TwitterScraper;
import io.github.nandandesai.twitterscraper4j.models.Media;
import io.github.nandandesai.twitterscraper4j.models.Tweet;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DiscordListener extends ListenerAdapter {
    private TwitterWatchdog twitterWatchdog;
    private JDA jda;

    private Map<String, Command> commands = new HashMap<>();

    private String userId = Utils.getAllLinesFromFile("DiscordChannel.setup").get(0);
    private boolean isUser = true;

    public DiscordListener(TwitterWatchdog twitterWatchdog) {
        commands.put("!help", new HelpCommand());
        commands.put("!settrack", new SetTrackCommand());
        commands.put("!viewtrack", new ViewTrackCommand());
        commands.put("!start", new StartCommand());
        commands.put("!stop", new StopCommand());
        commands.put("!status", new StatusCommand());
        commands.put("!viewnotify", new ViewNotifyCommand());
        commands.put("!setnotify", new SetNotify());

        this.twitterWatchdog = twitterWatchdog;

        List<String> lines =
                Utils.getAllLinesFromFile("Discord.token");

        JDABuilder builder = new JDABuilder(AccountType.BOT);
        builder.setToken(lines.get(0));

        try {
            JDA jda = builder.build();
            jda.awaitReady();
            jda.addEventListener(this);
            this.setJda(jda);
        }
        catch (Exception e)  {
            e.printStackTrace();
        }
    }


    public MessageEmbed tweetToEmbed(Tweet tweet)  {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("There was a new tweet!", "https://twitter.com/" +
                tweet.getAuthorUsername() + "/status/" + tweet.getTweetID());
        eb.setColor(new Color(40, 171, 219));
        eb.addField("Tweet posted by " + tweet.getAuthorUsername() +
                        ", " + tweet.getTimestamp() + " ago",
                tweet.getTweetText(), false);

        eb.setFooter("https://twitter.com/" +
                tweet.getAuthorUsername() + "/status/" + tweet.getTweetID());
        eb.setThumbnail(
                "https://cdn3.iconfinder.com/data/icons/sociocons/256/twitter-sociocon.png"
        );


        try {
            TwitterScraper twitterScraper = Utils.createTwtter();
            String tweetId = tweet.getTweetID();
            try  {
                tweet = twitterScraper.getTweet(tweetId);
            }
            catch (Exception e)  {
                //e.printStackTrace();
                System.err.println("Couldn't find media");
            }

            List<Media> media = tweet.getMedia();
            List<String> photos = new ArrayList<>();
            for (Media m : media) {
                if (m.getType() == Media.TYPE.PICTURE) {
                    photos.add(m.getLink());
                }
            }

            if (photos.size() > 0) {
                eb.setImage(photos.get(0));
            }
        }
        catch (Exception e)  {
            e.printStackTrace();
        }
        return eb.build();
    }

    public void notify(Tweet tweet)  {
        User discordUser = jda.getUserById(userId);
        TextChannel textChannel = jda.getTextChannelById(userId);

        MessageEmbed embedToSend = tweetToEmbed(tweet);

        if (discordUser != null) {
            isUser = true;
            System.out.println("--> Notifying " + discordUser.getAsTag() +
                    " with " + tweet);

            discordUser.openPrivateChannel().queue(new Consumer<PrivateChannel>() {
                @Override
                public void accept(PrivateChannel channel) {
                    try {
                        channel.sendMessage(embedToSend).queue();
                    } catch (Exception e) {
                        e.printStackTrace();
                        //channel.sendMessage("**Couldn't send message to user " + discordUser.getName() + " please, check your input!**").queue();
                    }
                }
            });
        }
        else  if (textChannel != null){
            isUser = false;

            System.out.println("--> Notifying channel: " + userId +
                    " with " + tweet);
            textChannel.sendMessage(embedToSend).queue();
        }
        else  {
            System.err.println("Couldn't find a channel to send message to");
        }
    }

    public void setUserId(String userId)  {
        this.userId = userId;
        Utils.clearFileFromPath("DiscordChannel.setup");
        Utils.addLineToFileFromPath("DiscordChannel.setup", userId);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)  {
      //  userId = event.getAuthor().getId();
        if (event.getAuthor().isBot())  {
            return;
        }

        String message = event.getMessage().getContentRaw();

        Command command = null;
        for (Map.Entry<String, Command> e : commands.entrySet()) {
            if (message.startsWith(e.getKey())) {
                command = commands.get(e.getKey());
                command.setBody(message.replace(e.getKey(), "").trim());
                break;  //pick the first fitting
            }
        }

        if (command != null)  {
            String channel = event.getChannel().getId();
            if (event.getChannelType() == ChannelType.PRIVATE)  {
                if (!event.getAuthor().getId().equalsIgnoreCase(userId))  {
                    setUserId(event.getAuthor().getId());
                    new ViewNotifyCommand().execute(event);

                }
            }
            else if (!channel.equalsIgnoreCase(userId)) {
                setUserId(channel);
                new ViewNotifyCommand().execute(event);

            }

            command.execute(event);
        }
    }

    public JDA getJda() {
        return jda;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    abstract class Command  {
        protected String body;

        public void setBody(String body)  {this.body = body;}

        public abstract void execute(MessageReceivedEvent event);


    }

    class HelpCommand  extends Command {
        @Override
        public void execute(MessageReceivedEvent event)  {
            event.getChannel().sendMessage("__*You can use these commands " +
                    "to interact with the bot:*__\n" +
                    "`!help`: *to read the help message*\n" +
                    "`!settrack username`: *to add a Twitter user to track*\n" +
                    "`!viewtrack`: *to view a Twitter user that is currently being tracked*\n" +
                    "`!start`: *to start the tracking*\n" +
                    "`!stop`: *to stop the tracking*\n" +
                    "`!status`: *to view the current status of the bot*\n" +
                    "`!viewnotify`: *to view channel/user to bot is set to send " +
                    "notifies to*\n" +
                    "`!setnotify`: *to set a channel/user the bot should send " +
                    "notifies to*\n").queue();
        }
    }

    class StartCommand extends Command  {
        @Override
        public void execute(MessageReceivedEvent event)  {
            twitterWatchdog.start();
            event.getChannel().sendMessage("*Started the bot!*").queue();
        }
    }

    class StopCommand extends Command  {
        @Override
        public void execute(MessageReceivedEvent event)  {
            twitterWatchdog.stop();
            event.getChannel().sendMessage("*Stopped the bot!*").queue();
        }
    }

    class StatusCommand extends Command  {
        @Override
        public void execute(MessageReceivedEvent event)  {
            event.getChannel().sendMessage("*The bot is " +
                    (!twitterWatchdog.isStarted() ? "not " : "") + "tracking this user:* " +
                    new ViewTrackCommand().getUserLink()).queue();
        }
    }

    class ViewTrackCommand extends Command  {
        @Override
        public void execute(MessageReceivedEvent event)  {
            event.getChannel().sendMessage(getReply()).queue();
        }

        public String getUserLink()  {
            return "<https://twitter.com/" +
                    Utils.getAllLinesFromFile("TwitterUser.setup").get(0) + ">";
        }

        public String getReply()  {
            return "*The bot is set to track this user:* " +
                    getUserLink();
        }
    }

    class SetTrackCommand extends Command  {
        @Override
        public void execute(MessageReceivedEvent event)  {
            twitterWatchdog.setTwitterUsername(body);
            twitterWatchdog.setLastTweetId("");
            new ViewTrackCommand().execute(event);
        }
    }

    class ViewNotifyCommand extends Command  {
        @Override
        public void execute(MessageReceivedEvent event)  {
            User user = jda.getUserById(userId);
            String message;
            if (user == null)  {
                message = "channel: " + userId;
            }
            else {
                message = "user: " + user.getAsTag() + " (user/private channel id: " + userId + ")";
            }

            event.getChannel().sendMessage("*The bot is set to send notifies to " +
                    message + "*").queue();
        }
    }

    class SetNotify extends Command  {
        @Override
        public void execute(MessageReceivedEvent event)  {
            setUserId(body);
            new ViewNotifyCommand().execute(event);
        }
    }
}
