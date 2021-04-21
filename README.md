# Hex

A simple turn based strategy game aiming to clone Slay.

![Level select screen](./images/levelselect.png)

![Playing the game](./images/placing-unit.png)

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

## Help page

*Accurate as of version 1.0*

```
usage: [-h] [-d] [-t] [-s] [-e] [--i-am-a-cheater] [--disable-island-loading]
       [--draw-edges] [--stage-debug] [--update-previews] [--load-all-islands]
       [--scale SCALE] [--profile]

optional arguments:
  -h, --help                 show this help message and exit

  -d, --debug                Enable debug logging

  -t, --trace                Enable even more logging

  -s, --silent               Do not print anything to stdout or stderr

  -e, --map-editor           Start the program in map editor mode

  --i-am-a-cheater           Enable cheating

  --disable-island-loading   Don't load islands

  --draw-edges               Draw the edge hexagons to assists with debugging

  --stage-debug              Enable debug overlay for UI using scene2d

  --update-previews          Update pre-rendered previews of islands

  --load-all-islands         Load all islands at startup instead of when first
                             played

  --scale SCALE              Scale of UI, if <= 0 default scale apply

  --profile                  Enable GL profiling


Process finished with exit code 0

```

## TODO

* Add minimap
* Add relative strength bar
* Let AI surrender when it is clear that the player is winning
* Sounds
* Fix grave piece
    * Graves behaves differently for the AI than the player
    * It works correctly for players
    * Does not work for AI (it stays too long)
* Do not end the current AIs turn when exiting and re-entering a level, as this can be exploited
* Allow for scrolling farther down in settings to allow mobile user to see what they type
* Returning to level select screen is very slow
