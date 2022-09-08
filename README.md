# Abba Caving

## Placeholders

### Game

* `{current_score}`          - the current score of a player in the game
* `{highest_score}`          - the highest score a player has achieved
* `{current_ores_mined}`     - the total number of ores that a player has mined
* `{total_ores_mined}`       - the total number of ores that a player has mined
* `{wins}`                   - the total number of wins of a player
* `{game_players}`           - the number of players currently in the game
* `{game_state}`             - the current state of the game (Waiting, Starting, In Game, Game Over)
* `{leaderboard_score_<n>}`  - the current score of the n-th top player
* `{leaderboard_player_<n>}` - the name of the n-th top player

To use PlaceholdersAPI, add the `abbacaving_` prefix to any of the above placeholder names.

### Lobby

* `{abbacaving_online}`              - the current total number of players in all rounds
* `{abbacaving_roundid_<id>_state}`   - the current state of the round `<id>`
* `{abbacaving_roundid_<id>_players}` - the current number of players in the round `<id>`
* `{abbacaving_roundid_<id>_slots}`   - the current slots for the round `<id>`
* `{abbacaving_roundid_<id>_counter}` - the current counter (start countdown, game time remaining) of the round `<id>`