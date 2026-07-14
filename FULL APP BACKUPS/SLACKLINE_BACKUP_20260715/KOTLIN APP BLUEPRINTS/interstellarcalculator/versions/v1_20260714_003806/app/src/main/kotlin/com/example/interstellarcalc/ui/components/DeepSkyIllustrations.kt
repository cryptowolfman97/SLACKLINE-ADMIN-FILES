package com.example.interstellarcalc.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DeepSkyIllustration(category: String, objectType: String, size: Dp = 180.dp, modifier: Modifier = Modifier) {
    when (category) {
        "BLACK_HOLE" -> when {
            objectType.contains("Stellar", true)      -> StarIllustration(com.example.interstellarcalc.data.StarType.NEUTRON_STAR, size, modifier)
            objectType.contains("Intermediate", true) -> BlackHoleIllustration(com.example.interstellarcalc.data.BlackHoleType.INTERMEDIATE, size, modifier)
            else                                       -> BlackHoleIllustration(com.example.interstellarcalc.data.BlackHoleType.SUPERMASSIVE, size, modifier)
        }
        "STAR" -> when {
            objectType.contains("Pulsar", true) || objectType.contains("Magnetar", true) || objectType.contains("Neutron", true) ->
                StarIllustration(com.example.interstellarcalc.data.StarType.NEUTRON_STAR, size, modifier)
            objectType.contains("White Dwarf", true) ->
                StarIllustration(com.example.interstellarcalc.data.StarType.WHITE_DWARF, size, modifier)
            objectType.contains("Red Dwarf", true) ->
                StarIllustration(com.example.interstellarcalc.data.StarType.RED_DWARF, size, modifier)
            objectType.contains("Red", true) || objectType.contains("Hypergiant", true) || objectType.contains("Supergiant", true) && objectType.contains("M", true) ->
                StarIllustration(com.example.interstellarcalc.data.StarType.RED_GIANT, size, modifier)
            objectType.contains("Blue", true) || objectType.contains("O", true) || objectType.contains("B", true) ->
                StarIllustration(com.example.interstellarcalc.data.StarType.BLUE_SUPERGIANT, size, modifier)
            else ->
                StarIllustration(com.example.interstellarcalc.data.StarType.YELLOW_DWARF, size, modifier)
        }
        "GALAXY" -> when {
            objectType.contains("Elliptical", true) ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.GALAXY_ELLIPTICAL, size, modifier)
            objectType.contains("Irregular", true) || objectType.contains("Starburst", true) || objectType.contains("Interacting", true) ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.GALAXY_IRREGULAR, size, modifier)
            else ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.GALAXY_SPIRAL, size, modifier)
        }
        "NEBULA" -> when {
            objectType.contains("Planetary", true) ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.NEBULA_PLANETARY, size, modifier)
            objectType.contains("Supernova", true) || objectType.contains("Remnant", true) ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.NEBULA_SUPERNOVA, size, modifier)
            else ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.NEBULA_EMISSION, size, modifier)
        }
        "GLOBULAR_CLUSTER" ->
            UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.CLUSTER_GLOBULAR, size, modifier)
        "OPEN_CLUSTER" ->
            UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.CLUSTER_OPEN, size, modifier)
        "PLANET", "MOON" ->
            StarIllustration(com.example.interstellarcalc.data.StarType.RED_GIANT, size, modifier)
        "OTHER" -> when {
            objectType.contains("Quasar", true) || objectType.contains("Blazar", true) ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.QUASAR, size, modifier)
            else ->
                UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.UNKNOWN, size, modifier)
        }
        else ->
            UniverseIllustration(com.example.interstellarcalc.data.UniverseObjectType.UNKNOWN, size, modifier)
    }
}
