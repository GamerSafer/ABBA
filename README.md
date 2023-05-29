# Abba Caving

## Commands
* `acreload` - Reloads the configuration. (abbacaving.reload)
* `aclookup` - Looks up stats for a specific round. (abbacaving.aclookup)
* `bcastnp` - Broadcast a message. (abbacaving.broadcast)
* `forcestart` - Force starts the game. (abbacaving.forcestart)
* `join` - Joins a game lobby. (abbacaving.join)
* `leave` - Leaves a game lobby. (abbacaving.leave)
* `nightvision` -Night vision toggle. (abbacaving.nightvision)
* `stats` - Display a player's stats. (abbacaving.stats)
* `points` - Display a player's points. (abbacaving.points)
* `respawns` - Allows you to set the respawns of a .player (abbacaving.respawns)
  * ``/respawns Owen1212055 1``- Sets Owen1212055's respawn amount to 1
  * ``/respawns Owen1212055 add 1``- Adds 1 to Owen1212055's respawn amount
  * ``/respawns Owen1212055``- Gets Owen1212055's respawn amount

## Placeholders

### Game

* `%abbacaving_current_score%`          - the current score of a player in the game
* `%abbacaving_current_respawns%`       - the current amount of respawns that a player has
* `%abbacaving_highest_score%`          - the highest score a player has achieved
* `%abbacaving_current_ores_mined%`     - the total number of ores that a player has mined
* `%abbacaving_total_ores_mined%`       - the total number of ores that a player has mined
* `%abbacaving_wins%`                   - the total number of wins of a player
* `%abbacaving_game_players%`           - the number of players currently in the game
* `%abbacaving_game_maxplayers%`        - the maximum number of players the game allows
* `%abbacaving_game_name%`               - the name of the world/map the game's using
* `%abbacaving_game_id%`                - the current round/game's ID
* `%abbacaving_game_state%`             - the current state of the game (Waiting, Starting, In Game, Game Over)
* `%abbacaving_leaderboard_score_<n>%`  - the current score of the n-th top player
* `%abbacaving_leaderboard_player_<n>%` - the name of the n-th top player
* `%abbacaving_x%`                      - the current x coordinate of a player
* `%abbacaving_y%`                      - the current y coordinate of a player
* `%abbacaving_z%`                      - the current z coordinate of a player

### Lobby

* `%abbacaving_online%`                    - the current total number of players in all maps
* `%abbacaving_roundid_<mapName>_state%`   - the current state of the map `<mapName>`
* `%abbacaving_roundid_<mapName>_players%` - the current number of players in the map `<mapName>`
* `%abbacaving_roundid_<mapName>_slots%`   - the current slots for the map `<mapName>`
* `%abbacaving_roundid_<mapName>_counter%` - the current counter (start countdown, game time remaining) of the map `<mapName>`
* `%abbacaving_roundid_<mapName>_required%`- the required player count in order to start the countdown for the map `<mapName>`

### Old Games

* `%abbacaving_game_<gameid>_leaderboard_<num>_playername%`  - the name of a player on game `<gameid>` who placed `<num>`
* `%abbacaving_game_<gameid>_leaderboard_<num>_playeruuid%`  - the uuid of a player on game `<gameid>` who placed `<num>`
* `%abbacaving_game_<gameid>_leaderboard_<num>_score%`       - the score of a player on game `<gameid>` who placed `<num>`

Note: Num starts at 1

### All games
* `%abbacaving_global_leaderboard_score_<num>_playername%`  - the name of a player who is `<num>` overall
* `%abbacaving_global_leaderboard_score_<num>_playeruuid%`  - the uuid of a player who is `<num>` overall
* `%abbacaving_global_leaderboard_score_<num>_score%`       - the score of a player who is `<num>` overall

Note: Num starts at 1



### All games
* `%abbacaving_global_block_leaderboard_score_<num>_playername%`  - the name of a player who is `<num>` overall
* `%abbacaving_global_block_leaderboard_score_<num>_playeruuid%`  - the uuid of a player who is `<num>` overall
* `%abbacaving_global_block_leaderboard_score_<num>_score%`       - the score of a player who is `<num>` overall

Note: Num starts at 1

