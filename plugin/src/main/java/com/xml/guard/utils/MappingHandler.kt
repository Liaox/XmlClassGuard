package com.xml.guard.utils

import java.io.File
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class MappingHandler(private val mappingFile: File) {

    private val mapping: MutableMap<String, String> = mutableMapOf()

    init {
        loadMapping()
    }

    private fun loadMapping() {
        if (mappingFile.exists()) {
            try {
                BufferedReader(FileReader(mappingFile)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        val parts = line.split(" -> ")
                        if (parts.size == 2) {
                            mapping[parts[0]] = parts[1]
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun saveMapping() {
        try {
            BufferedWriter(FileWriter(mappingFile)).use { writer ->
                mapping.forEach { (originalClass, renamedClass) ->
                    writer.write("$originalClass -> $renamedClass")
                    writer.newLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun containsClass(className: String): Boolean {
        return mapping.containsKey(className)
    }

    fun isRenamed(className: String):Boolean{
        return mapping.containsValue(className)
    }

    fun getMappedClassName(className: String): String? {
        return mapping[className]
    }

    fun addMapping(originalClass: String, renamedClass: String) {
        mapping[originalClass] = renamedClass
    }

    fun getMapping(): Map<String, String> {
        return mapping
    }
}
