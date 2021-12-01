package org.mushare.applestore

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "apple")
data class AppleStoreProperties(
    val base: String,
    val products: List<String>,
    val slack: String,
    val stores: List<String>,
)