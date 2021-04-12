package no.elg.hex

import com.badlogic.gdx.Application.ApplicationType.Android
import com.badlogic.gdx.Application.ApplicationType.Desktop
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.PixmapIO
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
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.TimeUtils
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.VisUI.SkinScale.X1
import com.kotcrab.vis.ui.VisUI.SkinScale.X2
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.style.imageTextButton
import ktx.style.label
import ktx.style.menu
import ktx.style.menuItem
import ktx.style.set
import ktx.style.textButton
import ktx.style.textField
import ktx.style.visCheckBox
import ktx.style.visImageTextButton
import ktx.style.visTextButton
import ktx.style.visTextField
import ktx.style.window
import no.elg.hex.Hex.scale
import no.elg.hex.assets.IslandAsynchronousAssetLoader
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.island.IslandFiles
import no.elg.hex.util.debug
import no.elg.hex.util.defaultDisplayWidth
import no.elg.hex.util.trace
import com.badlogic.gdx.utils.Array as GdxArray

/** @author Elg */
class Assets : AssetManager() {

  var mainFinishedLoading: Boolean = false
    private set

  var loadingInfo = "not begun"
    private set

  companion object {

    private val TEXTURE_ATLAS = TextureAtlas::class.java
    private val MUSIC = Music::class.java
    private val SOUND = Sound::class.java
    private val PNG = PixmapIO.PNG::class.java
    private val BITMAP_FONT = BitmapFont::class.java
    private val FREE_TYPE_FONT_GEN = FreeTypeFontGenerator::class.java

    private val MIN_ISLAND =
      """{"width":3,"height":3,"layout":"HEXAGONAL","hexagonData":{
        |"1,1":{"@id":1,"team":"STONE"},
        |"1,0":{"@id":2,"edge":true,"isOpaque":true,"isPassable":false,"team":"SUN"},
        |"2,1":2,"2,0":2,"0,2":2,"0,1":2,"1,2":2},
        |"selectedCoordinate":null,"piece":null}""".replaceIndentByMargin("")

    const val ISLAND_SAVES_DIR = "islands"
    const val ISLAND_PREVIEWS_DIR = "$ISLAND_SAVES_DIR/previews"
    const val ISLAND_FILE_ENDING = "is"

    const val SPRITE_ATLAS = "sprites/sprites.atlas"
    const val TUTORIAL_ATLAS = "sprites/tutorial.atlas"

    const val BOLD_FONT = "fonts/UbuntuMono-B.ttf"
    const val BOLD_ITALIC_FONT = "fonts/UbuntuMono-BI.ttf"
    const val REGULAR_FONT = "fonts/UbuntuMono-R.ttf"
    const val REGULAR_ITALIC_FONT = "fonts/UbuntuMono-RI.ttf"

    private const val FONT_SIZE = 20

    private val FALLBACK_FONT by lazy {
      BitmapFont(false)
    }

    val nativeScale: Int by lazy {
      val size = when (Gdx.app.type) {
        Desktop -> defaultDisplayWidth
        Android -> Gdx.graphics.backBufferHeight * 2
        else -> Gdx.graphics.backBufferWidth
      }
      Gdx.app.debug("SCALE", "Screen size: $size")
      (size / 1920).coerceAtLeast(1)
    }
  }

  val regularFont: BitmapFont by lazy { if (isLoaded(REGULAR_FONT)) get(REGULAR_FONT) else FALLBACK_FONT }
  val regularItalicFont: BitmapFont by lazy { get(REGULAR_ITALIC_FONT) }
  val boldFont: BitmapFont by lazy { get(BOLD_FONT) }
  val boldItalicFont: BitmapFont by lazy { get(BOLD_ITALIC_FONT) }

  private val sprites: TextureAtlas by lazy { get(SPRITE_ATLAS) }
  val tutorialScreenShots: TextureAtlas by lazy { get(TUTORIAL_ATLAS) }

  val fontSize by lazy { FONT_SIZE * scale }

  private val resolver: FileHandleResolver

  private fun findSprite(regionName: String): AtlasRegion {
    val region: AtlasRegion = try {
      Hex.assets.sprites.findRegion(regionName)
    } catch (e: GdxRuntimeException) {
      throw IllegalArgumentException("Failed to find loaded sprite $regionName")
    } ?: throw IllegalArgumentException("No sprite with the name $regionName is loaded. Loaded are ${Hex.assets.sprites.regions.map { it.name }}")

    require(region.originalHeight == region.originalWidth) {
      "Different originalWidth and originalHeight for region $region, width: ${region.originalWidth}, height ${region.originalHeight}"
    }
    require(region.packedWidth == region.packedHeight) {
      "Different packedWidth and packedHeight for region $region, width: ${region.packedWidth}, height ${region.packedHeight}"
    }
    region.flip(false, true)
    return region
  }

  private fun findAnimation(
    regionPrefix: String,
    totalFrames: Int,
    frameDuration: Float
  ): Animation<AtlasRegion> {
    val array = GdxArray<AtlasRegion>()
    for (frame in 0 until totalFrames) {
      array.add(findSprite(regionPrefix + frame))
    }

    return Animation(frameDuration, array, LOOP_PINGPONG)
  }

  val hand by lazy { findSprite("hand") }
  val background by lazy { findSprite("slay_game_background") }
  val pine by lazy { findSprite("pine") }
  val palm by lazy { findSprite("palm") }
  val capital by lazy { findSprite("village") }
  val capitalFlag by lazy { findAnimation("village", 8, 1 / 10f) }
  val castle by lazy { findSprite("castle") }
  val grave by lazy { findSprite("grave") }
  val peasant by lazy { findAnimation("man0", 5, 1 / 17f) }
  val spearman by lazy { findAnimation("man1", 5, 1 / 15f) }
  val knight by lazy { findAnimation("man2", 5, 1 / 15f) }
  val baron by lazy { findAnimation("man3", 5, 1 / 10f) }

  val surrender by lazy { findSprite("surrender") }
  val undo by lazy { findSprite("undo") }
  val redo by lazy { findSprite("redo") }
  val undoAll by lazy { findSprite("undo_all") }
  val settings by lazy { findSprite("settings") }
  val settingsDown by lazy { findSprite("settings_selected") }
  val help by lazy { findSprite("help") }
  val helpDown by lazy { findSprite("help_selected") }

  fun loadFont(bold: Boolean, italic: Boolean, flip: Boolean = true, fontSize: Int = this.fontSize) {
    val boldness = if (bold) "B" else "R"
    val italicness = if (italic) "I" else ""

    val parameter = FreeTypeFontLoaderParameter()
    parameter.fontParameters.size = fontSize
    parameter.fontParameters.minFilter = Linear
    parameter.fontParameters.flip = flip
    parameter.fontFileName = "fonts/UbuntuMono-$boldness$italicness.ttf"
    val name = fontName(bold, italic, flip, fontSize)

    Gdx.app.debug("ASSET", "loading font '$name'")
    if (fileHandleResolver.resolve(parameter.fontFileName).exists()) {
      load(name, BITMAP_FONT, parameter)
    } else {
      Gdx.app.log("ASSET", "Failed to find font file for '$name'")
    }
  }

  init {
    Gdx.app.debug("ASSET", "Using ${scale}x scale")

    KtxAsync.launch(Hex.asyncThread) {
      val warmUpStart = TimeUtils.millis()
      for (i in 1..100) {
        val deserStart = TimeUtils.millis()
        Island.deserialize(MIN_ISLAND)
        Gdx.app.trace("WARMUP") { "Warmup pass $i took ${TimeUtils.timeSinceMillis(deserStart)} ms" }
      }
      Gdx.app.debug("WARMUP") { "Island deserialization warmup complete in ${TimeUtils.timeSinceMillis(warmUpStart)} ms" }
    }

    super.setErrorListener { asset, throwable ->
      MessagesRenderer.publishError("Failed to load ${asset.type.simpleName} asset ${asset.fileName}")
      throwable.printStackTrace()
    }
    resolver = InternalFileHandleResolver()

    setLoader(BITMAP_FONT, ".ttf", FreetypeFontLoader(resolver))
    setLoader(FREE_TYPE_FONT_GEN, FreeTypeFontGeneratorLoader(resolver))

    loadFont(bold = false, italic = false)

    // essential assets for the loading splash screen

    // assets above this line must be loaded before the splash screen is shown. Keep it to a minimum
    finishLoading()
  }

  fun loadAssets() {

    setLoader(Island::class.java, ".$ISLAND_FILE_ENDING", IslandAsynchronousAssetLoader(resolver))
    setLoader(Island::class.java, IslandAsynchronousAssetLoader(resolver))

    loadingInfo = "fonts"
    // rest of the fonts
    loadFont(bold = false, italic = true)
    loadFont(bold = true, italic = false)
    loadFont(bold = true, italic = true)
    loadFont(bold = false, italic = false, flip = false)
    loadFont(bold = false, italic = true, flip = false)
    loadFont(bold = true, italic = false, flip = false)
    loadFont(bold = true, italic = true, flip = false)

    loadingInfo = "VisUI"

    if (!VisUI.isLoaded()) {
      // skip version check as there is a bug in Gdx Version 1.9.12
      // see https://github.com/libgdx/libgdx/commit/091f3bac7fc68ce96732897a5fcd80578bdba893#r43758450
      VisUI.setSkipGdxVersionCheck(true)
      if (scale > 1) VisUI.load(X2) else VisUI.load(X1)
    }
    with(VisUI.getSkin() as Skin) {
      val notFlippedFont = getFont(bold = false, italic = false, flip = false)
      val boldNotFlippedFont = getFont(bold = false, italic = false, flip = false)

      this["default-font"] = notFlippedFont

      label(extend = "default") { font = notFlippedFont }
      label(extend = "link-label") { font = notFlippedFont }
      label(extend = "small") { font = notFlippedFont }
      label(extend = "menuitem-shortcut") { font = notFlippedFont }

      visTextField(extend = "default") { font = notFlippedFont }
      textField(extend = "default") { font = notFlippedFont }

      visTextButton(extend = "default") { font = notFlippedFont }
      visTextButton(extend = "menu-bar") { font = notFlippedFont }
      visTextButton(extend = "toggle") { font = notFlippedFont }
      visTextButton(extend = "blue") { font = notFlippedFont }

      visCheckBox(extend = "default") { font = notFlippedFont }

      textButton(extend = "default") { font = notFlippedFont }

      val newOpenButtonStyle = visImageTextButton(extend = "default") { font = notFlippedFont }
      visImageTextButton(extend = "menu-bar") { font = notFlippedFont }
      imageTextButton(extend = "default") { font = notFlippedFont }

      window(extend = "default") { titleFont = boldNotFlippedFont }
      window(extend = "resizable") { titleFont = boldNotFlippedFont }
      window(extend = "noborder") { titleFont = boldNotFlippedFont }
      window(extend = "dialog") { titleFont = boldNotFlippedFont }

      menuItem(extend = "default") { font = notFlippedFont }
      menu { openButtonStyle = newOpenButtonStyle }
    }

    loadingInfo = "sprites"

    load(SPRITE_ATLAS, TEXTURE_ATLAS)
    load(TUTORIAL_ATLAS, TEXTURE_ATLAS)

    loadingInfo = "islands"

    IslandFiles // find all island files
  }

  fun getFont(bold: Boolean, italic: Boolean, flip: Boolean = true, fontSize: Int = this.fontSize): BitmapFont {
    return finishLoadingAsset(fontName(bold, italic, flip, fontSize))
  }

  private fun fontName(bold: Boolean, italic: Boolean, flip: Boolean = true, fontSize: Int = this.fontSize): String {
    val boldness = if (bold) "B" else "R"
    val italicness = if (italic) "I" else ""
    val flippiness = if (flip) "" else "-NF"
    val sizeiness = if (fontSize != this.fontSize) "-s$fontSize" else ""
    return "fonts/UbuntuMono-$boldness$italicness$flippiness$sizeiness.ttf"
  }

  fun finishMain() {
    Gdx.app.trace("ASSET", "Main finished")
    mainFinishedLoading = true
  }

  override fun unload(fileName: String?) {
    if (isLoaded(fileName)) {
      super.unload(fileName)
    }
  }
}
