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

## Placeholders

### Game

* `%abbacaving_current_score%`          - the current score of a player in the game
* `%abbacaving_highest_score%`          - the highest score a player has achieved
* `%abbacaving_current_ores_mined%`     - the total number of ores that a player has mined
* `%abbacaving_total_ores_mined%`       - the total number of ores that a player has mined
* `%abbacaving_wins%`                   - the total number of wins of a player
* `%abbacaving_game_players%`           - the number of players currently in the game
* `%abbacaving_game_maxplayers`         - the maximum number of players the game allows
* `%abbacaving_map_name`                - the name of the world/map the game's using
* `%abbacaving_game_id`                 - the current round/game's ID
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
