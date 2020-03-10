package com.hyu.coroutinetest

import kotlinx.coroutines.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class CoroutinSimpleTest{
    @Test
    fun basicCoroutine() {
        GlobalScope.launch {
            delay(1000)
            println("world ${this.coroutineContext}")
        }
        println("hello")

//        Thread.sleep(2000) same code
        runBlocking {
            delay(2000)
        }
    }

    @Test
    fun basicCoroutine2() = runBlocking{
        val job = GlobalScope.launch {
            delay(1000)
            println("world")
        }
        println("hello")
//        delay(2000) same code
        job.join()
    }


    @Test
    fun basicCoroutine3() = runBlocking{
        launch {
            delay(1000)
            println("world")
        }
        println("hello")
    }

    @Test
    fun basicCoroutine4() = runBlocking{
        launch {
            myDelay()
        }
        println("hello")
    }

    private suspend fun myDelay(){
        delay(1000)
        println("world")
    }

    @Test
    fun coroutinScope() = runBlocking {

        launch {
            delay(200L)
            println("Task from runBlocking")
        }

        coroutineScope {
            launch {
                delay(100L)
                println("Task from nested launch")
            }
            delay(500L)
            println("Task from coroutine scope")
        }

        println("Coroutine scope is over")
    }

    @Test
    fun coroutinScope2() = runBlocking {

        launch {
            delay(200L)
            log("Task from runBlocking")
        }

        val result = coroutineScope {
            launch {
                delay(100L)
                log("Task from nested launch")
            }
            delay(500L)
            log("Task from coroutine scope")
            10
        }

        log("scope Result : $result")

        val coroutineContext = Dispatchers.Default + Job()
        withContext(coroutineContext){
            delay(500L)
            log("withContext !!!!")
        }

        log("Coroutine scope is over")
    }

    @Test
    fun cancelTest() = runBlocking{
        val job = launch {
            try {
                repeat(1000){
                    println("I'm sleeping $it ...")
                    delay(500L)
                }
            }
            finally {
                withContext(NonCancellable) {
                    delay(1000)
                    println("main : I'm running finally!")
                }
            }
//            for (i in 1..10) {
////                yield()
//                if (!isActive) {
//                    break
//                }
//                println("I'm sleeping $i ...")
//                Thread.sleep(500L)
//            }
        }

        delay(1300L)
        println("main : I'm tired of waiting!")
        job.cancelAndJoin()
        println("main : Now I can quit.")
    }

    @Test
    fun timeOutCoroutine() = runBlocking{
        withTimeout(1300L) {
            launch {
                try {
                    repeat(1000) { i ->
                        println("I'm sleeping $i ...")
                        delay(500L)
                    }
                } finally {
                    println("main : I'm running finally!")
                }
            }
            println("main : startLaunch")
        }
    }

    @Test
    fun withContextTest() = runBlocking {
        withContext(this.coroutineContext) {
            launch {
                delay(100L)
                println("Task from nested launch")
            }
            delay(500L)
            println("Task from coroutine scope")
        }

        println("Coroutine scope is over")
    }

    @Test
    fun composingSuspendingTest()= runBlocking<Unit> {
        val time = measureTimeMillis {
            //step1 call delay function
//            val one = doSomethingUsefulOne()
//            val two = doSomethingUsefulTwo()
//            println("The answer is ${one + two}")

            //step2 async coroutinBuilder
//            val one = async { doSomethingUsefulOne() }
//            val two = async { doSomethingUsefulTwo() }
//            println("The answer is ${one.await() + two.await()}")

            //step3 using coroutinScope
//            println("The answer is ${concurrentSum()}")

            //step4 check cancellation
            println("The answer is ${failedConcurrentSum()}")

        }
        println("Completed in $time ms")
    }

    suspend fun failedConcurrentSum(): Int = coroutineScope {
        val one = async {

            try {
                delay(Long.MAX_VALUE) // emulates very long computation
                doSomethingUsefulOne()
            } finally {
                println("First child was canceled.")
            }
        }
        val two = async<Int> {
            println("Second child throw an exception.")
            doSomethingUsefulTwo()
            throw ArithmeticException("Exception on purpose.")
        }
        one.await() + two.await()
    }

    suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }

        one.await() + two.await()
    }

    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L)
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L)
        return 29
    }

    @Test
    fun dispatcherTest() = runBlocking{
        println("runBlocking      : I'm working in thread ${Thread.currentThread().name}")

        launch {
            // context of the parent, main runBlocking coroutine
            println("Not Define      : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Default) {
            // will get dispatched to DefaultDispatcher
            println("Default               : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(newSingleThreadContext("MyOwnThread")) {
            // will get its own new thread
            println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
        }
        println("end")
    }

    @Test
    fun unconfinedDispatcherTest() = runBlocking {
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
        }
        launch {
            // context of the parent, main runBlocking coroutine
            println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
        }
        println()
    }

    @Test
    fun threadJumpTest() = runBlocking {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")
                    withContext(ctx2) {
                        log("Working in ctx2")
                    }
                    log("Back to ctx1")
                }
            }
        }
    }

    private fun log(message : String){
        println("[${Thread.currentThread().name}] $message")
    }


    @Test
    fun childJobCancel() = runBlocking {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            // it spawns two other jobs, one with GlobalScope
            GlobalScope.launch {
                log("job1: I run in GlobalScope and execute independently!")
                delay(1000)
                log("job1: I am not affected by cancellation of the request")
            }
            // and the other inherits the parent context
            launch {
                delay(100)
                log("job2: I am a child of the request coroutine")
                delay(1000)
                log("job2: I will not execute this line if my parent request is cancelled")
            }
        }
        delay(500)
        request.cancel() // cancel processing of the request
        delay(1000) // delay a second to see what happens
        log("main: Who has survived request cancellation?")
    }

    @Test
    fun sumContextTest() = runBlocking {
        val activity = SimpleActivity()
        activity.onCreate()
        activity.onResume()
        activity.onPause()
        activity.onDestroy()
    }

    class SimpleActivity {
        private val activityScope = CoroutineScope(Job() + Dispatchers.Default)

        fun onCreate(){
            println("onCreate")
            activityScope.launch {
                println("hello onCreate")
            }
        }

        fun onResume(){
            println("onResume")
        }

        fun onPause(){
            println("onPause")

            delay1000RunScope()
            activityScope.launch {
                delay1000()
                println("delay 1000")
            }

        }

        fun onDestroy(){
            println("onDestroy")
            activityScope.cancel()
        }

        suspend fun delay1000(){
            delay(1000)
        }

        fun delay1000RunScope() = activityScope.async{
            delay(1000)
        }
    }
}