package com.overseas.reschiper.plugin.tasks

import com.android.build.gradle.BaseExtension
import com.overseas.reschiper.plugin.Extension
import groovy.util.Node
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Collections
import javax.inject.Inject

/**
 * User: ljx
 * Date: 2022/4/3
 * Time: 20:17
 */
open class RCFindConstraintReferencedIdsTask @Inject constructor(
    private val extensionName: String,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    @TaskAction
    fun execute() {
        val layoutDirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                layoutDirs.addAll(it.findLayoutDirs(variantName))
            }
        }
        val set = findReferencedIds(layoutDirs)
        println("ids size is ${set.size} \n$set")
        val extension = project.extensions.getByName(extensionName)
        val whiteList =
            if ("resChiper" == extensionName && extension is Extension) {
                extension.whiteList
            } else {
                throw IllegalArgumentException("extensionName is $extensionName")
            }
        (whiteList as MutableCollection<String>).addAll(set)
        println("WhiteList1:$whiteList")
    }

    private fun findReferencedIds(layoutDirs: List<File>): Collection<String> {
        val set = HashSet<String>()
        layoutDirs
            .flatMap {
                val listFiles: Array<File>? = it.listFiles { file -> file.name.endsWith(".xml") }
                listFiles?.toMutableList() ?: Collections.emptyList()
            }.forEach { layoutFile ->
                set.addAll(layoutFile.findReferencedIds())
            }
        return set
    }

    private fun File.findReferencedIds(): Set<String> {
        val set = HashSet<String>()
        val childrenList = XmlParser(false, false).parse(this).breadthFirst()
        for (children in childrenList) {
            val childNode = children as? Node ?: continue
            val ids = childNode.attribute("app:constraint_referenced_ids")?.toString()
            if (ids.isNullOrBlank()) continue
            ids.split(",").forEach {
                val id = it.trim()
                if (id.isNotEmpty()) {
                    set.add(if ("resChiper" == extensionName) "*.R.id.${id}" else "R.id.${id}")
                }
            }
        }
        return set
    }
}

fun Project.isAndroidProject() =
    plugins.hasPlugin("com.android.application")
            || plugins.hasPlugin("com.android.library")

fun Project.findLayoutDirs(variantName: String) = findXmlDirs(variantName, "layout")
fun Project.findXmlDirs(variantName: String, vararg dirName: String): ArrayList<File> {
    return resDirs(variantName).flatMapTo(ArrayList()) { dir ->
        dir.listFiles { file, name ->
            //过滤res目录下xxx目录
            file.isDirectory && dirName.any { name.startsWith(it) }
        }?.toList() ?: emptyList()
    }
}
//返回res目录,可能有多个
fun Project.resDirs(variantName: String): List<File> {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    val nameSet = mutableSetOf<String>()
    nameSet.add("main")
    if (isAndroidProject()) {
        nameSet.addAll(variantName.splitWords())
    }
    val resDirs = mutableListOf<File>()
    sourceSet.names.forEach { name ->
        if (nameSet.contains(name)) {
            sourceSet.getByName(name).res.srcDirs.mapNotNullTo(resDirs) {
                if (it.exists()) it else null
            }
        }
    }
    return resDirs
}
internal fun String.splitWords(): List<String> {
    val regex = Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
    return split(regex).map { it.lowercase() }
}



