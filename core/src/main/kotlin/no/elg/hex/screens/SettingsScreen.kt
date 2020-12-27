package no.elg.hex.screens

import com.badlogic.gdx.Gdx
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
import no.elg.hex.hud.MessagesRenderer.publishError
import no.elg.hex.screens.SplashScreen.refreshAndSetScreen
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
              setProgrammaticChangeEvents(false)
              onChange {
                property.set(Settings, isChecked)
                isChecked = property.get(Settings) as Boolean
              }
            }
            String::class -> horizontalGroup {
              space(20f)
              left()
              visLabel(name) {
                style.fontColor = Color.WHITE
              }
              visTextField {
                text = property.get(Settings).toString()
                programmaticChangeEvents = false
                onChange {
                  property.set(Settings, text)
                  text = property.get(Settings).toString()
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
                programmaticChangeEvents = false
                onChange {
                  val char = text.lastOrNull() ?: return@onChange
                  if (isInputValid) {
                    property.set(Settings, char)
                    text = property.get(Settings).toString()
                  }
                }
              }
            }
            Byte::class -> spinner(name, IntSpinnerModel((property.get(Settings) as Byte).toInt(), Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt())) {
              isProgrammaticChangeEvents = false
              onChange {
                val intModel = model as IntSpinnerModel
                property.set(Settings, intModel.value.toByte())
                intModel.value = (property.get(Settings) as Byte).toInt()
              }
            }
            Short::class -> spinner(name, IntSpinnerModel((property.get(Settings) as Short).toInt(), Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())) {
              isProgrammaticChangeEvents = false
              onChange {
                val intModel = model as IntSpinnerModel
                property.set(Settings, intModel.value.toShort())
                intModel.value = (property.get(Settings) as Short).toInt()
              }
            }
            Int::class -> spinner(name, IntSpinnerModel(property.get(Settings) as Int, Int.MIN_VALUE, Int.MAX_VALUE)) {
              isProgrammaticChangeEvents = false
              onChange {
                val intModel = model as IntSpinnerModel
                property.set(Settings, intModel.value)
                intModel.value = property.get(Settings) as Int
              }
            }
            Long::class -> spinner(name, IntSpinnerModel((property.get(Settings) as Long).toInt(), Int.MIN_VALUE, Int.MAX_VALUE)) {
              isProgrammaticChangeEvents = false
              onChange {
                val intModel = model as IntSpinnerModel
                property.set(Settings, intModel.value.toLong())
                intModel.value = (property.get(Settings) as Long).toInt()
              }
            }
            Float::class -> spinner(name, SimpleFloatSpinnerModel(property.get(Settings) as Float, Float.MIN_VALUE, Float.MAX_VALUE, 1f, 1000)) {
              isProgrammaticChangeEvents = false
              onChange {
                val floatModel = model as SimpleFloatSpinnerModel
                property.set(Settings, floatModel.value)
                floatModel.value = property.get(Settings) as Float
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
                isProgrammaticChangeEvents = false
                onChange {
                  val floatModel = model as FloatSpinnerModel
                  property.set(Settings, floatModel.value.toDouble())
                  floatModel.value = (property.get(Settings) as Double).toBigDecimal()
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
    if (Hex.screen !== this) {
      publishError("Settings screen is not currently being shown")
      return
    } else if (!::previousScreen.isInitialized) {
      publishError("Previous screen is not initialized")
      Hex.screen = LevelSelectScreen
      return
    }

    refreshAndSetScreen(previousScreen)
  }

  override fun show() {
    super.show()
    previousScreen = Hex.screen
    Gdx.app.debug("SETTINGS", "Previous screen is ${previousScreen::class.simpleName}")
    if (previousScreen === this) {
      publishError("Previous screen cannot be this Setting screen")
      return
    }
  }

  override fun hide() = Unit
}
