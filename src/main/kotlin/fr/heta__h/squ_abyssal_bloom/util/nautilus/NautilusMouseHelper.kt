package fr.heta__h.squ_abyssal_bloom.util.nautilus

import net.minecraft.client.Minecraft
import net.minecraft.client.MouseHandler
import org.lwjgl.glfw.GLFW

object NautilusMouseHelper {
    private var lastMouseX: Double = -1.0
    private var lastMouseY: Double = -1.0
    private var savedTopPos: Int = 0
    private var shouldRestoreMouse: Boolean = false

    fun save(mouse: MouseHandler, topPos: Int) {
        lastMouseX = mouse.xpos()
        lastMouseY = mouse.ypos()
        savedTopPos = topPos
        shouldRestoreMouse = true
    }

    fun restore(newTopPos: Int) {
        if (shouldRestoreMouse && lastMouseX != -1.0) {
            val mc = Minecraft.getInstance()
            val guiScale = mc.window.guiScale
            val deltaY = (newTopPos - savedTopPos) * guiScale
            GLFW.glfwSetCursorPos(mc.window.handle(), lastMouseX, lastMouseY + deltaY)
            shouldRestoreMouse = false
        }
    }
}