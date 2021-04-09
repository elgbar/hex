package no.elg.hex.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.SimpleFloatSpinnerModel
import ktx.actors.onChange
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visCheckBox
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextField
import ktx.scene2d.vis.visTextTooltip
import no.elg.hex.Settings
import no.elg.hex.util.delegate.PreferenceDelegate
import no.elg.hex.util.toTitleCase
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class SettingsScreen : OverlayScreen() {

  val onShowListener = mutableListOf<() -> Unit>()

  init {

    rootTable {
      center()
      pad(0f)

      @Scene2dDsl
      fun addSetting(property: KMutableProperty1<Settings, Any>, delegate: PreferenceDelegate<*>) {
        val name = property.name.toTitleCase()

        val restartLabel = VisLabel("*", Color.RED).apply {
          visTextTooltip("Restart the app to apply this change")
          isVisible = delegate.displayRestartWarning()
          onChange {
            isVisible = delegate.displayRestartWarning()
          }
        }

        fun commonStyle(it: Cell<*>) {
          it.right()
          it.pad(0f)
          it.space(20f)
//          it.expand()
        }

        when (val classifier = property.returnType.classifier) {
          Boolean::class -> visCheckBox("") {
            commonStyle(it)

            val readSetting: () -> Unit = { isChecked = property.get(Settings) as Boolean }
            onShowListener += readSetting
            readSetting()

            setProgrammaticChangeEvents(false)
            onChange {
              property.set(Settings, isChecked)
              isChecked = property.get(Settings) as Boolean
              restartLabel.fire(ChangeListener.ChangeEvent())
            }
          }

          String::class ->
            visTextField {
              commonStyle(it)

              val readSetting: () -> Unit = { text = property.get(Settings).toString() }
              onShowListener += readSetting
              readSetting()

              programmaticChangeEvents = false
              onChange {
                property.set(Settings, text)
                text = property.get(Settings).toString()
                restartLabel.fire(ChangeListener.ChangeEvent())
              }
            }

          Int::class ->
            spinner("", IntSpinnerModel(property.get(Settings) as Int, Int.MIN_VALUE, Int.MAX_VALUE)) {
              commonStyle(it)

              onShowListener += {
                (model as IntSpinnerModel).value = property.get(Settings) as Int
              }

              isProgrammaticChangeEvents = false
              onChange {
                val intModel = model as IntSpinnerModel
                property.set(Settings, intModel.value)
                intModel.value = property.get(Settings) as Int
                restartLabel.fire(ChangeListener.ChangeEvent())
              }
            }

          Float::class ->
            spinner("", SimpleFloatSpinnerModel(property.get(Settings) as Float, Float.MIN_VALUE, Float.MAX_VALUE, 1f, 1000)) {
              commonStyle(it)

              onShowListener += {
                (model as SimpleFloatSpinnerModel).value = property.get(Settings) as Float
              }

              isProgrammaticChangeEvents = false
              onChange {
                val floatModel = model as SimpleFloatSpinnerModel
                property.set(Settings, floatModel.value)
                floatModel.value = property.get(Settings) as Float
                restartLabel.fire(ChangeListener.ChangeEvent())
              }
            }

          Double::class ->
            spinner(
              "",
              FloatSpinnerModel(
                property.get(Settings).toString(),
                Double.MIN_VALUE.toString(),
                Double.MAX_VALUE.toString(),
                "1",
                1000
              )
            ) {
              commonStyle(it)

              onShowListener += {
                (model as FloatSpinnerModel).value = (property.get(Settings) as Double).toBigDecimal()
              }

              isProgrammaticChangeEvents = false
              onChange {
                val floatModel = model as FloatSpinnerModel
                property.set(Settings, floatModel.value.toDouble())
                floatModel.value = (property.get(Settings) as Double).toBigDecimal()
                restartLabel.fire(ChangeListener.ChangeEvent())
              }
            }

          else -> error("The class $classifier is not yet supported as a settings")
        }

        horizontalGroup {
          it.left()
          it.pad(0f)
          it.space(20f)
          // Show settings name after setting editor
          visLabel(name) {
            style.fontColor = Color.WHITE
          }
          addActor(restartLabel)
        }
        row()
      }

      for (
        (property, delegate) in Settings::class.declaredMemberProperties //
          .associateWith { it.also { it.isAccessible = true }.getDelegate(Settings) } //
          .filterValues { it is PreferenceDelegate<*> } //
          .toSortedMap { o1, o2 ->
            val delegate1Pri = (o1.getDelegate(Settings) as PreferenceDelegate<*>).priority
            val delegate2Pri = (o2.getDelegate(Settings) as PreferenceDelegate<*>).priority

            return@toSortedMap if (delegate1Pri == delegate2Pri) o1.name.compareTo(o2.name, true)
            else delegate1Pri - delegate2Pri
          } //
      ) {

        require(property is KMutableProperty1) { "All settings properties with delegates must be mutable, ${property.name} is not mutable" }

        println("${property.name}")

        @Suppress("UNCHECKED_CAST")
        addSetting(property as KMutableProperty1<Settings, Any>, delegate as PreferenceDelegate<*>)
      }
      addBackButton {
        it.colspan(2)
        it.prefWidth(Value.percentHeight(0.25f, this@rootTable))
        it.space(0f)
      }
      pack()
    }
  }

  override fun show() {
    super.show()
    for (function in onShowListener) {
      function()
    }
  }
}
