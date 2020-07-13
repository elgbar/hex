# Hex 

A simple turn based strategy game aiming to clone Slay.

## Development

### Phase 1

Initial development, creating a solid framework to build the game further. 

* Status: Partially complete

#### Goals

* [x] Display a hexagon grid
* [x] Allow player to drag the grid around
* [x] Allow player to zoom
* [ ] Allow player to highlight the hexagon currently under the mouse

### Phase 2

The second phase has been split into two halves that can be worked on in parallel.

* Status: Pending

#### Phase 2A - World 

Use noise algorithms to create worlds

* Status: Pending

#### Phase 2B-A - Gameplay

Implement the gamerules in the following subphase

* Economy
  
* Protection

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
