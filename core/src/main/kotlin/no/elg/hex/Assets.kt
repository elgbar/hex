package no.elg.hex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter


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

      println("Gdx.files.internal(ISLAND_SAVES_DIR).exists() = ${Gdx.files.internal(ISLAND_SAVES_DIR).list().map { it.name() }}")

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
