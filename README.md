# Hex 

A simple turn based strategy game aiming to clone Slay.

## Phrases

| Prase | Meaning |
|:---|:---|
| Team | A player or computer |
| Territory | Two or more hexes of the same team |
| Piece | An piece on a hex (for example capitals, castles, or peasants) | 
| Capital | The ruling piece of a territory, gives same protection as a peasant |
| Castle | A stationary piece that gives more protection |
| peasant | Least powerful movable piece, can only only take over unprotected land |
| spearman | Slightly more powerful movable piece, can only take over unprotected land, capitals, and kill peasants |
| knight | Even more powerful movable piece, can take over castles, and everything a spearman can |
| baron | The most powerful piece in the game, can kill every piece (including othe barons) |
| pine | A type of tree that grows everywhere |
| palm | A type of tree that grows along the coast |
| tree | Collective word for `pine` and `palm` |

## Development

### Phase 1

Initial development, creating a solid framework to build the game further. 

* Status: Complete

#### Goals

* [x] Display a hexagon grid
* [x] Allow player to drag the grid around
* [x] Allow player to zoom
* [x] Allow player to highlight the hexagon currently under the mouse

### Phase 2 - World 

Use noise algorithms to create worlds

#### Goals

* [X] Map editor
* [ ] World generation
    * Low priority as the map editor works for now

* Status: Complete

#### Phase 3 - Gameplay

Implement the gamerules in the following subphase

* Basic teams
    * User is assigned a team (defaults to green/`LEAF`)
    * Highlight `territory` (make every other hex darker)
    * 
* Economy
* Protection
* Undoing
    * Record each change to the map to allow undoing by "reloading" last state

* Status: Pending

##### Goals

* Two or more cells of the same color (ie team) should have a `capital`
    * Is will be referred to as a `territory`

#### Phase 2C
* Units
    * There should be four types of units in the game
* Economy
    * Every turn each territory gain currency
        * +1 for each empty cell (or cell covered by a building)
        * 0 for cells covered by trees
        * -2 for each `peasant` in the territory
        * -6 for each `spearman` in the territory
        * -18 for each `knight` in the territory
        * -54 for each `baron` in the territory
* War
    * A `capital` will protect it's surrounding cells from `peasants`
    * A `tower` will protect it's surrounding cells from `peasants` and `spearmen`
    * Each type of infantry can only kill other infantry that are lower than itself
* Ecology
    * Palm trees
        * Grows along the coast in all possible directions
    * Forests
        * A new `pine tree` grows on a hex if
            1. It is empty
            2. It is adjacent to at least two other trees

## Set of pair to map

* `\{\"first\":\{"x":([0-9-]+),\"z\":([0-9-]+)\},"second":([0-9]+|\{.+?\})\}(,|\])`
* `\"$1,$2\":$3$4`
