@file:Suppress(
    "CANNOT_OVERRIDE_INVISIBLE_MEMBER",
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
)

import api.EchoSubscription
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.*
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import kotlin.test.Test

class MainTest {
    @Test
    fun testStuff(): Unit = runBlocking {
        MockServer.Builder()
            .handler(MyMockServerHandler())
            .build()
            .use { mockServer ->
                ApolloClient.Builder()
                    .networkTransport(
                        HttpNetworkTransport.Builder()
                            .serverUrl("${mockServer.url()}subscription")
                            .build()
                    )
                    .build()
                    .use { apolloClient ->

                        println(
                            """
                            |This has started a server and a client that sent a multipart request to that server. 
                            |That server also supports out of band request that will be forwarded to the client.
                            |To send an event:
                            |curl -d '{"payload": {"data": { "echo": "hello world" }}}}' ${mockServer.url()}echo 
                            |To terminate the subscription cleanly, send an empty body:
                            |curl -d '' ${mockServer.url()}echo 
                            |""".trimMargin()
                        )
                        apolloClient.subscription(EchoSubscription())
                            .toFlow()
                            .collect {
                                if (it.data != null) {
                                    println("got data: ${it.data}")
                                } else {
                                    println("got exception: ${it.exception}")
                                }
                            }
                        println("subscription is completed")

                    }
            }
    }
}

class MyMockServerHandler : MockServerHandler {
    private var channel: Channel<ByteString>? = null
    private val boundary = "graphql"

    override fun handle(request: MockRequestBase): MockResponse {
        request as MockRequest
        return if (request.path == "/subscription") {
            check(channel === null) {
                error("There can be only one multipart body active at a time")
            }
            channel = Channel(Channel.UNLIMITED)
            channel!!.trySend("\r\n--$boundary".toByteArray().toByteString())

            MockResponse.Builder()
                .addHeader("Content-Type", """multipart/mixed; boundary="$boundary""")
                .addHeader("Transfer-Encoding", "chunked")
                .body(channel!!.consumeAsFlow().asChunked())
                .build()
        } else if (request.method == "POST") {
            val close = request.body.size == 0
            if (close) {
                channel!!.trySend("--".toByteArray().toByteString())
                channel!!.close()
            } else {
                channel!!.trySend(
                    Buffer().apply {
                        writeUtf8("\r\n")
                        writeUtf8("Content-Length: ${request.body.size}\r\n")
                        writeUtf8("Content-Type: application/json\r\n")
                        writeUtf8("\r\n")
                        write(request.body)
                        // Write the beginning of next part so that the client can know where to stop
                        writeUtf8("\r\n--$boundary")
                    }.readByteString()
                )
            }

            MockResponse.Builder()
                .statusCode(200)
                .body("ok")
                .build()
        } else {
            MockResponse.Builder()
                .statusCode(400)
                .body("Only post is supported")
                .build()
        }
    }
}