plugins {
    id("org.jetbrains.kotlin.jvm").version("1.9.22")
    id("com.apollographql.apollo3").version("4.0.0-beta.4")
}

dependencies {
    implementation("com.apollographql.apollo3:apollo-mockserver")
    implementation("com.apollographql.apollo3:apollo-runtime")
    testImplementation(kotlin("test"))
}

apollo {
    service("api") {
        packageName.set("api")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        showStandardStreams = true
    }
}