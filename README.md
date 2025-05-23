# Hex

[Download the app on Google Play Store](https://play.google.com/store/apps/details?id=no.elg.hex)

A simple turn based strategy game aiming to clone the gameplay of Slay.

![Level select screen](./images/level-select.png)

![Playing the game](./images/placing-unit.png)

![Creating a new level in map editor](./images/level-creation.png)

![Editing a map in map editor](./images/map-editor.png)

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

*Accurate as of version 2.4.0-snapshot*

```
usage: [-h] [-d] [-t] [-s] [-e] [--i-am-a-cheater] [--disable-island-loading]
       [--draw-edges] [--stage-debug] [--update-previews]
       [--update-saved-islands] [--load-all-islands] [--reset-all] [--ai-debug]
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

  --stage-debug              Enable debug overlay for UI

  --update-previews          Update pre-rendered previews of islands

  --update-saved-islands     Update the saved islands by saving them again
                             after loading them. Only really useful when
                             changing the save format

  --load-all-islands         Load all islands at startup instead of when first
                             played

  --reset-all                Resetting settings and progress

  --ai-debug                 Listen to the AI thinking

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

## Import/export

### Format

The progress is exported as a xz compressed json file then base85 encoded to make it smaller.
The uncompressed json is either an ongoing game or a completed game.
When the game is ongoing a list of hexagons that have changed from the initial map is stored
When the game is completed the winner is stored.

## For maintainers

### Creating release files

#### Android

**First bump the android version code!**
In the `Build` > `Generate Signed App Bundle or Apk` choose `Android App Bundle` then (if not already done: import the `hex.jks` and get the password from the password manager) then
choose `release`.
It should be found [here](./android/release)

#### Desktop

Run `gradle desktop:dist` output jar should be [here](./desktop/build/libs) as `desktop-<version>.jar`

### TODO

* Add minimap
* Add a proper tutorial, nobody likes to read
* Reimplement `DefaultHexagonDataStorage` to use `IntMap` and where the key is a compacted key
* Add fling support in the level select screen
* When clicking an island to play it, zoom into the sprite while loading the island
* Add support for ctrl-z and ctrl-y in the map editor

#### Known bugs

* Lag when un/redoing after a long game (not verified it still happens after 2.0.0)
  * Hard to do since it's loading the previous version of the map. Have to look into why it takes longer late-game
  * The reason for the lag is when there are a large amount of actions done there are lots of data
  * Ideas to mitigate this issue
    * Make the history contain a delta not the whole history