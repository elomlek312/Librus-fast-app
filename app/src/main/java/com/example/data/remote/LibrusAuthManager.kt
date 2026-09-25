package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class InMemoryCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val current = cookieStore.getOrPut(host) { mutableListOf() }
        synchronized(current) {
            cookies.forEach { newCookie ->
                current.removeAll { it.name == newCookie.name }
                current.add(newCookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = mutableListOf<Cookie>()
        cookieStore.forEach { (savedHost, list) ->
            if (host.endsWith(savedHost) || savedHost.endsWith(host)) {
                synchronized(list) {
                    cookies.addAll(list)
                }
            }
        }
        return cookies
    }

    fun addManualCookie(host: String, name: String, value: String) {
        val cookie = Cookie.Builder()
            .domain(host)
            .path("/")
            .name(name)
            .value(value)
            .secure()
            .httpOnly()
            .build()
        val list = cookieStore.getOrPut(host) { mutableListOf() }
        synchronized(list) {
            list.removeAll { it.name == name }
            list.add(cookie)
        }
    }

    fun getCookieValue(name: String): String? {
        cookieStore.values.forEach { list ->
            synchronized(list) {
                list.firstOrNull { it.name == name }?.let { return it.value }
            }
        }
        return null
    }

    fun clear() {
        cookieStore.clear()
    }
}

object LibrusAuthManager {
    private const val TAG = "LibrusAuth"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    val cookieJar = InMemoryCookieJar()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun getClient(): OkHttpClient = httpClient

    private fun shiftCharacters(value: Any): String {
        return value.toString().map { (it.code + 20).toChar() }.joinToString("")
    }

    private fun makeBannerHeader(): String {
        val r = Math.random()
        val t = System.currentTimeMillis()
        return "${shiftCharacters(r)}_${shiftCharacters(t)}"
    }

    /**
     * Executes the authentic Mati365/librus-api authorization sequence
     */
    suspend fun authorize(login: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            cookieJar.clear()

            // Step 1: Request portalRodzina
            val portalUrl = "https://synergia.librus.pl/loguj/portalRodzina"
            val req1 = Request.Builder()
                .url(portalUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://portal.librus.pl/")
                .build()

            val res1 = httpClient.newCall(req1).execute()
            val loc1 = res1.header("Location")
            res1.close()

            if (loc1 == null) {
                return@withContext Result.failure(Exception("Brak przekierowania z portalu Librus (Krok 1). Kod odpowiedzi: ${res1.code}"))
            }

            val authorizationUrl = loc1.toHttpUrlOrNull()?.toString()
                ?: ("https://synergia.librus.pl" + loc1)

            // Step 2: Request authorizationUrl to obtain login form endpoint
            val req2 = Request.Builder()
                .url(authorizationUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", portalUrl)
                .build()

            val res2 = httpClient.newCall(req2).execute()
            val loc2 = res2.header("Location")
            res2.close()

            val loginUrl = if (loc2 != null) {
                if (loc2.startsWith("http")) loc2 else "https://api.librus.pl$loc2"
            } else {
                authorizationUrl
            }

            // Step 3: GET loginUrl to initialize cookies
            val req3 = Request.Builder()
                .url(loginUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", authorizationUrl)
                .build()
            val res3 = httpClient.newCall(req3).execute()
            res3.close()

            // Step 4: Submit login form matching Mati365/librus-api exactly
            val formBody = FormBody.Builder()
                .add("action", "login")
                .add("login", login)
                .add("pass", pass)
                .build()

            val req4 = Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .header("User-Agent", USER_AGENT)
                .header("x-baner", makeBannerHeader())
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Referer", loginUrl)
                .header("Origin", "https://api.librus.pl")
                .build()

            val res4 = httpClient.newCall(req4).execute()
            val res4Code = res4.code
            val res4Body = res4.body?.string() ?: ""
            val res4Loc = res4.header("Location")
            res4.close()

            var nextTarget = res4Loc
            if (nextTarget == null && res4Body.isNotEmpty()) {
                try {
                    val json = JSONObject(res4Body)
                    if (json.has("goTo")) {
                        nextTarget = json.getString("goTo")
                    } else if (json.has("errors")) {
                        val errors = json.getJSONArray("errors")
                        val errList = mutableListOf<String>()
                        for (i in 0 until errors.length()) {
                            errList.add(errors.getString(i))
                        }
                        return@withContext Result.failure(Exception(errList.joinToString(", ")))
                    } else if (json.has("message")) {
                        return@withContext Result.failure(Exception(json.getString("message")))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Response was not JSON: $res4Body")
                }
            }

            if (nextTarget == null) {
                // If invalid credentials or 401
                if (res4Code == 401 || res4Body.contains("błędne", ignoreCase = true) || res4Body.contains("nieprawidłowe", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("Nieprawidłowy login lub hasło do Librus Synergia."))
                }
                return@withContext Result.failure(Exception("Serwer Librus zwrócił odpowiedź bez adresu kontynuacji (kod $res4Code). Możliwa blokada 2FA lub ochrona Cloudflare."))
            }

            // Step 5: Follow continuation chain (up to 8 hops)
            var currentUrl = if (nextTarget.startsWith("http")) nextTarget else "https://api.librus.pl$nextTarget"
            var lastReferer = loginUrl

            for (hop in 0 until 8) {
                val reqHop = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", lastReferer)
                    .build()

                val resHop = httpClient.newCall(reqHop).execute()
                val hopLoc = resHop.header("Location")
                resHop.close()

                if (hopLoc == null || resHop.code !in 300..399) {
                    break
                }

                lastReferer = currentUrl
                currentUrl = if (hopLoc.startsWith("http")) hopLoc else {
                    val base = currentUrl.toHttpUrl().resolve(hopLoc)
                    base?.toString() ?: "https://synergia.librus.pl$hopLoc"
                }

                if (currentUrl.contains("error=", ignoreCase = true)) {
                    val errCode = currentUrl.substringAfter("error=")
                    return@withContext Result.failure(Exception("Librus odrzucił autoryzację: $errCode. Skorzystaj z planu awaryjnego."))
                }

                if (currentUrl.contains("przegladaj_oceny") || currentUrl.contains("uczen") || currentUrl.contains("panel")) {
                    break
                }
            }

            // Check if we captured session cookie DZIENNIKSID or SDZIENNIKSID
            val sid = cookieJar.getCookieValue("DZIENNIKSID") ?: cookieJar.getCookieValue("SDZIENNIKSID")
            return@withContext Result.success(sid ?: "authenticated_session")
        } catch (e: Exception) {
            Log.e(TAG, "Authorization error", e)
            Result.failure(Exception("Błąd połączenia z Librus: ${e.localizedMessage}"))
        }
    }

    /**
     * Authorize using a direct session cookie (DZIENNIKSID) - standard backup plan for web scraping
     */
    fun authorizeWithSessionCookie(cookieValue: String): Result<String> {
        val cleanSid = cookieValue.trim()
            .removePrefix("DZIENNIKSID=")
            .removePrefix("SDZIENNIKSID=")
            .substringBefore(";")
            .trim()

        if (cleanSid.isEmpty()) {
            return Result.failure(Exception("Nieprawidłowy token sesji DZIENNIKSID"))
        }

        cookieJar.clear()
        cookieJar.addManualCookie("synergia.librus.pl", "DZIENNIKSID", cleanSid)
        cookieJar.addManualCookie("synergia.librus.pl", "SDZIENNIKSID", cleanSid)
        cookieJar.addManualCookie("api.librus.pl", "DZIENNIKSID", cleanSid)

        return Result.success(cleanSid)
    }
}
