# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

### Removed

---

# v2.0.1 - 2024-01-26

### Added

* Add icons to the map editor screens selector
* Add three more islands

### Changed

* Automatically change the editor type to 'set team/piece' when changing the team/piece in the map editor

### Fixed

* Do not show save confirmation when exiting map editor without doing any changes

### Removed

* Remove `Editor Type Specific` button as it is redundant and does not update the UI properly

---
# v2.0.0 - 2023-06-17

Major revision as there are some game play and technical breaking changes in this release.

## Added

### Game play

* New `normal` and `easy` AI
  * These are all the same underlying AI different kind of restrictions
  * Placing castles are disallowed until a few turns in for `normal` and never allowed for `easy` AI
  * The chance of different AIs to just end their turn without considering all their options are depending on the difficultly
* Islands are now sorted in the level select screen

### Input

* Allow player to place castles on living pieces, the living piece will be in the players hand afterward
* Pressing escape/back in the island load screen changes the screen to the level select screen

### Visual

* Show a castle on the main menu when the AI is done with a game
* A white line is drawn below whose turn it currently is on the strength bar
* Level select screen now remember where you last left it off
* Render a grid of disabled hexagons when in map editor mode
* Render relative strength bar in map editor mode
* Attackable hexagons blink periodically
* Attackable hexagons outline color is now based on the strength difference between the held piece and the hexagon to attack
* Add background to the android icon
* Show how many previews to load when in the splash screen
* Show island id and ARtB in debug hud

### Technical

* Add `--save-island-on-loading-it` program argument flag
* On android, allow users to set the orientation

## Changed

### Gameplay

* **BREAKING CHANGE** Limit the maximum start capital to be 25
* Living pieces behave like capitals and will instantly become a grave when no longer a part of a territory
* Reorder teams from `SUN`, `LEAF`, `FOREST`, `EARTH`, `STONE` to `LEAF`, `FOREST`, `STONE`, `EARTH`, `SUN`
* Improve which piece will be replaced by a capital when there is no capital in a territory
  * The order of selection is as follows `empty`, `trees`/`graves`, `baron`, `knight`, `spearman`, `peasant`, `castle`
  * E.g., if there is a peasant and a castle to choose from the algorithm will choose the peasant
* Change calculation of best capital placement
  * Count invisible hexagons as slight more (i.e, 10%) valuable then friendly hexagons
  * Count the surrounding hexagons of a castle as twice as valuable
* Trees will grow at the beginning of each players turn but the newly grown tree will now be able to grow again

### Input

* Double tapping while in-game will smoothly zoom the camera
* Improve enum sorting in the settings screen
* Pressing back on android when in the level select screen will exit the app
* Allow dragging while zooming

### Visual

* Visual strength hint is enabled by default
* Adjust team color to make them easier to distinguish
* Reduce maximum zoom from `2.5` to `2`
* Move the non-islands buttons in the level select screen closer to the center of the screen
* Draw box around island without a Author Rounds to Beat
* Remember last zoom position after resuming on an island

### Technical

* **BREAKING CHANGE** Save metadata on disk as smile instead of json, old saves will not work
* Rendering now happens non-continuously, that is, only when needed
  * This drastically reduces the amount of power the game uses
* Minimum android SDK version have been reduced to 26
  * Turns out not everyone have the latest android version :surprised_pikachu:
* Rename `turn` to `round`
* You will not get the `you lost` message when only AIs are playing against each other
* Flip the setting `disable audio` to `enable audio`
* Only call `ensureCapitalStartFunds` when saving, this speeds up loading drastically
* On desktop, only move/ the camera when pressing the right mouse button
* Reset ARtB when doing an edit to a map
* Don't allow fps to be shown when playing normally
* Improve settings screen
  * Clicking the text will toggle boolean settings
  * Improved reset buttons instead of having to type in a string
  * Use drop down menu for enum selection
  * Increase size elements so its easier to click them on mobile

## Fixed

### Gameplay

* Fix estimated income not updating when buying pieces
* Fix starting capital was higher for the player who didn't start
* Fix living pieces getting killed at the end of round and not start of turn
* Fix merging two territories always keeps the capital of the selected territory
* Fix double processing of pieces in a bankrupt territory at the beginning of a players turn
* Fix false positive `end turn` warnings
* Fix game not ending when there is only a single team alive, while it has two territories
  * If you cannot attack hexagons by buying new units the warning will not be displayed (unless you can buy and place a castle)

### Visual

* Fix capitals rendering "buy" flag when it is an AIs turn
* Fix pieces being animated when in map editor mode
* Black bars on android

### Input

*No input fixes*

### Technical

* Fix map editor not finding the island folder on a fresh clone
* Fix saving while holding piece threw a serialization exception
* Fix double recording of selecting territory event
* Fix players with no territories left still get to play
* Fix trees not remember if they have grown or not after deserialization
* Fix map editor screen not handling pausing/resuming the app
* Do not save invisible hexagons in the islandDTO
* Fix text sometimes being rendered upside down
* Fix rendering a few frames when resuming with disposed screen causing crashes sometimes
* Fix resizing stage screens did not scale properly
* Fix crash when there are no visible hexagons
* Fix exceptions being thrown if an island had no hexagons loaded
* Fix wrong AI team being announced as the winner when you loose
* Fix wrong tree type in island 24
* Fix crash on chrome os desktop
* Fix click sound on android
* Fix missing clicking sound on interaction

## Removed

### Gameplay

* Remove the old `easy` and `un_losable` AIs

### Input

*No input removed*

### Visual

* Remove the boxes around islands on the level select screen

### Technical

* Remove the AI end turn delay
* Remove `vsync`, `msaa`, and `audio` settings on android due to the platform not supporting changing them or being badly supported
* Remove `limit fps`, `target fps` settings from all platforms, as they are now irrelevant
* Remove no-op editor

---

## [1.2.0] - 2023-02-06

### Added

* Add a visual strength hint in the current selected territory
  * Disabled by default, can be turned on in the settings
* Add debug fps graph
* Add relative strength bar
* Add debug option to disallow AI from surrendering

### Changed

* Java 17 is the minimum Java version
* Minimum android SDK version have been bumped to 33
* Minor improvement to the hard AI
  * [c0c024d](https://github.com/elgbar/hex/commit/c0c024db4cab54fccf2d5ee0d395cf524c16c305), Clear attack blacklist after merging pieces, as a higher level piece might be able
    attack something new
  * [7434fcf](https://github.com/elgbar/hex/commit/7434fcf1b25bb1dad6a2bfbe9dcd6c2283d797f1), Attack highest possible defended hexagon if all attackable hexagons are empty
  * [8e7488e](https://github.com/elgbar/hex/commit/8e7488eb929b2896bf9c38145f2e7993ac36afd4), Place living pieces defensively different from castles
  * [7a5bbcf](https://github.com/elgbar/hex/commit/7a5bbcf44d7231480747964e2bd3fb453361c693), Do not select pieces which cannot do anything
  * [fdcbe26](https://github.com/elgbar/hex/commit/fdcbe26f1433fcd001039327d41579cec26b7b12), When testing whether to merge/buy a piece or not, look at all hexagons in neighbouring
    territories and not only the bordering hexagons
  * [351344b](https://github.com/elgbar/hex/commit/351344be3f44afbab58fa496a0c82c25f543fb33), When merging a piece, pick it up again straight away
  * [940e556](https://github.com/elgbar/hex/commit/940e5568a0bdd62e902ede32e61079527979b2eb), Only buy new units when they can be used for something
  * [1df3a6b](https://github.com/elgbar/hex/commit/1df3a6bc91ff646c726d0d7ddfc6a74dee069cdf), Disallow merging if the new piece cannot attack
* The time AI uses on its move is subtracted from the minimum AI delay
* Increase default outline width
* Cheating can be toggled when cheating via the `--i-am-a-cheater` flag; i.e., debug and trace logging is disabled
* Use lwjgl3 for desktop backend
* Improve overlay window for android

### Fixed

* Fix exception when entering invalid enum in settings
* If the AI throw an exception, handle it
* An AI will not cause an end-of-turn delay if it has no territories
* Fix right control cannot be used to undo and redo moves
* Fix certain settings name being badly formatted
* Fix bankrupt territory not having their balance set to 0
* Remember whose turn it is when loading a saved island
* Fix crash when resizing the window to zero height
* Fix horrible zoom on android
* Fix UI elements not rendering after calling pause the resume
* Fix piece down sound not playing on android

### Removed

* Remove the 11th piece down sound

---

## [1.1.0] - 2021-05-06

### Added

* Added sounds!
  * Might be a bit buggy on android
* AI will now surrender if they most definitely have lost to a player
* Improve hard AI
  * It will now try to merge territories if it can
* Add scrolling to settings to allow mobile users a better experience with the settings
* Add mouse scrolling back to the level select screen
* Display version in the settings screen
* Display version in the desktop title
* Add `hold to march` option
* Add `vsync` option
* Pressing `escape` or `space` will re-place the selected piece, if holding any
* Allow cheaters to buy even if the selected territory normally couldn't afford it

### Changed

* Hide some settings behind launching in debug mode (i.e. specifying the `debug` or `trace` flag)
* Reorder settings
* Update `README` with more information
* Improve load time of level select screen
* Do not render the hand when its an AIs turn
* The AI will no longer cheat when the player does
* Allow debug HUD to be disabled when in debug mode

### Fixed

* Fix a series of serialization bugs
  * Piece state was not serialized, if a piece had moved, treasury of a capital, etc. was not persisted
* Fix `help` launch flag not displaying help, but crashing instead
* Fix last four (islands per row) islands in level select screen not working
* Fix AI using a very long time when they have large territories
* Island now remember what turn it is when re-entering island
* The player will now lose the game when no players have any capitals left, but AI(s) do

---

## [1.0.0] - 2021.04.13

### Added

* Initial release of the game