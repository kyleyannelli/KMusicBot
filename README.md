# KMusicBot
## Built on Maven with [Javacord](https://github.com/Javacord/Javacord) & [Lavaplayer](https://github.com/sedmelluq/lavaplayer)
[![CodeQL](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml)

# Getting Started (Ubuntu & Mac OS)

## Prerequisites
- [Spotify Tokens](https://developer.spotify.com/dashboard/login)
- [Discord Tokens](https://discord.com/developers/applications)
- [MariaDB](https://www.digitalocean.com/community/tutorials/how-to-install-mariadb-on-ubuntu-22-04)

## Ubuntu
1. git clone https://github.com/kyleyannelli/KMusicBot.git OR wget the latest release (recommended for stability)
2. cd KMusicBot
3. Create .env file in this directory
4. Add line, DISCORD_BOT_TOKEN="$TOKEN" to your .env file. $TOKEN being your discord bot token.
5. Add line, SPOTIFY_CLIENT_ID="$CLIENT_ID" to your .env file. $CLIENT_ID being your spotify api client id.
6. Add line, SPOTIFY_SECRET_ID="$SECRET_ID" to your .env. $SECRET_ID being your spotify api secret.
7. Add line, DB_URL="$DB_URL" to your .env. $DB_URL being your database url.
8. Add line, DB_USER="$DB_USER" to your .env. $DB_USER being your database user.
9. Add line, DB_USER_PASSWORD="$DB_USER_PASSWORD" to your .env. $DB_USER_PASSWORD being your database user password.
10. apt install maven
11. apt install openjdk-11-jre-headless
12. Command line run, mvn install && mvn exec:java

## Mac OS
1. git clone https://github.com/kyleyannelli/KMusicBot.git OR download the latest release (recommended for stability)
2. cd KMusicBot
3. Create .env file in this directory
4. Add line, DISCORD_BOT_TOKEN="$TOKEN" to your .env file. $TOKEN being your discord bot token.
5. Add line, SPOTIFY_CLIENT_ID="$CLIENT_ID" to your .env file. $CLIENT_ID being your spotify api client id.
6. Add line, SPOTIFY_SECRET_ID="$SECRET_ID" to your .env. $SECRET_ID being your spotify api secret.
7. Add line, DB_URL="$DB_URL" to your .env. $DB_URL being your database url.
8. Add line, DB_USER="$DB_USER" to your .env. $DB_USER being your database user.
9. Add line, DB_USER_PASSWORD="$DB_USER_PASSWORD" to your .env. $DB_USER_PASSWORD being your database user password.
10. Install [OpenJDK 11](https://www.openlogic.com/openjdk-downloads)
11. Install [Brew](https://brew.sh/)
12. brew install openjdk@11
13. brew install maven
14. Command line run, mvn install && mvn exec:java

#### I do not plan on adding a guide for Windows.
