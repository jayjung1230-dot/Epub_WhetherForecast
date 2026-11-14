package com.ridipaper.weather.data

import android.content.Context
import com.ridipaper.weather.model.CityPreset
import java.io.BufferedReader
import java.io.InputStreamReader

object RegionStore {

    private var tree: LinkedHashMap<String, LinkedHashMap<String, MutableList<CityPreset>>>? = null

    @Synchronized
    private fun ensureLoaded(context: Context): LinkedHashMap<String, LinkedHashMap<String, MutableList<CityPreset>>> {
        tree?.let { return it }
        val map = LinkedHashMap<String, LinkedHashMap<String, MutableList<CityPreset>>>()
        try {
            context.assets.open("kr_regions.tsv").use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.split('\t')
                        if (parts.size >= 5) {
                            val sido = parts[0]
                            val sigungu = parts[1]
                            val dong = parts[2]
                            val lat = parts[3].toDoubleOrNull()
                            val lon = parts[4].toDoubleOrNull()
                            if (lat != null && lon != null) {
                                val fullName = if (sido == sigungu) "$sido $dong" else "$sido $sigungu $dong"
                                val sigunguMap = map.getOrPut(sido) { LinkedHashMap() }
                                val dongList = sigunguMap.getOrPut(sigungu) { ArrayList() }
                                dongList.add(CityPreset(fullName, lat, lon))
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (_: Exception) {
        }
        tree = map
        return map
    }

    fun getSidos(context: Context): List<String> = ensureLoaded(context).keys.toList()

    fun getSigungus(context: Context, sido: String): List<String> =
        ensureLoaded(context)[sido]?.keys?.toList() ?: emptyList()

    fun getDongs(context: Context, sido: String, sigungu: String): List<CityPreset> =
        ensureLoaded(context)[sido]?.get(sigungu) ?: emptyList()
}
