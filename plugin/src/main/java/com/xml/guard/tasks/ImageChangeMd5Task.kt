package com.xml.guard.tasks

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.model.ClassInfo
import com.xml.guard.model.MappingParser
import com.xml.guard.utils.allDependencyAndroidProjects
import com.xml.guard.utils.findClassByLayoutXml
import com.xml.guard.utils.findClassByManifest
import com.xml.guard.utils.findFragmentInfoList
import com.xml.guard.utils.findLocationProject
import com.xml.guard.utils.findPackage
import com.xml.guard.utils.findXmlDirs
import com.xml.guard.utils.getDirPath
import com.xml.guard.utils.javaDirs
import com.xml.guard.utils.manifestFile
import com.xml.guard.utils.removeSuffix
import com.xml.guard.utils.replaceWords
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * 修改图片md5.
 */
open class ImageChangeMd5Task @Inject constructor(
    guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    private var whiteList = guardExtension.imageWhiteList
    private var count = guardExtension.changeImageMD5Count

    @TaskAction
    fun execute() {
        println(" whiteList : $whiteList")
        val androidProjects = allDependencyAndroidProjects()
        //1、遍历res下的xml文件，找到自定义的类(View/Fragment/四大组件等)，并将混淆结果同步到xml文件内
        androidProjects.forEach { handleResDir(it) }
//        //2、仅修改文件名及文件路径，返回本次修改的文件
//        val classMapping = mapping.obfuscateAllClass(project, variantName,whiteList)
//        if (hasNavigationPlugin && fragmentDirectionList.isNotEmpty()) {
//            fragmentDirectionList.forEach {
//                classMapping["${it}Directions"] = "${classMapping[it]}Directions"
//            }
//        }
//        //3、替换Java/kotlin文件里引用到的类
//        if (classMapping.isNotEmpty()) {
//            androidProjects.forEach { replaceJavaText(it, classMapping) }
//        }
//        //4、混淆映射写出到文件
//        mapping.writeMappingToFile(mappingFile)
    }

    //处理res目录
    private fun handleResDir(project: Project) {
        val packageName = project.findPackage()
        //过滤res目录下的drawable,mipmap目录
        val xmlDirs = project.findXmlDirs(variantName, "drawable", "mipmap")
        project.files(xmlDirs).asFileTree.forEach { xmlFile ->
            guardImage(project, xmlFile, packageName)
        }
    }

    private fun guardImage(project: Project, xmlFile: File, packageName: String) {
        val parentName = xmlFile.parentFile.name
        println("parentName: $parentName")
        println("file name: ${xmlFile.name}")
        if (!xmlFile.name.isNullOrEmpty()){
            if (xmlFile.name.lowercase().endsWith(".png")
                || xmlFile.name.lowercase().endsWith(".jpg")
                || xmlFile.name.lowercase().endsWith(".jpeg")){
                //如果在白名单，
                if (whiteList.contains(xmlFile.name)){
                    println("[image whiteList] path: ${xmlFile.name}")
                    return
                }

                val byte = ByteArray(count)
                for (i in 0 until  count) {
                    byte[i]=0
                }
                xmlFile.appendBytes(byte)
            }
        }
    }
}