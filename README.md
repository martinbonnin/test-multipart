This is a sample of a server that sends an empty part when a subscription stops.

Try the server & client:

```
$ ./gradlew test --console=plain
> Task :checkKotlinGradlePluginConfigurationErrors
> Task :checkApolloVersions UP-TO-DATE
> Task :generateApiApolloSources UP-TO-DATE
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE
> Task :processTestResources NO-SOURCE
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :testClasses UP-TO-DATE

> Task :test

MainTest > testStuff STANDARD_OUT
    This has started a server and a client that sent a multipart request to that server. 
    That server also supports out of band request that will be forwarded to the client.
    To send an event:
    curl -d '{"payload": {"data": { "echo": "hello world" }}}}' http://127.0.0.1:50507/echo 
    To terminate the subscription cleanly, send an empty body:
    curl -d '' http://127.0.0.1:50507/echo 
```

From another terminal (the subgraph), send events:

```
curl -d '{"payload": {"data": { "echo": "hello world" }}}}' http://127.0.0.1:50507/echo 
curl -d '{"payload": {"data": { "echo": "hello world again" }}}}' http://127.0.0.1:50507/echo 
curl -d '' http://127.0.0.1:50507/echo 
```

In the first terminal, you should see an output like this (apologies about the "Connection Closed" messages, they're not relevant)

```
    got data: Data(echo=hello world)
    Connection Closed
    got data: Data(echo=hello world again)
    Connection Closed
    Connection Closed
    subscription is completed
```

It works by always sending `\r\n--graphql` after each json payload. When the subscription is over, the server just sends `--` to terminate the multipart body.