package com.example.interstellarcalc.data

// Object types returned by SIMBAD — maps to illustration type
enum class UniverseObjectType {
    STAR, BLACK_HOLE, GALAXY_SPIRAL, GALAXY_ELLIPTICAL, GALAXY_IRREGULAR,
    NEBULA_EMISSION, NEBULA_PLANETARY, NEBULA_SUPERNOVA,
    CLUSTER_OPEN, CLUSTER_GLOBULAR, QUASAR, PULSAR, UNKNOWN
}

data class UniverseObject(
    val id              : String,         // SIMBAD main identifier
    val name            : String,         // display name
    val objectType      : UniverseObjectType,
    val rawType         : String,         // raw SIMBAD type string e.g. "Galaxy", "Star"
    val ra              : String,         // right ascension
    val dec             : String,         // declination
    val redshift        : Double?,        // z
    val parallaxMas     : Double?,        // parallax in mas
    val distanceLy      : Double?,        // derived from parallax or redshift
    val bMagnitude      : Double?,        // B band magnitude
    val vMagnitude      : Double?,        // V band magnitude
    val spectralType    : String?,        // spectral class if available
    val nasaImageUrl    : String?         // populated by NasaImageRepository
)

// Category filter shown as chips on the search screen
enum class UniverseCategory(val label: String, val simbadTypes: List<String>) {
    ALL       ("All",           listOf()),
    STARS     ("Stars",         listOf("Star", "StarInCluster", "Variable*", "RotV*", "Pulsar", "Neutron*", "WhiteDwarf*", "Nova", "Supernova")),
    GALAXIES  ("Galaxies",      listOf("Galaxy", "GinGroup", "GinPair", "GinCl", "AGN", "Seyfert", "Blazar", "QSO")),
    NEBULAE   ("Nebulae",       listOf("PN", "HII", "SNR", "Nebula", "EmObj", "ISM")),
    CLUSTERS  ("Clusters",      listOf("Cl*", "GlCl", "OpCl", "Association*")),
    BLACK_HOLES("Black Holes",  listOf("BH", "XB*", "LMXB", "HMXB")),
    OTHER     ("Other",         listOf())
}

data class SearchState(
    val query      : String                  = "",
    val category   : UniverseCategory        = UniverseCategory.ALL,
    val results    : List<UniverseObject>    = emptyList(),
    val isLoading  : Boolean                 = false,
    val errorMsg   : String?                 = null,
    val hasSearched: Boolean                 = false
)
