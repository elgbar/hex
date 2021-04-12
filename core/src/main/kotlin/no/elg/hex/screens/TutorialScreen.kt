package no.elg.hex.screens

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.GdxRuntimeException
import ktx.actors.setScrollFocus
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.container
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visScrollPane
import ktx.scene2d.vis.visTable
import no.elg.hex.Hex

class TutorialScreen : OverlayScreen(false) {

  init {

    fun tutorialImage(regionName: String): AtlasRegion {
      try {
        return Hex.assets.tutorialScreenShots.findRegion(regionName)
          ?: throw IllegalArgumentException("No sprite with the name $regionName is loaded. Loaded tutorial are ${Hex.assets.tutorialScreenShots.regions.map { it.name }}")
      } catch (e: GdxRuntimeException) {
        throw IllegalArgumentException("Failed to find loaded sprite $regionName.")
      }
    }

    val border = TextureRegionDrawable(tutorialImage("border"))

    stage.actors {
      val table = this@actors.visTable(defaultSpacing = true) {
        top()
        left()
        pad(Value.percentWidth(0.01f, this@visTable))

        @Scene2dDsl
        fun KWidget<Cell<*>>.lab(text: String, regionName: String? = null, addBackground: Boolean = true, textAlign: Int = Align.left) {
          this.visLabel(text) {
            setAlignment(textAlign)
            wrap = true
            it.width(Value.percentWidth(0.97f, this@visTable))
            pack()
          }
          this@visTable.row()
          if (regionName != null) {
            visTable {
              if (addBackground) {
                background = border
              }
              this.visImage(tutorialImage(regionName)) {
                it.pad(2f)
              }
            }

            this@visTable.row()
          }
        }

        lab("Cost overview", "costs", false, Align.center)

        lab(
          "When you start a new game the island is divided up between the six players. Adjoining hexagons " +
            "of the same colour form territories and each territory which is larger than one hexagon has a Capital." +
            " On your turn each capital that has enough money to buy new peasants or castles will have a flag waving" +
            " over it. To select a territory just tap anywhere on one of its hexagons.",
          "tutorial-1"
        )
        lab(
          "When you select a territory you will see its economic situation in the Info window at the top" +
            " right of the screen. This shows the territory's profit (or loss) and how much money the territory" +
            " currently has. You can buy things (10 for a peasant, 15 for a castle) if you have enough money. Just" +
            " tap on the peasant or castle icon that you want to buy.",
          "tutorial-2"
        )
        lab(
          "Once you have bought a peasant you can tap any empty hexagon in the territory that you bought him" +
            " from to place him. He will jump up and down to show you that he can be picked up and moved again. If you" +
            " put him on to a tree in his territory he will chop the tree down and stop jumping to show that he has" +
            " moved this turn and cannot be moved again. Chopping a tree down is a good idea as you don't earn money" +
            " for hexagons with trees on.",
          "tutorial-3"
        )
        lab(
          "You can capture hexagons adjacent to your territory as long as the hexagon isn't protected by an" +
            " enemy piece. Pieces (capitals, castles or peasants) protect all the hexagons in their territory that are" +
            " immediately surrounding them. You can see what hexagons you can move a held piece by the red outline around" +
            " enemy hexagons. Trees in your territory will not have this outline, but can always be moved onto.",
          "tutorial-4"
        )
        lab(
          "If you capture a hexagon which joins two of your territories together one of the capitals will be" +
            " removed and all of its money will be transferred into the other capital. Joining two territories " +
            "together is a very good idea as the combined territory is stronger than the two smaller individual ones.",
          "tutorial-5"
        )
        lab(
          "You can make more powerful men by dropping one peasant on top of another. This will make a spearman" +
            " who has enough strength to take a hexagon protected by an enemy peasant or capital (or the hexagons that" +
            " they are protecting). Adding another peasant to a spearman will make a knight who has the strength to" +
            " kill an enemy peasant, spearman or castle (or to take the hexagons they are protecting). Adding another" +
            " peasant to a knight makes a baron who has the strength to kill anything except another baron or capture" +
            " any hexagon except those protected by other barons.",
          "tutorial-6"
        )
        lab(
          "Combining men to make more powerful ones has one big disadvantage - they are much more expensive" +
            " to keep. A peasant costs 2 per turn, a spearman 6, a knight 18 and a baron 54. If at the beginning of" +
            " your turn any of your territories can't afford to pay for all the men in it they will all die and become" +
            " gravestones. For this reason it's a good idea to cut enemy territories in half so that part of them goes" +
            " bankrupt.",
          "tutorial-7"
        )
        lab(
          text =
          "When you have finished buying, moving and attacking click on the 'End Turn' button. The computers" +
            " will make their moves and when it comes around to your turn again trees will grow on your hexagons (palm" +
            " trees grow along the coast next to existing palm trees and pine trees grow on hexagons surrounded by two" +
            " or more other pine trees) and each territory will earn money. A territory earns one for every hexagon" +
            " that doesn't have a tree, and pays for the cost of people that it has."
        )

        container {
          addBackButton()
          center()
          it.minWidth(Value.percentWidth(0.97f, this@visTable))
        }
      }

      visScrollPane {
        // remove the weird edge color
        this.style.background = null

        setFillParent(true)
        setFlickScroll(true)
        fadeScrollBars = false
        setScrollFocus(true)
        actor = table
      }
    }
  }
}
