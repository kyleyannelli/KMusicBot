# KMusic API Routes Documentation

## Public Routes

### `/api/login`
- **Method:** GET
- **Description:** Initiates login, returning a URL for Discord OAuth2 authentication.

### `/api/callback`
- **Method:** GET
- **Description:** Callback for Discord OAuth2; handles `code` and `state` URL parameters. `state` will attempt to redirect to a url.

### `/api/logout`
- **Method:** GET
- **Description:** Logs out the user, clearing cookies.

## Authenticated Routes

### `/api/secure/tracked-song/:trackedSongId`
- **Method:** GET
- **Description:** Retrieves a specific tracked song by ID. Returns JSON response with song details, listen times, associated guild, and total seconds played.
- **Example Response:**
  ```json
  {"data": {
    "id": 149,
    "kmusicSong": {"id": 153, "youtubeUrl": "https://www.youtube.com/watch?v=x4ygVwbOyJU"},
    "songInitializations": [
      {"initializingDiscordUser": {"discordId": "1234567890", "username": "user#1234"}, "timesInitialized": 1}
    ],
    "songPlaytimes": [
      {"listeningDiscordUser": {"discordId": "1234567890", "username": "user#1234"}, "secondsListened": 6090}
    ]
  }}
  ```

### `/api/secure/guild/:guildId/tracked-songs`
- **Method:** GET
- **Description:** Retrieves tracked songs for a specific guild in paginated JSON format. Optional `page` and `size` parameters (max 20 per page).
- **Example Response:** (for the empty data array reference tracked-song/:trackedSongId)
  ```json
  {"currentPage": 0, "totalPages": 9, "totalItems": "172", "pageSize": 20, "data": [...]}
  ```
### `/api/secure/guild/:guildId`
- **Method:** GET
- **Description:** Provides a general statistical overview for a guild, including total number of songs and total listen time.
- **Example Response:**
  ```json
  {"trackedSongCount": "171", "totalPlaytimeSeconds": "70067"}
  ```
### `/api/secure/me`
- **Method:** GET
- **Description:** Retrieves information about the authenticated user.
- **Example Response:**
  ```json
  {"DISCORD_ID": "806350925723205642", "DISCORD_USERNAME": "kyleyannelli", "DISCORD_AVATAR": "7e0f67b09adcf774a3b1e48d7ffd9508", "TOTAL_LISTEN_SECONDS": "70307", "TOTAL_INITIALIZATIONS": "70", "LISTEN_SECONDS_BY_GUILD": [...]}
  ```
### `/api/secure/me/:guildId`
- **Method:** GET
- **Description:** Retrieves information about the authenticated user in a specific guild.

### `/api/secure/guilds`
- **Method:** GET
- **Description:** Retrieves a list of guilds the authenticated user is a part of.
- **Example Response:**
  ```json
  [{"name": "Javacord", "iconHash": "34d343ac114efced730f56f6a0107355", "id": "151037561152733184"}, ...]
  ```
