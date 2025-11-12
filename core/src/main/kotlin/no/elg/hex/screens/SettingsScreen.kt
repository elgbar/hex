package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.utils.Align
import com.fasterxml.jackson.core.type.TypeReference
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.setScrollFocus
import ktx.collections.toGdxArray
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visCheckBox
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visScrollPane
import ktx.scene2d.vis.visSelectBox
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextField
import ktx.scene2d.vis.visTextTooltip
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.event.events.SettingsChangeEvent
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.ExportedIsland
import no.elg.hex.util.compressB85
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.delegate.PreferenceDelegate
import no.elg.hex.util.delegate.ResetSetting
import no.elg.hex.util.exportIsland
import no.elg.hex.util.findEnumValues
import no.elg.hex.util.importIslands
import no.elg.hex.util.info
import no.elg.hex.util.padAndSpace
import no.elg.hex.util.platformButtonPadding
import no.elg.hex.util.platformCheckBoxSize
import no.elg.hex.util.platformSpacing
import no.elg.hex.util.playClick
import no.elg.hex.util.playMoney
import no.elg.hex.util.safeGetDelegate
import no.elg.hex.util.separator
import no.elg.hex.util.show
import no.elg.hex.util.toTitleCase
import no.elg.hex.util.tryDecompressB85
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.system.measureTimeMillis

class SettingsScreen : OverlayScreen() {

  private val delegateToReadSettings: MutableMap<PreferenceDelegate<*>, () -> Unit> = mutableMapOf()
  private val onSettingChange = SimpleEventListener.create<SettingsChangeEvent<*>> { (delegate, _, _) ->
    delegateToReadSettings[delegate]?.invoke()
  }

  private var allowedToPlayClick = false

  private fun tryPlayClick() {
    if (allowedToPlayClick) {
      playClick()
    }
  }

  init {
    stage.actors {

      visScrollPane {

        // remove the weird edge color
        this.style.background = null
        setFillParent(true)
        setFlickScroll(true)
        setScrollbarsVisible(false)
        setClamp(false)
        setForceScroll(false, true)
        setupFadeScrollBars(0f, 0f)
        setScrollFocus(true)
        actor = visTable {
          pad(10f)

          // allow scroll past back button
          padBottom(this.prefHeight)

          val init: (@Scene2dDsl VisLabel).(Cell<*>) -> Unit = {
            it.minWidth(Value.percentWidth(0.4f, this@visTable))
            it.expandX()
            it.center()
            this.setAlignment(Align.center)
          }

          visLabel("Value", init = init)
          visLabel("Description", init = init)

          row()
          separator()

          val (settingsProperties, otherProperties) = Settings::class.declaredMemberProperties.partition {
            it.safeGetDelegate(Settings) is PreferenceDelegate<*>
          }

          val settings = settingsProperties.associateWith { it.getDelegate(Settings) }
            .filterValues { it is PreferenceDelegate<*> && !it.shouldHide() }
            .toSortedMap { o1, o2 ->
              val delegate1Pri = (o1.getDelegate(Settings) as PreferenceDelegate<*>).priority
              val delegate2Pri = (o2.getDelegate(Settings) as PreferenceDelegate<*>).priority

              return@toSortedMap if (delegate1Pri == delegate2Pri) {
                o1.name.compareTo(o2.name, true)
              } else {
                delegate1Pri - delegate2Pri
              }
            }
          for ((property, delegate) in settings) {
            require(property is KMutableProperty1) { "All settings properties with delegates must be mutable, ${property.name} is not mutable" }

            @Suppress("UNCHECKED_CAST")
            addSetting(property as KMutableProperty1<Settings, Any>, delegate as PreferenceDelegate<*>)
          }

          separator()

          @Scene2dDsl
          fun otherSettingsStyle(cell: Cell<*>) {
            settingsStyle(cell)
            padAndSpace(cell)
            cell.colspan(2)
            cell.prefWidth(Value.percentHeight(0.25f, this@visTable))
            cell.prefHeight(Value.percentHeight(0.03f, this@visTable))
          }

          row()
          visTextButton("Export islands", "export") {
            otherSettingsStyle(it)
            onClick {
              tryPlayClick()
              val exportMs = measureTimeMillis {
                val progress = Hex.assets.islandFiles.islandIds
                  .mapNotNull(FastIslandMetadata::loadProgress)
                  .map(::exportIsland)
                  .toTypedArray() // must be array to save type correctly

                if (progress.isEmpty()) {
                  MessagesRenderer.publishWarning("No island progress found to export, try playing some islands first")
                  return@onClick
                }
                val data = Hex.mapper.writeValueAsString(progress)
                val compressedData = if (Settings.compressExport) compressB85(data) ?: data else data
                if (Hex.platform.writeToClipboard("Hex island export", compressedData)) {
                  MessagesRenderer.publishMessage("Copied the progress of ${progress.size} islands to clipboard")
                }
              }
              Gdx.app.info("EXPORT") { "Exported all islands in $exportMs ms" }
              playMoney()
            }
            pack()
          }

          row()
          visTextButton("Import islands", "export") {
            otherSettingsStyle(it)
            onClick {
              tryPlayClick()
              val clipboardText = Hex.platform.readStringFromClipboard()?.let(::tryDecompressB85)

              if (clipboardText == null) {
                MessagesRenderer.publishWarning("No valid text found in clipboard")
                return@onClick
              }

              val progress = try {
                Hex.mapper.readValue(clipboardText, islandsExportType)
              } catch (e: Exception) {
                MessagesRenderer.publishError("Invalid island data found in clipboard")
                Gdx.app.error("SettingsScreen", "Failed to parse islands from clipboard", e)
                return@onClick
              }
              if (progress.isEmpty()) {
                MessagesRenderer.publishWarning("No islands found in clipboard")
                return@onClick
              }

              importIslands(progress.toList())
            }
            pack()
          }
          row()

          separator()

          for (property in otherProperties) {
            val name = property.name.toTitleCase()
            if (property.returnType.jvmErasure.isSubclassOf(ResetSetting::class)) {
              val delegate = property.get(Settings) as ResetSetting
              val confirmResetWindow = this@actors.confirmWindow(name, delegate.confirmText) {
                MessagesRenderer.publishWarning("Resetting $name")
                delegate.onResetConfirmed()
              }
              row()
              visTextButton(name, "dangerous") {
                otherSettingsStyle(it)
                onClick {
                  tryPlayClick()
                  confirmResetWindow.show(stage)
                }
                pack()
              }
            }
          }

          Hex.platform.version?.also { version ->
            row()
            visLabel("Version: $version") {
              it.colspan(2)
              it.center()
              color = Color.LIGHT_GRAY
            }
          }

          row()
          addBackButton {
            it.colspan(2)
            it.prefWidth(Value.percentHeight(0.2f, this@visTable))
            it.prefHeight(Value.percentHeight(0.05f, this@visTable))
            it.space(0f)
            pack()
          }
          pack()
        }
      }
    }
  }

  private fun commonStyle(it: Cell<*>, includeWidth: Boolean = true) {
    it.right()
    it.pad(0f)
    it.space(20f)
    if (includeWidth) {
      it.minWidth(Hex.assets.fontSize * MIN_FIELD_WIDTH)
    }
  }

  @Scene2dDsl
  fun KVisTable.addSetting(property: KMutableProperty1<Settings, Any>, delegate: PreferenceDelegate<*>) {
    val name = property.name.toTitleCase()

    val restartLabel = VisLabel("*", Color.RED).apply {
      visTextTooltip("Restart the app to apply this change")
      isVisible = delegate.displayRestartWarning()
      onChange {
        isVisible = delegate.displayRestartWarning()
      }
    }

    val clazz = delegate.initialValue::class
    val writeSetting: () -> Unit
    val readSetting: () -> Unit

    if (clazz.java.isEnum) {
      @Suppress("UNCHECKED_CAST")
      val enumValues = findEnumValues(clazz as KClass<out Enum<*>>).sortedBy { it.ordinal }.toGdxArray()
      visSelectBox<Enum<*>> {
        this.items = enumValues
        val cell = this@addSetting.getCell(this)
        cell.prefWidth(Value.percentHeight(0.15f, this@addSetting))
        cell.prefHeight(Value.percentHeight(0.03f, this@addSetting))
        commonStyle(cell, false)

        readSetting = { this.selected = property.get(Settings) as Enum<*> }
        writeSetting = {
          property.set(Settings, this.selected)
          readSetting()
          restartLabel.fire(ChangeEvent())
        }

        onClick { tryPlayClick() }
      }
    } else {
      when (clazz) {
        Boolean::class -> visCheckBox("") {
          commonStyle(it, false)
          cells.get(0)?.also { cell ->
            cell.size(platformCheckBoxSize)
          }

          readSetting = { isChecked = property.get(Settings) as Boolean }

          setProgrammaticChangeEvents(false)
          writeSetting = {
            val wasChecked = property.get(Settings) as Boolean
            if (isChecked != wasChecked) {
              property.set(Settings, isChecked)
            } else {
              property.set(Settings, !wasChecked)
            }
            readSetting()
            restartLabel.fire(ChangeEvent())
          }
        }

        String::class ->
          visTextField {
            commonStyle(it)

            readSetting = {
              text = property.get(Settings).toString()

              val chars =
                (max(text.length, (delegate.initialValue as CharSequence).length) / 2f + 1).coerceAtLeast(
                  MIN_FIELD_WIDTH
                )
              it.minWidth(Hex.assets.fontSize * chars)
            }

            programmaticChangeEvents = false
            writeSetting = {
              property.set(Settings, text)
              readSetting()
              restartLabel.fire(ChangeEvent())
            }
          }

        Int::class, Long::class -> {
          fun propToInt(): Int = (property.get(Settings) as Number).toInt()
          spinner("", IntSpinnerModel(propToInt(), Int.MIN_VALUE, Int.MAX_VALUE)) {
            commonStyle(it)
            val intModel = model as IntSpinnerModel

            isProgrammaticChangeEvents = false
            readSetting = {
              intModel.value = propToInt()
            }
            writeSetting = {
              if (clazz == Long::class) {
                property.set(Settings, intModel.value.toLong())
              } else {
                property.set(Settings, intModel.value)
              }
              readSetting()
              restartLabel.fire(ChangeEvent())
            }
          }
        }

        Float::class, Double::class ->
          spinner(
            "",
            FloatSpinnerModel(
              property.get(Settings).toString(),
              Double.MIN_VALUE.toString(),
              Double.MAX_VALUE.toString(),
              "0.1",
              1000
            )
          ) {
            commonStyle(it)
            val floatModel = model as FloatSpinnerModel

            isProgrammaticChangeEvents = false
            readSetting = { floatModel.value = (property.get(Settings) as Float).toBigDecimal() }
            writeSetting = {
              property.set(Settings, floatModel.value.toFloat())
              readSetting()
              restartLabel.fire(ChangeEvent())
            }
          }

        else -> error("The class $clazz is not yet supported as a settings")
      }
    }.apply {
      // Common config
      onChange {
        tryPlayClick()
        writeSetting()
      }
      pack()
    }

    horizontalGroup {
      onClick {
        tryPlayClick()
        writeSetting()
      }
      settingsStyle(it)
      it.minWidth(Value.percentWidth(minWidth, this@addSetting))

      // Show settings name after setting editor
      visLabel(name) {
        style.fontColor = Color.WHITE
      }

      addActor(restartLabel)
      pack()
    }

    readSetting()
    delegateToReadSettings[delegate] = readSetting
    row()
    pack()
  }

  @Scene2dDsl
  private fun settingsStyle(cell: Cell<*>) {
    cell.pad(platformButtonPadding)
    cell.space(platformSpacing)
    cell.expandX()
  }

  override fun show() {
    super.show()
    for (function in delegateToReadSettings.values) {
      function()
    }
    allowedToPlayClick = true
  }

  override fun hide() {
    allowedToPlayClick = false
    super.hide()
    onSettingChange.dispose()
  }

  companion object {
    const val MIN_FIELD_WIDTH = 5f

    val islandsExportType: TypeReference<Array<ExportedIsland>> = object : TypeReference<Array<ExportedIsland>>() {}
  }
}