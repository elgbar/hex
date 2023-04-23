package org.hexworks.mixite.core.api.contract

/**
 * Represents arbitrary data which can be attached to a Hexagon.
 * An implementation should contain a set of fields for advanced
 * grid algorithms like pathfinding.
 */
interface SatelliteData {

  /**
   * @return wether this hexagon should be interacted with in the map
   */
  var isDisabled: Boolean
}