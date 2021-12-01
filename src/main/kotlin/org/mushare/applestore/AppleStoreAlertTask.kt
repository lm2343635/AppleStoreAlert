package org.mushare.applestore

import com.github.kevinsawicki.http.HttpRequest
import net.sf.json.JSONObject
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.ArrayList

@Service
@EnableConfigurationProperties(AppleStoreProperties::class)
class AppleStoreAlertTask(
    private val appleStoreProperties: AppleStoreProperties,
) {
    private var lastMessage = ""

    @Scheduled(fixedRate = 1000L * 10)
    fun checkStore() {
        val time = "Check at ${Date().toString()}\n--------------------------------------------------------"
        println(time)
        var products = checkProducts()
        if (products.map { it.stores.size }.sum() == 0) {
            return
        }
        val message = products.filter { it.stores.size > 0 }
            .map { "${it.name}:\n ${it.stores.joinToString("\n")}" }
            .joinToString("\n")
        println(message)
        if (message.equals(lastMessage)) {
            return
        }
        lastMessage = message
        val slackHttpRequest = HttpRequest(appleStoreProperties.slack, "POST")
        slackHttpRequest.contentType("application/json", "UTF-8")
        slackHttpRequest.send("""
            {"text":"$time\n$message"}
        """.trimIndent())
        println(slackHttpRequest.body())
    }

    fun checkProducts(): List<Product> {
        val products = appleStoreProperties.products.map {
            Product(
                name = it,
                stores = ArrayList(),
            )
        }
        var storeNames = mutableListOf<String>()
        appleStoreProperties.stores.forEach {
            val response = HttpRequest
                .get("https://www.apple.com/jp/shop/pickup-message-recommendations?mt=compact&searchNearby=true&store=$it&product=${appleStoreProperties.base}")
                .body()
            val stores = JSONObject.fromObject(response)
                .getJSONObject("body")
                .getJSONObject("PickupMessage")
                .getJSONArray("stores")

            for (i in 0..(stores.size - 1)) {
                val store = stores.getJSONObject(i)
                val storenName = store.getString("storeName")
                val partsAvailability = store.getJSONObject("partsAvailability")
                if (partsAvailability.toString().equals("{}")) {
                    continue
                }
                for (key in partsAvailability.keys) {
                    val model = partsAvailability.getJSONObject(key as String?)
                    val storePickupProductTitle = model.getString("storePickupProductTitle")
                    val name = getHopeProduct(storePickupProductTitle.replace(" ", ""))
                    if (name != null) {
                        products.firstOrNull { it.name.equals(name) }?.let {
                            it.stores.add("$storenName($storePickupProductTitle)")
                        }
                    }
                }
            }
        }

        return products
    }

    fun getHopeProduct(name: String): String? {
        for (hope in appleStoreProperties.products) {
            if (name.contains(hope)) {
                return hope
            }
        }
        return null
    }
}