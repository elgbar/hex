package no.elg.hex.screens

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
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.ExportedIslandData
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.delegate.PreferenceDelegate
import no.elg.hex.util.delegate.ResetSetting
import no.elg.hex.util.exportIsland
import no.elg.hex.util.findEnumValues
import no.elg.hex.util.importIslands
import no.elg.hex.util.padAndSpace
import no.elg.hex.util.platformButtonPadding
import no.elg.hex.util.platformCheckBoxSize
import no.elg.hex.util.platformSpacing
import no.elg.hex.util.playClick
import no.elg.hex.util.playMoney
import no.elg.hex.util.separator
import no.elg.hex.util.show
import no.elg.hex.util.toTitleCase
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

class SettingsScreen : OverlayScreen() {

  private val onShowListeners = mutableListOf<() -> Unit>()
  private val onHideListeners = mutableListOf<() -> Unit>()

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
            it.also { it.isAccessible = true }.getDelegate(Settings) is PreferenceDelegate<*>
          }

          val settings = settingsProperties.associateWith { it.getDelegate(Settings) } //
            .filterValues { it is PreferenceDelegate<*> && !it.shouldHide() } //
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
              playClick()
              val progress = Hex.assets.islandFiles.islandIds
                .mapNotNull(FastIslandMetadata::loadProgress)
                .map(::exportIsland)
              if (progress.isEmpty()) {
                MessagesRenderer.publishWarning("No island progress found to export, try playing some islands first")
                return@onClick
              }
              if (Hex.platform.writeToClipboard("Hex island export", progress)) {
                MessagesRenderer.publishMessage("Copied the progress of ${progress.size} islands to clipboard")
              }
              playMoney()
            }
          }

          row()
          visTextButton("Import islands", "export") {
            otherSettingsStyle(it)
            onClick {
              playClick()
              val clipboardText = Hex.platform.readFromClipboard()
              if (clipboardText == null) {
                MessagesRenderer.publishWarning("No valid text found in clipboard")
                return@onClick
              }

              val progress = try {
                Hex.mapper.readValue(clipboardText, islandsExportType)
              } catch (e: Exception) {
                MessagesRenderer.publishError("Invalid island data found in clipboard")
                return@onClick
              }
              if (progress.isEmpty()) {
                MessagesRenderer.publishWarning("No islands found in clipboard")
                return@onClick
              }

              importIslands(progress)
            }
          }
          row()

          separator()

          for (property in otherProperties) {
            val name = property.name.toTitleCase()
            if (property.returnType.jvmErasure.isSubclassOf(ResetSetting::class)) {
              val delegate = property.get(Settings) as ResetSetting
              val confirmResetWindow = this@actors.confirmWindow(name, delegate.confirmText) {
                delegate.onResetConfirmed()
                MessagesRenderer.publishWarning("Resetting $name")
                Hex.screen = SplashScreen(LevelSelectScreen())
              }
              row()
              visTextButton(name, "dangerous") {
                otherSettingsStyle(it)
                onClick {
                  confirmResetWindow.show(stage)
                }
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
          }
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
    val onChange: () -> Unit
    if (clazz.java.isEnum) {
      val enumValues = findEnumValues(clazz as KClass<out Enum<*>>).sortedBy { it.ordinal }.toGdxArray()
      visSelectBox<Enum<*>> {
        this.items = enumValues
        val cell = this@addSetting.getCell(this)
        cell.prefWidth(Value.percentHeight(0.15f, this@addSetting))
        cell.prefHeight(Value.percentHeight(0.03f, this@addSetting))
        commonStyle(cell, false)

        onShowListeners += {
          this.selected = property.get(Settings) as Enum<*>
        }
        onHideListeners += { delegate.hide(property) }

        onChange = {
          property.set(Settings, this.selected)
          restartLabel.fire(ChangeEvent())
        }

        onClick { playClick() }

        onChange {
          onChange()
        }
      }
    } else {
      when (clazz) {
        Boolean::class -> visCheckBox("") {
          commonStyle(it, false)
          cells.get(0)?.also { cell ->
            cell.size(platformCheckBoxSize)
          }

          val readSetting: () -> Unit = { isChecked = property.get(Settings) as Boolean }
          onShowListeners += readSetting
          readSetting()

          onHideListeners += { delegate.hide(property) }

          setProgrammaticChangeEvents(false)
          onChange = {
            val wasChecked = property.get(Settings) as Boolean
            if (isChecked != wasChecked) {
              property.set(Settings, isChecked)
            } else {
              property.set(Settings, !wasChecked)
            }
            isChecked = property.get(Settings) as Boolean
            restartLabel.fire(ChangeEvent())
          }

          onChange {
            playClick()
            onChange()
          }
        }

        String::class ->
          visTextField {
            commonStyle(it)

            val readSetting: () -> Unit = {
              text = property.get(Settings).toString()

              val chars =
                (max(text.length, (delegate.initialValue as CharSequence).length) / 2f + 1).coerceAtLeast(
                  MIN_FIELD_WIDTH
                )
              it.minWidth(Hex.assets.fontSize * chars)
            }
            onShowListeners += readSetting
            readSetting()

            onHideListeners += { delegate.hide(property) }

            programmaticChangeEvents = false
            onChange = {
              property.set(Settings, text)
              text = property.get(Settings).toString()
              restartLabel.fire(ChangeEvent())
            }

            onChange {
              playClick()
              onChange()
            }
          }

        Int::class ->
          spinner("", IntSpinnerModel(property.get(Settings) as Int, Int.MIN_VALUE, Int.MAX_VALUE)) {
            commonStyle(it)

            onShowListeners += {
              (model as IntSpinnerModel).value = property.get(Settings) as Int
            }

            onHideListeners += { delegate.hide(property) }

            isProgrammaticChangeEvents = false
            onChange = {
              val intModel = model as IntSpinnerModel
              property.set(Settings, intModel.value)
              intModel.value = property.get(Settings) as Int
              restartLabel.fire(ChangeEvent())
            }

            onChange {
              playClick()
              onChange()
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

            onShowListeners += {
              (model as FloatSpinnerModel).value = (property.get(Settings) as Float).toBigDecimal()
            }

            onHideListeners += { delegate.hide(property) }

            isProgrammaticChangeEvents = false
            onChange = {
              val floatModel = model as FloatSpinnerModel
              property.set(Settings, floatModel.value.toFloat())
              floatModel.value = (property.get(Settings) as Float).toBigDecimal()
              restartLabel.fire(ChangeEvent())
            }

            onChange {
              playClick()
              onChange()
            }
          }

        else -> error("The class $clazz is not yet supported as a settings")
      }
    }

    horizontalGroup {
      onClick {
        playClick()
        onChange()
      }
      settingsStyle(it)
      it.minWidth(Value.percentWidth(minWidth, this@addSetting))

      // Show settings name after setting editor
      visLabel(name) {
        style.fontColor = Color.WHITE
      }

      addActor(restartLabel)
    }
    row()
  }

  @Scene2dDsl
  private fun settingsStyle(cell: Cell<*>) {
    cell.pad(platformButtonPadding)
    cell.space(platformSpacing)
    cell.expandX()
  }

  override fun show() {
    super.show()
    for (function in onShowListeners) {
      function()
    }
  }

  override fun hide() {
    super.hide()
    for (function in onHideListeners) {
      function()
    }
  }

  companion object {
    const val MIN_FIELD_WIDTH = 5f

    val islandsExportType: TypeReference<List<ExportedIslandData>> = object : TypeReference<List<ExportedIslandData>>() {}
  }
}