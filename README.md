# KMusicBot
## Built on Maven with [Javacord](https://github.com/Javacord/Javacord) & [dev.kmfg.lavaplayer](https://github.com/sedmelluq/lavaplayer)
[![CodeQL](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml)

### Table of Contents
- [API Documentation](API.md)
1. [Prerequisites](#prerequisites)
2. [Running the Discord Bot](#running-the-discord-bot)
    1. [Without a Database](#without-a-database)
    2. [With a Database](#with-a-database)
3. [Running the Web API Service](#running-the-web-api-service)
4. [Additional Notes](#additional-notes)

## Prerequisites
- **Discord Tokens:** Obtain from [Discord Developer Portal](https://discord.com/developers/applications).
- **Spotify Tokens:** Obtain from [Spotify Developer Dashboard](https://developer.spotify.com/dashboard/login).
- **Optional:** [MariaDB](https://www.digitalocean.com/community/tutorials/how-to-install-mariadb-on-ubuntu-22-04) for database functionality.

# Running the Discord Bot

### Without a Database
#### Please note, you may see errors or warnings in the logs about Hibernate, SQL, or general Database langauge. If you go this route, please ignore them.
1. Install Java 11 & Maven:
   - MacOS: `brew install openjdk@11 maven`
   - Debian/Ubuntu: `sudo apt install openjdk-11-jre maven`
2. Create a Discord Application [here](https://discord.com/developers/applications). Note down the bot token.
3. Create a Spotify Application [here](https://developer.spotify.com/dashboard/login). Record client & secret IDs.
4. Clone the repository: `git clone https://github.com/kyleyannelli/KMusicBot && cd KMusicBot`.
5. Now, go into the `kmusicbot-core` directory, and create a file called .env, like so:
```
DISCORD_BOT_TOKEN="YOUR_BOT_TOKEN"

SPOTIFY_CLIENT_ID="YOUR_SPOTIFY_CLIENT_ID"
SPOTIFY_SECRET_ID="YOUR_SPOTIFY_SECRET_ID"

# The token to allow a websocket connection to core. Please note, while having a static authentication token is inherently less secure, this is entirely read-only, and non-sensitive data. However, you should still proxy the connection when going across a network.
WEBSOCKET_TOKEN="GenerateAToken"

# The following are for number of threads for the bot's services.
#   If you are unsure about this, leave it as 10 (default)
#   If you are CERTAIN this will run in only one server, or you are tight on resources, set each to 1
MAX_RECOMMENDER_THREADS=10
MAX_COMMAND_THREADS=10
```
6. Go to the root directory of the project... `cd ../`
7. Run the bot:
- With Docker: `./BuildThenRunWithDocker -d`
- Without Docker: `mvn clean install && cd kmusicbot-core && mvn exec:java`
8. Done :)

### With a Database
1. Follow steps 1-6 from [Without a Database](#without-a-database).
2. If you already setup the SQL user and know what you're doing, skip to step 13. Install MariaDB (or MySQL). On MacOS, `brew install mariadb`. Debian-based/Ubuntu, `sudo apt install mariadb-server`.
3. Run the command `sudo mysql`.
4. You are now in the MariaDB/MySQL CLI. You need to create a database and a user. First, the database.
`CREATE DATABASE MAKE_YOUR_OWN_NAME_PLEASE;` You now have a database with the name you created!
5. Now, creating the user. This requires you to be aware of where the database and bot are running from. If they are running on the same machine, we can use localhost.
`CREATE USER 'kmusic'@'localhost' IDENTIFIED BY 'very-strong-password-please-dont-use-this;' Make SURE to create a STRONG password.
6. From here we need to ensure this kmusic@localhost user can access MAKE_YOUR_OWN_NAME_PLEASE database, and only that database. Do not grant *.*. 
`GRANT ALL PRIVILEGES ON MAKE_YOUR_OWN_NAME_PLEASE.* TO 'kmusic'@'localhost' WITH GRANT OPTION;'
7. To ensure the changes are in place, finally, run `FLUSH PRIVILEGES;` You can now exit the SQL CLI with `exit;`
8. Now, make sure you are in the kmusicbot-database directory. From there, in `src/main/resources/`, copy the `hibernate.cfg.xml.example` to `hibernate.cfg.xml` and `liquibase.properties.example` to `liquibase.properties`
9. Begin with the hibernate file. Adjust the line with the URL like so, `jdbc:mariadb://localhost:3306/MAKE_YOUR_OWN_NAME_PLEASE`
Next, adjust the user and password accordingly.
10. Moving onto the liquibase file, do the same!
11. That was a lot of steps, I know. Your bot will now record user listening activity. You can now continue back from step 7 of [Without a Database](#without-a-database).

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
# I'd recommend using your parent domain. For example, my api is served on apiv1.kmfg.dev. This means for the cookie to be valid (browser only) and be accessed by svelte on music.kmfg.dev, the URI needs to be the parent.
COOKIE_URI="kmfg.dev"

# This is the address kmusicbot-core is running on. The port will always be 30106
WEBSOCKET_URI="ws://10.1.0.70:30106"
# This is the token for the websocket you set in the core .env
WEBSOCKET_TOKEN="GenerateAToken"
```
3. Make sure the entire project has been compiled, if not, go to the root directory and `mvn clean install`.
In `kmusicbot-api` run `mvn exec:java`. Your API is now running!

#### I do not plan on adding a guide for Windows.
