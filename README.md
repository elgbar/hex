# Hex

A simple turn based strategy game aiming to clone the gameplay of Slay.

![Level select screen](./images/levelselect.png)

![Playing the game](./images/placing-unit.png)

## Glossaries

| Glossary  | Explanation                                                                                            |
|:----------|:-------------------------------------------------------------------------------------------------------|
| Team      | A player or computer                                                                                   |
| Territory | Two or more hexes of the same team connected together                                                  |
| Piece     | A chip on a hex (for example `capital`, `castle`, or `peasant`)                                        |
| Capital   | The ruling piece of a territory, gives same protection as a peasant                                    |
| Castle    | A stationary piece that gives protection equal to a spearman                                           |
| peasant   | Least powerful movable piece, can only only take over unprotected land                                 |
| spearman  | Slightly more powerful movable piece, can only take over unprotected land, capitals, and kill peasants |
| knight    | Even more powerful movable piece, can take over castles, and everything a spearman can do              |
| baron     | The most powerful piece in the game, can kill every piece (including other barons)                     |
| pine      | A type of tree that grows when there are to adjacent pines                                             |
| palm      | A type of tree that grows along the coast                                                              |
| tree      | Collective word for `pine` and `palm`                                                                  |

## Help page

*Accurate as of version 1.2.0*

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
```

## Hints

* Launch the game with `--map-editor` to be able to edit all maps, and even create new ones
    * Example launch command `java -jar Hex.jar --map-editor`
* If cheating try to press...
    * `F10` to re-move your moved pieces
    * `F11` to make the AI surrender
    * `F12` to toggle cheating
* If launching with `--debug` or `--trace` mode try to press `F12` to enable cheating
* There are more options available in the settings when launching with `--debug` or `--trace`
* Pressing space or backspace will unselect piece in hand if holding something or unselect selected territory if not
* Use `F1` to buy a castle in the currently selected territory
* Likewise, `F2` will buy a spearman, `F3` a knight, and `F4` a baron
* Undo and redo can be done with `Ctrl-Z` and `Ctrl-Y` respectfully

## TODO

* Add minimap
* Generate trees when generating random island
* Rework settings screen
* Look into replacement logic for capitals
  * Optimize for survivability

### Known bugs

* Limit FPS does not work
* Lag when un/redoing after a long game
* Grave take too long to become trees
* Calculate estimated income with holding piece
* Map editor is not working