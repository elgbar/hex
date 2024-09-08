package no.elg.hex.screens

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Scaling
import ktx.actors.setScrollFocus
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visScrollPane
import ktx.scene2d.vis.visTable
import no.elg.hex.Hex
import no.elg.hex.util.platformSpacing
import no.elg.hex.util.separator

class TutorialScreen : OverlayScreen(false) {

  init {
    fun findTutorialRegion(regionName: String): AtlasRegion {
      try {
        return Hex.assets.tutorialScreenShots.findRegion(regionName)
          ?: throw IllegalArgumentException("No sprite with the name $regionName is loaded. Loaded tutorial are ${Hex.assets.tutorialScreenShots.regions.map { it.name }}")
      } catch (e: GdxRuntimeException) {
        throw IllegalArgumentException("Failed to find loaded sprite $regionName.")
      }
    }

    stage.actors {
      val table = this@actors.visTable(defaultSpacing = true) {
        val superTable = this@visTable
        center()
        val padding = 0.01f
        val width = 0.95f
        pad(Value.percentWidth(padding, superTable))

        @Scene2dDsl
        fun KWidget<Cell<*>>.lab(text: String, regionName: String? = null) {
          this.visLabel(text) {
            setAlignment(Align.left)
            wrap = true
            it.width(Value.percentWidth(width, superTable))
            it.pad(2f)
            pack()
          }
          superTable.row()
          if (regionName != null) {
            visTable {
              this.visImage(findTutorialRegion(regionName)) {
                setScaling(Scaling.contain)
              }
            }
            superTable.row()
          }
        }

        visLabel("Tutorial", style = "h1") {
          setAlignment(Align.center)
          it.padTop(Value.Fixed(platformSpacing))
          it.width(Value.percentWidth(width, superTable))
        }

        superTable.row()
        lab("Upkeep per turn for each type", "costs")

        separator()

        lab(
          "When starting a new game, the island is divided between six players. " +
            "Two or more hexagons of the same color form a territory, each with a capital. " +
            "On your turn, each capital that has enough money to buy new peasants or castles will have a flag waving over it. " +
            "To select a territory, tap anywhere on one of its hexagons.",
          "tutorial-1"
        )
        lab(
          "Once you select a territory, you can view its economic situation in the Info Window at the top right of the screen. " +
            "This shows the territory's profit (or loss) and how much money the territory currently has. " +
            "You can buy peasants, costing 10 or castles, costing 15, if you have enough money. " +
            "Just tap on the icon in the lower left corner of the screen of the piece you want to buy.",
          "tutorial-2"
        )
        lab(
          "When you buy a peasant, you can place him on any empty hexagon in the territory you bought him from. He will jump up and down to show you that he can be picked " +
            "up and moved again. If you put him on a tree in his territory, he will chop the tree down and stop jumping to show that he has moved this turn and cannot be moved " +
            "again. Chopping a tree down is a good idea since you don't earn money for hexagons with trees on.",
          "tutorial-3"
        )
        lab(
          "You can capture hexagons adjacent to your territory as long as the hexagon isn't protected by an enemy piece. Pieces like capitals, castles, or peasants protect" +
            " all the hexagons in their territory that are immediately surrounding them. You can see which hexagons you can move a held piece to by the red outline around enemy" +
            " hexagons. Trees in your territory will not have this outline, but you can still move pieces onto them.",
          "tutorial-4"
        )
        lab(
          "If you capture a hexagon that joins two of your territories together, one of the capitals will be removed, and all of its money will be transferred into the" +
            " other capital. Joining two territories together is a good idea since the combined territory is stronger than the two smaller individual ones.",
          "tutorial-5"
        )
        lab(
          "You can create more powerful men by dropping one peasant on top of another. This will create a spearman who has enough strength to take a hexagon protected by" +
            " an enemy peasant or capital (or the hexagons that they are protecting). Adding another peasant to a spearman will create a knight who has the strength to kill" +
            " an enemy peasant, spearman, or castle (or to take the hexagons they are protecting). Adding another peasant to a knight creates a baron who has the strength to" +
            " kill anything except another baron or capture any hexagon except those protected by other barons.",
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
          "At the end of your turn, click the 'End Turn' button to allow the computer to make its moves. Trees will grow on your hexagons, earning you money for every hexagon " +
            "without a tree and costing you for each unit you own. Palms grow next to existing palms along the coast, and pine trees grow on hexagons surrounded by two or more " +
            "other pine trees."
        )

        addBackButton {
          it.fill(0.66f, it.fillY)
        }
      }

      visScrollPane {
        // remove the weird edge color
        this.style.background = null
        setScrollingDisabled(true, false)
        setFillParent(true)
        setFlickScroll(true)
        fadeScrollBars = false
        setScrollFocus(true)
        actor = table
      }
    }
  }
}