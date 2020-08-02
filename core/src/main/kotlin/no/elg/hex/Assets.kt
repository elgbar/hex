package no.elg.hex

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP_PINGPONG
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter
import com.badlogic.gdx.utils.Array


/**
 * @author Elg
 */
class Assets : AssetManager() {

  var finishMainConst: Boolean = false
    private set

  companion object {

    private val TEXTURE_ATLAS = TextureAtlas::class.java
    private val MUSIC = Music::class.java
    private val SOUND = Sound::class.java
    private val BITMAP_FONT = BitmapFont::class.java
    private val FREE_TYPE_FONT_GEN = FreeTypeFontGenerator::class.java

    const val ISLAND_SAVES_DIR = "islands"
    const val ISLAND_FILE_ENDING = "is"

    const val SPRITE_ATLAS = "sprites/sprites.atlas"
    const val ORIGINAL_SPRITES_ATLAS = "sprites/original_sprites.atlas"

    const val BOLD_FONT = "fonts/UbuntuMono-B.ttf"
    const val BOLD_ITALIC_FONT = "fonts/UbuntuMono-BI.ttf"
    const val REGULAR_FONT = "fonts/UbuntuMono-R.ttf"
    const val REGULAR_ITALIC_FONT = "fonts/UbuntuMono-RI.ttf"

    const val FONT_SIZE = 20
  }

  val boldFont: BitmapFont by lazy { get<BitmapFont>(BOLD_FONT) }
  val boldItalicFont: BitmapFont by lazy { get<BitmapFont>(BOLD_ITALIC_FONT) }
  val regularFont: BitmapFont by lazy { get<BitmapFont>(REGULAR_FONT) }
  val regularItalicFont: BitmapFont by lazy { get<BitmapFont>(REGULAR_ITALIC_FONT) }

  val sprites: TextureAtlas by lazy { get<TextureAtlas>(SPRITE_ATLAS) }
  val originalSprites: TextureAtlas by lazy { get<TextureAtlas>(ORIGINAL_SPRITES_ATLAS) }

  private fun findSprite(regionName: String): AtlasRegion {
    val region = if (Hex.args.retro) {
      Hex.assets.originalSprites.findRegion(regionName)
    } else {
      Hex.assets.sprites.findRegion(regionName) ?: Hex.assets.originalSprites.findRegion(regionName)
    }
    region.flip(false, true)
    return region
  }

  private fun findAnimation(regionPrefix: String, totalFrames: Int, frameDuration: Float): Animation<AtlasRegion> {
    val array = Array<AtlasRegion>()
    for (frame in 0 until totalFrames) {
      array.add(findSprite(regionPrefix + frame))
    }

    return Animation(frameDuration, array, LOOP_PINGPONG)
  }

  val pine by lazy { findSprite("pine") }
  val palm by lazy { findSprite("palm") }
  val capital by lazy { findSprite("village") }
  val capitalFlag by lazy { findAnimation("village", 8, 1 / 10f) }
  val castle by lazy { findSprite("castle") }
  val grave by lazy { findSprite("grave") }
  val peasant by lazy { findAnimation("man0", 5, 1 / 17f) }
  val spearman by lazy { findAnimation("man1", 5, 1 / 15f) }
  val knight by lazy { findAnimation("man2", 5, 1 / 17f) }
  val baron by lazy { findAnimation("man3", 5, 1 / 10f) }

  init {
    super.setErrorListener { _, throwable -> throwable.printStackTrace() }
    val resolver = InternalFileHandleResolver()

    setLoader(BITMAP_FONT, ".ttf", FreetypeFontLoader(resolver))
    setLoader(FREE_TYPE_FONT_GEN, FreeTypeFontGeneratorLoader(resolver))

    fun font(bold: Boolean, italic: Boolean) {
      val boldness = if (bold) "B" else "R"
      val italicness = if (italic) "I" else ""

      val parameter = FreeTypeFontLoaderParameter()
      parameter.fontParameters.size = FONT_SIZE * Hex.scale
      parameter.fontParameters.minFilter = Linear
      parameter.fontFileName = "fonts/UbuntuMono-$boldness$italicness.ttf"
      load(parameter.fontFileName, BITMAP_FONT, parameter)
    }

    font(bold = false, italic = false)

    //essential assets for the loading splash screen

    // assets above this line must be loaded before the splash screen is shown. Keep it to a minimum
    finishLoading()

    //rest of the fonts
    font(bold = false, italic = true)
    font(bold = true, italic = false)
    font(bold = true, italic = true)

    load(SPRITE_ATLAS, TEXTURE_ATLAS)
    load(ORIGINAL_SPRITES_ATLAS, TEXTURE_ATLAS)

  }

  fun finishMain() {
    finishMainConst = true
  }

}
