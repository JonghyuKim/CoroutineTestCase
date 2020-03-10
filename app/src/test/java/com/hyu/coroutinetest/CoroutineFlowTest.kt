package com.hyu.coroutinetest

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class CoroutineFlowTest {

    @Test
    fun flowBasicTest() = runBlocking {
        //        fun foo() = listOf(1, 2, 3)
//        fun foo(): Sequence<Int> = sequence { // sequence builder
//            for (i in 1..3) {
//                Thread.sleep(100) // pretend we are computing it
//                yield(i) // yield next value
//            }
//        }
//        suspend fun foo(): List<Int> {
//            delay(200) // pretend we are doing something asynchronous here
//            return listOf(1, 2, 3)
//        }

        fun foo(): Flow<Int> = flow { // flow builder
            println("Flow started")
            for (i in 1..3) {
                delay(100) // pretend we are doing something useful here
                emit(i) // emit next value
            }
        }

        var flow = foo()

        flow = (1..3).asFlow()

        launch {
            for (k in 1..3) {
                println("I'm not blocked $k")
                delay(100)
//                Thread.sleep(100)
            }

        }
//        foo().forEach { value -> println(value) }
        println("Calling foo...")
        println("Calling collect...")
        flow.collect { value -> println(value) }
        println("Calling collect again...")
        flow.collect { value -> println(value) }
        println("end!?")
    }

    @Test
    fun flowOperatorTest() = runBlocking{


        suspend fun performRequest(request: Int): String {
            delay(1000) // imitate long-running asynchronous work
            return "response $request"
        }

        val flowOperator = flowOf(1,2,3).map {
            delay(1000) // imitate long-running asynchronous work
            it + 10
        }.transform { request ->
            emit("Making request $request")
            emit(performRequest(request))
        }.take(2)

        flowOperator.collect{ println(it) }
        println()
    }

    @Test
    fun flowTerminalTest() = runBlocking {
        val flow = (1..3).asFlow().flowOn(Dispatchers.Default)

        flow.collect{ println(it) }

        println(flow.reduce { a, b -> a + b })
        flow.fold(0){ a, b -> a + b }

        println()
    }

    @Test
    fun flowOnTest() = runBlocking {
        val job = Job() + Dispatchers.Default
        fun foo(): Flow<Int> = flow {
            // The WRONG way to change context for CPU-consuming code in flow builder
            for (i in 1..3) {
                Thread.sleep(100) // pretend we are computing it in CPU-consuming way
                log("Emitting $i")
                emit(i) // emit next value
            }
        }.flowOn(job)

        foo().collect { value -> log("$value") }

        println()
    }

    @Test
    fun flowBufferTest() = runBlocking {
        fun foo(): Flow<Int> = flow {
            for (i in 1..3) {
                delay(100) // pretend we are asynchronously waiting 100 ms
                emit(i) // emit next value
            }
        }

        val time = measureTimeMillis {
            foo().buffer().collect { value ->
                delay(300) // pretend we are processing it for 300 ms
                println(value)
            }
        }
        println("Collected in $time ms")
    }

    @Test
    fun flowConflateTest() = runBlocking {
        fun foo(): Flow<Int> = flow {
            for (i in 1..3) {
                delay(100) // pretend we are asynchronously waiting 100 ms
                emit(i) // emit next value
            }
        }

        val time = measureTimeMillis {
            foo()
                .conflate() // conflate emissions, don't process each one
                .collect { value ->
                    delay(300) // pretend we are processing it for 300 ms
                    println(value)
                }
        }
        println("Collected in $time ms")


        val time2 = measureTimeMillis {
            foo().collectLatest{ value ->
                println("Collecting $value")
                delay(300) // pretend we are processing it for 300 ms
                println("Done $value")
            }
        }
        println("Collected in $time2 ms")
    }

    @Test
    fun flowComposingTest() = runBlocking{
        val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3
        val strs = flowOf("one", "two", "three").onEach { delay(400) } // strings
        var startTime = System.currentTimeMillis()

        nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string
            .collect {value ->
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            } // collect and print

        startTime = System.currentTimeMillis()
        nums.combine(strs) { a, b -> "$a -> $b" } // compose a single string
            .collect {value ->
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            } // collect and print
    }

    @Test
    fun flowFlatteningTest() = runBlocking{
        val flow1 = (1..3).asFlow().onEach { delay(300) }
        val flow2 = (4..6).asFlow().onEach { delay(400) }
        val flow3 = (7..9).asFlow().onEach { delay(500) }

        fun requestFlow(i: Int): Flow<String> = flow {
            emit("$i: First")
            delay(500) // wait 500 ms
            emit("$i: Second")
        }

//        (1..3).asFlow().map { requestFlow(it) }.collect{ req -> req.collect{println(it)}}
        var startTime = System.currentTimeMillis()

        (1..3).asFlow().map { requestFlow(it) }.flattenConcat().collect{ value ->
            println("$value at ${System.currentTimeMillis() - startTime} ms from start")
        }

        (1..3).asFlow().flatMapConcat { requestFlow(it) }.collect{ value ->
            println("$value at ${System.currentTimeMillis() - startTime} ms from start")
        }

        println()
        println()

        startTime = System.currentTimeMillis()
        (1..3).asFlow().flatMapMerge { requestFlow(it) }.collect{ value ->
            println("$value at ${System.currentTimeMillis() - startTime} ms from start")
        }

        println()
        println()

        startTime = System.currentTimeMillis()
        (1..3).asFlow().flatMapLatest { requestFlow(it) }.collect{ value ->
            println("$value at ${System.currentTimeMillis() - startTime} ms from start")
        }
        println()
    }

    @Test
    fun flowMergeTest() = runBlocking {

        val flow1 = (1..3).asFlow().map{ "one $it" }.onEach { delay(300) }
        val flow2 = (4..6).asFlow().map{ "two $it" }.onEach { delay(400) }
        val flow3 = (7..9).asFlow().map{ "three $it" }.onEach { delay(500) }
        val flow4 = flowOf(flow1, flow2, flow3).flattenMerge()

        flow4.collect {
            println(it)
        }
    }

    @Test
    fun flowExceptionTest() = runBlocking {
        fun foo(): Flow<Int> = flow {
            emit(1)
            throw RuntimeException()
        }

        foo()
            .onCompletion { cause -> if (cause != null) println("Flow completed exceptionally") }
            .catch { cause -> println("Caught exception") }
            .collect { value -> println(value) }

        fun foo2(): Flow<Int> = (1..3).asFlow()

        foo2()
            .onCompletion { cause -> println("Flow completed with $cause") }
            .collect { value ->
                check(value <= 1) { "Collected $value" }
                println(value)
            }
    }

    @Test
    fun flowLaunchingTest() = runBlocking {
        fun events(): Flow<Int> = (1..3).asFlow().onEach { delay(100) }

        events()
            .onEach { event -> println("Event: $event") }
            .launchIn(this)
        println("Done")
    }

}