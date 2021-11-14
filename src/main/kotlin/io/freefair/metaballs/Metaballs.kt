package io.freefair.metaballs

import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL32.GL_PROGRAM_POINT_SIZE
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.math.pow


class Metaballs {
    // The window handle
    private var window: Long = 0
    fun run() {
        init()
        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(600, 600, "Metaballs", NULL, NULL)
        if (window == NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(
            window
        ) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(
                window,
                true
            ) // We will detect this in the rendering loop
        }
        stackPush().let { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode!!.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )

            stack.close()
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)
    }

    private fun loop() {
        GL.createCapabilities()
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        glScalef(1f/scaling,1f/scaling,1f/scaling)
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            draw()

            animate()

            glfwSwapBuffers(window)
            glfwPollEvents()
        }
    }

    private fun metaballFunction(xPos: Float, yPos: Float, xPoint: Float, yPoint: Float, size: Float): Float? {
        val dist = (xPoint - xPos).pow(2) + (yPoint - yPos).pow(2)
        if (dist > 1f || dist < -1f) return null
        return (1 - dist).pow(2)
    }

    val size = 1f
    val metaBalls: Array<Metaball> = arrayOf(
        Metaball(0.25f, 0.25f, size, 0.27f, 0.20f),
        Metaball(-0.25f, -0.25f, size, 0.12f, -0.21f),
        Metaball(0.25f, -0.25f, size, -0.19f, 0.13f),
        Metaball(-0.25f, 0.25f, size, -0.21f, -0.27f)
    )

    private val renderSize = 100
    private val scaling = 3.5f
    private val threshold: Float = 0.25f

    private var lastRender = System.currentTimeMillis()
    private fun animate() {
        val now = System.currentTimeMillis()
        val diffInSecs = (now - lastRender) / 1000f * scaling
        for (ball in metaBalls) {
            ball.x += ball.xForce * diffInSecs
            ball.y += ball.yForce * diffInSecs

            if(ball.x >= scaling) {
                ball.xForce = -ball.xForce
            } else if (ball.x <= -scaling) {
                ball.xForce = -ball.xForce
            }

            if(ball.y >= scaling) {
                ball.yForce = -ball.yForce
            } else if (ball.y <= -scaling) {
                ball.yForce = -ball.yForce
            }
        }
        lastRender = now
    }

    private fun draw() {
        glEnable(GL_PROGRAM_POINT_SIZE);
        glPointSize(5f)
        glColor3f(0.0f, 1.0f, 0.0f)
        glBegin(GL_POINTS)
        val loopSize = (renderSize * scaling).toInt()
        for(i in -loopSize..loopSize) {
            val xP = i / renderSize.toFloat()
            for(j in -loopSize..loopSize) {
                val yP = j / renderSize.toFloat()
                var sum = 0f
                for(ball in metaBalls) {
                    val metaballFunction = metaballFunction(ball.x, ball.y, xP, yP, ball.size) ?: continue
                    sum += metaballFunction
                    if(metaballFunction > threshold) {
                        glVertex2f(xP, yP)
                    }
                }
                if(sum > threshold) {
                    glVertex2f(xP, yP)
                }
            }
        }
        glEnd()
    }
}

