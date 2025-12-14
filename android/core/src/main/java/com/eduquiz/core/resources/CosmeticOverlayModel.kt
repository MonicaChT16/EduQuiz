package com.eduquiz.core.resources

import android.content.Context

private const val DRAWABLE_PREFIX = "drawable://"

/**
 * Resuelve el modelo que se le pasa a Coil (AsyncImage) para overlays de cosméticos.
 *
 * Convención soportada:
 * - `drawable://<nombre>` -> resuelve a `R.drawable.<nombre>` del paquete de la app (GIFs incluidos).
 * - cualquier otro valor -> se devuelve tal cual (URL remota u otros esquemas).
 */
fun resolveCosmeticOverlayModel(
    context: Context,
    overlayRef: String?
): Any? {
    val value = overlayRef?.trim().orEmpty()
    if (value.isEmpty()) return null

    if (value.startsWith(DRAWABLE_PREFIX)) {
        val rawName = value.removePrefix(DRAWABLE_PREFIX).trim()
        val name = rawName.substringBeforeLast('.')
        if (name.isBlank()) return null

        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) return null
        return resId
    }

    return value
}

