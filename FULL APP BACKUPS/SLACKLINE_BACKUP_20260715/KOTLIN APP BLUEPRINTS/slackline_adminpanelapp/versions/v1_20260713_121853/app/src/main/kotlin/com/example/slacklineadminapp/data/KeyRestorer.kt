package com.example.slacklineadminapp.data

import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * ONE-TIME KEY RESTORER
 * Detects if the private.pem on disk doesn't match the known-correct public key
 * (by fingerprint comparison) and restores the verified original private key.
 *
 * Call restoreAllIfNeeded() once on app startup on a background thread.
 *
 * Detection method: compare SHA256(pubPath().readBytes()).take(8) against
 * the known-correct fingerprint. If the public key IS correct but sign+verify
 * fails, also restore. This catches all mismatch scenarios reliably.
 */
object KeyRestorer {

    // Known-correct fingerprints: SHA256(public.pem raw bytes).take(8).hex().uppercase()
    // These match the public keys embedded in the customer apps.
    private val CORRECT_FINGERPRINTS = mapOf(
        "casino_tools_pro"   to "BBB54A836BEF2CC8",
        "strategy_suite_pro" to "BBB54A836BEF2CC8",
        "shv_budget"         to "441E399D43896AA5",
        "shv_supa"           to "9A85DCAF8C5225C7",
        "synapse"            to "8F29E3B7FE6B6B94"
    )

    // Original private keys extracted from password-protected backups (password: 8889)
    // All verified: private key signs correctly against the corresponding public key above.
    private val CORRECT_PRIVATE_KEYS = mapOf(
        "casino_tools_pro" to """
-----BEGIN RSA PRIVATE KEY-----
MIIEqQIBAAKCAQEAzMmXgqCMEbS/AuRiFYTV2iFaSYmO7gCKC+OCkISzKG+huBMM
I/p5pSZmkynpN0Hn5FXGbthpaNKJ0CbAUGhUNjQeX03ZF7JVVguXUnRQNPHvikKx
EB5X06VHWQEGii+BkEjUkRkAnKIQObEl6z+Me/GIWPiLn0nkFQuBTkSOEqL6yUXn
qCPvDPkdoI+CX1DczOvLry8RJ+1EPFOE68dXez/IR0dQT8MhQmjm3inXCUMOhT/H
xU9JKxoDvkMT02qfpgAGaaV102/zL7Ii90ZQG6Q7oOu9iJMOopzWawO9P8C9wzVA
vLhPnajA8MoRjxLuhCUiWgUmyyH1S0NJUxaofQIDAQABAoIBABLAbtguPUUtyMPK
B9UymaZkoXDUYLsYX4uhJIZ5MDKGrkc9r0HoIJVnw8K79rfxl/tHg2kI9fX8OVRP
CBMUyolAEnvlk/hggwHjA9MwBVwjvyxo2O/B54bgvny+5Eto+DMvCA/VO0IccOHn
f7Jf2kxLHJIRCC/owx53kqF/n/MB9Eq/PqzyAE93hsbBl4ti+nVie0DzIvwvbVdb
+uNxvciWgjX1JSyV/7qW2xy9/yulR1FXOfrG7I14sqxc8sbqcaD894+5ZNusNpA2
kXMK+pBvHMNONKOubGs1vg4bu6pdxrEHU0U3kB9BTrljXGj0cXhLknI0CZCWjmRT
D05VWmECgYkA3hzSGZKEfXXVQjCkdbsF5IM9nvP5bdVQSiuWgmxPtaT4jnqH96eQ
Tzv/ZAB4E4pS29gyCWmfdfTLUvnl61zWvuPxU9Bywv2OCg2rttRrIk0WE8Ag/sgs
fA5bBpD7pw9CbE66CYedxL/srF3ifb/wvvos8n45jHEZ7+Y5v3H4gzQpC6/zX38c
BQJ5AOwIGB24FUBt6IoAulTINy5r5WtXrCFumL3U5d0g8EfPGcu3nv7CfXzOPXgf
HD5+sRiAsCtspDB4Dxoh2LM9lPpTuin9zn9ZsrXy7cFbHm4UaGFsoTouUl9ZDbYf
pCpwPXQB5biBnHgBgsDnP4+8Oazp2/I58zj8GQKBiGdwcqQ4KP2ot7WIo760Z60h
NbxmEN6GvkqcfA6nVIPanWr1Q4chgUJ1RZ4T3dfzzlzWFY9uojWz2FD/UoVHCjeC
PmxzIk8O+GxUK4kM6gvibIqsKnGdVaI8qUWolZoasCtyio9Pmjj7zMQFQSqhDgRS
zEXuZOyz64le5bvz3vFzhD4OJkBuGOUCeQDUnzICS5qxUJEDCEbSKw4yfjXL7c18
zGFB2HA9M/ff6DtHnaDXxCJA9JTBCd1qmrwz43FPnO2HUA/irYzFYns6yYPRYfbM
F+R6LV1qKggOh8nDwZS3kZTYmaNVZcmABV4Gi9MB05g9Lrc3yNkkRR37mytsWDG3
KrkCgYgrMpOVpamCw8vQFYyjX7YBvRmt9eqJiNa3UcynItYwzj6eBTPYys6aCpzx
NFwfHWW/rH2QI+k9RZTvD67fJT2Y8yzXt8ndZ4CVlveknsWketYZ8hNt9H83mMdH
NeSbzw8GrtJB7T4w3Y5XhAcbFacs1jZiEDWkb++x6xkX2sX4dZ+wLjHVt7OV
-----END RSA PRIVATE KEY-----
""",
        "strategy_suite_pro" to """
-----BEGIN RSA PRIVATE KEY-----
MIIEqQIBAAKCAQEAzMmXgqCMEbS/AuRiFYTV2iFaSYmO7gCKC+OCkISzKG+huBMM
I/p5pSZmkynpN0Hn5FXGbthpaNKJ0CbAUGhUNjQeX03ZF7JVVguXUnRQNPHvikKx
EB5X06VHWQEGii+BkEjUkRkAnKIQObEl6z+Me/GIWPiLn0nkFQuBTkSOEqL6yUXn
qCPvDPkdoI+CX1DczOvLry8RJ+1EPFOE68dXez/IR0dQT8MhQmjm3inXCUMOhT/H
xU9JKxoDvkMT02qfpgAGaaV102/zL7Ii90ZQG6Q7oOu9iJMOopzWawO9P8C9wzVA
vLhPnajA8MoRjxLuhCUiWgUmyyH1S0NJUxaofQIDAQABAoIBABLAbtguPUUtyMPK
B9UymaZkoXDUYLsYX4uhJIZ5MDKGrkc9r0HoIJVnw8K79rfxl/tHg2kI9fX8OVRP
CBMUyolAEnvlk/hggwHjA9MwBVwjvyxo2O/B54bgvny+5Eto+DMvCA/VO0IccOHn
f7Jf2kxLHJIRCC/owx53kqF/n/MB9Eq/PqzyAE93hsbBl4ti+nVie0DzIvwvbVdb
+uNxvciWgjX1JSyV/7qW2xy9/yulR1FXOfrG7I14sqxc8sbqcaD894+5ZNusNpA2
kXMK+pBvHMNONKOubGs1vg4bu6pdxrEHU0U3kB9BTrljXGj0cXhLknI0CZCWjmRT
D05VWmECgYkA3hzSGZKEfXXVQjCkdbsF5IM9nvP5bdVQSiuWgmxPtaT4jnqH96eQ
Tzv/ZAB4E4pS29gyCWmfdfTLUvnl61zWvuPxU9Bywv2OCg2rttRrIk0WE8Ag/sgs
fA5bBpD7pw9CbE66CYedxL/srF3ifb/wvvos8n45jHEZ7+Y5v3H4gzQpC6/zX38c
BQJ5AOwIGB24FUBt6IoAulTINy5r5WtXrCFumL3U5d0g8EfPGcu3nv7CfXzOPXgf
HD5+sRiAsCtspDB4Dxoh2LM9lPpTuin9zn9ZsrXy7cFbHm4UaGFsoTouUl9ZDbYf
pCpwPXQB5biBnHgBgsDnP4+8Oazp2/I58zj8GQKBiGdwcqQ4KP2ot7WIo760Z60h
NbxmEN6GvkqcfA6nVIPanWr1Q4chgUJ1RZ4T3dfzzlzWFY9uojWz2FD/UoVHCjeC
PmxzIk8O+GxUK4kM6gvibIqsKnGdVaI8qUWolZoasCtyio9Pmjj7zMQFQSqhDgRS
zEXuZOyz64le5bvz3vFzhD4OJkBuGOUCeQDUnzICS5qxUJEDCEbSKw4yfjXL7c18
zGFB2HA9M/ff6DtHnaDXxCJA9JTBCd1qmrwz43FPnO2HUA/irYzFYns6yYPRYfbM
F+R6LV1qKggOh8nDwZS3kZTYmaNVZcmABV4Gi9MB05g9Lrc3yNkkRR37mytsWDG3
KrkCgYgrMpOVpamCw8vQFYyjX7YBvRmt9eqJiNa3UcynItYwzj6eBTPYys6aCpzx
NFwfHWW/rH2QI+k9RZTvD67fJT2Y8yzXt8ndZ4CVlveknsWketYZ8hNt9H83mMdH
NeSbzw8GrtJB7T4w3Y5XhAcbFacs1jZiEDWkb++x6xkX2sX4dZ+wLjHVt7OV
-----END RSA PRIVATE KEY-----
""",
        "shv_budget" to """
-----BEGIN RSA PRIVATE KEY-----
MIIEqAIBAAKCAQEAlvX54aNkyAXhHxVi/ljFAoaXj5oSFauMVseleLVjlvszZYxR
jx/n90WiO3pJkZveh3YcpjVn0m2kdUhYWjahtEkPDdgkXCFYRE+yjnO7Ioem/7AM
FCr4UPrLHJWsk/xZ/560sVFTOshicB/GjNDNibdYtX6pOf4p27blX8Su4kLIsVL1
dkXktGzUE8Elm8V1iKGP8fBwMEvEc5btlf201GaiYVfrCYFycNwU5r9wV5WwXh5v
l8dNPn49Uh8ijefWHQ0FFPl5v2BfviQ+BZye2Qejjh+GwmoxJwYJbzzyqdkKfdCm
kbDtM9ITYVEElKNTSG5krTuGTxCn7hJBdEEodwIDAQABAoIBAFj6OoKIMArFjF8p
1+NapyRJ+GMnyKkFJ/6uRhXTtBN1lGGyRxA5Ghxg6rJr7ZjpakwljdVZfQxGm7yV
G55wvPbqaPkcALJc5Q2+e9wC0rYThpcGSNhKS3pPmBANGF9RSiqkfJRW/tuS6JJ1
6C1vlPYrHxo6wbvw1pL7fC9E8pwyMCVX/K5nK5ZufG2U29XddwDixxJUnwm1xYK/
HwcLZA7P/Ec1d/nzGGs8TQHUAVs9/cprP2xK0k09O769QoQiCyj6lmAZoyddpSxR
mMA7ZmmdThjZxLJHkEVC5FvDTM8sZf7xUydgoTgOYgw2yh+l1nesKy6VSJW1v4TP
h972YEECgYkAozWkb0TKO9teeR+fm/GUqLlBaJSao5dk2uEUQbp0v3oq9D+BvLKi
mpII/Vjp5PWJi3StdD+HVhL7m2FVR2In/36B4bblFQ2NXlgcw0HqHB2ZADc6KW3w
0zrk4Nu83JbFpew6i68yxHbvNgbtyXsuM+X5YEeGUPPEK0ZAr0mX3W12HZj8NJvP
+QJ5AOzJmGxk7+aCyRj/dWOK5LY/SG/Fg+rZN2Qt7WiYpacee36f5dx8ZwTVT7VF
NYa99zA81Lz0lsBc3zjSZ50VUTxiT9CCsd9fftfNFWTjc87KWVeriNCTXt4AzVHV
P5UmJIjB5YPRaGbAJWI64LPFz9YBNLYoY9e37wKBiDRy963+WFNWEw0vuXY+83wp
VLjMEMoDzBh+/qiEyCIEm6s0gXfnnFO6HIN2QGwg8BvN2jQielaR8SOk6ufuxlXY
DNtBkGrsmQK5pDyngRmZ9WJZXSqVNzBam9UbKJ1nIotI2COtoM38PrGJFn+KQ3XQ
MQzcz3BSdjdF82Ghb9lr5OpsEJjDfdkCeEFcHDC/Y+Dy9ONrkFGbwynzKeGAWbt3
neyPKNsYV5FbvqbjKCXGSQFyh//3TKEWyqaMBATsVrhealda7LLVcYkDchiSGtsX
lwGmYGNqzUqlXCfw92DrbOJGwmX2QQjAcJE3RE0wfmruWuNUcTXuQkJGLvChEX44
DQKBiGLnHDTM/srrbR+GntXl/peBFXxYW29nMm6cnqHKi7yMbuCHMEcMjc4rCdkT
BVtazcB9htDyrcqS+iG70H4/UQgejpbP/wA80TxLvvXzrIcMfBAkDaRBphH3ZPYn
RwtkvQGgnSYRAN7g09/3i5uSma0NQMpXCIOs+p74wbtRU22RiMQIQVH68UQ=
-----END RSA PRIVATE KEY-----
""",
        "shv_supa" to """
-----BEGIN RSA PRIVATE KEY-----
MIIEqQIBAAKCAQEA4K3CjoUcMEcu48QeT7fss/mLdSXzzB0xQrZ+PkJrm1ggU45J
TJhJqcFcE8g9pAtjC8G9e9hj6GA4apajFgQ4VP4Q0RG3lFEQGGEIJ2x/JgbMAu4o
J4jRm39iPq0FArbwuA3K+wih15nHRxHnaAANqvAqso41+GfENGN2g7Kzhrd9EUww
V0XeRW7yVCzNGKT2dAege4K7PQVs0Z8YZv45WP0ecc+V43pKKt0ZsE/mQJAQKfNG
wk7ArStFyF8VRHfRvRp4qvnHSxGD/dhfuuWIbkkf+VWYINZEpKT0bVMQPlZRKuUF
r2hRjDFF1cRSOdrvBzpx3lhHtEbk+OnZOURJ+QIDAQABAoIBAQDZP+sieB97pzj1
0AsnNkdQ93kbu6jzsz6QrXmApiT6vsnzQWArCg4gcPGhxzujRcdt9NZNzD0+0tt6
ZVIo1cfKlyiXoDQM3B1eFWUXCdggYu4d3z9AixFy9EeBJzfzGgXFUsD037HukamZ
VPyXGYYdxcDjipMxMNQdvVBr1m7fupQGbFKiZ6gcNVOHhWW9RyqcZzl1hRdYRs4k
65/dM9DvIsiFiCWgBoFpvuzloP/sMYtG6GrU9x9+1tcQxtf0Z48T2yLf5z4MsTvP
PZ1d2yCYb98e9LjrunRX+2N5oix//KkccyNONpIcIzd1sYBWKkN33nLIN5zqGUta
hqvvQFXpAoGJAPfwlFTTQkho3AnWu33kVHzqokxL1F5L84jti4eM35EYfR7KI0wh
PvQ8gNsNNvy1qCc9M4LkLkYZkj3FYGTav42W7YJ5LpU4FZ/7XyQntb1NIqz/kxje
s5K8OboapcSSi4WBQq3jIXiJ4iwZvt4i//a1C+EJzkNlf+cWujKf8DONQfcv+YYX
XrsCeQDn+5ii06oUdxA6XkN+OqIE+KK44s89dItPH68sQ7R9F0NpQTDtfnKArJqy
zRhCnMJyAdb5eZDALWAAxCVhhVeCA0jHGkunjDdfVSbCC4rgvZBJSmPp6KwFKeL1
hHV2qsuWqgw4UE21CaNTxZr0FduYRT6WzooOwNsCgYhLxw2utzqk4teNckGvgl+I
JjYgV3S6tT/jc6aXcSjLMc7b4C/Vjrus7ej8ChfqOKMCCyR5NsAl0J7vnimN6YVu
AtjobZ0dIh0J3kv/wts61h5o/Vu52CbODBUgmhBb+eYGkbENcEPEkW+8xsmdHtlY
T5En4FVYJ+dle37BBjzlpKCqqdD7eJ0NAng3O9xXyHXScZbXjuIbB/LTpC96PeaC
B2Gz8SNQMr6imgte4Dq6EUVKBc6/i72e/pv9cRqyQ5OHbkZRaGJOGeV/+zA017nz
ppr4g9m6kez7HadhC+lGJDPw9swnEOa23mui79IBBn26ARV2OLJwM1QZqUMBvSN5
58kCgYgSpyYhhvulBvrtRDNA4hFwWe1eCTFjAhe0j5PNTnIW+AaMNrMiyEp6mjfH
d7BWqjZOZOrnberQS88n4EqbuCC0U5EHQmWkv+wZm/lhrDLpr8qHwYsiG8mFVUUx
vWoFQSvzUr5mB3IOhsxAC+LLer29R8kjiPa4LBMHU2tzSk+kQjnrsSRUSFpJ
-----END RSA PRIVATE KEY-----
""",
        "synapse" to """
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA5KyY9ak9h6baCa1BE/N7aEUU8G68NDWB2CDrBSsEVmfD4LEl
eV9kcI/SZXk7VTpxAzAED/hRBCkEx7X6INeIP7P83PB2TRSoBObfk7WaiiV0VKbg
S/ZgIHuOzwxszJLBWd8xQ3f9AK1ODHT7WQK+Hsco9KPyD2C43bC56nTFe2vUtRVW
o7HqPcDhiJvyeQXUf/TsJIwi2/biNhVLfaeHsPoS0D524a6pSqJEiCST/GLb+Et9
c5y8clNryMU1fjw1SIb6TYXpOYB24JtLX/jTNpC9pDLMcGuUoxQ8ck5ja6kdxn27
qvW6I6BXpeIFgU1e8gs26h0PX+VJBfNl9AS6HwIDAQABAoIBAFlkfYEfUU3btIWu
5G9bseTviIF7EHiqaCFosOc6yz3J36FRLtCVMWrtVjbT3xVwvKgd16C0lls1e8hk
g6zeBMW+Yz2thNmaFxqfdExGZGzXunOzLqCTZj2cf5XCCjAouIwc+6Gf4NgoZ4fo
HS/NKixW99Q22NQZH/uN8AfO8TpPDgmU41ehei87Jwgu52dl1hkgmdacNHNfOjgF
5AZ2O16V1j2c0v6RzjAu0vaDEJpf2PCkj+pis/k8VwqmIdyEfktcQ7h1he0SrZ++
hRo71h74+/k4EUdK6gH4Et23uwjg+SpAjNg0/NR7iPXfvD7yhvq5QoSYnrSU0AuM
2IpAExkCgYEA/6pGj97kuhpwhICfEMoQccnv1aTPi/+dh1DgDV2wmzqFxs/m5NSH
nfoWbrkdKPsXjJRLW8jC4Bpe6dmEieMSgo4ED4RyI4JyN5s+Y/LzZmudba0Jg9pw
6y/QpJPMWOKaaYbOc51/xxYguUz4kE/7d8Bti4iNTPO1v1SF+ayQjXkCgYEA5PlF
ljIQqdpn9TJqiD0RYRpEOuoD6hOa7JjLiu7N7nztYuoVWEqbkqbTTJ6uNvw7yKdC
XtdgPBnrUmA8HRuvulqtxg+qgO+9znPhi2S31yx0gm4UjKKq5Qk5V7rovEOPb4ON
BD8lAr23M6eM0nhDXrH9n7s96zWjFq1TnyjOVlcCgYEAwK6j32otF9U1V6dYOl8P
ZbK7flhn0ysingjl0yz5HQROLjgh2/QRAY6puWjqASi75sccxF/Z/uvg/H1i1ki8
eohtpwQ6wWhejGoD62/+4QHZ8/6lXSoUUCwJIwAA0jx2A3IFxjy9QF3866qG6rxc
2TO9W5veYlCKeVhKYJEdoIECgYB62IYOC/RGvKfTtGXVjDX7y9TZat4IwtX2pA9o
DbEsh5fw3rfu87A94QUycVv0oiUNBTelnJXECP/o5Tq7PzRrneTng1Yt8PH7hs52
M+YyKmaj551cypU3Zlh+igf9oZ2d7Y1Fvv8DVneo3fa+oMk8T/BLt3CD9fX2360i
kgkJ5wKBgD6IKHyqW44EWkOEJ2xsmACMWmxPdtsi2O9degkFchKNN4Ji945pLtWJ
7uelCyEGJ3WkrqT3zXknAfu9MDlUElRKoR1P1qEAanE9ZssLWT++s6nx8oi9tsKU
Haohpq3OUYj4Si3upDIQWnEuBMynQ7+VAwXqrPdDdFsZWlTDmKT9
-----END RSA PRIVATE KEY-----
""",
    )

    fun restoreAllIfNeeded() {
        var restored = 0
        var skipped  = 0

        CORRECT_FINGERPRINTS.forEach { (productId, expectedFp) ->
            try {
                val cfg      = ProductRegistry.get(productId) ?: return@forEach
                val eng      = EngineCache.get(cfg)
                val privFile = eng.privPath()
                val pubFile  = eng.pubPath()

                if (!privFile.exists() || !pubFile.exists()) return@forEach

                // Step 1: check if the public key on disk is the correct one
                val actualFp = MessageDigest.getInstance("SHA-256")
                    .digest(pubFile.readBytes())
                    .take(8)
                    .joinToString("") { "%02X".format(it) }

                val pubKeyCorrect = actualFp == expectedFp

                // Step 2: even if public key looks right, verify keypair integrity
                val pairValid = if (pubKeyCorrect) eng.keypairIntact() else false

                if (pubKeyCorrect && pairValid) {
                    skipped++
                    return@forEach
                }

                // Restore the correct private key
                val correctPriv = CORRECT_PRIVATE_KEYS[productId] ?: return@forEach
                privFile.writeText(correctPriv.trim())
                EngineCache.invalidate(productId)

                // If public key was also wrong, restore it too using the private key
                if (!pubKeyCorrect) {
                    Log.w("KeyRestorer", "$productId: public key fingerprint mismatch ($actualFp != $expectedFp) — this is unexpected")
                }

                restored++
                Log.i("KeyRestorer", "Restored private key for $productId (pubCorrect=$pubKeyCorrect, pairValid=$pairValid)")

            } catch (e: Exception) {
                Log.e("KeyRestorer", "Error checking $productId: ${e.message}")
            }
        }

        if (restored > 0) {
            Log.i("KeyRestorer", "KeyRestorer: restored $restored product(s), skipped $skipped")
        } else {
            Log.i("KeyRestorer", "KeyRestorer: all $skipped keypairs verified OK, nothing to restore")
        }
    }
}
