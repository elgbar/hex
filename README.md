# Hex

A simple turn based strategy game aiming to clone Slay.

## Glossaries

| Glossary  | Explanation                                                                                            |
| :-------- | :----------------------------------------------------------------------------------------------------- |
| Team      | A player or computer                                                                                   |
| Territory | Two or more hexes of the same team                                                                     |
| Piece     | An piece on a hex (for example capitals, castles, or peasants)                                         |
| Capital   | The ruling piece of a territory, gives same protection as a peasant                                    |
| Castle    | A stationary piece that gives more protection                                                          |
| peasant   | Least powerful movable piece, can only only take over unprotected land                                 |
| spearman  | Slightly more powerful movable piece, can only take over unprotected land, capitals, and kill peasants |
| knight    | Even more powerful movable piece, can take over castles, and everything a spearman can                 |
| baron     | The most powerful piece in the game, can kill every piece (including other barons)                     |
| pine      | A type of tree that grows everywhere                                                                   |
| palm      | A type of tree that grows along the coast                                                              |
| tree      | Collective word for `pine` and `palm`                                                                  |

## TODO

* Allow splash screen to be used to show the game is loading something other than the intro
  * Allow to specify what the next screen will be
  * Used when loading island (when loading lazily)
* Remember state of islands between sessions
* Add help page
* Add minimap
* Add relative strength bar
* local multiplayer
* Let AI surrender when it is clear that the player is winning
* Hold to march
  * Double click/long click to move all units to around the hexagon pressed
  * disable hold to march setting
* Sounds
* Fix grave piece
  * Graves behaves differently for the AI than the player
  * It works correctly for players
  * Does not work for AI (it stays too long)
* Do not end the current AIs turn when exiting and re-entering a level, as this can be exploited
