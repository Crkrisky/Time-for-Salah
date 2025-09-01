package com.crk.timeforsalah.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Simple, offline City Picker:
 * - Searchable input
 * - List filters by city or country (case-insensitive)
 * - Returns the selected city via onPick(cityName, countryName)
 *
 * You can replace SAMPLE_CITIES with a real dataset later or GPS reverse geocoding.
 */
@Composable
fun CityPickerScreen(
    onPick: (city: String, country: String) -> Unit,
    onBack: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        SAMPLE_CITIES.filter {
            val q = query.trim().lowercase()
            if (q.isEmpty()) true
            else it.city.lowercase().contains(q) || it.country.lowercase().contains(q)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose City",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search city or country") }
            )

            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(filtered, key = { it.key }) { item ->
                    CityRow(
                        city = item.city,
                        country = item.country,
                        onClick = { onPick(item.city, item.country) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun CityRow(
    city: String,
    country: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = city, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = country, style = MaterialTheme.typography.bodySmall)
    }
}

/* ---- Sample data (replace later with a real list) ---- */

data class CityItem(val key: String, val city: String, val country: String)

val SAMPLE_CITIES = listOf(
    CityItem("karachi_pk", "Karachi", "Pakistan"),
    CityItem("lahore_pk", "Lahore", "Pakistan"),
    CityItem("islamabad_pk", "Islamabad", "Pakistan"),
    CityItem("riyadh_sa", "Riyadh", "Saudi Arabia"),
    CityItem("makkah_sa", "Makkah", "Saudi Arabia"),
    CityItem("madinah_sa", "Madinah", "Saudi Arabia"),
    CityItem("dubai_ae", "Dubai", "United Arab Emirates"),
    CityItem("doha_qa", "Doha", "Qatar"),
    CityItem("istanbul_tr", "Istanbul", "Türkiye"),
    CityItem("cairo_eg", "Cairo", "Egypt"),
    CityItem("jakarta_id", "Jakarta", "Indonesia"),
    CityItem("kuala_lumpur_my", "Kuala Lumpur", "Malaysia"),
    CityItem("london_uk", "London", "United Kingdom"),
    CityItem("new_york_us", "New York", "United States"),
    CityItem("toronto_ca", "Toronto", "Canada")
)