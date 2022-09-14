# KMusicBot
## Built on Maven with [Javacord](https://github.com/Javacord/Javacord) & [Lavaplayer](https://github.com/sedmelluq/lavaplayer)
[![CodeQL](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/kyleyannelli/KMusicBot/actions/workflows/codeql-analysis.yml)

# Getting Started (Ubuntu & Mac OS)

## Prerequisites
- [Spotify Tokens](https://developer.spotify.com/dashboard/login)
- [Discord Tokens](https://discord.com/developers/applications)

## Ubuntu
1. git clone https://github.com/kyleyannelli/KMusicBot.git OR wget the latest release (reccommened for stability)
2. cd KMusicBot
3. Create .env file in this directory
4. Add line, DISCORD_BOT_TOKEN="$TOKEN" to your .env file. $TOKEN being your discord bot token.
5. Add line, SPOTIFY_CLIENT_ID="$CLIENT_ID" to your .env file. $CLIENT_ID being your spotify api client id.
6. Add line, SPOTIFY_SECRET_ID="$SECRET_ID" to your .env. $SECRET_ID being your spotify api secret.
7. apt install maven
8. apt install openjdk-11-jre-headless
9. Command line run, mvn install && mvn exec:java

## Mac OS
1. git clone https://github.com/kyleyannelli/KMusicBot.git OR download the latest release (reccommened for stability)
2. cd KMusicBot
3. Create .env file in this directory
4. Add line, DISCORD_BOT_TOKEN="$TOKEN" to your .env file. $TOKEN being your discord bot token.
5. Add line, SPOTIFY_CLIENT_ID="$CLIENT_ID" to your .env file. $CLIENT_ID being your spotify api client id.
6. Add line, SPOTIFY_SECRET_ID="$SECRET_ID" to your .env. $SECRET_ID being your spotify api secret.
7. Install [OpenJDK 11](https://www.openlogic.com/openjdk-downloads)
8. Install [Brew](https://brew.sh/)
9. brew install maven
10. Command line run, mvn install && mvn exec:java

#### I do not plan on adding a guide for Windows.
