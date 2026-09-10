package com.blockforge.installer.saf

import com.blockforge.installer.model.PojavInstance
import org.json.JSONObject

/** Parses the Minecraft Java launcher profiles.json used by PojavLauncher. */
object PojavProfilesParser {
    fun parse(jsonText: String): List<PojavInstance> {
        val root = JSONObject(jsonText)
        val profiles = root.optJSONObject("profiles") ?: return emptyList()
        val result = mutableListOf<PojavInstance>()
        val keys = profiles.keys()

        while (keys.hasNext()) {
            val id = keys.next()
            val profile = profiles.optJSONObject(id) ?: continue
            result += PojavInstance(
                id = id,
                name = profile.optString("name").ifBlank { id },
                version = profile.optString("lastVersionId").takeIf { it.isNotBlank() },
                gameDir = profile.optString("gameDir").ifBlank { null }
            )
        }
        return result
    }
}
