package com.ljx.example

import org.junit.Test

import org.junit.Assert.*
import test.BI

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val content = "        public void setProducts(BH... ps) {\n" +
                "            this.id = id;\n" +
                "        }\n"
        val oldClassName = "BH"
        val newClassName = "AB"
        BI.match(content,oldClassName,newClassName)
    }
}