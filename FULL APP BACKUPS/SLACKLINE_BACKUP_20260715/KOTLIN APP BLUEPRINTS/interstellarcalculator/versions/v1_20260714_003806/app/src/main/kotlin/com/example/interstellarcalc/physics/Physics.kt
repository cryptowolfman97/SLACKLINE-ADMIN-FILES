package com.example.interstellarcalc.physics

import kotlin.math.*

const val G_TO_LY_YR2 = 1.03227
const val G_STANDARD  = 9.80665
const val C_MS        = 2.998e8
const val C_SQ        = C_MS * C_MS
const val SCHW_FACTOR = 2 * 6.674e-11 / (C_MS * C_MS)

// ── Existing results ──────────────────────────────────────────────────────────

data class RocketResult(
    val shipTimeYears  : Double,
    val earthTimeYears : Double,
    val peakBeta       : Double,
    val peakGamma      : Double,
    val dilationRatio  : Double,
    val massRatioOneWay: Double,
    val massRatioBrach : Double
)

fun computeRocket(distanceLy: Double, accelG: Double, brach: Boolean, coastFrac: Double = 0.0): RocketResult {
    val a      = accelG * G_TO_LY_YR2
    val cf     = coastFrac.coerceIn(0.0, 0.99)
    val dA     = if (brach) distanceLy * (1 - cf) / 2 else distanceLy * (1 - cf)
    val dC     = distanceLy * cf
    val phases = if (brach) 2 else 1
    val gamma  = 1 + a * dA
    val beta   = sqrt(1 - 1 / (gamma * gamma))
    val tauA   = acosh(gamma) / a
    val tA     = sqrt(gamma * gamma - 1) / a
    val tCoast   = if (beta > 0) dC / beta else 0.0
    val tauCoast = if (beta > 0) dC / (beta * gamma) else 0.0
    val r1 = gamma * (1 + beta)
    return RocketResult(
        shipTimeYears   = phases * tauA + tauCoast,
        earthTimeYears  = phases * tA + tCoast,
        peakBeta        = beta,
        peakGamma       = gamma,
        dilationRatio   = (phases * tA + tCoast) / (phases * tauA + tauCoast),
        massRatioOneWay = r1,
        massRatioBrach  = if (brach) r1 * r1 else r1
    )
}

data class OrbitalResult(val orbitalVelocityMs: Double, val periodSeconds: Double, val escapeVelocityMs: Double)
fun computeOrbit(centralMassKg: Double, bodyRadiusM: Double, altitudeM: Double): OrbitalResult {
    val G = 6.674e-11; val r = bodyRadiusM + altitudeM
    return OrbitalResult(sqrt(G * centralMassKg / r), 2 * PI * r / sqrt(G * centralMassKg / r), sqrt(2 * G * centralMassKg / r))
}

data class TsiolkovskyResult(val deltaVMs: Double, val fuelMassKg: Double, val massRatio: Double)
fun computeTsiolkovsky(ve: Double, wet: Double, dry: Double): TsiolkovskyResult {
    val r = wet / dry; return TsiolkovskyResult(ve * ln(r), wet - dry, r)
}
fun computeTsiolkovskyInverse(ve: Double, dv: Double, dry: Double): TsiolkovskyResult {
    val r = exp(dv / ve); return TsiolkovskyResult(dv, dry * r - dry, r)
}

fun escapeVelocity(massKg: Double, radiusM: Double) = sqrt(2 * 6.674e-11 * massKg / radiusM)
fun schwarzschildRadius(massKg: Double) = SCHW_FACTOR * massKg
fun gravitationalTimeDilation(massKg: Double, radiusM: Double, heightM: Double): Double {
    val rs = schwarzschildRadius(massKg)
    return sqrt(1 - rs / radiusM) / sqrt(1 - rs / (radiusM + heightM))
}

data class HohmannResult(val dv1Ms: Double, val dv2Ms: Double, val totalDvMs: Double, val transferTimeS: Double)
fun computeHohmann(cm: Double, r1: Double, r2: Double): HohmannResult {
    val mu = 6.674e-11 * cm
    val vt1 = sqrt(mu * (2 / r1 - 2 / (r1 + r2))); val vt2 = sqrt(mu * (2 / r2 - 2 / (r1 + r2)))
    val dv1 = abs(vt1 - sqrt(mu / r1)); val dv2 = abs(sqrt(mu / r2) - vt2)
    return HohmannResult(dv1, dv2, dv1 + dv2, PI * sqrt((r1 + r2).pow(3) / (8 * mu)))
}

// ── New calculator functions ──────────────────────────────────────────────────

// Special relativity time dilation: gamma * proper time = coordinate time
data class VelocityDilationResult(
    val gamma           : Double, // Lorentz factor
    val properTimeDays  : Double, // days experienced by traveller
    val coordinateTimeDays: Double, // days elapsed on Earth
    val timeDiffDays    : Double, // extra days on Earth
    val betaFraction    : Double  // v/c
)
fun computeVelocityDilation(velocityFractionC: Double, properTimeDays: Double): VelocityDilationResult {
    val beta  = velocityFractionC.coerceIn(0.0, 0.9999999)
    val gamma = 1.0 / sqrt(1.0 - beta * beta)
    val coordDays = gamma * properTimeDays
    return VelocityDilationResult(gamma, properTimeDays, coordDays, coordDays - properTimeDays, beta)
}

// Surface gravity and weight on different bodies
data class PlanetWeightResult(
    val surfaceGravityMs2: Double,
    val weightNewtons    : Double,
    val relativeToEarth  : Double
)
fun computePlanetWeight(bodyMassKg: Double, bodyRadiusM: Double, personMassKg: Double): PlanetWeightResult {
    val g = 6.674e-11 * bodyMassKg / (bodyRadiusM * bodyRadiusM)
    return PlanetWeightResult(g, personMassKg * g, g / G_STANDARD)
}

// Schwarzschild radius with extra context
data class SchwarzschildResult(
    val radiusM         : Double,
    val radiusKm        : Double,
    val inSolarRadii    : Double,
    val densityKgM3     : Double, // average density inside event horizon
    val hawkingTempK    : Double  // Hawking temperature
)
fun computeSchwarzschild(massKg: Double): SchwarzschildResult {
    val rs   = SCHW_FACTOR * massKg
    val vol  = (4.0 / 3.0) * PI * rs * rs * rs
    val rSun = 6.957e8
    val hbar = 1.0546e-34
    val kB   = 1.381e-23
    val temp = (hbar * C_MS * C_MS * C_MS) / (8.0 * PI * 6.674e-11 * massKg * kB)
    return SchwarzschildResult(rs, rs / 1000.0, rs / rSun, massKg / vol, temp)
}

// Light travel time between two distances
data class LightTravelResult(
    val distanceM       : Double,
    val lightSeconds    : Double,
    val lightMinutes    : Double,
    val lightHours      : Double,
    val lightYears      : Double
)
fun computeLightTravel(distanceM: Double): LightTravelResult {
    val secs = distanceM / C_MS
    return LightTravelResult(distanceM, secs, secs / 60.0, secs / 3600.0, secs / (365.25 * 24 * 3600))
}

// Stellar lifetime based on mass (main sequence)
data class StellarLifetimeResult(
    val lifetimeYears   : Double,
    val luminositySolar : Double,
    val spectralClass   : String,
    val endState        : String
)
fun computeStellarLifetime(massSolar: Double): StellarLifetimeResult {
    // L ∝ M^3.5 (rough), t ∝ M/L ∝ M^-2.5
    val lum      = massSolar.pow(3.5)
    val lifetime = 1.0e10 * massSolar / lum  // solar lifetime ~10 billion years
    val spectral = when {
        massSolar > 16   -> "O-type (Blue)"
        massSolar > 2.1  -> "B-type (Blue-White)"
        massSolar > 1.4  -> "A-type (White)"
        massSolar > 1.04 -> "F-type (Yellow-White)"
        massSolar > 0.8  -> "G-type (Yellow, Sun-like)"
        massSolar > 0.45 -> "K-type (Orange)"
        else             -> "M-type (Red Dwarf)"
    }
    val endState = when {
        massSolar > 20   -> "Black Hole"
        massSolar > 8    -> "Neutron Star / Pulsar"
        massSolar > 0.5  -> "White Dwarf"
        else             -> "White Dwarf (very slowly)"
    }
    return StellarLifetimeResult(lifetime, lum, spectral, endState)
}

// Cosmological redshift
data class RedshiftResult(
    val z                   : Double, // redshift parameter
    val recessionalVelocityMs: Double,
    val recessionalVelocityC : Double,
    val observedWavelengthNm : Double,
    val distanceMly         : Double  // Hubble law distance estimate
)
fun computeRedshift(emittedWavelengthNm: Double, observedWavelengthNm: Double): RedshiftResult {
    val z   = (observedWavelengthNm - emittedWavelengthNm) / emittedWavelengthNm
    // Special relativistic recession velocity from z
    val zp1 = z + 1.0
    val v   = C_MS * (zp1 * zp1 - 1.0) / (zp1 * zp1 + 1.0)
    val H0  = 70.0 * 1000.0 / (3.086e22) // H0 in s^-1 (70 km/s/Mpc)
    val dist = v / H0 / 3.086e22 / 1e6 // Mly
    return RedshiftResult(z, v, v / C_MS, observedWavelengthNm, dist)
}

// ── Formatters (unchanged) ────────────────────────────────────────────────────

fun formatTime(years: Double): String {
    if (!years.isFinite()) return "---"
    return when {
        years < 1.0/365.25/24 -> { val m = years*365.25*24*60; if (m<1) "${(m*60).fmt(1)} sec" else "${m.fmt(0)} min" }
        years < 1.0/365.25    -> "${(years*365.25*24).fmt(1)} hr"
        years < 1.0/12        -> "${(years*365.25).fmt(0)} days"
        years < 1.0           -> "${(years*12).toInt()} months"
        years < 100           -> { val y=years.toInt(); val mo=((years-y)*12).toInt(); if(mo>0) "$y yr, $mo mo" else "$y years" }
        years < 1e3           -> "${years.toInt()} years"
        years < 1e6           -> "${(years/1e3).fmt(1)}k years"
        years < 1e9           -> "${(years/1e6).fmt(2)}M years"
        years < 1e12          -> "${(years/1e9).fmt(2)}B years"
        else                  -> "${(years/1e12).fmt(2)}T years"
    }
}
fun formatVelocityMs(ms: Double): String { if (!ms.isFinite()) return "---"; return when { ms>=C_MS*0.01 -> "${(ms/C_MS*100).fmt(3)}% c"; ms>=1000 -> "${(ms/1000).fmt(2)} km/s"; else -> "${ms.fmt(1)} m/s" } }
fun formatMass(kg: Double): String { if (!kg.isFinite()) return "---"; val t=kg/1000; return when { kg<1000->"${kg.fmt(1)} kg"; t<1e3->"${t.fmt(1)} t"; t<1e6->"${(t/1e3).fmt(2)}k t"; t<1e9->"${(t/1e6).fmt(2)}M t"; else->"${(t/1e9).fmt(2)}B t" } }
fun formatDistance(ly: Double): String = when { ly<100->"${ly.fmt(2)} ly"; ly<1e6->"${ly.toLong()} ly"; ly<1e9->"${(ly/1e6).fmt(1)}M ly"; else->"${(ly/1e12).fmt(1)}T ly" }
fun formatSeconds(s: Double): String = when { s<60->"${s.fmt(1)} s"; s<3600->"${(s/60).fmt(1)} min"; s<86400->"${(s/3600).fmt(2)} hr"; s<86400*365->"${(s/86400).fmt(1)} days"; else->"${(s/86400/365.25).fmt(2)} yr" }
fun formatMeters(m: Double): String = when { m<1e-9->"${(m*1e12).fmt(1)} pm"; m<1e-3->"${(m*1e6).fmt(1)} um"; m<1->"${(m*1000).fmt(1)} mm"; m<1000->"${m.fmt(1)} m"; m<1e9->"${(m/1000).fmt(1)} km"; else->"${(m/1.496e11).fmt(3)} AU" }
fun formatEnergy(j: Double): String = when { j<1e15->"${(j/1e12).fmt(1)} TJ"; j<1e18->"${(j/1e15).fmt(1)} PJ"; j<1e21->"${(j/1e18).fmt(1)} EJ"; j<1e24->"${(j/1e21).fmt(1)} ZJ"; else->"${(j/1e24).fmt(1)} YJ" }
private fun Double.fmt(d: Int) = "%.${d}f".format(this)
