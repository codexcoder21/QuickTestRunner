@file:DependsOn("org.example.buildMaven()")

import org.example.add
import org.example.subtract
import community.kotlin.test.quicktest.DependsOn

fun addTest() { kotlin.test.assertEquals(5, add(2,3)) }
fun subTest() { kotlin.test.assertEquals(1, subtract(3,2)) }

