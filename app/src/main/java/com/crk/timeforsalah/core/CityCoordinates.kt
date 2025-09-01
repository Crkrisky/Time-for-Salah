package com.crk.timeforsalah.core

import java.time.ZoneId

/**
 * Minimal, offline city → (lat, lon, zone) resolver.
 * This matches the sample cities you already show in Settings.
 *
 * You can expand or replace this with a real database later.
 */
object CityCoordinates {

    data class Geo(val lat: Double, val lon: Double, val zone: ZoneId)

    // Key format: "City, Country" to match how we store in Settings.manualCity
    private val MAP: Map<String, Geo> = mapOf(
        "Karachi, Pakistan"            to Geo(24.8607, 67.0011, ZoneId.of("Asia/Karachi")),
        "Lahore, Pakistan"             to Geo(31.5204, 74.3587, ZoneId.of("Asia/Karachi")),
        "Islamabad, Pakistan"          to Geo(33.6844, 73.0479, ZoneId.of("Asia/Karachi")),

        "Riyadh, Saudi Arabia"         to Geo(24.7136, 46.6753, ZoneId.of("Asia/Riyadh")),
        "Makkah, Saudi Arabia"         to Geo(21.3891, 39.8579, ZoneId.of("Asia/Riyadh")),
        "Madinah, Saudi Arabia"        to Geo(24.5247, 39.5692, ZoneId.of("Asia/Riyadh")),

        "Dubai, United Arab Emirates"  to Geo(25.2048, 55.2708, ZoneId.of("Asia/Dubai")),
        "Doha, Qatar"                  to Geo(25.2854, 51.5310, ZoneId.of("Asia/Qatar")),
        "Istanbul, Türkiye"            to Geo(41.0082, 28.9784, ZoneId.of("Europe/Istanbul")),
        "Cairo, Egypt"                 to Geo(30.0444, 31.2357, ZoneId.of("Africa/Cairo")),

        "Jakarta, Indonesia"           to Geo( -6.2088,106.8456, ZoneId.of("Asia/Jakarta")),
        "Kuala Lumpur, Malaysia"       to Geo( 3.1390,101.6869, ZoneId.of("Asia/Kuala_Lumpur")),

        "London, United Kingdom"       to Geo(51.5074,  -0.1278, ZoneId.of("Europe/London")),
        "New York, United States"      to Geo(40.7128, -74.0060, ZoneId.of("America/New_York")),
        "Toronto, Canada"              to Geo(43.6532, -79.3832, ZoneId.of("America/Toronto"))
    )

    /**
     * Resolve a city label (e.g., "Karachi, Pakistan") to coordinates + time zone.
     * Falls back to device time zone and (0,0) if not found.
     */
    fun resolve(label: String): Geo {
        val cleaned = label.trim()
        MAP[cleaned]?.let { return it }

        // Try a tolerant match if the user saved only "Karachi" without country.
        val partial = MAP.entries.firstOrNull { (k, _) ->
            k.startsWith(cleaned, ignoreCase = true) || k.contains(", $cleaned", ignoreCase = true)
        }?.value
        if (partial != null) return partial

        // Fallback: (0,0) with device zone (still lets the app run)
        return Geo(0.0, 0.0, ZoneId.systemDefault())
    }
}
