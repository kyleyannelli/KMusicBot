# KMusicBot
## Built on Maven with [Javacord](https://github.com/Javacord/Javacord) & [dev.kmfg.lavaplayer](https://github.com/sedmelluq/lavaplayer)
[![CodeQL](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml)

## Prerequisites
- [Discord Tokens](https://discord.com/developers/applications)
- [Spotify Tokens](https://developer.spotify.com/dashboard/login)
## Optional
- [MariaDB](https://www.digitalocean.com/community/tutorials/how-to-install-mariadb-on-ubuntu-22-04)

# Running the Discord Bot

## Running without a Database connection
#### Please note, you may see errors or warnings in the logs about Hibernate, SQL, or general Database langauge. If you go this route, please ignore them.
1. Install Java 11 & Maven. For example, (MacOS) `brew install openjdk@11 maven` (Debian-based/Ubuntu) `sudo apt install openjdk-11-jre maven`.
2. Create a [Discord Application](https://discord.com/developers/applications). The bot does not need any privileged intents. Write down your bot token here, you will need it in the future.
3. Create a [Spotify Application](https://developer.spotify.com/dashboard/login). Write down your client & secret IDs. Note: it's possible you can skip the spotify steps, however, I haven't tested it. All these tokens are used for are adding related songs to the queue.
4. Run `git clone https://github.com/kyleyannelli/KMusicBot && cd KMusicBot`.
5. Now, go into the `kmusicbot-core` directory, and create a file called .env, like so:
```
DISCORD_BOT_TOKEN="YOUR_BOT_TOKEN"

SPOTIFY_CLIENT_ID="YOUR_SPOTIFY_CLIENT_ID"
SPOTIFY_SECRET_ID="YOUR_SPOTIFY_SECRET_ID"

# The following are for number of threads for the bot's services.
#   If you are unsure about this, leave it as 10 (default)
#   If you are CERTAIN this will run in only one server, or you are tight on resources, set each to 1
MAX_RECOMMENDER_THREADS=10
MAX_COMMAND_THREADS=10
```
6. Go to the root directory of the project... `cd ../`
7. If you have docker, you can simply run ./BuildThenRunWithDocker -d
If you do not have docker, run `mvn clean install && cd kmusicbot-core && mvn exec:java`. Your bot is now running!

## Running WITH a Databse Connection
#### Continuing from step 6 of the previous directions...
7. If you already setup the SQL user and know what you're doing, skip to step 13. Install MariaDB (or MySQL). On MacOS, `brew install mariadb`. Debian-based/Ubuntu, `sudo apt install mariadb-server`.
8. Run the command `sudo mysql`.
9. You are now in the MariaDB/MySQL CLI. You need to create a database and a user. First, the database.
`CREATE DATABASE MAKE_YOUR_OWN_NAME_PLEASE;` You now have a database with the name you created!
10. Now, creating the user. This requires you to be aware of where the database and bot are running from. If they are running on the same machine, we can use localhost.
`CREATE USER 'kmusic'@'localhost' IDENTIFIED BY 'very-strong-password-please-dont-use-this;' Make SURE to create a STRONG password.
11. From here we need to ensure this kmusic@localhost user can access MAKE_YOUR_OWN_NAME_PLEASE database, and only that database. Do not grant *.*. 
`GRANT ALL PRIVILEGES ON MAKE_YOUR_OWN_NAME_PLEASE.* TO 'kmusic'@'localhost' WITH GRANT OPTION;'
12. To ensure the changes are in place, finally, run `FLUSH PRIVILEGES;` You can now exit the SQL CLI with `exit;`
13. Now, make sure you are in the kmusicbot-database directory. From there, in `src/main/resources/`, copy the `hibernate.cfg.xml.example` to `hibernate.cfg.xml` and `liquibase.properties.example` to `liquibase.properties`
14. Begin with the hibernate file. Adjust the line with the URL like so, `jdbc:mariadb://localhost:3306/MAKE_YOUR_OWN_NAME_PLEASE`
Next, adjust the user and password accordingly.
15. Moving onto the liquibase file, do the same!
16. That was a lot of steps, I know. Your bot will now record user listening activity. You can now continue back from step 7 of the previous directions! 

## Running the Web API Service
#### I will not provide assistance with DNS, Port Forwarding, and the like.
1. From the Discord Developer portal, obtain your OAuth credentials (ID & Secret). Write them down.
2. Go into the kmusicbot-api, create a file called .env, like so:
```
DISCORD_CLIENT_ID=""
DISCORD_CLIENT_SECRET=""
# Fully qualified domain name (FQDN), such as music.kmfg.dev. If just running locally, something like http://192.168.1.55.
DISCORD_REDIRECT_URI="https://FQDN/api/callback"

# This encrypts the discord tokens. The reason the API does this is so the discord tokens are useless outside of KMusic API.
#   I recommend making this a strong password!
ENCRYPTION_PASS="notSomethingLike1234Please"

# If you are running the API for a Web interface or the like, set the base route of your site's domain!
#   You can run into CORS issues here if you put the API on https://api.kyle.city and host the site on https://music.kmfg.dev.
#   I would recommend keeping the parent domain the same.
CORS_URI="https://music.kmfg.dev"
```
3. Make sure the entire project has been compiled, if not, go to the root directory and `mvn clean install`.
In `kmusicbot-api` run `mvn exec:java`. Your API is now running!

#### I do not plan on adding a guide for Windows.
