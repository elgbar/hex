package no.elg.hex.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.utils.Align
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
import ktx.scene2d.vis.separator
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
import no.elg.hex.util.buttonPadding
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.delegate.PreferenceDelegate
import no.elg.hex.util.delegate.ResetSetting
import no.elg.hex.util.findEnumValues
import no.elg.hex.util.platformCheckBoxSize
import no.elg.hex.util.platformSpacing
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

          val minWidth = 0.4f

          val init: (@Scene2dDsl VisLabel).(Cell<*>) -> Unit = {
            it.minWidth(Value.percentWidth(minWidth, this@visTable))
            it.expandX()
            it.center()
            this.setAlignment(Align.center)
          }

          visLabel("Value", init = init)
          visLabel("Description", init = init)

          row()

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

          separator {
            it.expand()
            it.fillX()
            it.colspan(2)
          }

          for (property in otherProperties) {
            val name = property.name.toTitleCase()
            if (property.returnType.jvmErasure.isSubclassOf(ResetSetting::class)) {
              val delegate = property.get(Settings) as ResetSetting
              val confirmResetWindow = this@actors.confirmWindow(name, delegate.confirmText) { delegate.onResetConfirmed() }
              row()
              visTextButton(name, "dangerous") {
                settingsStyle(it)
                it.colspan(2)
                it.pad(buttonPadding)
                it.space(platformSpacing)
                onClick {
                  confirmResetWindow.show(stage)
                }
              }
            }
          }

          Hex.assets.version?.also { version ->
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
            it.prefWidth(Value.percentHeight(0.25f, this@visTable))
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
        commonStyle(this@addSetting.getCell(this))
        onShowListeners += {
          this.selected = property.get(Settings) as Enum<*>
        }
        onHideListeners += { delegate.hide(property) }

        property.set(Settings, this.selected)
        this.selected = property.get(Settings) as Enum<*>
        onChange = {
          property.set(Settings, this.selected)
          this.selected = property.get(Settings) as Enum<*>
          restartLabel.fire(ChangeEvent())
        }
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

          onChange { onChange() }
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

            onChange { onChange() }
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

            onChange { onChange() }
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

            onChange { onChange() }
          }

        else -> error("The class $clazz is not yet supported as a settings")
      }
    }

    horizontalGroup {
      onClick {
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

  private fun settingsStyle(cell: Cell<*>) {
    cell.pad(0f)
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
  }
}