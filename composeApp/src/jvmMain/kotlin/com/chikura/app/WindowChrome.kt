package com.chikura.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Window as AwtWindow
import java.awt.image.BufferedImage

// Native decorated window + forced-dark titlebar + painted Vault C icon.
// Undecorated was tried: it kills Windows snap and shakes under manual drag.
// DWMWA_* via JNA keeps native snap/maximize while matching AMOLED chrome.

@Composable
fun rememberChikuraWindowIcon(): Painter {
    return remember {
        val px = 64
        val img = BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = java.awt.Color(255, 255, 255, 255)
        g.fillRoundRect(2, 2, 60, 60, 36, 36)
        g.color = java.awt.Color(0, 0, 0, 255)
        g.fillOval(13, 13, 38, 38)
        g.color = java.awt.Color(255, 255, 255, 255)
        g.fillOval(20, 20, 24, 24)
        g.fillPolygon(intArrayOf(32, 66, 66), intArrayOf(32, -2, 66), 3)
        g.color = java.awt.Color(0, 0, 0, 255)
        g.fillOval(27, 27, 10, 10)
        g.dispose()
        BitmapPainter(img.toComposeImageBitmap())
    }
}

private class HWND(p: Pointer?) : com.sun.jna.PointerType(p)

private interface Dwmapi : StdCallLibrary {
    fun DwmSetWindowAttribute(hwnd: HWND, dwAttribute: Int, pvAttribute: IntByReference, cbAttribute: Int): Int
}

private fun applyDark(awtWindow: AwtWindow) {
    try {
        if (!System.getProperty("os.name", "").startsWith("Windows")) return
        val dwm: Dwmapi = Native.load("dwmapi", Dwmapi::class.java)
        val hwnd = HWND(Native.getWindowPointer(awtWindow))
        dwm.DwmSetWindowAttribute(hwnd, 20, IntByReference(1), 4) // DWMWA_USE_IMMERSIVE_DARK_MODE
        dwm.DwmSetWindowAttribute(hwnd, 35, IntByReference(0x0A0A0A), 4) // DWMWA_CAPTION_COLOR #0A0A0A
    } catch (_: Exception) {
    }
}

@Composable
fun ApplyWindowsDarkChrome(window: AwtWindow) {
    DisposableEffect(window) {
        applyDark(window)
        val listener = object : java.awt.event.WindowAdapter() {
            override fun windowOpened(e: java.awt.event.WindowEvent) = applyDark(window)
        }
        window.addWindowListener(listener)
        onDispose { window.removeWindowListener(listener) }
    }
}
