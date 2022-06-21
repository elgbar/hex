# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed
* Java 17 is the minimum java version
* Minor improvement to the hard AI
  * [c0c024d](https://github.com/elgbar/hex/commit/c0c024db4cab54fccf2d5ee0d395cf524c16c305), Clear attack blacklist after merging pieces, as a higher level piece might be able attack something new
  * [7434fcf](https://github.com/elgbar/hex/commit/7434fcf1b25bb1dad6a2bfbe9dcd6c2283d797f1), Attack highest possible defended hexagon if all attackable hexagons are empty
  * [8e7488e](https://github.com/elgbar/hex/commit/8e7488eb929b2896bf9c38145f2e7993ac36afd4), Place living pieces defensively different from castles
  * [7a5bbcf](https://github.com/elgbar/hex/commit/7a5bbcf44d7231480747964e2bd3fb453361c693), Do not select pieces which cannot do anything
* The time AI uses on its move is subtracted from the minimum AI delay 
* Increase default outline width
* Cheating can be toggled when cheating via the `--i-am-a-cheater` flag; i.e., debug and trace logging is disabled

### Fixed
* Fix exception when entering invalid enum in settings
* If the AI throw an exception, handle it
* An AI will not cause an end-of-turn delay if it has no territories

### Removed

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

## [1.0.0] - 2021.04.13

### Added
* Initial release of the game