package com.hyu.coroutinetest

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.junit.Test

class CoroutineChannel {
    @Test
    fun basicChannelTest() = runBlocking{
        val channel = Channel<Int>()
        launch {
            // this might be heavy CPU-consuming computation or async logic, we'll just send five squares
            for (x in 1..5) {
                log("send $x")
                channel.send(x)
            }
            channel.close()
        }
        launch {
            for (y in channel) log("receive1 : $y")

//            repeat(5) { log("receive1 : ${channel.receive()}") }
        }
        // here we print five received integers:
        repeat(5) { log("receive2 : ${channel.receive()}") }
        log("Done!")
    }

    @Test
    fun producerChannelTest() = runBlocking {
        val squares = produce {
            for (x in 1..100) {
                send(x * x)
            }
        }

        squares.consumeEach { println(it) }
        println("Done")
    }


    @Test
    fun primeNumberChannelTest() = runBlocking {
        fun CoroutineScope.numbersFrom(start: Int) = produce {
            var x = start
            while (true) send(x++) // infinite stream of integers from start
        }

        fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
//            for (x in numbers) {
//                if (x % prime != 0) send(x)
//            }

            numbers.consumeEach {x ->
                if (x % prime != 0) send(x)
            }
        }
        var cur = numbersFrom(2)

        for (i in 1..10) {
            val prime = cur.receive()
            println(prime)
            cur = filter(cur, prime)
        }
        coroutineContext.cancelChildren()
        println("Done")
    }

    @Test
    fun primeNumberIteratorTest() {
        fun numbersFrom(start: Int) = iterator {
            var x = start
            while (true) yield(x++) // infinite stream of integers from start
        }

        fun filter(numbers: Iterator<Int>, prime: Int) = iterator<Int> {
            for (x in numbers) {
                if (x % prime != 0) yield(x)
            }
        }
        var cur = numbersFrom(2)

        for (i in 1..10) {
            val prime = cur.next()
            println(prime)
            cur = filter(cur, prime)
        }

        println("Done")
    }

    @Test
    fun fanOutTest()= runBlocking{
        fun produceNumbers() = produce {
            var x = 1
            while (true) {
                send(x++)
                delay(100L)
            }
        }

        fun launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
//            for (msg in channel) {
            channel.consumeEach { msg ->
                println("Processor #$id received $msg")
            }
        }

        val producer = produceNumbers()
        repeat(5) {
//            launchProcessor(it, producer)
            val job = launchProcessor(it, producer)

            if (it == 3) {
                delay( 200)
                job.cancel()
            }
        }
        delay(950L)
        producer.cancel()
    }

    @Test
    fun fanInTest() = runBlocking {
        suspend fun sendString(channel: SendChannel<String>, text: String, time: Long) {
            while (true) {
                delay(time)
                channel.send(text)
            }
        }

        val channel = Channel<String>()
        launch { sendString(channel, "Foo", 200L) }
        launch { sendString(channel, "Bar", 500L) }
        repeat(6) {
            println(channel.receive())
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun bufferChannelTest() = runBlocking {
        val channel = Channel<Int>(4)

        val sender = launch {
            repeat(10) {
                print("Try to send $it : ")
                channel.send(it)
                print("Done\n")
            }
        }

        delay(1000)
        sender.cancel()

    }

    data class Ball(var hits: Int)

    @Test
    fun fairChannelTest() = runBlocking {
        suspend fun player(name: String, table: Channel<Ball>) {
            for (ball in table) {
                ball.hits++
                println("$name $ball")
                // Comment out below delay to see the fairness a bit more.
                delay(300)
                table.send(ball)
            }
        }
        val table = Channel<Ball>()

        launch { player("ping", table) }
        launch { player("pong", table) }

        table.send(Ball(0))
        delay(1000)
        coroutineContext.cancelChildren()
    }

    @Test
    fun tickerChannel() = runBlocking {
        val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0, mode = TickerMode.FIXED_PERIOD) // create ticker channel
        var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Initial element is available immediately: $nextElement") // initial delay hasn't passed yet

        nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // all subsequent elements has 100ms delay
        println("Next element is not ready in 50 ms: $nextElement")

        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 100 ms: $nextElement")

        // Emulate large consumption delays
        println("Consumer pauses for 150ms")
        delay(150)
        // Next element is available immediately
        nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Next element is available immediately after large consumer delay: $nextElement")
        // Note that the pause between `receive` calls is taken into account and next element arrives faster
        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

        tickerChannel.cancel() // indicate that no more elements are needed
    }
}