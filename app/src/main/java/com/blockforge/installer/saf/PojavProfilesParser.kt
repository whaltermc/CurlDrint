package com.blockforge.installer.saf

import com.blockforge.installer.model.PojavInstance
import org.json.JSONObject

/** Parses .minecraft/launcher_profiles.json used by Pojav-style launchers. */
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
                name = profile.optString("name").ifBlank { "Unnamed instance" },
                version = profile.optString("lastVersionId").ifBlank { null },
                gameDir = profile.optString("gameDir").ifBlank { null },
                iconData = profile.optString("icon").ifBlank { null }
            )
        }

        return result.sortedBy { it.name.lowercase() }
    }
}
