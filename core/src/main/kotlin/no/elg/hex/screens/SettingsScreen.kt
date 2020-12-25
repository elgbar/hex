package no.elg.hex.screens

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.SimpleFloatSpinnerModel
import ktx.actors.onChange
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.horizontalGroup
import ktx.scene2d.verticalGroup
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visCheckBox
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextField
import ktx.scene2d.vis.visValidatableTextField
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.util.onInteract
import no.elg.hex.util.toTitleCase
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties

object SettingsScreen : StageScreen() {

  private lateinit var previousScreen: AbstractScreen

  init {
    rootTable {

      verticalGroup {
        space(20f)
        left()
        fill()
        grow()
        @Scene2dDsl
        fun addSetting(property: KMutableProperty1<Settings, Any>) {
          val name = property.name.toTitleCase()
          val classifier = property.returnType.classifier
          when (classifier) {
            Boolean::class -> visCheckBox(name) {
              left()
              isChecked = property.get(Settings) as Boolean
              onChange {
                property.set(Settings, isChecked)
              }
            }
            String::class -> horizontalGroup {
              space(20f)
              left()
              visLabel(name) {
                style.fontColor = Color.WHITE
              }
              visTextField() {
                text = property.get(Settings).toString()
                onChange {
                  property.set(Settings, text)
                }
              }
            }
            Char::class -> horizontalGroup {
              space(20f)
              left()
              visLabel(name) { style.fontColor = Color.WHITE }

              visValidatableTextField {
                text = property.get(Settings).toString()
                addValidator { it.length <= 1 }
                onChange {
                  val char = text.lastOrNull() ?: return@onChange
                  if (isInputValid) {
                    property.set(Settings, char)
                  }
                }
              }
            }
            Byte::class -> spinner(name, IntSpinnerModel((property.get(Settings) as Byte).toInt(), Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt())) {
              onChange {
                property.set(Settings, (model as IntSpinnerModel).value.toByte())
              }
            }
            Short::class -> spinner(name, IntSpinnerModel((property.get(Settings) as Short).toInt(), Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())) {
              onChange {
                property.set(Settings, (model as IntSpinnerModel).value.toShort())
              }
            }
            Int::class -> spinner(name, IntSpinnerModel(property.get(Settings) as Int, Int.MIN_VALUE, Int.MAX_VALUE)) {
              onChange {
                property.set(Settings, (model as IntSpinnerModel).value)
              }
            }
            Long::class -> spinner(name, IntSpinnerModel((property.get(Settings) as Long).toInt(), Int.MIN_VALUE, Int.MAX_VALUE)) {
              onChange {
                property.set(Settings, (model as IntSpinnerModel).value.toLong())
              }
            }
            Float::class -> spinner(name, SimpleFloatSpinnerModel(property.get(Settings) as Float, Float.MIN_VALUE, Float.MAX_VALUE, 1f, 1000)) {
              onChange {
                property.set(Settings, (model as SimpleFloatSpinnerModel).value)
              }
            }
            Double::class ->
              spinner(
                name,
                FloatSpinnerModel(
                  property.get(Settings).toString(),
                  Double.MIN_VALUE.toString(),
                  Double.MAX_VALUE.toString(),
                  "1",
                  1000
                )
              ) {
                onChange {
                  property.set(Settings, (model as FloatSpinnerModel).value.toDouble())
                }
              }
            else -> error("The class $classifier is not yet supported as a settings")
          }
        }

        for (property in Settings::class.declaredMemberProperties) {
          require(property is KMutableProperty1) { "All settings properties must be mutable, ${property.name} is not mutable" }
          @Suppress("UNCHECKED_CAST")
          addSetting(property as KMutableProperty1<Settings, Any>)
        }
        visTextButton("Back") {
          onInteract(stage, intArrayOf(Keys.ESCAPE), intArrayOf(Keys.BACK)) {
            backToPreviousScreen()
          }
        }
      }
    }
  }

  private fun backToPreviousScreen() {
    require(Hex.screen === this) { "Settings screen is not currently being shown" }
    require(::previousScreen.isInitialized) { "Previous screen is not initialized" }
    Hex.screen = previousScreen
  }

  override fun show() {
    super.show()
    previousScreen = Hex.screen
    require(previousScreen !== this) { "Previous screen cannot be this" }
  }
}
