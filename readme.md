### This bot notifies you in Discord when a specific Twitter user has made a new tweet. It checks Twitter for new tweets every 30 seconds and uses TOR for better reliability.

## How to use TwitterWatchdog
1. Download and compile sources, package into a jar file
2. Create a folder named `setups` with the following text files inside: `Discord.token` (with your Discord bot's token inside), `DiscordChannel.setup` (with and ID of a user/channel that you want notifications to be sent to), `LastTweet.setup` (can be leaved empty), `TwitterUser.setup` (with name of a twitter user that you want to track).
3. Put the jar and `setups` inside the same folder.
4. Run the jar file