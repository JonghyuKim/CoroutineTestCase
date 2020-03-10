package com.hyu.coroutinetest

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class CoroutinConcurency{
    var counter = 0
    //solution 1
    var atomicCounter = AtomicInteger()

    //solution 2 local, wide local singleThreadContext
    var singleThread = newSingleThreadContext("CounterContext")

    //solution 3 lock
    val mutex = Mutex()


    @Test
    fun concurencyTest() = runBlocking{
        suspend fun CoroutineScope.massiveRun(action: suspend () -> Unit) {
            val n = 100  // number of coroutines to launch
            val k = 1000 // times an action is repeated by each coroutine
            val time = measureTimeMillis {
                val jobs = List(n) {
                    launch {
                        repeat(k) { action() }
                    }
                }
                jobs.forEach { it.join() }
            }
            println("Completed ${n * k} actions in $time ms")
        }

//        CoroutineScope(singleThread).massiveRun {
        GlobalScope.massiveRun {
            mutex.withLock {
                counter++
            }
//            atomicCounter.incrementAndGet()
//            withContext(singleThread){
//                counter++
//            }
        }

        println("Counter = ${counter}")
//        println("Counter = ${atomicCounter.get()}")
    }
}