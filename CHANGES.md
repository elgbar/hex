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

# v2.4.0 - 2025-01-16

### Added

* New music music by [Stey](https://soundcloud.com/stey_music)
* Add 50 new playable maps
* Scroll one page at a time in the level select screen with `page up` and `page down`
* Draw island id and ARtB on the island previews when in map editor mode
* Show strength for each hexagon in map editor mode, and can be toggled in the settings
* Add debug option to show strength hint for all player territories
* Add program flag `--ai-debug` to print what the AI is thinking
* Add test islands to persist test maps in VCS
* Add shortcut `ctrl` + `N` to create a new island in the map editor
* Add button to create new islands directly from the level creation screen
* New islands validation rule: There must be at least two different teams with a capital
* Show the island id to make it easier to communicate about islands
* Show high score for each island in the level select screen

### Changed

* Sort islands without ARtB by their id in ascending order
* Change color of boxes in the level select screen based on the ARtB
* Show more info about the current island and hovered hexagon when in map editor mode
* Show the number of rounds it took for AI to be done with a map on the island preview
* Do not render the ARtB and id on the preview
  * Instead, render it dynamically and show it when in debug or in map editor
* Always allow surrendering when there is AIs playing against each other
* Remain in the settings screen when resetting setting/progress
* Tune AI
  * Prioritize attacking hexagons that are a part of a territory
  * Change chances of AI to buy a castle based on the difficulty
  * Increase the chance of easy and normal AI to end their turn early
  * Fix AI holding a piece when it does not think it does
* Added remaining ARtB to existing islands
* Rename the `UNKNOWN_ROUNDS_TO_BEAT` constant to `NEVER_PLAYED` to make it clear what it means
* Add settings, which defaults to `false`, to toggle double tapping to zoom

### Fixed

* Fix unnecessary loading all assets after pausing and resuming the game
* Toggle music button was disabled when island interaction was disabled
* Fix UI sometimes lagging when fading in windows
* Win text on the islands preview was worded differently to the other results
* Fix AI not calculating the strength of attackable territories correctly
* Fix click were going through windows in map editor
* Move windows in map editor a bit from the edge
* Fix island deletion crashing the game
* Fix do not leave the playing island when a single exception occurs
* Harden against exceptions within a start/end block

### Removed

* All old music tracks have been removed

---

# v2.3.0 - 2024-09-27

### Added

* Added music by Stey [SoundCloud](https://soundcloud.com/stey_music)
  * New mute icon on the level select screen and in game
* Add global shortcuts
  * `ctrl` + `m` to toggle music
  * `alt` + `enter` to toggle fullscreen

### Changed

* Rename `volume` setting to `master volume`
* Tweak island 62 to make it possible for leaf to win
* Display more info in the map editor
* Update the ARtB of the smaller islands (id 57-63)
* Tune AI
  * Disallow hard AI to buy castles on the first round
  * Double the number of rounds before normal AI can buy castles
  * AI will try to attack enemy living pieces in the order of `baron`, `knight`, `spearman`, `peasant`
  * AI will try to attack enemy trees 
  * Add special case when there is only a single least defended hexagon, will try to place it adjacent to it if possible to protect the living piece 
  * Only prioritize cutting down trees that will propagate into friendly territory
  * Improve castle placement to be allowed to place on movable living pieces
  * Improve castle placement to ignore the capital and living pieces 
    * to more fairly distribute them regardless of how living pieces are placed
    * to place castles near capitals to defend them

### Fixed

* Improve audio on android by using `AsynchronousAndroidAudio`
* Do not prompt for end turn when the player cannot afford merging two pieces
* Trees are only allowed grow once per round, but it does grow after each turn
* Fix UI wiggle when interacting with it
* Fix lag when scrolling through the level select screen

### Removed

* Remove background texture as texture atlas is too large

---

# v2.2.0 - 2024-09-15

### Added

* Add export and import of islands
  * Currently only to the clipboard
  * A screen will show the import progress
* Add nine new islands
* Play a "bad click" sound when doing an action that does nothing
* Add a highlight to actionable hexagons when trying to end turn
  * It can also be permanently enabled in the settings
* Add randomize every team editor
* `home` and `end` keys will now take you to the top and bottom of the level select screen respectively

### Changed

* Only clear island progress when restarting island
* Do not clear trees when AI is surrendering
* Do not animate when rendering previews
* Never fail on any properties
* Allow AI to surrender when there is only a single real player left
  * Even when playing against other players, the AI will surrender if there is only one player left
* Update ArTB, all times by Willy
  * island 5 to 33 rounds
  * island 13 from 16 to 14 rounds
  * island 10 from 60 to 36 rounds
  * island 15 from 61 to 25 rounds
  * island 19 from 36 to 19 rounds
  * island 21 to 33 rounds
  * island 24 from 52 to 28 rounds
  * island 25 from 55 to 23 rounds
  * island 34 from 9 to 7 rounds
  * island 49 from 41 to 21 rounds
  * island 50 from 20 to 18 rounds
* Randomize teams on islands 1, 9, 16 as they were impossible to win with leaf on

### Fixed

* Fix not displaying version in the settings screen and title on desktop
* Fix potential memory leak when updating previews
* Fix tutorial screen showing vertical scroll bar on high w:h ratio devices
* Fix possibility of the async thread not being disposed properly
* Fix game not starting when the first player is an AI
* Fix crash when exiting island when there are no hexagons visible
* Fix inputting ARtB would not update until the spinner lost focus
* Fix team percentages not updating when hexagons changes

### Removed

* Remove useless ARtB menu entry

---

# v2.1.0 - 2024-07-13

* New islands validation rule: No pieces on invisible hexagons
* Add confirmation dialog when restarting a completed island
  * Can be disabled with the 'disable restart confirmation' setting
* Write round number on the island previews

### Changed

* Make it easier to read the upkeep cost in the tutorial screen by adding a white border to the numbers
* Tweak tutorial screen
* Pressing escape on desktop will take you back to the level select screen
* Allow islands to be manually placed last with ARtB
* Make ARtB editing more user friendly
  * Add a warning when resetting ARtB of an island
  * Display the previous ARtB when editing an island
* Always render the previews as 1024x1024
* Update ARtB of island 11 to be 11 rounds
* Draw a capital on the preview when the island has been conquered
* Suggest that there are actions to do when a piece can chop down a tree

### Fixed

* Using regular font instead of bold font
* Fix infinite tower glitch
  * A player could swap a living piece and a just-bought castle to get the castle for free
  * Thanks Willy!
* Fix sorting islands without a rating before islands with ratings
* Fix screen messages not fading out smoothly/at all
* Fix previews being inconsistently rendered
* Remove the raw tutorial images from the app/jar
* Fix switching app after winning/loosing but before pressing OK would cause the island to be loaded in a non-playable state
* Fix game not ending properly when the player has no territories left

### Removed

---

# v2.0.2 - 2024-06-29

### Fixed

* Fix critical crash when selecting territory after placing a piece
* Fix surrendering not working correctly
* Fix camera jumping when saving

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