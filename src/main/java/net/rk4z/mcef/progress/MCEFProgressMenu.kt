package net.rk4z.mcef.progress

import net.rk4z.mcef.MCEF
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class MCEFProgressMenu(brand: String) : Screen(Text.literal("$brand is downloading required libraries...")) {

    override fun render(graphics: DrawContext, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val cx = width / 2.0
        val cy = height / 2.0

        val progressBarHeight = 14.0
        val progressBarWidth = width / 3.0

        val poseStack = graphics.matrices

        /* Draw Progress Bar */
        poseStack.push()
        poseStack.translate(cx, cy, 0.0)
        poseStack.translate(-progressBarWidth / 2.0, -progressBarHeight / 2.0, 0.0)
        graphics.fill( // bar border
            0, 0,
            progressBarWidth.toInt(),
            progressBarHeight.toInt(),
            -1
        )
        graphics.fill( // bar padding
            2, 2,
            progressBarWidth.toInt() - 2,
            progressBarHeight.toInt() - 2,
            -16777215
        )
        graphics.fill( // bar
            4, 4,
            ((progressBarWidth - 4) * MCEF.resourceManager!!.progressTracker.progress).toInt(),
            progressBarHeight.toInt() - 4,
            -1
        )
        poseStack.pop()

        // putting this here incase I want to re-add a third line later on
        // allows me to generalize the code to not care about line count
        val text = arrayOf(
            MCEF.resourceManager!!.progressTracker.task,
            Math.round(MCEF.resourceManager!!.progressTracker.progress * 100).toString() + "%"
        )

        /* Draw Text */
        // calculate offset for the top line
        val oSet = ((textRenderer.fontHeight / 2) + (textRenderer.fontHeight + 2) * (text.size + 2)) + 4
        poseStack.push()
        poseStack.translate(
            cx,
            (cy - oSet),
            0.0
        )
        // draw menu name
        graphics.drawText(
            textRenderer,
            Formatting.GOLD.toString() + title.string,
            -(textRenderer.getWidth(title.string) / 2.0).toInt(),
            -textRenderer.fontHeight - 2,
            0xFFFFFF,
            true
        )
        // draw text
        for ((index, s) in text.withIndex()) {
            if (index == 1) {
                poseStack.translate(0.0, (textRenderer.fontHeight + 2).toDouble(), 0.0)
            }

            poseStack.translate(0.0, (textRenderer.fontHeight + 2).toDouble(), 0.0)
            graphics.drawText(
                textRenderer,
                s,
                -(textRenderer.getWidth(s) / 2.0).toInt(), 0,
                0xFFFFFF,
                false
            )
        }
        poseStack.pop()
    }

    override fun tick() {
        if (MCEF.resourceManager!!.progressTracker.isDone) {
            close()
            client!!.setScreen(TitleScreen())
        }
    }

    override fun shouldCloseOnEsc(): Boolean {
        return false
    }

    override fun shouldPause(): Boolean {
        return true
    }
}