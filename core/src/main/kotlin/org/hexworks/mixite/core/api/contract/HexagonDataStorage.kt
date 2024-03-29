package org.hexworks.mixite.core.api.contract

import org.hexworks.cobalt.datatypes.Maybe

/**
 * This interface represents all storage operations which are needed for a working
 * [HexagonalGrid].
 * @param <T> the type of the stored [SatelliteData] implementation
 */
interface HexagonDataStorage<K, T : SatelliteData> {

  /**
   * Returns all coordinates which are stored in this object.
   */
  val coordinates: Iterable<K>

  /**
   * Adds a [K] for this grid without any [SatelliteData].
   * Does not overwrite the coordinate if it is already present.
   */
  fun addCoordinate(cubeCoordinate: K)

  /**
   * Adds a [K] for this grid with [SatelliteData].
   * Overwrites previous [SatelliteData] if it was present.
   * @return true if overwrote data false otherwise.
   */
  fun addCoordinate(cubeCoordinate: K, satelliteData: T): Boolean

  /**
   * Gets the [SatelliteData] stored on a [K] if present.
   * Also returns empty [Maybe] when `cubeCoordinate` is not present.
   * @return optional [SatelliteData].
   *
   * TODO replace return type with T?
   */
  fun getSatelliteDataBy(cubeCoordinate: K): Maybe<T>

  /**
   * Tells whether there is a [Hexagon] on the given [K] or not.
   * @return true if present false if not
   */
  fun containsCoordinate(cubeCoordinate: K): Boolean

  /**
   * Tells whether there is [SatelliteData] stored for a [K] or not.
   * Also returns false if `cubeCoordinate` is not present in the storage.
   */
  fun hasDataFor(cubeCoordinate: K): Boolean

  /**
   * Clears the [SatelliteData] for the given [K].
   * @return true if the storage was changed false otherwise.
   */
  fun clearDataFor(cubeCoordinate: K): Boolean
}