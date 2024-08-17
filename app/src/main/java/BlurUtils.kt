package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

object BlurUtils {
    fun blur(context: Context, image: Bitmap, radius: Float): Bitmap {
        val outputBitmap = Bitmap.createBitmap(image)
        val renderScript = RenderScript.create(context)
        val input = Allocation.createFromBitmap(renderScript, image)
        val output = Allocation.createFromBitmap(renderScript, outputBitmap)
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, input.element)

        scriptIntrinsicBlur.setInput(input)
        scriptIntrinsicBlur.setRadius(radius)
        scriptIntrinsicBlur.forEach(output)
        output.copyTo(outputBitmap)

        renderScript.destroy()

        return outputBitmap
    }
}
