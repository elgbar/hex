// MIT License
//
// Copyright(c) 2020 Jordan Peck (jordan.me2@gmail.com)
// Copyright(c) 2020 Contributors
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// .'',;:cldxkO00KKXXNNWWWNNXKOkxdollcc::::::;:::ccllloooolllllllllooollc:,'...        ...........',;cldxkO000Okxdlc::;;;,,;;;::cclllllll
// ..',;:ldxO0KXXNNNNNNNNXXK0kxdolcc::::::;;;,,,,,,;;;;;;;;;;:::cclllllc:;'....       ...........',;:ldxO0KXXXK0Okxdolc::;;;;::cllodddddo
// ...',:loxO0KXNNNNNXXKK0Okxdolc::;::::::::;;;,,'''''.....''',;:clllllc:;,'............''''''''',;:loxO0KXNNNNNXK0Okxdollccccllodxxxxxxd
// ....';:ldkO0KXXXKK00Okxdolcc:;;;;;::cclllcc:;;,''..... ....',;clooddolcc:;;;;,,;;;;;::::;;;;;;:cloxk0KXNWWWWWWNXKK0Okxddoooddxxkkkkkxx
// .....';:ldxkOOOOOkxxdolcc:;;;,,,;;:cllooooolcc:;'...      ..,:codxkkkxddooollloooooooollcc:::::clodkO0KXNWWWWWWNNXK00Okxxxxxxxxkkkkxxx
// . ....';:cloddddo___________,,,,;;:clooddddoolc:,...      ..,:ldx__00OOOkkk___kkkkkkxxdollc::::cclodkO0KXXNNNNNNXXK0OOkxxxxxxxxxxxxddd
// .......',;:cccc:|           |,,,;;:cclooddddoll:;'..     ..';cox|  \KKK000|   |KK00OOkxdocc___;::clldxxkO0KKKKK00Okkxdddddddddddddddoo
// .......'',,,,,''|   ________|',,;;::cclloooooolc:;'......___:ldk|   \KK000|   |XKKK0Okxolc|   |;;::cclodxxkkkkxxdoolllcclllooodddooooo
// ''......''''....|   |  ....'',,,,;;;::cclloooollc:;,''.'|   |oxk|    \OOO0|   |KKK00Oxdoll|___|;;;;;::ccllllllcc::;;,,;;;:cclloooooooo
// ;;,''.......... |   |_____',,;;;____:___cllo________.___|   |___|     \xkk|   |KK_______ool___:::;________;;;_______...'',;;:ccclllloo
// c:;,''......... |         |:::/     '   |lo/        |           |      \dx|   |0/       \d|   |cc/        |'/       \......',,;;:ccllo
// ol:;,'..........|    _____|ll/    __    |o/   ______|____    ___|   |   \o|   |/   ___   \|   |o/   ______|/   ___   \ .......'',;:clo
// dlc;,...........|   |::clooo|    /  |   |x\___   \KXKKK0|   |dol|   |\   \|   |   |   |   |   |d\___   \..|   |  /   /       ....',:cl
// xoc;'...  .....'|   |llodddd|    \__|   |_____\   \KKK0O|   |lc:|   |'\       |   |___|   |   |_____\   \.|   |_/___/...      ...',;:c
// dlc;'... ....',;|   |oddddddo\          |          |Okkx|   |::;|   |..\      |\         /|   |          | \         |...    ....',;:c
// ol:,'.......',:c|___|xxxddollc\_____,___|_________/ddoll|___|,,,|___|...\_____|:\ ______/l|___|_________/...\________|'........',;::cc
// c:;'.......';:codxxkkkkxxolc::;::clodxkOO0OOkkxdollc::;;,,''''',,,,''''''''''',,'''''',;:loxkkOOkxol:;,'''',,;:ccllcc:;,'''''',;::ccll
// ;,'.......',:codxkOO0OOkxdlc:;,,;;:cldxxkkxxdolc:;;,,''.....'',;;:::;;,,,'''''........,;cldkO0KK0Okdoc::;;::cloodddoolc:;;;;;::ccllooo
// .........',;:lodxOO0000Okdoc:,,',,;:clloddoolc:;,''.......'',;:clooollc:;;,,''.......',:ldkOKXNNXX0Oxdolllloddxxxxxxdolccccccllooodddd
// .    .....';:cldxkO0000Okxol:;,''',,;::cccc:;,,'.......'',;:cldxxkkxxdolc:;;,'.......';coxOKXNWWWNXKOkxddddxxkkkkkkxdoollllooddxxxxkkk
//       ....',;:codxkO000OOxdoc:;,''',,,;;;;,''.......',,;:clodkO00000Okxolc::;,,''..',;:ldxOKXNWWWNNK0OkkkkkkkkkkkxxddooooodxxkOOOOO000
//       ....',;;clodxkkOOOkkdolc:;,,,,,,,,'..........,;:clodxkO0KKXKK0Okxdolcc::;;,,,;;:codkO0XXNNNNXKK0OOOOOkkkkxxdoollloodxkO0KKKXXXXX
//
// VERSION: 1.0.1
// https://github.com/Auburn/FastNoise
// To switch between using floats or doubles for input position,
// perform a file-wide replace on the following strings (including )
//  float
//  double
package no.elg.hex.util.noise

import no.elg.hex.util.noise.CellularDistanceFunction.EUCLIDEAN
import no.elg.hex.util.noise.CellularDistanceFunction.EUCLIDEAN_SQ
import no.elg.hex.util.noise.CellularDistanceFunction.HYBRID
import no.elg.hex.util.noise.CellularDistanceFunction.MANHATTAN
import no.elg.hex.util.noise.CellularReturnType.CELL_VALUE
import no.elg.hex.util.noise.CellularReturnType.DISTANCE
import no.elg.hex.util.noise.CellularReturnType.DISTANCE_2
import no.elg.hex.util.noise.CellularReturnType.DISTANCE_2_ADD
import no.elg.hex.util.noise.CellularReturnType.DISTANCE_2_DIV
import no.elg.hex.util.noise.CellularReturnType.DISTANCE_2_MUL
import no.elg.hex.util.noise.CellularReturnType.DISTANCE_2_SUB
import no.elg.hex.util.noise.DomainWarpType.BASIC_GRID
import no.elg.hex.util.noise.DomainWarpType.OPEN_SIMPLEX_2_REDUCED
import no.elg.hex.util.noise.FractalType.DOMAIN_WARP_INDEPENDENT
import no.elg.hex.util.noise.FractalType.DOMAIN_WARP_PROGRESSIVE
import no.elg.hex.util.noise.FractalType.FBM
import no.elg.hex.util.noise.FractalType.PING_PONG
import no.elg.hex.util.noise.FractalType.RIDGED
import no.elg.hex.util.noise.NoiseType.CELLULAR
import no.elg.hex.util.noise.NoiseType.OPEN_SIMPLEX_2
import no.elg.hex.util.noise.NoiseType.OPEN_SIMPLEX_2_S
import no.elg.hex.util.noise.NoiseType.PERLIN
import no.elg.hex.util.noise.NoiseType.VALUE
import no.elg.hex.util.noise.NoiseType.VALUE_CUBIC
import no.elg.hex.util.noise.RotationType3D.IMPROVE_XY_PLANES
import no.elg.hex.util.noise.RotationType3D.IMPROVE_XZ_PLANES
import no.elg.hex.util.noise.RotationType3D.NONE
import no.elg.hex.util.noise.TransformType3D.DEFAULT_OPEN_SIMPLEX_2

@Suppress("DuplicatedCode")
class FastNoiseLite {
  var seed = 1337
    private set
  var frequency = 0.01f
    private set
  var noiseType = OPEN_SIMPLEX_2
    private set
  var rotationType3D = NONE
    private set
  var fractalType = FractalType.NONE
    private set
  var octaves = 3
    private set
  var lacunarity = 2.0f
    private set
  var gain = 0.5f
    private set
  var weightedStrength = 0f
    private set
  var pingPongStength = 2.0f
    private set
  var fractalBounding = 1 / 1.75f
    private set
  var cellularDistanceFunction = EUCLIDEAN_SQ
    private set
  var cellularReturnType = DISTANCE
    private set
  var cellularJitterModifier = 1.0f
    private set
  var domainWarpType = DomainWarpType.OPEN_SIMPLEX_2
    private set
  var domainWarpAmp = 1.0f
    private set

  private var transformType3D = DEFAULT_OPEN_SIMPLEX_2
  private var warpTransformType3D = DEFAULT_OPEN_SIMPLEX_2

  /**
   * Create new FastNoise object with default seed
   */
  constructor()

  /**
   * Sets seed used for all noise types
   *
   * Default: 1337
   */
  fun setSeed(seed: Int) {
    this.seed = seed
  }

  /**
   * Sets frequency for all noise types
   *
   * Default: 0.01
   */
  fun setFrequency(frequency: Float) {
    this.frequency = frequency
  }

  /**
   * Sets noise algorithm used for GetNoise(...)
   *
   * Default: OpenSimplex2
   */
  fun setNoiseType(noiseType: NoiseType) {
    this.noiseType = noiseType
    UpdateTransformType3D()
  }

  private fun UpdateTransformType3D() {
    when (rotationType3D) {
      IMPROVE_XY_PLANES -> transformType3D = TransformType3D.IMPROVE_XY_PLANES
      IMPROVE_XZ_PLANES -> transformType3D = TransformType3D.IMPROVE_XZ_PLANES
      else -> when (noiseType) {
        OPEN_SIMPLEX_2, OPEN_SIMPLEX_2_S -> transformType3D = DEFAULT_OPEN_SIMPLEX_2
        else -> transformType3D = TransformType3D.NONE
      }
    }
  }

  /**
   * Sets domain rotation type for 3D Noise and 3D DomainWarp.
   * Can aid in reducing directional artifacts when sampling a 2D plane in 3D
   *
   * Default: None
   */
  fun setRotationType3D(rotationType3D: RotationType3D) {
    this.rotationType3D = rotationType3D
    UpdateTransformType3D()
    UpdateWarpTransformType3D()
  }

  private fun UpdateWarpTransformType3D() {
    when (rotationType3D) {
      IMPROVE_XY_PLANES -> warpTransformType3D = TransformType3D.IMPROVE_XY_PLANES
      IMPROVE_XZ_PLANES -> warpTransformType3D = TransformType3D.IMPROVE_XZ_PLANES
      else -> when (domainWarpType) {
        DomainWarpType.OPEN_SIMPLEX_2, OPEN_SIMPLEX_2_REDUCED -> warpTransformType3D = DEFAULT_OPEN_SIMPLEX_2
        else -> warpTransformType3D = TransformType3D.NONE
      }
    }
  }

  /**
   * Sets method for combining octaves in all fractal noise types
   *
   * Default: None
   * Note: FractalType.DomainWarp... only affects DomainWarp(...)
   */
  fun setFractalType(fractalType: FractalType) {
    this.fractalType = fractalType
  }

  /**
   * Sets octave count for all fractal noise types
   *
   * Default: 3
   */
  fun setFractalOctaves(octaves: Int) {
    this.octaves = octaves
    CalculateFractalBounding()
  }

  private fun CalculateFractalBounding() {
    val gain: Float = FastAbs(gain)
    var amp = gain
    var ampFractal = 1.0f
    for (i in 1 until octaves) {
      ampFractal += amp
      amp *= gain
    }
    fractalBounding = 1 / ampFractal
  }

  /**
   * Sets octave lacunarity for all fractal noise types
   *
   * Default: 2.0
   */
  fun setFractalLacunarity(lacunarity: Float) {
    this.lacunarity = lacunarity
  }

  /**
   * Sets octave gain for all fractal noise types
   *
   * Default: 0.5
   */
  fun setFractalGain(gain: Float) {
    this.gain = gain
    CalculateFractalBounding()
  }

  /**
   * Sets octave weighting for all none DomainWarp fratal types
   *
   * Default: 0.0
   * Note: Keep between 0...1 to maintain -1...1 output bounding
   */
  fun setFractalWeightedStrength(weightedStrength: Float) {
    this.weightedStrength = weightedStrength
  }

  /**
   * Sets strength of the fractal ping pong effect
   *
   * Default: 2.0
   */
  fun setFractalPingPongStrength(pingPongStrength: Float) {
    pingPongStength = pingPongStrength
  }

  /**
   * Sets distance function used in cellular noise calculations
   *
   * Default: Distance
   */
  fun setCellularDistanceFunction(cellularDistanceFunction: CellularDistanceFunction) {
    this.cellularDistanceFunction = cellularDistanceFunction
  }

  /**
   * Sets return type from cellular noise calculations
   *
   * Default: EuclideanSq
   */
  fun setCellularReturnType(cellularReturnType: CellularReturnType) {
    this.cellularReturnType = cellularReturnType
  }

  /**
   * Sets the maximum distance a cellular point can move from it's grid position
   *
   * Default: 1.0
   * Note: Setting this higher than 1 will cause artifacts
   */
  fun setCellularJitter(cellularJitter: Float) {
    cellularJitterModifier = cellularJitter
  }

  /**
   * Sets the warp algorithm when using DomainWarp(...)
   *
   * Default: OpenSimplex2
   */
  fun setDomainWarpType(domainWarpType: DomainWarpType) {
    this.domainWarpType = domainWarpType
    UpdateWarpTransformType3D()
  }

  /**
   * Sets the maximum warp distance from original position when using DomainWarp(...)
   *
   * Default: 1.0
   */
  fun setDomainWarpAmp(domainWarpAmp: Float) {
    this.domainWarpAmp = domainWarpAmp
  }

  /**
   * 2D noise at given position using current settings
   *
   * Noise output bounded between -1...1
   */
  fun getNoise(
    x: Float,
    y: Float
  ): Float {
    var x = x
    var y = y
    x *= frequency
    y *= frequency
    when (noiseType) {
      OPEN_SIMPLEX_2, OPEN_SIMPLEX_2_S -> {
        val SQRT3 = 1.7320508075688772935274463415059.toFloat()
        val F2 = 0.5f * (SQRT3 - 1)

        val t = (x + y) * F2
        x += t
        y += t
      }

      else -> {
      }
    }
    return when (fractalType) {
      FBM -> GenFractalFBm(x, y)
      RIDGED -> GenFractalRidged(x, y)
      PING_PONG -> GenFractalPingPong(x, y)
      else -> GenNoiseSingle(seed, x, y)
    }
  }

  /**
   * 3D noise at given position using current settings
   *
   * Noise output bounded between -1...1
   */
  fun getNoise(
    x: Float,
    y: Float,
    z: Float
  ): Float {
    var x = x
    var y = y
    var z = z
    x *= frequency
    y *= frequency
    z *= frequency
    when (transformType3D) {
      TransformType3D.IMPROVE_XY_PLANES -> {
        val xy = x + y

        val s2 = xy * (-0.211324865405187).toFloat()
        z *= 0.577350269189626.toFloat()
        x += s2 - z
        y = y + s2 - z
        z += xy * 0.577350269189626.toFloat()
      }

      TransformType3D.IMPROVE_XZ_PLANES -> {
        val xz = x + z

        val s2 = xz * (-0.211324865405187).toFloat()
        y *= 0.577350269189626.toFloat()
        x += s2 - y
        z += s2 - y
        y += xz * 0.577350269189626.toFloat()
      }

      DEFAULT_OPEN_SIMPLEX_2 -> {
        val R3 = (2.0 / 3.0).toFloat()

        val r = (x + y + z) * R3 // Rotation, not skew
        x = r - x
        y = r - y
        z = r - z
      }

      else -> {
      }
    }
    return when (fractalType) {
      FBM -> GenFractalFBm(x, y, z)
      RIDGED -> GenFractalRidged(x, y, z)
      PING_PONG -> GenFractalPingPong(x, y, z)
      else -> GenNoiseSingle(seed, x, y, z)
    }
  }

  /**
   * 2D warps the input position using current domain warp settings
   *
   * Example usage with GetNoise
   * / **DomainWarp(coord)
   * noise = GetNoise(x, y)
   */
  fun DomainWarp(coord: Vector2FNL) {
    when (fractalType) {
      DOMAIN_WARP_PROGRESSIVE -> DomainWarpFractalProgressive(coord)
      DOMAIN_WARP_INDEPENDENT -> DomainWarpFractalIndependent(coord)
      else -> DomainWarpSingle(coord)
    }
  }

  /**
   * 3D warps the input position using current domain warp settings
   *
   * Example usage with GetNoise
   * / **DomainWarp(coord)
   * noise = GetNoise(x, y, z)
   */
  fun DomainWarp(coord: Vector3FNL) {
    when (fractalType) {
      DOMAIN_WARP_PROGRESSIVE -> DomainWarpFractalProgressive(coord)
      DOMAIN_WARP_INDEPENDENT -> DomainWarpFractalIndependent(coord)
      else -> DomainWarpSingle(coord)
    }
  }

  private fun GenNoiseSingle(
    seed: Int,
    x: Float,
    y: Float
  ): Float {
    return when (noiseType) {
      OPEN_SIMPLEX_2 -> SingleSimplex(seed, x, y)
      OPEN_SIMPLEX_2_S -> SingleOpenSimplex2S(seed, x, y)
      CELLULAR -> SingleCellular(seed, x, y)
      PERLIN -> SinglePerlin(seed, x, y)
      VALUE_CUBIC -> SingleValueCubic(seed, x, y)
      VALUE -> SingleValue(seed, x, y)
    }
  }

  private fun GenNoiseSingle(
    seed: Int,
    x: Float,
    y: Float,
    z: Float
  ): Float {
    return when (noiseType) {
      OPEN_SIMPLEX_2 -> SingleOpenSimplex2(seed, x, y, z)
      OPEN_SIMPLEX_2_S -> SingleOpenSimplex2S(seed, x, y, z)
      CELLULAR -> SingleCellular(seed, x, y, z)
      PERLIN -> SinglePerlin(seed, x, y, z)
      VALUE_CUBIC -> SingleValueCubic(seed, x, y, z)
      VALUE -> SingleValue(seed, x, y, z)
    }
  }

  private fun GenFractalFBm(
    x: Float,
    y: Float
  ): Float {
    var x = x
    var y = y
    var seed = seed
    var sum = 0f
    var amp = fractalBounding
    for (i in 0 until octaves) {
      val noise = GenNoiseSingle(seed++, x, y)
      sum += noise * amp
      amp *= Lerp(1.0f, FastMin(noise + 1, 2f) * 0.5f, weightedStrength)
      x *= lacunarity
      y *= lacunarity
      amp *= gain
    }
    return sum
  }

  private fun GenFractalFBm(
    x: Float,
    y: Float,
    z: Float
  ): Float {
    var x = x
    var y = y
    var z = z
    var seed = seed
    var sum = 0f
    var amp = fractalBounding
    for (i in 0 until octaves) {
      val noise = GenNoiseSingle(seed++, x, y, z)
      sum += noise * amp
      amp *= Lerp(1.0f, (noise + 1) * 0.5f, weightedStrength)
      x *= lacunarity
      y *= lacunarity
      z *= lacunarity
      amp *= gain
    }
    return sum
  }

  private fun GenFractalRidged(
    x: Float,
    y: Float
  ): Float {
    var x = x
    var y = y
    var seed = seed
    var sum = 0f
    var amp = fractalBounding
    for (i in 0 until octaves) {
      val noise: Float = FastAbs(GenNoiseSingle(seed++, x, y))
      sum += (noise * -2 + 1) * amp
      amp *= Lerp(1.0f, 1 - noise, weightedStrength)
      x *= lacunarity
      y *= lacunarity
      amp *= gain
    }
    return sum
  }

  // Generic noise gen
  private fun GenFractalRidged(
    x: Float,
    y: Float,
    z: Float
  ): Float {
    var x = x
    var y = y
    var z = z
    var seed = seed
    var sum = 0f
    var amp = fractalBounding
    for (i in 0 until octaves) {
      val noise: Float = FastAbs(GenNoiseSingle(seed++, x, y, z))
      sum += (noise * -2 + 1) * amp
      amp *= Lerp(1.0f, 1 - noise, weightedStrength)
      x *= lacunarity
      y *= lacunarity
      z *= lacunarity
      amp *= gain
    }
    return sum
  }

  private fun GenFractalPingPong(
    x: Float,
    y: Float
  ): Float {
    var x = x
    var y = y
    var seed = seed
    var sum = 0f
    var amp = fractalBounding
    for (i in 0 until octaves) {
      val noise: Float = PingPong((GenNoiseSingle(seed++, x, y) + 1) * pingPongStength)
      sum += (noise - 0.5f) * 2 * amp
      amp *= Lerp(1.0f, noise, weightedStrength)
      x *= lacunarity
      y *= lacunarity
      amp *= gain
    }
    return sum
  }

  // Noise Coordinate Transforms (frequency, and possible skew or rotation)
  private fun GenFractalPingPong(
    x: Float,
    y: Float,
    z: Float
  ): Float {
    var x = x
    var y = y
    var z = z
    var seed = seed
    var sum = 0f
    var amp = fractalBounding
    for (i in 0 until octaves) {
      val noise: Float = PingPong((GenNoiseSingle(seed++, x, y, z) + 1) * pingPongStength)
      sum += (noise - 0.5f) * 2 * amp
      amp *= Lerp(1.0f, noise, weightedStrength)
      x *= lacunarity
      y *= lacunarity
      z *= lacunarity
      amp *= gain
    }
    return sum
  }

  private fun SingleSimplex(
    seed: Int,
    x: Float,
    y: Float
  ): Float {
    // 2D OpenSimplex2 case uses the same algorithm as ordinary Simplex.
    val SQRT3 = 1.7320508075688772935274463415059f
    val G2 = (3 - SQRT3) / 6

    /*
     * --- Skew moved to switch statements before fractal evaluation ---
     * final FNLfloat F2 = 0.5f * (SQRT3 - 1);
     * FNLfloat s = (x + y) * F2;
     * x += s; y += s;
     */
    var i: Int = FastFloor(x)
    var j: Int = FastFloor(y)
    val xi = x - i
    val yi = y - j
    val t = (xi + yi) * G2
    val x0 = xi - t
    val y0 = yi - t
    i *= PRIME_X
    j *= PRIME_Y
    val n0: Float
    val n1: Float
    val n2: Float
    val a = 0.5f - x0 * x0 - y0 * y0
    if (a <= 0) {
      n0 = 0f
    } else {
      n0 = a * a * (a * a) * GradCoord(seed, i, j, x0, y0)
    }
    val c = 2 * (1 - 2 * G2) * (1 / G2 - 2) * t + (-2 * (1 - 2 * G2) * (1 - 2 * G2) + a)
    if (c <= 0) {
      n2 = 0f
    } else {
      val x2 = x0 + (2 * G2 - 1)
      val y2 = y0 + (2 * G2 - 1)
      n2 = c * c * (c * c) * GradCoord(seed, i + PRIME_X, j + PRIME_Y, x2, y2)
    }
    if (y0 > x0) {
      val x1 = x0 + G2
      val y1 = y0 + (G2 - 1)
      val b = 0.5f - x1 * x1 - y1 * y1
      if (b <= 0) {
        n1 = 0f
      } else {
        n1 = b * b * (b * b) * GradCoord(seed, i, j + PRIME_Y, x1, y1)
      }
    } else {
      val x1 = x0 + (G2 - 1)
      val y1 = y0 + G2
      val b = 0.5f - x1 * x1 - y1 * y1
      if (b <= 0) {
        n1 = 0f
      } else {
        n1 = b * b * (b * b) * GradCoord(seed, i + PRIME_X, j, x1, y1)
      }
    }
    return (n0 + n1 + n2) * 99.83685446303647f
  }

  // Fractal FBm
  private fun SingleOpenSimplex2(
    seed: Int,
    x: Float,
    y: Float,
    z: Float
  ): Float {
    // 3D OpenSimplex2 case uses two offset rotated cube grids.

    /*
     * --- Rotation moved to switch statements before fractal evaluation ---
     * final FNLfloat R3 = (FNLfloat)(2.0 / 3.0);
     * FNLfloat r = (x + y + z) * R3; // Rotation, not skew
     * x = r - x; y = r - y; z = r - z;
     */
    var seed = seed
    var i: Int = FastRound(x)
    var j: Int = FastRound(y)
    var k: Int = FastRound(z)
    var x0 = x - i
    var y0 = y - j
    var z0 = z - k
    var xNSign = (-1.0f - x0).toInt() or 1
    var yNSign = (-1.0f - y0).toInt() or 1
    var zNSign = (-1.0f - z0).toInt() or 1
    var ax0 = xNSign * -x0
    var ay0 = yNSign * -y0
    var az0 = zNSign * -z0
    i *= PRIME_X
    j *= PRIME_Y
    k *= PRIME_Z
    var value = 0f
    var a = 0.6f - x0 * x0 - (y0 * y0 + z0 * z0)
    var l = 0
    while (true) {
      if (a > 0) {
        value += a * a * (a * a) * GradCoord(seed, i, j, k, x0, y0, z0)
      }
      if (ax0 >= ay0 && ax0 >= az0) {
        var b = a + ax0 + ax0
        if (b > 1) {
          b -= 1f
          value += b * b * (b * b) * GradCoord(seed, i - xNSign * PRIME_X, j, k, x0 + xNSign, y0, z0)
        }
      } else if (ay0 > ax0 && ay0 >= az0) {
        var b = a + ay0 + ay0
        if (b > 1) {
          b -= 1f
          value += b * b * (b * b) * GradCoord(seed, i, j - yNSign * PRIME_Y, k, x0, y0 + yNSign, z0)
        }
      } else {
        var b = a + az0 + az0
        if (b > 1) {
          b -= 1f
          value += b * b * (b * b) * GradCoord(seed, i, j, k - zNSign * PRIME_Z, x0, y0, z0 + zNSign)
        }
      }
      if (l == 1) {
        break
      }
      ax0 = 0.5f - ax0
      ay0 = 0.5f - ay0
      az0 = 0.5f - az0
      x0 = xNSign * ax0
      y0 = yNSign * ay0
      z0 = zNSign * az0
      a += 0.75f - ax0 - (ay0 + az0)
      i += xNSign shr 1 and PRIME_X
      j += yNSign shr 1 and PRIME_Y
      k += zNSign shr 1 and PRIME_Z
      xNSign = -xNSign
      yNSign = -yNSign
      zNSign = -zNSign
      seed = seed.inv()
      l++
    }
    return value * 32.69428253173828125f
  }

  private fun SingleOpenSimplex2S(
    seed: Int,
    x: Float,
    y: Float
  ): Float {
    // 2D OpenSimplex2S case is a modified 2D simplex noise.
    val SQRT3 = 1.7320508075688772935274463415059f
    val G2 = (3 - SQRT3) / 6

    /*
     * --- Skew moved to TransformNoiseCoordinate method ---
     * final FNLfloat F2 = 0.5f * (SQRT3 - 1);
     * FNLfloat s = (x + y) * F2;
     * x += s; y += s;
     */
    var i: Int = FastFloor(x)
    var j: Int = FastFloor(y)
    val xi = x - i
    val yi = y - j
    i *= PRIME_X
    j *= PRIME_Y
    val i1: Int = i + PRIME_X
    val j1: Int = j + PRIME_Y
    val t = (xi + yi) * G2
    val x0 = xi - t
    val y0 = yi - t
    val a0 = 2.0f / 3.0f - x0 * x0 - y0 * y0
    var value: Float = a0 * a0 * (a0 * a0) * GradCoord(seed, i, j, x0, y0)
    val a1 = 2 * (1 - 2 * G2) * (1 / G2 - 2) * t + (-2 * (1 - 2 * G2) * (1 - 2 * G2) + a0)
    val x1 = x0 - (1 - 2 * G2)
    val y1 = y0 - (1 - 2 * G2)
    value += a1 * a1 * (a1 * a1) * GradCoord(seed, i1, j1, x1, y1)

    // Nested conditionals were faster than compact bit logic/arithmetic.
    val xmyi = xi - yi
    if (t > G2) {
      if (xi + xmyi > 1) {
        val x2 = x0 + (3 * G2 - 2)
        val y2 = y0 + (3 * G2 - 1)
        val a2 = 2.0f / 3.0f - x2 * x2 - y2 * y2
        if (a2 > 0) {
          value += a2 * a2 * (a2 * a2) * GradCoord(seed, i + (PRIME_X shl 1), j + PRIME_Y, x2, y2)
        }
      } else {
        val x2 = x0 + G2
        val y2 = y0 + (G2 - 1)
        val a2 = 2.0f / 3.0f - x2 * x2 - y2 * y2
        if (a2 > 0) {
          value += a2 * a2 * (a2 * a2) * GradCoord(seed, i, j + PRIME_Y, x2, y2)
        }
      }
      if (yi - xmyi > 1) {
        val x3 = x0 + (3 * G2 - 1)
        val y3 = y0 + (3 * G2 - 2)
        val a3 = 2.0f / 3.0f - x3 * x3 - y3 * y3
        if (a3 > 0) {
          value += a3 * a3 * (a3 * a3) * GradCoord(seed, i + PRIME_X, j + (PRIME_Y shl 1), x3, y3)
        }
      } else {
        val x3 = x0 + (G2 - 1)
        val y3 = y0 + G2
        val a3 = 2.0f / 3.0f - x3 * x3 - y3 * y3
        if (a3 > 0) {
          value += a3 * a3 * (a3 * a3) * GradCoord(seed, i + PRIME_X, j, x3, y3)
        }
      }
    } else {
      if (xi + xmyi < 0) {
        val x2 = x0 + (1 - G2)
        val y2 = y0 - G2
        val a2 = 2.0f / 3.0f - x2 * x2 - y2 * y2
        if (a2 > 0) {
          value += a2 * a2 * (a2 * a2) * GradCoord(seed, i - PRIME_X, j, x2, y2)
        }
      } else {
        val x2 = x0 + (G2 - 1)
        val y2 = y0 + G2
        val a2 = 2.0f / 3.0f - x2 * x2 - y2 * y2
        if (a2 > 0) {
          value += a2 * a2 * (a2 * a2) * GradCoord(seed, i + PRIME_X, j, x2, y2)
        }
      }
      if (yi < xmyi) {
        val x2 = x0 - G2
        val y2 = y0 - (G2 - 1)
        val a2 = 2.0f / 3.0f - x2 * x2 - y2 * y2
        if (a2 > 0) {
          value += a2 * a2 * (a2 * a2) * GradCoord(seed, i, j - PRIME_Y, x2, y2)
        }
      } else {
        val x2 = x0 + G2
        val y2 = y0 + (G2 - 1)
        val a2 = 2.0f / 3.0f - x2 * x2 - y2 * y2
        if (a2 > 0) {
          value += a2 * a2 * (a2 * a2) * GradCoord(seed, i, j + PRIME_Y, x2, y2)
        }
      }
    }
    return value * 18.24196194486065f
  }

  // Fractal Ridged
  private fun SingleOpenSimplex2S(
    seed: Int,
    x: Float,
    y: Float,
    z: Float
  ): Float {
    // 3D OpenSimplex2S case uses two offset rotated cube grids.

    /*
     * --- Rotation moved to TransformNoiseCoordinate method ---
     * final FNLfloat R3 = (FNLfloat)(2.0 / 3.0);
     * FNLfloat r = (x + y + z) * R3; // Rotation, not skew
     * x = r - x; y = r - y; z = r - z;
     */
    var i: Int = FastFloor(x)
    var j: Int = FastFloor(y)
    var k: Int = FastFloor(z)
    val xi = x - i
    val yi = y - j
    val zi = z - k
    i *= PRIME_X
    j *= PRIME_Y
    k *= PRIME_Z
    val seed2 = seed + 1293373
    val xNMask = (-0.5f - xi).toInt()
    val yNMask = (-0.5f - yi).toInt()
    val zNMask = (-0.5f - zi).toInt()
    val x0 = xi + xNMask
    val y0 = yi + yNMask
    val z0 = zi + zNMask
    val a0 = 0.75f - x0 * x0 - y0 * y0 - z0 * z0
    var value: Float = a0 * a0 * (a0 * a0) * GradCoord(
      seed,
      i + (xNMask and PRIME_X),
      j + (yNMask and PRIME_Y),
      k + (zNMask and PRIME_Z),
      x0,
      y0,
      z0
    )
    val x1 = xi - 0.5f
    val y1 = yi - 0.5f
    val z1 = zi - 0.5f
    val a1 = 0.75f - x1 * x1 - y1 * y1 - z1 * z1
    value += a1 * a1 * (a1 * a1) * GradCoord(
      seed2,
      i + PRIME_X,
      j + PRIME_Y,
      k + PRIME_Z,
      x1,
      y1,
      z1
    )
    val xAFlipMask0 = (xNMask or 1 shl 1) * x1
    val yAFlipMask0 = (yNMask or 1 shl 1) * y1
    val zAFlipMask0 = (zNMask or 1 shl 1) * z1
    val xAFlipMask1 = (-2 - (xNMask shl 2)) * x1 - 1.0f
    val yAFlipMask1 = (-2 - (yNMask shl 2)) * y1 - 1.0f
    val zAFlipMask1 = (-2 - (zNMask shl 2)) * z1 - 1.0f
    var skip5 = false
    val a2 = xAFlipMask0 + a0
    if (a2 > 0) {
      val x2 = x0 - (xNMask or 1)
      value += a2 * a2 * (a2 * a2) * GradCoord(
        seed,
        i + (xNMask.inv() and PRIME_X),
        j + (yNMask and PRIME_Y),
        k + (zNMask and PRIME_Z),
        x2,
        y0,
        z0
      )
    } else {
      val a3 = yAFlipMask0 + zAFlipMask0 + a0
      if (a3 > 0) {
        val y3 = y0 - (yNMask or 1)
        val z3 = z0 - (zNMask or 1)
        value += a3 * a3 * (a3 * a3) * GradCoord(
          seed,
          i + (xNMask and PRIME_X),
          j + (yNMask.inv() and PRIME_Y),
          k + (zNMask.inv() and PRIME_Z),
          x0,
          y3,
          z3
        )
      }
      val a4 = xAFlipMask1 + a1
      if (a4 > 0) {
        val x4 = (xNMask or 1) + x1
        value += a4 * a4 * (a4 * a4) * GradCoord(
          seed2,
          i + (xNMask and PRIME_X * 2),
          j + PRIME_Y,
          k + PRIME_Z,
          x4,
          y1,
          z1
        )
        skip5 = true
      }
    }
    var skip9 = false
    val a6 = yAFlipMask0 + a0
    if (a6 > 0) {
      val y6 = y0 - (yNMask or 1)
      value += a6 * a6 * (a6 * a6) * GradCoord(
        seed,
        i + (xNMask and PRIME_X),
        j + (yNMask.inv() and PRIME_Y),
        k + (zNMask and PRIME_Z),
        x0,
        y6,
        z0
      )
    } else {
      val a7 = xAFlipMask0 + zAFlipMask0 + a0
      if (a7 > 0) {
        val x7 = x0 - (xNMask or 1)
        val z7 = z0 - (zNMask or 1)
        value += a7 * a7 * (a7 * a7) * GradCoord(
          seed,
          i + (xNMask.inv() and PRIME_X),
          j + (yNMask and PRIME_Y),
          k + (zNMask.inv() and PRIME_Z),
          x7,
          y0,
          z7
        )
      }
      val a8 = yAFlipMask1 + a1
      if (a8 > 0) {
        val y8 = (yNMask or 1) + y1
        value += a8 * a8 * (a8 * a8) * GradCoord(
          seed2,
          i + PRIME_X,
          j + (yNMask and (PRIME_Y shl 1)),
          k + PRIME_Z,
          x1,
          y8,
          z1
        )
        skip9 = true
      }
    }
    var skipD = false
    val aA = zAFlipMask0 + a0
    if (aA > 0) {
      val zA = z0 - (zNMask or 1)
      value += aA * aA * (aA * aA) * GradCoord(
        seed,
        i + (xNMask and PRIME_X),
        j + (yNMask and PRIME_Y),
        k + (zNMask.inv() and PRIME_Z),
        x0,
        y0,
        zA
      )
    } else {
      val aB = xAFlipMask0 + yAFlipMask0 + a0
      if (aB > 0) {
        val xB = x0 - (xNMask or 1)
        val yB = y0 - (yNMask or 1)
        value += aB * aB * (aB * aB) * GradCoord(
          seed,
          i + (xNMask.inv() and PRIME_X),
          j + (yNMask.inv() and PRIME_Y),
          k + (zNMask and PRIME_Z),
          xB,
          yB,
          z0
        )
      }
      val aC = zAFlipMask1 + a1
      if (aC > 0) {
        val zC = (zNMask or 1) + z1
        value += aC * aC * (aC * aC) * GradCoord(
          seed2,
          i + PRIME_X,
          j + PRIME_Y,
          k + (zNMask and (PRIME_Z shl 1)),
          x1,
          y1,
          zC
        )
        skipD = true
      }
    }
    if (!skip5) {
      val a5 = yAFlipMask1 + zAFlipMask1 + a1
      if (a5 > 0) {
        val y5 = (yNMask or 1) + y1
        val z5 = (zNMask or 1) + z1
        value += a5 * a5 * (a5 * a5) * GradCoord(
          seed2,
          i + PRIME_X,
          j + (yNMask and (PRIME_Y shl 1)),
          k + (zNMask and (PRIME_Z shl 1)),
          x1,
          y5,
          z5
        )
      }
    }
    if (!skip9) {
      val a9 = xAFlipMask1 + zAFlipMask1 + a1
      if (a9 > 0) {
        val x9 = (xNMask or 1) + x1
        val z9 = (zNMask or 1) + z1
        value += a9 * a9 * (a9 * a9) * GradCoord(
          seed2,
          i + (xNMask and PRIME_X * 2),
          j + PRIME_Y,
          k + (zNMask and (PRIME_Z shl 1)),
          x9,
          y1,
          z9
        )
      }
    }
    if (!skipD) {
      val aD = xAFlipMask1 + yAFlipMask1 + a1
      if (aD > 0) {
        val xD = (xNMask or 1) + x1
        val yD = (yNMask or 1) + y1
        value += aD * aD * (aD * aD) * GradCoord(
          seed2,
          i + (xNMask and (PRIME_X shl 1)),
          j + (yNMask and (PRIME_Y shl 1)),
          k + PRIME_Z,
          xD,
          yD,
          z1
        )
      }
    }
    return value * 9.046026385208288f
  }

  private fun SingleCellular(
    seed: Int,
    x: Float,
    y: Float
  ): Float {
    val xr: Int = FastRound(x)
    val yr: Int = FastRound(y)
    var distance0 = Float.MAX_VALUE
    var distance1 = Float.MAX_VALUE
    var closestHash = 0
    val cellularJitter = 0.43701595f * cellularJitterModifier
    var xPrimed: Int = (xr - 1) * PRIME_X
    val yPrimedBase: Int = (yr - 1) * PRIME_Y
    when (cellularDistanceFunction) {
      EUCLIDEAN, EUCLIDEAN_SQ -> {
        var xi = xr - 1
        while (xi <= xr + 1) {
          var yPrimed = yPrimedBase
          var yi = yr - 1
          while (yi <= yr + 1) {
            val hash: Int = Hash(seed, xPrimed, yPrimed)
            val idx = hash and (255 shl 1)
            val vecX: Float = xi - x + RandVecs2D[idx] * cellularJitter
            val vecY: Float = yi - y + RandVecs2D[idx or 1] * cellularJitter
            val newDistance = vecX * vecX + vecY * vecY
            distance1 = FastMax(FastMin(distance1, newDistance), distance0)
            if (newDistance < distance0) {
              distance0 = newDistance
              closestHash = hash
            }
            yPrimed += PRIME_Y
            yi++
          }
          xPrimed += PRIME_X
          xi++
        }
      }

      MANHATTAN -> {
        var xi = xr - 1
        while (xi <= xr + 1) {
          var yPrimed = yPrimedBase
          var yi = yr - 1
          while (yi <= yr + 1) {
            val hash: Int = Hash(seed, xPrimed, yPrimed)
            val idx = hash and (255 shl 1)
            val vecX: Float = xi - x + RandVecs2D[idx] * cellularJitter
            val vecY: Float = yi - y + RandVecs2D[idx or 1] * cellularJitter
            val newDistance: Float = FastAbs(vecX) + FastAbs(vecY)
            distance1 = FastMax(FastMin(distance1, newDistance), distance0)
            if (newDistance < distance0) {
              distance0 = newDistance
              closestHash = hash
            }
            yPrimed += PRIME_Y
            yi++
          }
          xPrimed += PRIME_X
          xi++
        }
      }

      HYBRID -> {
        var xi = xr - 1
        while (xi <= xr + 1) {
          var yPrimed = yPrimedBase
          var yi = yr - 1
          while (yi <= yr + 1) {
            val hash: Int = Hash(seed, xPrimed, yPrimed)
            val idx = hash and (255 shl 1)
            val vecX: Float = xi - x + RandVecs2D[idx] * cellularJitter
            val vecY: Float = yi - y + RandVecs2D[idx or 1] * cellularJitter
            val newDistance: Float = FastAbs(vecX) + FastAbs(vecY) + (vecX * vecX + vecY * vecY)
            distance1 = FastMax(FastMin(distance1, newDistance), distance0)
            if (newDistance < distance0) {
              distance0 = newDistance
              closestHash = hash
            }
            yPrimed += PRIME_Y
            yi++
          }
          xPrimed += PRIME_X
          xi++
        }
      }

      else -> {
        var xi = xr - 1
        while (xi <= xr + 1) {
          var yPrimed = yPrimedBase
          var yi = yr - 1
          while (yi <= yr + 1) {
            val hash: Int = Hash(seed, xPrimed, yPrimed)
            val idx = hash and (255 shl 1)
            val vecX: Float = xi - x + RandVecs2D[idx] * cellularJitter
            val vecY: Float = yi - y + RandVecs2D[idx or 1] * cellularJitter
            val newDistance = vecX * vecX + vecY * vecY
            distance1 = FastMax(FastMin(distance1, newDistance), distance0)
            if (newDistance < distance0) {
              distance0 = newDistance
              closestHash = hash
            }
            yPrimed += PRIME_Y
            yi++
          }
          xPrimed += PRIME_X
          xi++
        }
      }
    }
    if (cellularDistanceFunction === EUCLIDEAN && cellularReturnType !== CELL_VALUE) {
      distance0 = FastSqrt(distance0)
      if (cellularReturnType !== CELL_VALUE) {
        distance1 = FastSqrt(distance1)
      }
    }
    return when (cellularReturnType) {
      CELL_VALUE -> closestHash * (1 / 2147483648.0f)
      DISTANCE -> distance0 - 1
      DISTANCE_2 -> distance1 - 1
      DISTANCE_2_ADD -> (distance1 + distance0) * 0.5f - 1
      DISTANCE_2_SUB -> distance1 - distance0 - 1
      DISTANCE_2_MUL -> distance1 * distance0 * 0.5f - 1
      DISTANCE_2_DIV -> distance0 / distance1 - 1
    }
  }

  // Fractal PingPong
  private fun SingleCellular(
    seed: Int,
    x: Float,
    y: Float,
    z: Float
  ): Float {
    val xr: Int = FastRound(x)
    val yr: Int = FastRound(y)
    val zr: Int = FastRound(z)
    var distance0 = Float.MAX_VALUE
    var distance1 = Float.MAX_VALUE
    var closestHash = 0
    val cellularJitter = 0.39614353f * cellularJitterModifier
    var xPrimed: Int = (xr - 1) * PRIME_X
    val yPrimedBase: Int = (yr - 1) * PRIME_Y
    val zPrimedBase: Int = (zr - 1) * PRIME_Z
    when (cellularDistanceFunction) {
      EUCLIDEAN, EUCLIDEAN_SQ -> {
        var xi = xr - 1
        while (xi <= xr + 1) {
          var yPrimed = yPrimedBase
          var yi = yr - 1
          while (yi <= yr + 1) {
            var zPrimed = zPrimedBase
            var zi = zr - 1
            while (zi <= zr + 1) {
              val hash: Int = Hash(seed, xPrimed, yPrimed, zPrimed)
              val idx = hash and (255 shl 2)
              val vecX: Float = xi - x + RandVecs3D[idx] * cellularJitter
              val vecY: Float = yi - y + RandVecs3D[idx or 1] * cellularJitter
              val vecZ: Float = zi - z + RandVecs3D[idx or 2] * cellularJitter
              val newDistance = vecX * vecX + vecY * vecY + vecZ * vecZ
              distance1 = FastMax(FastMin(distance1, newDistance), distance0)
              if (newDistance < distance0) {
                distance0 = newDistance
                closestHash = hash
              }
              zPrimed += PRIME_Z
              zi++
            }
            yPrimed += PRIME_Y
            yi++
          }
          xPrimed += PRIME_X
          xi++
        }
      }

      MANHATTAN -> {
        var xi = xr - 1
        while (xi <= xr + 1) {
          var yPrimed = yPrimedBase
          var yi = yr - 1
          while (yi <= yr + 1) {
            var zPrimed = zPrimedBase
            var zi = zr - 1
            while (zi <= zr + 1) {
              val hash: Int = Hash(seed, xPrimed, yPrimed, zPrimed)
              val idx = hash and (255 shl 2)
              val vecX: Float = xi - x + RandVecs3D[idx] * cellularJitter
              val vecY: Float = yi - y + RandVecs3D[idx or 1] * cellularJitter
              val vecZ: Float = zi - z + RandVecs3D[idx or 2] * cellularJitter
              val newDistance: Float = FastAbs(vecX) + FastAbs(vecY) + FastAbs(vecZ)
              distance1 = FastMax(FastMin(distance1, newDistance), distance0)
              if (newDistance < distance0) {
                distance0 = newDistance
                closestHash = hash
              }
              zPrimed += PRIME_Z
              zi++
            }
            yPrimed += PRIME_Y
            yi++
          }
          xPrimed += PRIME_X
          xi++
        }
      }

      HYBRID -> {
        var xi = xr - 1
        while (xi <= xr + 1) {
          var yPrimed = yPrimedBase
          var yi = yr - 1
          while (yi <= yr + 1) {
            var zPrimed = zPrimedBase
            var zi = zr - 1
            while (zi <= zr + 1) {
              val hash: Int = Hash(seed, xPrimed, yPrimed, zPrimed)
              val idx = hash and (255 shl 2)
              val vecX: Float = xi - x + RandVecs3D[idx] * cellularJitter
              val vecY: Float = yi - y + RandVecs3D[idx or 1] * cellularJitter
              val vecZ: Float = zi - z + RandVecs3D[idx or 2] * cellularJitter
              val newDistance: Float =
                FastAbs(vecX) + FastAbs(vecY) + FastAbs(vecZ) + (vecX * vecX + vecY * vecY + vecZ * vecZ)
              distance1 = FastMax(FastMin(distance1, newDistance), distance0)
              if (newDistance < distance0) {
                distance0 = newDistance
                closestHash = hash
              }
              zPrimed += PRIME_Z
              zi++
            }
            yPrimed += PRIME_Y
            yi++
          }
          xPrimed += PRIME_X
          xi++
        }
      }
    }
    if (cellularDistanceFunction === EUCLIDEAN && cellularReturnType !== CELL_VALUE) {
      distance0 = FastSqrt(distance0)
      if (cellularReturnType !== CELL_VALUE) {
        distance1 = FastSqrt(distance1)
      }
    }
    return when (cellularReturnType) {
      CELL_VALUE -> closestHash * (1 / 2147483648.0f)
      DISTANCE -> distance0 - 1
      DISTANCE_2 -> distance1 - 1
      DISTANCE_2_ADD -> (distance1 + distance0) * 0.5f - 1
      DISTANCE_2_SUB -> distance1 - distance0 - 1
      DISTANCE_2_MUL -> distance1 * distance0 * 0.5f - 1
      DISTANCE_2_DIV -> distance0 / distance1 - 1
    }
  }

  private fun SinglePerlin(
    seed: Int,
    x: Float,
    y: Float
  ): Float {
    var x0: Int = FastFloor(x)
    var y0: Int = FastFloor(y)
    val xd0 = x - x0
    val yd0 = y - y0
    val xd1 = xd0 - 1
    val yd1 = yd0 - 1
    val xs: Float = InterpQuintic(xd0)
    val ys: Float = InterpQuintic(yd0)
    x0 *= PRIME_X
    y0 *= PRIME_Y
    val x1: Int = x0 + PRIME_X
    val y1: Int = y0 + PRIME_Y
    val xf0: Float = Lerp(GradCoord(seed, x0, y0, xd0, yd0), GradCoord(seed, x1, y0, xd1, yd0), xs)
    val xf1: Float = Lerp(GradCoord(seed, x0, y1, xd0, yd1), GradCoord(seed, x1, y1, xd1, yd1), xs)
    return Lerp(xf0, xf1, ys) * 1.4247691104677813f
  }

  // Simplex/OpenSimplex2 Noise
  private fun SinglePerlin(
    seed: Int,
    x: Float,
    y: Float,
    z: Float
  ): Float {
    var x0: Int = FastFloor(x)
    var y0: Int = FastFloor(y)
    var z0: Int = FastFloor(z)
    val xd0 = x - x0
    val yd0 = y - y0
    val zd0 = z - z0
    val xd1 = xd0 - 1
    val yd1 = yd0 - 1
    val zd1 = zd0 - 1
    val xs: Float = InterpQuintic(xd0)
    val ys: Float = InterpQuintic(yd0)
    val zs: Float = InterpQuintic(zd0)
    x0 *= PRIME_X
    y0 *= PRIME_Y
    z0 *= PRIME_Z
    val x1: Int = x0 + PRIME_X
    val y1: Int = y0 + PRIME_Y
    val z1: Int = z0 + PRIME_Z
    val xf00: Float =
      Lerp(GradCoord(seed, x0, y0, z0, xd0, yd0, zd0), GradCoord(seed, x1, y0, z0, xd1, yd0, zd0), xs)
    val xf10: Float =
      Lerp(GradCoord(seed, x0, y1, z0, xd0, yd1, zd0), GradCoord(seed, x1, y1, z0, xd1, yd1, zd0), xs)
    val xf01: Float =
      Lerp(GradCoord(seed, x0, y0, z1, xd0, yd0, zd1), GradCoord(seed, x1, y0, z1, xd1, yd0, zd1), xs)
    val xf11: Float =
      Lerp(GradCoord(seed, x0, y1, z1, xd0, yd1, zd1), GradCoord(seed, x1, y1, z1, xd1, yd1, zd1), xs)
    val yf0: Float = Lerp(xf00, xf10, ys)
    val yf1: Float = Lerp(xf01, xf11, ys)
    return Lerp(yf0, yf1, zs) * 0.964921414852142333984375f
  }

  private fun SingleValueCubic(
    seed: Int,
    x: Float,
    y: Float
  ): Float {
    var x1: Int = FastFloor(x)
    var y1: Int = FastFloor(y)
    val xs = x - x1
    val ys = y - y1
    x1 *= PRIME_X
    y1 *= PRIME_Y
    val x0: Int = x1 - PRIME_X
    val y0: Int = y1 - PRIME_Y
    val x2: Int = x1 + PRIME_X
    val y2: Int = y1 + PRIME_Y
    val x3: Int = x1 + (PRIME_X shl 1)
    val y3: Int = y1 + (PRIME_Y shl 1)
    return CubicLerp(
      CubicLerp(
        ValCoord(seed, x0, y0),
        ValCoord(seed, x1, y0),
        ValCoord(seed, x2, y0),
        ValCoord(seed, x3, y0),
        xs
      ),
      CubicLerp(
        ValCoord(seed, x0, y1),
        ValCoord(seed, x1, y1),
        ValCoord(seed, x2, y1),
        ValCoord(seed, x3, y1),
        xs
      ),
      CubicLerp(
        ValCoord(seed, x0, y2),
        ValCoord(seed, x1, y2),
        ValCoord(seed, x2, y2),
        ValCoord(seed, x3, y2),
        xs
      ),
      CubicLerp(
        ValCoord(seed, x0, y3),
        ValCoord(seed, x1, y3),
        ValCoord(seed, x2, y3),
        ValCoord(seed, x3, y3),
        xs
      ),
      ys
    ) * (1 / (1.5f * 1.5f))
  }

  // OpenSimplex2S Noise
  private fun SingleValueCubic(
    seed: Int,
    x: Float,
    y: Float,
    z: Float
  ): Float {
    var x1: Int = FastFloor(x)
    var y1: Int = FastFloor(y)
    var z1: Int = FastFloor(z)
    val xs = x - x1
    val ys = y - y1
    val zs = z - z1
    x1 *= PRIME_X
    y1 *= PRIME_Y
    z1 *= PRIME_Z
    val x0: Int = x1 - PRIME_X
    val y0: Int = y1 - PRIME_Y
    val z0: Int = z1 - PRIME_Z
    val x2: Int = x1 + PRIME_X
    val y2: Int = y1 + PRIME_Y
    val z2: Int = z1 + PRIME_Z
    val x3: Int = x1 + (PRIME_X shl 1)
    val y3: Int = y1 + (PRIME_Y shl 1)
    val z3: Int = z1 + (PRIME_Z shl 1)
    return CubicLerp(
      CubicLerp(
        CubicLerp(
          ValCoord(seed, x0, y0, z0),
          ValCoord(seed, x1, y0, z0),
          ValCoord(seed, x2, y0, z0),
          ValCoord(seed, x3, y0, z0),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y1, z0),
          ValCoord(seed, x1, y1, z0),
          ValCoord(seed, x2, y1, z0),
          ValCoord(seed, x3, y1, z0),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y2, z0),
          ValCoord(seed, x1, y2, z0),
          ValCoord(seed, x2, y2, z0),
          ValCoord(seed, x3, y2, z0),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y3, z0),
          ValCoord(seed, x1, y3, z0),
          ValCoord(seed, x2, y3, z0),
          ValCoord(seed, x3, y3, z0),
          xs
        ),
        ys
      ),
      CubicLerp(
        CubicLerp(
          ValCoord(seed, x0, y0, z1),
          ValCoord(seed, x1, y0, z1),
          ValCoord(seed, x2, y0, z1),
          ValCoord(seed, x3, y0, z1),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y1, z1),
          ValCoord(seed, x1, y1, z1),
          ValCoord(seed, x2, y1, z1),
          ValCoord(seed, x3, y1, z1),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y2, z1),
          ValCoord(seed, x1, y2, z1),
          ValCoord(seed, x2, y2, z1),
          ValCoord(seed, x3, y2, z1),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y3, z1),
          ValCoord(seed, x1, y3, z1),
          ValCoord(seed, x2, y3, z1),
          ValCoord(seed, x3, y3, z1),
          xs
        ),
        ys
      ),
      CubicLerp(
        CubicLerp(
          ValCoord(seed, x0, y0, z2),
          ValCoord(seed, x1, y0, z2),
          ValCoord(seed, x2, y0, z2),
          ValCoord(seed, x3, y0, z2),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y1, z2),
          ValCoord(seed, x1, y1, z2),
          ValCoord(seed, x2, y1, z2),
          ValCoord(seed, x3, y1, z2),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y2, z2),
          ValCoord(seed, x1, y2, z2),
          ValCoord(seed, x2, y2, z2),
          ValCoord(seed, x3, y2, z2),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y3, z2),
          ValCoord(seed, x1, y3, z2),
          ValCoord(seed, x2, y3, z2),
          ValCoord(seed, x3, y3, z2),
          xs
        ),
        ys
      ),
      CubicLerp(
        CubicLerp(
          ValCoord(seed, x0, y0, z3),
          ValCoord(seed, x1, y0, z3),
          ValCoord(seed, x2, y0, z3),
          ValCoord(seed, x3, y0, z3),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y1, z3),
          ValCoord(seed, x1, y1, z3),
          ValCoord(seed, x2, y1, z3),
          ValCoord(seed, x3, y1, z3),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y2, z3),
          ValCoord(seed, x1, y2, z3),
          ValCoord(seed, x2, y2, z3),
          ValCoord(seed, x3, y2, z3),
          xs
        ),
        CubicLerp(
          ValCoord(seed, x0, y3, z3),
          ValCoord(seed, x1, y3, z3),
          ValCoord(seed, x2, y3, z3),
          ValCoord(seed, x3, y3, z3),
          xs
        ),
        ys
      ),
      zs
    ) * (1 / (1.5f * 1.5f * 1.5f))
  }

  private fun SingleValue(
    seed: Int,
    x: Float,
    y: Float
  ): Float {
    var x0: Int = FastFloor(x)
    var y0: Int = FastFloor(y)
    val xs: Float = InterpHermite(x - x0)
    val ys: Float = InterpHermite(y - y0)
    x0 *= PRIME_X
    y0 *= PRIME_Y
    val x1: Int = x0 + PRIME_X
    val y1: Int = y0 + PRIME_Y
    val xf0: Float = Lerp(ValCoord(seed, x0, y0), ValCoord(seed, x1, y0), xs)
    val xf1: Float = Lerp(ValCoord(seed, x0, y1), ValCoord(seed, x1, y1), xs)
    return Lerp(xf0, xf1, ys)
  }

  // Cellular Noise
  private fun SingleValue(
    seed: Int,
    x: Float,
    y: Float,
    z: Float
  ): Float {
    var x0: Int = FastFloor(x)
    var y0: Int = FastFloor(y)
    var z0: Int = FastFloor(z)
    val xs: Float = InterpHermite(x - x0)
    val ys: Float = InterpHermite(y - y0)
    val zs: Float = InterpHermite(z - z0)
    x0 *= PRIME_X
    y0 *= PRIME_Y
    z0 *= PRIME_Z
    val x1: Int = x0 + PRIME_X
    val y1: Int = y0 + PRIME_Y
    val z1: Int = z0 + PRIME_Z
    val xf00: Float = Lerp(ValCoord(seed, x0, y0, z0), ValCoord(seed, x1, y0, z0), xs)
    val xf10: Float = Lerp(ValCoord(seed, x0, y1, z0), ValCoord(seed, x1, y1, z0), xs)
    val xf01: Float = Lerp(ValCoord(seed, x0, y0, z1), ValCoord(seed, x1, y0, z1), xs)
    val xf11: Float = Lerp(ValCoord(seed, x0, y1, z1), ValCoord(seed, x1, y1, z1), xs)
    val yf0: Float = Lerp(xf00, xf10, ys)
    val yf1: Float = Lerp(xf01, xf11, ys)
    return Lerp(yf0, yf1, zs)
  }

  private fun DoSingleDomainWarp(
    seed: Int,
    amp: Float,
    freq: Float,
    x: Float,
    y: Float,
    coord: Vector2FNL
  ) {
    when (domainWarpType) {
      DomainWarpType.OPEN_SIMPLEX_2 -> SingleDomainWarpSimplexGradient(
        seed,
        amp * 38.283687591552734375f,
        freq,
        x,
        y,
        coord,
        false
      )

      OPEN_SIMPLEX_2_REDUCED -> SingleDomainWarpSimplexGradient(seed, amp * 16.0f, freq, x, y, coord, true)
      BASIC_GRID -> SingleDomainWarpBasicGrid(seed, amp, freq, x, y, coord)
    }
  }

  // Perlin Noise
  private fun DoSingleDomainWarp(
    seed: Int,
    amp: Float,
    freq: Float,
    x: Float,
    y: Float,
    z: Float,
    coord: Vector3FNL
  ) {
    when (domainWarpType) {
      DomainWarpType.OPEN_SIMPLEX_2 -> SingleDomainWarpOpenSimplex2Gradient(
        seed,
        amp * 32.69428253173828125f,
        freq,
        x,
        y,
        z,
        coord,
        false
      )

      OPEN_SIMPLEX_2_REDUCED -> SingleDomainWarpOpenSimplex2Gradient(
        seed,
        amp * 7.71604938271605f,
        freq,
        x,
        y,
        z,
        coord,
        true
      )

      BASIC_GRID -> SingleDomainWarpBasicGrid(seed, amp, freq, x, y, z, coord)
    }
  }

  private fun DomainWarpSingle(coord: Vector2FNL) {
    val seed = seed
    val amp = domainWarpAmp * fractalBounding
    val freq = frequency

    var xs = coord.x

    var ys = coord.y
    when (domainWarpType) {
      DomainWarpType.OPEN_SIMPLEX_2, OPEN_SIMPLEX_2_REDUCED -> {
        val SQRT3 = 1.7320508075688772935274463415059.toFloat()
        val F2 = 0.5f * (SQRT3 - 1)

        val t = (xs + ys) * F2
        xs += t
        ys += t
      }

      else -> {
      }
    }
    DoSingleDomainWarp(seed, amp, freq, xs, ys, coord)
  }

  // Value Cubic Noise
  private fun DomainWarpSingle(coord: Vector3FNL) {
    val seed = seed
    val amp = domainWarpAmp * fractalBounding
    val freq = frequency

    var xs = coord.x

    var ys = coord.y

    var zs = coord.z
    when (warpTransformType3D) {
      TransformType3D.IMPROVE_XY_PLANES -> {
        val xy = xs + ys

        val s2 = xy * (-0.211324865405187).toFloat()
        zs *= 0.577350269189626.toFloat()
        xs += s2 - zs
        ys = ys + s2 - zs
        zs += xy * 0.577350269189626.toFloat()
      }

      TransformType3D.IMPROVE_XZ_PLANES -> {
        val xz = xs + zs

        val s2 = xz * (-0.211324865405187).toFloat()
        ys *= 0.577350269189626.toFloat()
        xs += s2 - ys
        zs += s2 - ys
        ys += xz * 0.577350269189626.toFloat()
      }

      DEFAULT_OPEN_SIMPLEX_2 -> {
        val R3 = (2.0 / 3.0).toFloat()

        val r = (xs + ys + zs) * R3 // Rotation, not skew
        xs = r - xs
        ys = r - ys
        zs = r - zs
      }

      else -> {
      }
    }
    DoSingleDomainWarp(seed, amp, freq, xs, ys, zs, coord)
  }

  private fun DomainWarpFractalProgressive(coord: Vector2FNL) {
    var seed = seed
    var amp = domainWarpAmp * fractalBounding
    var freq = frequency
    for (i in 0 until octaves) {
      var xs = coord.x

      var ys = coord.y
      when (domainWarpType) {
        DomainWarpType.OPEN_SIMPLEX_2, OPEN_SIMPLEX_2_REDUCED -> {
          val SQRT3 = 1.7320508075688772935274463415059.toFloat()
          val F2 = 0.5f * (SQRT3 - 1)

          val t = (xs + ys) * F2
          xs += t
          ys += t
        }

        else -> {
        }
      }
      DoSingleDomainWarp(seed, amp, freq, xs, ys, coord)
      seed++
      amp *= gain
      freq *= lacunarity
    }
  }

  // Value Noise
  private fun DomainWarpFractalProgressive(coord: Vector3FNL) {
    var seed = seed
    var amp = domainWarpAmp * fractalBounding
    var freq = frequency
    for (i in 0 until octaves) {
      var xs = coord.x

      var ys = coord.y

      var zs = coord.z
      when (warpTransformType3D) {
        TransformType3D.IMPROVE_XY_PLANES -> {
          val xy = xs + ys

          val s2 = xy * (-0.211324865405187).toFloat()
          zs *= 0.577350269189626.toFloat()
          xs += s2 - zs
          ys = ys + s2 - zs
          zs += xy * 0.577350269189626.toFloat()
        }

        TransformType3D.IMPROVE_XZ_PLANES -> {
          val xz = xs + zs

          val s2 = xz * (-0.211324865405187).toFloat()
          ys *= 0.577350269189626.toFloat()
          xs += s2 - ys
          zs += s2 - ys
          ys += xz * 0.577350269189626.toFloat()
        }

        DEFAULT_OPEN_SIMPLEX_2 -> {
          val R3 = (2.0 / 3.0).toFloat()

          val r = (xs + ys + zs) * R3 // Rotation, not skew
          xs = r - xs
          ys = r - ys
          zs = r - zs
        }

        else -> {
        }
      }
      DoSingleDomainWarp(seed, amp, freq, xs, ys, zs, coord)
      seed++
      amp *= gain
      freq *= lacunarity
    }
  }

  // Domain Warp Fractal Independant
  private fun DomainWarpFractalIndependent(coord: Vector2FNL) {
    var xs = coord.x

    var ys = coord.y
    when (domainWarpType) {
      DomainWarpType.OPEN_SIMPLEX_2, OPEN_SIMPLEX_2_REDUCED -> {
        val SQRT3 = 1.7320508075688772935274463415059.toFloat()
        val F2 = 0.5f * (SQRT3 - 1)

        val t = (xs + ys) * F2
        xs += t
        ys += t
      }

      else -> {
      }
    }
    var seed = seed
    var amp = domainWarpAmp * fractalBounding
    var freq = frequency
    for (i in 0 until octaves) {
      DoSingleDomainWarp(seed, amp, freq, xs, ys, coord)
      seed++
      amp *= gain
      freq *= lacunarity
    }
  }

  // Domain Warp
  private fun DomainWarpFractalIndependent(coord: Vector3FNL) {
    var xs = coord.x

    var ys = coord.y

    var zs = coord.z
    when (warpTransformType3D) {
      TransformType3D.IMPROVE_XY_PLANES -> {
        val xy = xs + ys

        val s2 = xy * (-0.211324865405187).toFloat()
        zs *= 0.577350269189626.toFloat()
        xs += s2 - zs
        ys = ys + s2 - zs
        zs += xy * 0.577350269189626.toFloat()
      }

      TransformType3D.IMPROVE_XZ_PLANES -> {
        val xz = xs + zs

        val s2 = xz * (-0.211324865405187).toFloat()
        ys *= 0.577350269189626.toFloat()
        xs += s2 - ys
        zs += s2 - ys
        ys += xz * 0.577350269189626.toFloat()
      }

      DEFAULT_OPEN_SIMPLEX_2 -> {
        val R3 = (2.0 / 3.0).toFloat()

        val r = (xs + ys + zs) * R3 // Rotation, not skew
        xs = r - xs
        ys = r - ys
        zs = r - zs
      }

      else -> {
      }
    }
    var seed = seed
    var amp = domainWarpAmp * fractalBounding
    var freq = frequency
    for (i in 0 until octaves) {
      DoSingleDomainWarp(seed, amp, freq, xs, ys, zs, coord)
      seed++
      amp *= gain
      freq *= lacunarity
    }
  }

  private fun SingleDomainWarpBasicGrid(
    seed: Int,
    warpAmp: Float,
    frequency: Float,
    x: Float,
    y: Float,
    coord: Vector2FNL
  ) {
    val xf = x * frequency

    val yf = y * frequency
    var x0: Int = FastFloor(xf)
    var y0: Int = FastFloor(yf)
    val xs: Float = InterpHermite(xf - x0)
    val ys: Float = InterpHermite(yf - y0)
    x0 *= PRIME_X
    y0 *= PRIME_Y
    val x1: Int = x0 + PRIME_X
    val y1: Int = y0 + PRIME_Y
    var hash0: Int = Hash(seed, x0, y0) and (255 shl 1)
    var hash1: Int = Hash(seed, x1, y0) and (255 shl 1)
    val lx0x: Float = Lerp(RandVecs2D[hash0], RandVecs2D[hash1], xs)
    val ly0x: Float = Lerp(RandVecs2D[hash0 or 1], RandVecs2D[hash1 or 1], xs)
    hash0 = Hash(seed, x0, y1) and (255 shl 1)
    hash1 = Hash(seed, x1, y1) and (255 shl 1)
    val lx1x: Float = Lerp(RandVecs2D[hash0], RandVecs2D[hash1], xs)
    val ly1x: Float = Lerp(RandVecs2D[hash0 or 1], RandVecs2D[hash1 or 1], xs)
    coord.x += Lerp(lx0x, lx1x, ys) * warpAmp
    coord.y += Lerp(ly0x, ly1x, ys) * warpAmp
  }

  // Domain Warp Single Wrapper
  private fun SingleDomainWarpBasicGrid(
    seed: Int,
    warpAmp: Float,
    frequency: Float,
    x: Float,
    y: Float,
    z: Float,
    coord: Vector3FNL
  ) {
    val xf = x * frequency

    val yf = y * frequency

    val zf = z * frequency
    var x0: Int = FastFloor(xf)
    var y0: Int = FastFloor(yf)
    var z0: Int = FastFloor(zf)
    val xs: Float = InterpHermite(xf - x0)
    val ys: Float = InterpHermite(yf - y0)
    val zs: Float = InterpHermite(zf - z0)
    x0 *= PRIME_X
    y0 *= PRIME_Y
    z0 *= PRIME_Z
    val x1: Int = x0 + PRIME_X
    val y1: Int = y0 + PRIME_Y
    val z1: Int = z0 + PRIME_Z
    var hash0: Int = Hash(seed, x0, y0, z0) and (255 shl 2)
    var hash1: Int = Hash(seed, x1, y0, z0) and (255 shl 2)
    var lx0x: Float = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs)
    var ly0x: Float = Lerp(RandVecs3D[hash0 or 1], RandVecs3D[hash1 or 1], xs)
    var lz0x: Float = Lerp(RandVecs3D[hash0 or 2], RandVecs3D[hash1 or 2], xs)
    hash0 = Hash(seed, x0, y1, z0) and (255 shl 2)
    hash1 = Hash(seed, x1, y1, z0) and (255 shl 2)
    var lx1x: Float = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs)
    var ly1x: Float = Lerp(RandVecs3D[hash0 or 1], RandVecs3D[hash1 or 1], xs)
    var lz1x: Float = Lerp(RandVecs3D[hash0 or 2], RandVecs3D[hash1 or 2], xs)
    val lx0y: Float = Lerp(lx0x, lx1x, ys)
    val ly0y: Float = Lerp(ly0x, ly1x, ys)
    val lz0y: Float = Lerp(lz0x, lz1x, ys)
    hash0 = Hash(seed, x0, y0, z1) and (255 shl 2)
    hash1 = Hash(seed, x1, y0, z1) and (255 shl 2)
    lx0x = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs)
    ly0x = Lerp(RandVecs3D[hash0 or 1], RandVecs3D[hash1 or 1], xs)
    lz0x = Lerp(RandVecs3D[hash0 or 2], RandVecs3D[hash1 or 2], xs)
    hash0 = Hash(seed, x0, y1, z1) and (255 shl 2)
    hash1 = Hash(seed, x1, y1, z1) and (255 shl 2)
    lx1x = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs)
    ly1x = Lerp(RandVecs3D[hash0 or 1], RandVecs3D[hash1 or 1], xs)
    lz1x = Lerp(RandVecs3D[hash0 or 2], RandVecs3D[hash1 or 2], xs)
    coord.x += Lerp(lx0y, Lerp(lx0x, lx1x, ys), zs) * warpAmp
    coord.y += Lerp(ly0y, Lerp(ly0x, ly1x, ys), zs) * warpAmp
    coord.z += Lerp(lz0y, Lerp(lz0x, lz1x, ys), zs) * warpAmp
  }

  // Domain Warp Simplex/OpenSimplex2
  private fun SingleDomainWarpSimplexGradient(
    seed: Int,
    warpAmp: Float,
    frequency: Float,
    x: Float,
    y: Float,
    coord: Vector2FNL,
    outGradOnly: Boolean
  ) {
    var x = x
    var y = y
    val SQRT3 = 1.7320508075688772935274463415059f
    val G2 = (3 - SQRT3) / 6
    x *= frequency
    y *= frequency

    /*
     * --- Skew moved to switch statements before fractal evaluation  ---
     * final FNLfloat F2 = 0.5f * (SQRT3 - 1);
     * FNLfloat s = (x + y) * F2;
     * x += s; y += s;
     */
    var i: Int = FastFloor(x)
    var j: Int = FastFloor(y)
    val xi = x - i
    val yi = y - j
    val t = (xi + yi) * G2
    val x0 = xi - t
    val y0 = yi - t
    i *= PRIME_X
    j *= PRIME_Y
    var vx: Float
    var vy: Float
    vy = 0f
    vx = vy
    val a = 0.5f - x0 * x0 - y0 * y0
    if (a > 0) {
      val aaaa = a * a * (a * a)
      val xo: Float
      val yo: Float
      if (outGradOnly) {
        val hash: Int = Hash(seed, i, j) and (255 shl 1)
        xo = RandVecs2D[hash]
        yo = RandVecs2D[hash or 1]
      } else {
        val hash: Int = Hash(seed, i, j)
        val index1 = hash and (127 shl 1)
        val index2 = hash shr 7 and (255 shl 1)
        val xg: Float = Gradients2D[index1]
        val yg: Float = Gradients2D[index1 or 1]
        val value = x0 * xg + y0 * yg
        val xgo: Float = RandVecs2D[index2]
        val ygo: Float = RandVecs2D[index2 or 1]
        xo = value * xgo
        yo = value * ygo
      }
      vx += aaaa * xo
      vy += aaaa * yo
    }
    val c = 2 * (1 - 2 * G2) * (1 / G2 - 2) * t + (-2 * (1 - 2 * G2) * (1 - 2 * G2) + a)
    if (c > 0) {
      val x2 = x0 + (2 * G2 - 1)
      val y2 = y0 + (2 * G2 - 1)
      val cccc = c * c * (c * c)
      val xo: Float
      val yo: Float
      if (outGradOnly) {
        val hash: Int = Hash(seed, i + PRIME_X, j + PRIME_Y) and (255 shl 1)
        xo = RandVecs2D[hash]
        yo = RandVecs2D[hash or 1]
      } else {
        val hash: Int = Hash(seed, i + PRIME_X, j + PRIME_Y)
        val index1 = hash and (127 shl 1)
        val index2 = hash shr 7 and (255 shl 1)
        val xg: Float = Gradients2D[index1]
        val yg: Float = Gradients2D[index1 or 1]
        val value = x2 * xg + y2 * yg
        val xgo: Float = RandVecs2D[index2]
        val ygo: Float = RandVecs2D[index2 or 1]
        xo = value * xgo
        yo = value * ygo
      }
      vx += cccc * xo
      vy += cccc * yo
    }
    if (y0 > x0) {
      val x1 = x0 + G2
      val y1 = y0 + (G2 - 1)
      val b = 0.5f - x1 * x1 - y1 * y1
      if (b > 0) {
        val bbbb = b * b * (b * b)
        val xo: Float
        val yo: Float
        if (outGradOnly) {
          val hash: Int = Hash(seed, i, j + PRIME_Y) and (255 shl 1)
          xo = RandVecs2D[hash]
          yo = RandVecs2D[hash or 1]
        } else {
          val hash: Int = Hash(seed, i, j + PRIME_Y)
          val index1 = hash and (127 shl 1)
          val index2 = hash shr 7 and (255 shl 1)
          val xg: Float = Gradients2D[index1]
          val yg: Float = Gradients2D[index1 or 1]
          val value = x1 * xg + y1 * yg
          val xgo: Float = RandVecs2D[index2]
          val ygo: Float = RandVecs2D[index2 or 1]
          xo = value * xgo
          yo = value * ygo
        }
        vx += bbbb * xo
        vy += bbbb * yo
      }
    } else {
      val x1 = x0 + (G2 - 1)
      val y1 = y0 + G2
      val b = 0.5f - x1 * x1 - y1 * y1
      if (b > 0) {
        val bbbb = b * b * (b * b)
        val xo: Float
        val yo: Float
        if (outGradOnly) {
          val hash: Int = Hash(seed, i + PRIME_X, j) and (255 shl 1)
          xo = RandVecs2D[hash]
          yo = RandVecs2D[hash or 1]
        } else {
          val hash: Int = Hash(seed, i + PRIME_X, j)
          val index1 = hash and (127 shl 1)
          val index2 = hash shr 7 and (255 shl 1)
          val xg: Float = Gradients2D[index1]
          val yg: Float = Gradients2D[index1 or 1]
          val value = x1 * xg + y1 * yg
          val xgo: Float = RandVecs2D[index2]
          val ygo: Float = RandVecs2D[index2 or 1]
          xo = value * xgo
          yo = value * ygo
        }
        vx += bbbb * xo
        vy += bbbb * yo
      }
    }
    coord.x += vx * warpAmp
    coord.y += vy * warpAmp
  }

  // Domain Warp Fractal Progressive
  private fun SingleDomainWarpOpenSimplex2Gradient(
    seed: Int,
    warpAmp: Float,
    frequency: Float,
    x: Float,
    y: Float,
    z: Float,
    coord: Vector3FNL,
    outGradOnly: Boolean
  ) {
    var seed = seed
    var x = x
    var y = y
    var z = z
    x *= frequency
    y *= frequency
    z *= frequency

    /*
     * --- Rotation moved to switch statements before fractal evaluation ---
     * final FNLfloat R3 = (FNLfloat)(2.0 / 3.0);
     * FNLfloat r = (x + y + z) * R3; // Rotation, not skew
     * x = r - x; y = r - y; z = r - z;
     */
    var i: Int = FastRound(x)
    var j: Int = FastRound(y)
    var k: Int = FastRound(z)
    var x0 = x - i
    var y0 = y - j
    var z0 = z - k
    var xNSign = (-x0 - 1.0f).toInt() or 1
    var yNSign = (-y0 - 1.0f).toInt() or 1
    var zNSign = (-z0 - 1.0f).toInt() or 1
    var ax0 = xNSign * -x0
    var ay0 = yNSign * -y0
    var az0 = zNSign * -z0
    i *= PRIME_X
    j *= PRIME_Y
    k *= PRIME_Z
    var vx: Float
    var vy: Float
    var vz: Float
    vz = 0f
    vy = vz
    vx = vy
    var a = 0.6f - x0 * x0 - (y0 * y0 + z0 * z0)
    var l = 0
    while (true) {
      if (a > 0) {
        val aaaa = a * a * (a * a)
        var xo: Float
        var yo: Float
        var zo: Float
        if (outGradOnly) {
          val hash: Int = Hash(seed, i, j, k) and (255 shl 2)
          xo = RandVecs3D[hash]
          yo = RandVecs3D[hash or 1]
          zo = RandVecs3D[hash or 2]
        } else {
          val hash: Int = Hash(seed, i, j, k)
          val index1 = hash and (63 shl 2)
          val index2 = hash shr 6 and (255 shl 2)
          val xg: Float = Gradients3D[index1]
          val yg: Float = Gradients3D[index1 or 1]
          val zg: Float = Gradients3D[index1 or 2]
          val value = x0 * xg + y0 * yg + z0 * zg
          val xgo: Float = RandVecs3D[index2]
          val ygo: Float = RandVecs3D[index2 or 1]
          val zgo: Float = RandVecs3D[index2 or 2]
          xo = value * xgo
          yo = value * ygo
          zo = value * zgo
        }
        vx += aaaa * xo
        vy += aaaa * yo
        vz += aaaa * zo
      }
      var b = a
      var i1 = i
      var j1 = j
      var k1 = k
      var x1 = x0
      var y1 = y0
      var z1 = z0
      if (ax0 >= ay0 && ax0 >= az0) {
        x1 += xNSign.toFloat()
        b = b + ax0 + ax0
        i1 -= xNSign * PRIME_X
      } else if (ay0 > ax0 && ay0 >= az0) {
        y1 += yNSign.toFloat()
        b = b + ay0 + ay0
        j1 -= yNSign * PRIME_Y
      } else {
        z1 += zNSign.toFloat()
        b = b + az0 + az0
        k1 -= zNSign * PRIME_Z
      }
      if (b > 1) {
        b -= 1f
        val bbbb = b * b * (b * b)
        var xo: Float
        var yo: Float
        var zo: Float
        if (outGradOnly) {
          val hash: Int = Hash(seed, i1, j1, k1) and (255 shl 2)
          xo = RandVecs3D[hash]
          yo = RandVecs3D[hash or 1]
          zo = RandVecs3D[hash or 2]
        } else {
          val hash: Int = Hash(seed, i1, j1, k1)
          val index1 = hash and (63 shl 2)
          val index2 = hash shr 6 and (255 shl 2)
          val xg: Float = Gradients3D[index1]
          val yg: Float = Gradients3D[index1 or 1]
          val zg: Float = Gradients3D[index1 or 2]
          val value = x1 * xg + y1 * yg + z1 * zg
          val xgo: Float = RandVecs3D[index2]
          val ygo: Float = RandVecs3D[index2 or 1]
          val zgo: Float = RandVecs3D[index2 or 2]
          xo = value * xgo
          yo = value * ygo
          zo = value * zgo
        }
        vx += bbbb * xo
        vy += bbbb * yo
        vz += bbbb * zo
      }
      if (l == 1) {
        break
      }
      ax0 = 0.5f - ax0
      ay0 = 0.5f - ay0
      az0 = 0.5f - az0
      x0 = xNSign * ax0
      y0 = yNSign * ay0
      z0 = zNSign * az0
      a += 0.75f - ax0 - (ay0 + az0)
      i += xNSign shr 1 and PRIME_X
      j += yNSign shr 1 and PRIME_Y
      k += zNSign shr 1 and PRIME_Z
      xNSign = -xNSign
      yNSign = -yNSign
      zNSign = -zNSign
      seed += 1293373
      l++
    }
    coord.x += vx * warpAmp
    coord.y += vy * warpAmp
    coord.z += vz * warpAmp
  }

  companion object {
    private val Gradients2D = floatArrayOf(
      0.130526192220052f,
      0.99144486137381f,
      0.38268343236509f,
      0.923879532511287f,
      0.608761429008721f,
      0.793353340291235f,
      0.793353340291235f,
      0.608761429008721f,
      0.923879532511287f,
      0.38268343236509f,
      0.99144486137381f,
      0.130526192220051f,
      0.99144486137381f,
      -0.130526192220051f,
      0.923879532511287f,
      -0.38268343236509f,
      0.793353340291235f,
      -0.60876142900872f,
      0.608761429008721f,
      -0.793353340291235f,
      0.38268343236509f,
      -0.923879532511287f,
      0.130526192220052f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      -0.38268343236509f,
      -0.923879532511287f,
      -0.608761429008721f,
      -0.793353340291235f,
      -0.793353340291235f,
      -0.608761429008721f,
      -0.923879532511287f,
      -0.38268343236509f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      0.130526192220051f,
      -0.923879532511287f,
      0.38268343236509f,
      -0.793353340291235f,
      0.608761429008721f,
      -0.608761429008721f,
      0.793353340291235f,
      -0.38268343236509f,
      0.923879532511287f,
      -0.130526192220052f,
      0.99144486137381f,
      0.130526192220052f,
      0.99144486137381f,
      0.38268343236509f,
      0.923879532511287f,
      0.608761429008721f,
      0.793353340291235f,
      0.793353340291235f,
      0.608761429008721f,
      0.923879532511287f,
      0.38268343236509f,
      0.99144486137381f,
      0.130526192220051f,
      0.99144486137381f,
      -0.130526192220051f,
      0.923879532511287f,
      -0.38268343236509f,
      0.793353340291235f,
      -0.60876142900872f,
      0.608761429008721f,
      -0.793353340291235f,
      0.38268343236509f,
      -0.923879532511287f,
      0.130526192220052f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      -0.38268343236509f,
      -0.923879532511287f,
      -0.608761429008721f,
      -0.793353340291235f,
      -0.793353340291235f,
      -0.608761429008721f,
      -0.923879532511287f,
      -0.38268343236509f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      0.130526192220051f,
      -0.923879532511287f,
      0.38268343236509f,
      -0.793353340291235f,
      0.608761429008721f,
      -0.608761429008721f,
      0.793353340291235f,
      -0.38268343236509f,
      0.923879532511287f,
      -0.130526192220052f,
      0.99144486137381f,
      0.130526192220052f,
      0.99144486137381f,
      0.38268343236509f,
      0.923879532511287f,
      0.608761429008721f,
      0.793353340291235f,
      0.793353340291235f,
      0.608761429008721f,
      0.923879532511287f,
      0.38268343236509f,
      0.99144486137381f,
      0.130526192220051f,
      0.99144486137381f,
      -0.130526192220051f,
      0.923879532511287f,
      -0.38268343236509f,
      0.793353340291235f,
      -0.60876142900872f,
      0.608761429008721f,
      -0.793353340291235f,
      0.38268343236509f,
      -0.923879532511287f,
      0.130526192220052f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      -0.38268343236509f,
      -0.923879532511287f,
      -0.608761429008721f,
      -0.793353340291235f,
      -0.793353340291235f,
      -0.608761429008721f,
      -0.923879532511287f,
      -0.38268343236509f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      0.130526192220051f,
      -0.923879532511287f,
      0.38268343236509f,
      -0.793353340291235f,
      0.608761429008721f,
      -0.608761429008721f,
      0.793353340291235f,
      -0.38268343236509f,
      0.923879532511287f,
      -0.130526192220052f,
      0.99144486137381f,
      0.130526192220052f,
      0.99144486137381f,
      0.38268343236509f,
      0.923879532511287f,
      0.608761429008721f,
      0.793353340291235f,
      0.793353340291235f,
      0.608761429008721f,
      0.923879532511287f,
      0.38268343236509f,
      0.99144486137381f,
      0.130526192220051f,
      0.99144486137381f,
      -0.130526192220051f,
      0.923879532511287f,
      -0.38268343236509f,
      0.793353340291235f,
      -0.60876142900872f,
      0.608761429008721f,
      -0.793353340291235f,
      0.38268343236509f,
      -0.923879532511287f,
      0.130526192220052f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      -0.38268343236509f,
      -0.923879532511287f,
      -0.608761429008721f,
      -0.793353340291235f,
      -0.793353340291235f,
      -0.608761429008721f,
      -0.923879532511287f,
      -0.38268343236509f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      0.130526192220051f,
      -0.923879532511287f,
      0.38268343236509f,
      -0.793353340291235f,
      0.608761429008721f,
      -0.608761429008721f,
      0.793353340291235f,
      -0.38268343236509f,
      0.923879532511287f,
      -0.130526192220052f,
      0.99144486137381f,
      0.130526192220052f,
      0.99144486137381f,
      0.38268343236509f,
      0.923879532511287f,
      0.608761429008721f,
      0.793353340291235f,
      0.793353340291235f,
      0.608761429008721f,
      0.923879532511287f,
      0.38268343236509f,
      0.99144486137381f,
      0.130526192220051f,
      0.99144486137381f,
      -0.130526192220051f,
      0.923879532511287f,
      -0.38268343236509f,
      0.793353340291235f,
      -0.60876142900872f,
      0.608761429008721f,
      -0.793353340291235f,
      0.38268343236509f,
      -0.923879532511287f,
      0.130526192220052f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      -0.38268343236509f,
      -0.923879532511287f,
      -0.608761429008721f,
      -0.793353340291235f,
      -0.793353340291235f,
      -0.608761429008721f,
      -0.923879532511287f,
      -0.38268343236509f,
      -0.99144486137381f,
      -0.130526192220052f,
      -0.99144486137381f,
      0.130526192220051f,
      -0.923879532511287f,
      0.38268343236509f,
      -0.793353340291235f,
      0.608761429008721f,
      -0.608761429008721f,
      0.793353340291235f,
      -0.38268343236509f,
      0.923879532511287f,
      -0.130526192220052f,
      0.99144486137381f,
      0.38268343236509f,
      0.923879532511287f,
      0.923879532511287f,
      0.38268343236509f,
      0.923879532511287f,
      -0.38268343236509f,
      0.38268343236509f,
      -0.923879532511287f,
      -0.38268343236509f,
      -0.923879532511287f,
      -0.923879532511287f,
      -0.38268343236509f,
      -0.923879532511287f,
      0.38268343236509f,
      -0.38268343236509f,
      0.923879532511287f
    )
    private val RandVecs2D = floatArrayOf(
      -0.2700222198f,
      -0.9628540911f,
      0.3863092627f,
      -0.9223693152f,
      0.04444859006f,
      -0.999011673f,
      -0.5992523158f,
      -0.8005602176f,
      -0.7819280288f,
      0.6233687174f,
      0.9464672271f,
      0.3227999196f,
      -0.6514146797f,
      -0.7587218957f,
      0.9378472289f,
      0.347048376f,
      -0.8497875957f,
      -0.5271252623f,
      -0.879042592f,
      0.4767432447f,
      -0.892300288f,
      -0.4514423508f,
      -0.379844434f,
      -0.9250503802f,
      -0.9951650832f,
      0.0982163789f,
      0.7724397808f,
      -0.6350880136f,
      0.7573283322f,
      -0.6530343002f,
      -0.9928004525f,
      -0.119780055f,
      -0.0532665713f,
      0.9985803285f,
      0.9754253726f,
      -0.2203300762f,
      -0.7665018163f,
      0.6422421394f,
      0.991636706f,
      0.1290606184f,
      -0.994696838f,
      0.1028503788f,
      -0.5379205513f,
      -0.84299554f,
      0.5022815471f,
      -0.8647041387f,
      0.4559821461f,
      -0.8899889226f,
      -0.8659131224f,
      -0.5001944266f,
      0.0879458407f,
      -0.9961252577f,
      -0.5051684983f,
      0.8630207346f,
      0.7753185226f,
      -0.6315704146f,
      -0.6921944612f,
      0.7217110418f,
      -0.5191659449f,
      -0.8546734591f,
      0.8978622882f,
      -0.4402764035f,
      -0.1706774107f,
      0.9853269617f,
      -0.9353430106f,
      -0.3537420705f,
      -0.9992404798f,
      0.03896746794f,
      -0.2882064021f,
      -0.9575683108f,
      -0.9663811329f,
      0.2571137995f,
      -0.8759714238f,
      -0.4823630009f,
      -0.8303123018f,
      -0.5572983775f,
      0.05110133755f,
      -0.9986934731f,
      -0.8558373281f,
      -0.5172450752f,
      0.09887025282f,
      0.9951003332f,
      0.9189016087f,
      0.3944867976f,
      -0.2439375892f,
      -0.9697909324f,
      -0.8121409387f,
      -0.5834613061f,
      -0.9910431363f,
      0.1335421355f,
      0.8492423985f,
      -0.5280031709f,
      -0.9717838994f,
      -0.2358729591f,
      0.9949457207f,
      0.1004142068f,
      0.6241065508f,
      -0.7813392434f,
      0.662910307f,
      0.7486988212f,
      -0.7197418176f,
      0.6942418282f,
      -0.8143370775f,
      -0.5803922158f,
      0.104521054f,
      -0.9945226741f,
      -0.1065926113f,
      -0.9943027784f,
      0.445799684f,
      -0.8951327509f,
      0.105547406f,
      0.9944142724f,
      -0.992790267f,
      0.1198644477f,
      -0.8334366408f,
      0.552615025f,
      0.9115561563f,
      -0.4111755999f,
      0.8285544909f,
      -0.5599084351f,
      0.7217097654f,
      -0.6921957921f,
      0.4940492677f,
      -0.8694339084f,
      -0.3652321272f,
      -0.9309164803f,
      -0.9696606758f,
      0.2444548501f,
      0.08925509731f,
      -0.996008799f,
      0.5354071276f,
      -0.8445941083f,
      -0.1053576186f,
      0.9944343981f,
      -0.9890284586f,
      0.1477251101f,
      0.004856104961f,
      0.9999882091f,
      0.9885598478f,
      0.1508291331f,
      0.9286129562f,
      -0.3710498316f,
      -0.5832393863f,
      -0.8123003252f,
      0.3015207509f,
      0.9534596146f,
      -0.9575110528f,
      0.2883965738f,
      0.9715802154f,
      -0.2367105511f,
      0.229981792f,
      0.9731949318f,
      0.955763816f,
      -0.2941352207f,
      0.740956116f,
      0.6715534485f,
      -0.9971513787f,
      -0.07542630764f,
      0.6905710663f,
      -0.7232645452f,
      -0.290713703f,
      -0.9568100872f,
      0.5912777791f,
      -0.8064679708f,
      -0.9454592212f,
      -0.325740481f,
      0.6664455681f,
      0.74555369f,
      0.6236134912f,
      0.7817328275f,
      0.9126993851f,
      -0.4086316587f,
      -0.8191762011f,
      0.5735419353f,
      -0.8812745759f,
      -0.4726046147f,
      0.9953313627f,
      0.09651672651f,
      0.9855650846f,
      -0.1692969699f,
      -0.8495980887f,
      0.5274306472f,
      0.6174853946f,
      -0.7865823463f,
      0.8508156371f,
      0.52546432f,
      0.9985032451f,
      -0.05469249926f,
      0.1971371563f,
      -0.9803759185f,
      0.6607855748f,
      -0.7505747292f,
      -0.03097494063f,
      0.9995201614f,
      -0.6731660801f,
      0.739491331f,
      -0.7195018362f,
      -0.6944905383f,
      0.9727511689f,
      0.2318515979f,
      0.9997059088f,
      -0.0242506907f,
      0.4421787429f,
      -0.8969269532f,
      0.9981350961f,
      -0.061043673f,
      -0.9173660799f,
      -0.3980445648f,
      -0.8150056635f,
      -0.5794529907f,
      -0.8789331304f,
      0.4769450202f,
      0.0158605829f,
      0.999874213f,
      -0.8095464474f,
      0.5870558317f,
      -0.9165898907f,
      -0.3998286786f,
      -0.8023542565f,
      0.5968480938f,
      -0.5176737917f,
      0.8555780767f,
      -0.8154407307f,
      -0.5788405779f,
      0.4022010347f,
      -0.9155513791f,
      -0.9052556868f,
      -0.4248672045f,
      0.7317445619f,
      0.6815789728f,
      -0.5647632201f,
      -0.8252529947f,
      -0.8403276335f,
      -0.5420788397f,
      -0.9314281527f,
      0.363925262f,
      0.5238198472f,
      0.8518290719f,
      0.7432803869f,
      -0.6689800195f,
      -0.985371561f,
      -0.1704197369f,
      0.4601468731f,
      0.88784281f,
      0.825855404f,
      0.5638819483f,
      0.6182366099f,
      0.7859920446f,
      0.8331502863f,
      -0.553046653f,
      0.1500307506f,
      0.9886813308f,
      -0.662330369f,
      -0.7492119075f,
      -0.668598664f,
      0.743623444f,
      0.7025606278f,
      0.7116238924f,
      -0.5419389763f,
      -0.8404178401f,
      -0.3388616456f,
      0.9408362159f,
      0.8331530315f,
      0.5530425174f,
      -0.2989720662f,
      -0.9542618632f,
      0.2638522993f,
      0.9645630949f,
      0.124108739f,
      -0.9922686234f,
      -0.7282649308f,
      -0.6852956957f,
      0.6962500149f,
      0.7177993569f,
      -0.9183535368f,
      0.3957610156f,
      -0.6326102274f,
      -0.7744703352f,
      -0.9331891859f,
      -0.359385508f,
      -0.1153779357f,
      -0.9933216659f,
      0.9514974788f,
      -0.3076565421f,
      -0.08987977445f,
      -0.9959526224f,
      0.6678496916f,
      0.7442961705f,
      0.7952400393f,
      -0.6062947138f,
      -0.6462007402f,
      -0.7631674805f,
      -0.2733598753f,
      0.9619118351f,
      0.9669590226f,
      -0.254931851f,
      -0.9792894595f,
      0.2024651934f,
      -0.5369502995f,
      -0.8436138784f,
      -0.270036471f,
      -0.9628500944f,
      -0.6400277131f,
      0.7683518247f,
      -0.7854537493f,
      -0.6189203566f,
      0.06005905383f,
      -0.9981948257f,
      -0.02455770378f,
      0.9996984141f,
      -0.65983623f,
      0.751409442f,
      -0.6253894466f,
      -0.7803127835f,
      -0.6210408851f,
      -0.7837781695f,
      0.8348888491f,
      0.5504185768f,
      -0.1592275245f,
      0.9872419133f,
      0.8367622488f,
      0.5475663786f,
      -0.8675753916f,
      -0.4973056806f,
      -0.2022662628f,
      -0.9793305667f,
      0.9399189937f,
      0.3413975472f,
      0.9877404807f,
      -0.1561049093f,
      -0.9034455656f,
      0.4287028224f,
      0.1269804218f,
      -0.9919052235f,
      -0.3819600854f,
      0.924178821f,
      0.9754625894f,
      0.2201652486f,
      -0.3204015856f,
      -0.9472818081f,
      -0.9874760884f,
      0.1577687387f,
      0.02535348474f,
      -0.9996785487f,
      0.4835130794f,
      -0.8753371362f,
      -0.2850799925f,
      -0.9585037287f,
      -0.06805516006f,
      -0.99768156f,
      -0.7885244045f,
      -0.6150034663f,
      0.3185392127f,
      -0.9479096845f,
      0.8880043089f,
      0.4598351306f,
      0.6476921488f,
      -0.7619021462f,
      0.9820241299f,
      0.1887554194f,
      0.9357275128f,
      -0.3527237187f,
      -0.8894895414f,
      0.4569555293f,
      0.7922791302f,
      0.6101588153f,
      0.7483818261f,
      0.6632681526f,
      -0.7288929755f,
      -0.6846276581f,
      0.8729032783f,
      -0.4878932944f,
      0.8288345784f,
      0.5594937369f,
      0.08074567077f,
      0.9967347374f,
      0.9799148216f,
      -0.1994165048f,
      -0.580730673f,
      -0.8140957471f,
      -0.4700049791f,
      -0.8826637636f,
      0.2409492979f,
      0.9705377045f,
      0.9437816757f,
      -0.3305694308f,
      -0.8927998638f,
      -0.4504535528f,
      -0.8069622304f,
      0.5906030467f,
      0.06258973166f,
      0.9980393407f,
      -0.9312597469f,
      0.3643559849f,
      0.5777449785f,
      0.8162173362f,
      -0.3360095855f,
      -0.941858566f,
      0.697932075f,
      -0.7161639607f,
      -0.002008157227f,
      -0.9999979837f,
      -0.1827294312f,
      -0.9831632392f,
      -0.6523911722f,
      0.7578824173f,
      -0.4302626911f,
      -0.9027037258f,
      -0.9985126289f,
      -0.05452091251f,
      -0.01028102172f,
      -0.9999471489f,
      -0.4946071129f,
      0.8691166802f,
      -0.2999350194f,
      0.9539596344f,
      0.8165471961f,
      0.5772786819f,
      0.2697460475f,
      0.962931498f,
      -0.7306287391f,
      -0.6827749597f,
      -0.7590952064f,
      -0.6509796216f,
      -0.907053853f,
      0.4210146171f,
      -0.5104861064f,
      -0.8598860013f,
      0.8613350597f,
      0.5080373165f,
      0.5007881595f,
      -0.8655698812f,
      -0.654158152f,
      0.7563577938f,
      -0.8382755311f,
      -0.545246856f,
      0.6940070834f,
      0.7199681717f,
      0.06950936031f,
      0.9975812994f,
      0.1702942185f,
      -0.9853932612f,
      0.2695973274f,
      0.9629731466f,
      0.5519612192f,
      -0.8338697815f,
      0.225657487f,
      -0.9742067022f,
      0.4215262855f,
      -0.9068161835f,
      0.4881873305f,
      -0.8727388672f,
      -0.3683854996f,
      -0.9296731273f,
      -0.9825390578f,
      0.1860564427f,
      0.81256471f,
      0.5828709909f,
      0.3196460933f,
      -0.9475370046f,
      0.9570913859f,
      0.2897862643f,
      -0.6876655497f,
      -0.7260276109f,
      -0.9988770922f,
      -0.047376731f,
      -0.1250179027f,
      0.992154486f,
      -0.8280133617f,
      0.560708367f,
      0.9324863769f,
      -0.3612051451f,
      0.6394653183f,
      0.7688199442f,
      -0.01623847064f,
      -0.9998681473f,
      -0.9955014666f,
      -0.09474613458f,
      -0.81453315f,
      0.580117012f,
      0.4037327978f,
      -0.9148769469f,
      0.9944263371f,
      0.1054336766f,
      -0.1624711654f,
      0.9867132919f,
      -0.9949487814f,
      -0.100383875f,
      -0.6995302564f,
      0.7146029809f,
      0.5263414922f,
      -0.85027327f,
      -0.5395221479f,
      0.841971408f,
      0.6579370318f,
      0.7530729462f,
      0.01426758847f,
      -0.9998982128f,
      -0.6734383991f,
      0.7392433447f,
      0.639412098f,
      -0.7688642071f,
      0.9211571421f,
      0.3891908523f,
      -0.146637214f,
      -0.9891903394f,
      -0.782318098f,
      0.6228791163f,
      -0.5039610839f,
      -0.8637263605f,
      -0.7743120191f,
      -0.6328039957f
    )
    private val Gradients3D = floatArrayOf(
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      0f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      0f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      0f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      0f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      1f,
      0f,
      1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      -1f,
      0f,
      1f,
      1f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      1f,
      -1f,
      0f,
      0f,
      -1f,
      -1f,
      0f,
      0f,
      1f,
      1f,
      0f,
      0f,
      0f,
      -1f,
      1f,
      0f,
      -1f,
      1f,
      0f,
      0f,
      0f,
      -1f,
      -1f,
      0f
    )
    private val RandVecs3D = floatArrayOf(
      -0.7292736885f,
      -0.6618439697f,
      0.1735581948f,
      0f,
      0.790292081f,
      -0.5480887466f,
      -0.2739291014f,
      0f,
      0.7217578935f,
      0.6226212466f,
      -0.3023380997f,
      0f,
      0.565683137f,
      -0.8208298145f,
      -0.0790000257f,
      0f,
      0.760049034f,
      -0.5555979497f,
      -0.3370999617f,
      0f,
      0.3713945616f,
      0.5011264475f,
      0.7816254623f,
      0f,
      -0.1277062463f,
      -0.4254438999f,
      -0.8959289049f,
      0f,
      -0.2881560924f,
      -0.5815838982f,
      0.7607405838f,
      0f,
      0.5849561111f,
      -0.662820239f,
      -0.4674352136f,
      0f,
      0.3307171178f,
      0.0391653737f,
      0.94291689f,
      0f,
      0.8712121778f,
      -0.4113374369f,
      -0.2679381538f,
      0f,
      0.580981015f,
      0.7021915846f,
      0.4115677815f,
      0f,
      0.503756873f,
      0.6330056931f,
      -0.5878203852f,
      0f,
      0.4493712205f,
      0.601390195f,
      0.6606022552f,
      0f,
      -0.6878403724f,
      0.09018890807f,
      -0.7202371714f,
      0f,
      -0.5958956522f,
      -0.6469350577f,
      0.475797649f,
      0f,
      -0.5127052122f,
      0.1946921978f,
      -0.8361987284f,
      0f,
      -0.9911507142f,
      -0.05410276466f,
      -0.1212153153f,
      0f,
      -0.2149721042f,
      0.9720882117f,
      -0.09397607749f,
      0f,
      -0.7518650936f,
      -0.5428057603f,
      0.3742469607f,
      0f,
      0.5237068895f,
      0.8516377189f,
      -0.02107817834f,
      0f,
      0.6333504779f,
      0.1926167129f,
      -0.7495104896f,
      0f,
      -0.06788241606f,
      0.3998305789f,
      0.9140719259f,
      0f,
      -0.5538628599f,
      -0.4729896695f,
      -0.6852128902f,
      0f,
      -0.7261455366f,
      -0.5911990757f,
      0.3509933228f,
      0f,
      -0.9229274737f,
      -0.1782808786f,
      0.3412049336f,
      0f,
      -0.6968815002f,
      0.6511274338f,
      0.3006480328f,
      0f,
      0.9608044783f,
      -0.2098363234f,
      -0.1811724921f,
      0f,
      0.06817146062f,
      -0.9743405129f,
      0.2145069156f,
      0f,
      -0.3577285196f,
      -0.6697087264f,
      -0.6507845481f,
      0f,
      -0.1868621131f,
      0.7648617052f,
      -0.6164974636f,
      0f,
      -0.6541697588f,
      0.3967914832f,
      0.6439087246f,
      0f,
      0.6993340405f,
      -0.6164538506f,
      0.3618239211f,
      0f,
      -0.1546665739f,
      0.6291283928f,
      0.7617583057f,
      0f,
      -0.6841612949f,
      -0.2580482182f,
      -0.6821542638f,
      0f,
      0.5383980957f,
      0.4258654885f,
      0.7271630328f,
      0f,
      -0.5026987823f,
      -0.7939832935f,
      -0.3418836993f,
      0f,
      0.3202971715f,
      0.2834415347f,
      0.9039195862f,
      0f,
      0.8683227101f,
      -0.0003762656404f,
      -0.4959995258f,
      0f,
      0.791120031f,
      -0.08511045745f,
      0.6057105799f,
      0f,
      -0.04011016052f,
      -0.4397248749f,
      0.8972364289f,
      0f,
      0.9145119872f,
      0.3579346169f,
      -0.1885487608f,
      0f,
      -0.9612039066f,
      -0.2756484276f,
      0.01024666929f,
      0f,
      0.6510361721f,
      -0.2877799159f,
      -0.7023778346f,
      0f,
      -0.2041786351f,
      0.7365237271f,
      0.644859585f,
      0f,
      -0.7718263711f,
      0.3790626912f,
      0.5104855816f,
      0f,
      -0.3060082741f,
      -0.7692987727f,
      0.5608371729f,
      0f,
      0.454007341f,
      -0.5024843065f,
      0.7357899537f,
      0f,
      0.4816795475f,
      0.6021208291f,
      -0.6367380315f,
      0f,
      0.6961980369f,
      -0.3222197429f,
      0.641469197f,
      0f,
      -0.6532160499f,
      -0.6781148932f,
      0.3368515753f,
      0f,
      0.5089301236f,
      -0.6154662304f,
      -0.6018234363f,
      0f,
      -0.1635919754f,
      -0.9133604627f,
      -0.372840892f,
      0f,
      0.52408019f,
      -0.8437664109f,
      0.1157505864f,
      0f,
      0.5902587356f,
      0.4983817807f,
      -0.6349883666f,
      0f,
      0.5863227872f,
      0.494764745f,
      0.6414307729f,
      0f,
      0.6779335087f,
      0.2341345225f,
      0.6968408593f,
      0f,
      0.7177054546f,
      -0.6858979348f,
      0.120178631f,
      0f,
      -0.5328819713f,
      -0.5205125012f,
      0.6671608058f,
      0f,
      -0.8654874251f,
      -0.0700727088f,
      -0.4960053754f,
      0f,
      -0.2861810166f,
      0.7952089234f,
      0.5345495242f,
      0f,
      -0.04849529634f,
      0.9810836427f,
      -0.1874115585f,
      0f,
      -0.6358521667f,
      0.6058348682f,
      0.4781800233f,
      0f,
      0.6254794696f,
      -0.2861619734f,
      0.7258696564f,
      0f,
      -0.2585259868f,
      0.5061949264f,
      -0.8227581726f,
      0f,
      0.02136306781f,
      0.5064016808f,
      -0.8620330371f,
      0f,
      0.200111773f,
      0.8599263484f,
      0.4695550591f,
      0f,
      0.4743561372f,
      0.6014985084f,
      -0.6427953014f,
      0f,
      0.6622993731f,
      -0.5202474575f,
      -0.5391679918f,
      0f,
      0.08084972818f,
      -0.6532720452f,
      0.7527940996f,
      0f,
      -0.6893687501f,
      0.0592860349f,
      0.7219805347f,
      0f,
      -0.1121887082f,
      -0.9673185067f,
      0.2273952515f,
      0f,
      0.7344116094f,
      0.5979668656f,
      -0.3210532909f,
      0f,
      0.5789393465f,
      -0.2488849713f,
      0.7764570201f,
      0f,
      0.6988182827f,
      0.3557169806f,
      -0.6205791146f,
      0f,
      -0.8636845529f,
      -0.2748771249f,
      -0.4224826141f,
      0f,
      -0.4247027957f,
      -0.4640880967f,
      0.777335046f,
      0f,
      0.5257722489f,
      -0.8427017621f,
      0.1158329937f,
      0f,
      0.9343830603f,
      0.316302472f,
      -0.1639543925f,
      0f,
      -0.1016836419f,
      -0.8057303073f,
      -0.5834887393f,
      0f,
      -0.6529238969f,
      0.50602126f,
      -0.5635892736f,
      0f,
      -0.2465286165f,
      -0.9668205684f,
      -0.06694497494f,
      0f,
      -0.9776897119f,
      -0.2099250524f,
      -0.007368825344f,
      0f,
      0.7736893337f,
      0.5734244712f,
      0.2694238123f,
      0f,
      -0.6095087895f,
      0.4995678998f,
      0.6155736747f,
      0f,
      0.5794535482f,
      0.7434546771f,
      0.3339292269f,
      0f,
      -0.8226211154f,
      0.08142581855f,
      0.5627293636f,
      0f,
      -0.510385483f,
      0.4703667658f,
      0.7199039967f,
      0f,
      -0.5764971849f,
      -0.07231656274f,
      -0.8138926898f,
      0f,
      0.7250628871f,
      0.3949971505f,
      -0.5641463116f,
      0f,
      -0.1525424005f,
      0.4860840828f,
      -0.8604958341f,
      0f,
      -0.5550976208f,
      -0.4957820792f,
      0.667882296f,
      0f,
      -0.1883614327f,
      0.9145869398f,
      0.357841725f,
      0f,
      0.7625556724f,
      -0.5414408243f,
      -0.3540489801f,
      0f,
      -0.5870231946f,
      -0.3226498013f,
      -0.7424963803f,
      0f,
      0.3051124198f,
      0.2262544068f,
      -0.9250488391f,
      0f,
      0.6379576059f,
      0.577242424f,
      -0.5097070502f,
      0f,
      -0.5966775796f,
      0.1454852398f,
      -0.7891830656f,
      0f,
      -0.658330573f,
      0.6555487542f,
      -0.3699414651f,
      0f,
      0.7434892426f,
      0.2351084581f,
      0.6260573129f,
      0f,
      0.5562114096f,
      0.8264360377f,
      -0.0873632843f,
      0f,
      -0.3028940016f,
      -0.8251527185f,
      0.4768419182f,
      0f,
      0.1129343818f,
      -0.985888439f,
      -0.1235710781f,
      0f,
      0.5937652891f,
      -0.5896813806f,
      0.5474656618f,
      0f,
      0.6757964092f,
      -0.5835758614f,
      -0.4502648413f,
      0f,
      0.7242302609f,
      -0.1152719764f,
      0.6798550586f,
      0f,
      -0.9511914166f,
      0.0753623979f,
      -0.2992580792f,
      0f,
      0.2539470961f,
      -0.1886339355f,
      0.9486454084f,
      0f,
      0.571433621f,
      -0.1679450851f,
      -0.8032795685f,
      0f,
      -0.06778234979f,
      0.3978269256f,
      0.9149531629f,
      0f,
      0.6074972649f,
      0.733060024f,
      -0.3058922593f,
      0f,
      -0.5435478392f,
      0.1675822484f,
      0.8224791405f,
      0f,
      -0.5876678086f,
      -0.3380045064f,
      -0.7351186982f,
      0f,
      -0.7967562402f,
      0.04097822706f,
      -0.6029098428f,
      0f,
      -0.1996350917f,
      0.8706294745f,
      0.4496111079f,
      0f,
      -0.02787660336f,
      -0.9106232682f,
      -0.4122962022f,
      0f,
      -0.7797625996f,
      -0.6257634692f,
      0.01975775581f,
      0f,
      -0.5211232846f,
      0.7401644346f,
      -0.4249554471f,
      0f,
      0.8575424857f,
      0.4053272873f,
      -0.3167501783f,
      0f,
      0.1045223322f,
      0.8390195772f,
      -0.5339674439f,
      0f,
      0.3501822831f,
      0.9242524096f,
      -0.1520850155f,
      0f,
      0.1987849858f,
      0.07647613266f,
      0.9770547224f,
      0f,
      0.7845996363f,
      0.6066256811f,
      -0.1280964233f,
      0f,
      0.09006737436f,
      -0.9750989929f,
      -0.2026569073f,
      0f,
      -0.8274343547f,
      -0.542299559f,
      0.1458203587f,
      0f,
      -0.3485797732f,
      -0.415802277f,
      0.840000362f,
      0f,
      -0.2471778936f,
      -0.7304819962f,
      -0.6366310879f,
      0f,
      -0.3700154943f,
      0.8577948156f,
      0.3567584454f,
      0f,
      0.5913394901f,
      -0.548311967f,
      -0.5913303597f,
      0f,
      0.1204873514f,
      -0.7626472379f,
      -0.6354935001f,
      0f,
      0.616959265f,
      0.03079647928f,
      0.7863922953f,
      0f,
      0.1258156836f,
      -0.6640829889f,
      -0.7369967419f,
      0f,
      -0.6477565124f,
      -0.1740147258f,
      -0.7417077429f,
      0f,
      0.6217889313f,
      -0.7804430448f,
      -0.06547655076f,
      0f,
      0.6589943422f,
      -0.6096987708f,
      0.4404473475f,
      0f,
      -0.2689837504f,
      -0.6732403169f,
      -0.6887635427f,
      0f,
      -0.3849775103f,
      0.5676542638f,
      0.7277093879f,
      0f,
      0.5754444408f,
      0.8110471154f,
      -0.1051963504f,
      0f,
      0.9141593684f,
      0.3832947817f,
      0.131900567f,
      0f,
      -0.107925319f,
      0.9245493968f,
      0.3654593525f,
      0f,
      0.377977089f,
      0.3043148782f,
      0.8743716458f,
      0f,
      -0.2142885215f,
      -0.8259286236f,
      0.5214617324f,
      0f,
      0.5802544474f,
      0.4148098596f,
      -0.7008834116f,
      0f,
      -0.1982660881f,
      0.8567161266f,
      -0.4761596756f,
      0f,
      -0.03381553704f,
      0.3773180787f,
      -0.9254661404f,
      0f,
      -0.6867922841f,
      -0.6656597827f,
      0.2919133642f,
      0f,
      0.7731742607f,
      -0.2875793547f,
      -0.5652430251f,
      0f,
      -0.09655941928f,
      0.9193708367f,
      -0.3813575004f,
      0f,
      0.2715702457f,
      -0.9577909544f,
      -0.09426605581f,
      0f,
      0.2451015704f,
      -0.6917998565f,
      -0.6792188003f,
      0f,
      0.977700782f,
      -0.1753855374f,
      0.1155036542f,
      0f,
      -0.5224739938f,
      0.8521606816f,
      0.02903615945f,
      0f,
      -0.7734880599f,
      -0.5261292347f,
      0.3534179531f,
      0f,
      -0.7134492443f,
      -0.269547243f,
      0.6467878011f,
      0f,
      0.1644037271f,
      0.5105846203f,
      -0.8439637196f,
      0f,
      0.6494635788f,
      0.05585611296f,
      0.7583384168f,
      0f,
      -0.4711970882f,
      0.5017280509f,
      -0.7254255765f,
      0f,
      -0.6335764307f,
      -0.2381686273f,
      -0.7361091029f,
      0f,
      -0.9021533097f,
      -0.270947803f,
      -0.3357181763f,
      0f,
      -0.3793711033f,
      0.872258117f,
      0.3086152025f,
      0f,
      -0.6855598966f,
      -0.3250143309f,
      0.6514394162f,
      0f,
      0.2900942212f,
      -0.7799057743f,
      -0.5546100667f,
      0f,
      -0.2098319339f,
      0.85037073f,
      0.4825351604f,
      0f,
      -0.4592603758f,
      0.6598504336f,
      -0.5947077538f,
      0f,
      0.8715945488f,
      0.09616365406f,
      -0.4807031248f,
      0f,
      -0.6776666319f,
      0.7118504878f,
      -0.1844907016f,
      0f,
      0.7044377633f,
      0.312427597f,
      0.637304036f,
      0f,
      -0.7052318886f,
      -0.2401093292f,
      -0.6670798253f,
      0f,
      0.081921007f,
      -0.7207336136f,
      -0.6883545647f,
      0f,
      -0.6993680906f,
      -0.5875763221f,
      -0.4069869034f,
      0f,
      -0.1281454481f,
      0.6419895885f,
      0.7559286424f,
      0f,
      -0.6337388239f,
      -0.6785471501f,
      -0.3714146849f,
      0f,
      0.5565051903f,
      -0.2168887573f,
      -0.8020356851f,
      0f,
      -0.5791554484f,
      0.7244372011f,
      -0.3738578718f,
      0f,
      0.1175779076f,
      -0.7096451073f,
      0.6946792478f,
      0f,
      -0.6134619607f,
      0.1323631078f,
      0.7785527795f,
      0f,
      0.6984635305f,
      -0.02980516237f,
      -0.715024719f,
      0f,
      0.8318082963f,
      -0.3930171956f,
      0.3919597455f,
      0f,
      0.1469576422f,
      0.05541651717f,
      -0.9875892167f,
      0f,
      0.708868575f,
      -0.2690503865f,
      0.6520101478f,
      0f,
      0.2726053183f,
      0.67369766f,
      -0.68688995f,
      0f,
      -0.6591295371f,
      0.3035458599f,
      -0.6880466294f,
      0f,
      0.4815131379f,
      -0.7528270071f,
      0.4487723203f,
      0f,
      0.9430009463f,
      0.1675647412f,
      -0.2875261255f,
      0f,
      0.434802957f,
      0.7695304522f,
      -0.4677277752f,
      0f,
      0.3931996188f,
      0.594473625f,
      0.7014236729f,
      0f,
      0.7254336655f,
      -0.603925654f,
      0.3301814672f,
      0f,
      0.7590235227f,
      -0.6506083235f,
      0.02433313207f,
      0f,
      -0.8552768592f,
      -0.3430042733f,
      0.3883935666f,
      0f,
      -0.6139746835f,
      0.6981725247f,
      0.3682257648f,
      0f,
      -0.7465905486f,
      -0.5752009504f,
      0.3342849376f,
      0f,
      0.5730065677f,
      0.810555537f,
      -0.1210916791f,
      0f,
      -0.9225877367f,
      -0.3475211012f,
      -0.167514036f,
      0f,
      -0.7105816789f,
      -0.4719692027f,
      -0.5218416899f,
      0f,
      -0.08564609717f,
      0.3583001386f,
      0.929669703f,
      0f,
      -0.8279697606f,
      -0.2043157126f,
      0.5222271202f,
      0f,
      0.427944023f,
      0.278165994f,
      0.8599346446f,
      0f,
      0.5399079671f,
      -0.7857120652f,
      -0.3019204161f,
      0f,
      0.5678404253f,
      -0.5495413974f,
      -0.6128307303f,
      0f,
      -0.9896071041f,
      0.1365639107f,
      -0.04503418428f,
      0f,
      -0.6154342638f,
      -0.6440875597f,
      0.4543037336f,
      0f,
      0.1074204368f,
      -0.7946340692f,
      0.5975094525f,
      0f,
      -0.3595449969f,
      -0.8885529948f,
      0.28495784f,
      0f,
      -0.2180405296f,
      0.1529888965f,
      0.9638738118f,
      0f,
      -0.7277432317f,
      -0.6164050508f,
      -0.3007234646f,
      0f,
      0.7249729114f,
      -0.00669719484f,
      0.6887448187f,
      0f,
      -0.5553659455f,
      -0.5336586252f,
      0.6377908264f,
      0f,
      0.5137558015f,
      0.7976208196f,
      -0.3160000073f,
      0f,
      -0.3794024848f,
      0.9245608561f,
      -0.03522751494f,
      0f,
      0.8229248658f,
      0.2745365933f,
      -0.4974176556f,
      0f,
      -0.5404114394f,
      0.6091141441f,
      0.5804613989f,
      0f,
      0.8036581901f,
      -0.2703029469f,
      0.5301601931f,
      0f,
      0.6044318879f,
      0.6832968393f,
      0.4095943388f,
      0f,
      0.06389988817f,
      0.9658208605f,
      -0.2512108074f,
      0f,
      0.1087113286f,
      0.7402471173f,
      -0.6634877936f,
      0f,
      -0.713427712f,
      -0.6926784018f,
      0.1059128479f,
      0f,
      0.6458897819f,
      -0.5724548511f,
      -0.5050958653f,
      0f,
      -0.6553931414f,
      0.7381471625f,
      0.159995615f,
      0f,
      0.3910961323f,
      0.9188871375f,
      -0.05186755998f,
      0f,
      -0.4879022471f,
      -0.5904376907f,
      0.6429111375f,
      0f,
      0.6014790094f,
      0.7707441366f,
      -0.2101820095f,
      0f,
      -0.5677173047f,
      0.7511360995f,
      0.3368851762f,
      0f,
      0.7858573506f,
      0.226674665f,
      0.5753666838f,
      0f,
      -0.4520345543f,
      -0.604222686f,
      -0.6561857263f,
      0f,
      0.002272116345f,
      0.4132844051f,
      -0.9105991643f,
      0f,
      -0.5815751419f,
      -0.5162925989f,
      0.6286591339f,
      0f,
      -0.03703704785f,
      0.8273785755f,
      0.5604221175f,
      0f,
      -0.5119692504f,
      0.7953543429f,
      -0.3244980058f,
      0f,
      -0.2682417366f,
      -0.9572290247f,
      -0.1084387619f,
      0f,
      -0.2322482736f,
      -0.9679131102f,
      -0.09594243324f,
      0f,
      0.3554328906f,
      -0.8881505545f,
      0.2913006227f,
      0f,
      0.7346520519f,
      -0.4371373164f,
      0.5188422971f,
      0f,
      0.9985120116f,
      0.04659011161f,
      -0.02833944577f,
      0f,
      -0.3727687496f,
      -0.9082481361f,
      0.1900757285f,
      0f,
      0.91737377f,
      -0.3483642108f,
      0.1925298489f,
      0f,
      0.2714911074f,
      0.4147529736f,
      -0.8684886582f,
      0f,
      0.5131763485f,
      -0.7116334161f,
      0.4798207128f,
      0f,
      -0.8737353606f,
      0.18886992f,
      -0.4482350644f,
      0f,
      0.8460043821f,
      -0.3725217914f,
      0.3814499973f,
      0f,
      0.8978727456f,
      -0.1780209141f,
      -0.4026575304f,
      0f,
      0.2178065647f,
      -0.9698322841f,
      -0.1094789531f,
      0f,
      -0.1518031304f,
      -0.7788918132f,
      -0.6085091231f,
      0f,
      -0.2600384876f,
      -0.4755398075f,
      -0.8403819825f,
      0f,
      0.572313509f,
      -0.7474340931f,
      -0.3373418503f,
      0f,
      -0.7174141009f,
      0.1699017182f,
      -0.6756111411f,
      0f,
      -0.684180784f,
      0.02145707593f,
      -0.7289967412f,
      0f,
      -0.2007447902f,
      0.06555605789f,
      -0.9774476623f,
      0f,
      -0.1148803697f,
      -0.8044887315f,
      0.5827524187f,
      0f,
      -0.7870349638f,
      0.03447489231f,
      0.6159443543f,
      0f,
      -0.2015596421f,
      0.6859872284f,
      0.6991389226f,
      0f,
      -0.08581082512f,
      -0.10920836f,
      -0.9903080513f,
      0f,
      0.5532693395f,
      0.7325250401f,
      -0.396610771f,
      0f,
      -0.1842489331f,
      -0.9777375055f,
      -0.1004076743f,
      0f,
      0.0775473789f,
      -0.9111505856f,
      0.4047110257f,
      0f,
      0.1399838409f,
      0.7601631212f,
      -0.6344734459f,
      0f,
      0.4484419361f,
      -0.845289248f,
      0.2904925424f,
      0f
    )

    // Hashing
    private const val PRIME_X = 501125321
    private const val PRIME_Y = 1136930381
    private const val PRIME_Z = 1720413743
    private fun FastMin(a: Float, b: Float): Float {
      return if (a < b) a else b
    }

    private fun FastMax(a: Float, b: Float): Float {
      return if (a > b) a else b
    }

    private fun FastSqrt(f: Float): Float {
      return Math.sqrt(f.toDouble()).toFloat()
    }

    private fun FastFloor(
      f: Float
    ): Int {
      return if (f >= 0) f.toInt() else f.toInt() - 1
    }

    private fun FastRound(
      f: Float
    ): Int {
      return if (f >= 0) (f + 0.5f).toInt() else (f - 0.5f).toInt()
    }

    private fun Lerp(a: Float, b: Float, t: Float): Float {
      return a + t * (b - a)
    }

    private fun InterpHermite(t: Float): Float {
      return t * t * (3 - 2 * t)
    }

    private fun InterpQuintic(t: Float): Float {
      return t * t * t * (t * (t * 6 - 15) + 10)
    }

    private fun CubicLerp(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
      val p = d - c - (a - b)
      return t * t * t * p + t * t * (a - b - p) + t * (c - a) + b
    }

    private fun PingPong(t: Float): Float {
      var t = t
      t -= ((t * 0.5f).toInt() * 2).toFloat()
      return if (t < 1) t else 2 - t
    }

    private fun Hash(seed: Int, xPrimed: Int, yPrimed: Int): Int {
      var hash = seed xor xPrimed xor yPrimed
      hash *= 0x27d4eb2d
      return hash
    }

    private fun Hash(seed: Int, xPrimed: Int, yPrimed: Int, zPrimed: Int): Int {
      var hash = seed xor xPrimed xor yPrimed xor zPrimed
      hash *= 0x27d4eb2d
      return hash
    }

    private fun ValCoord(seed: Int, xPrimed: Int, yPrimed: Int): Float {
      var hash: Int = Hash(seed, xPrimed, yPrimed)
      hash *= hash
      hash = hash xor (hash shl 19)
      return hash * (1 / 2147483648.0f)
    }

    private fun ValCoord(seed: Int, xPrimed: Int, yPrimed: Int, zPrimed: Int): Float {
      var hash: Int = Hash(seed, xPrimed, yPrimed, zPrimed)
      hash *= hash
      hash = hash xor (hash shl 19)
      return hash * (1 / 2147483648.0f)
    }

    private fun GradCoord(seed: Int, xPrimed: Int, yPrimed: Int, xd: Float, yd: Float): Float {
      var hash: Int = Hash(seed, xPrimed, yPrimed)
      hash = hash xor (hash shr 15)
      hash = hash and (127 shl 1)
      val xg: Float = Gradients2D[hash]
      val yg: Float = Gradients2D[hash or 1]
      return xd * xg + yd * yg
    }

    private fun GradCoord(seed: Int, xPrimed: Int, yPrimed: Int, zPrimed: Int, xd: Float, yd: Float, zd: Float): Float {
      var hash: Int = Hash(seed, xPrimed, yPrimed, zPrimed)
      hash = hash xor (hash shr 15)
      hash = hash and (63 shl 2)
      val xg: Float = Gradients3D[hash]
      val yg: Float = Gradients3D[hash or 1]
      val zg: Float = Gradients3D[hash or 2]
      return xd * xg + yd * yg + zd * zg
    }

    private fun FastAbs(f: Float): Float {
      return if (f < 0) -f else f
    }
  }
}