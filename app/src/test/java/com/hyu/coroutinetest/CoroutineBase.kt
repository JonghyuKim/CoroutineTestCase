package com.hyu.coroutinetest

fun log(message : String){
    println("[${Thread.currentThread().name}] $message")
}