package com.xml.guard.tasks

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.utils.MappingHandler
import com.xml.guard.utils.allDependencyAndroidProjects
import com.xml.guard.utils.inClassNameBlackList
import com.xml.guard.utils.javaDirs
import com.xml.guard.utils.toUpperLetterStr
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

open class RenameSourceFilesWithReferencesTask @Inject constructor(
    guardExtension: GuardExtension,
    private val variantName: String,
)  : DefaultTask() {
    init {
        group = "guard"
    }

    private var whitelist = guardExtension.renameModelWhiteList
    private var targetPackages = guardExtension.renameModelPackages
    private var offsetNameIdx = guardExtension.renameNameOffsetIdx
    //类名索引
    private var classIdx = -1L

    @TaskAction
    fun execute() {
        println("new rename class whiteList:$whitelist")
        classIdx += offsetNameIdx
        val mappingFile: File = File("${project.projectDir}/rename_mapping.txt")
        val mappingHandler = MappingHandler(mappingFile)
        val androidProjects = allDependencyAndroidProjects()
        androidProjects.forEach {
            val dirs = it.javaDirs(variantName)
            dirs.forEach { dir->
                targetPackages.forEach { targetPackage->
                    val sourceDir = File("${dir.path}/${targetPackage.replace(".", "/")}")
                    println(sourceDir)
                    if (sourceDir.isDirectory){
                        processSourceDirectory(dir.path,sourceDir, mappingHandler)
                    }
                }
            }
        }
        androidProjects.forEach {
            println(it.projectDir)
            // 更新引用
            updateReferences(it.projectDir.path,mappingHandler)
        }

        // 保存类名映射文件
        mappingHandler.saveMapping()
    }

    private fun processSourceDirectory(path:String,dir: File, mappingHandler: MappingHandler) {
        classIdx = mappingHandler.getMapping().size.toLong()+offsetNameIdx
        dir.walkTopDown().forEach { file ->
            if (file.name.endsWith(".java") || file.name.endsWith(".kt")) {
                println(file.name)
                handleSourceFile(path,file, mappingHandler)
            }
        }
    }

    private fun handleSourceFile(path: String,file: File, mappingHandler: MappingHandler) {
//        println("$path,${file.parentFile}")
        if (!whitelist.contains(file.name)) {
            var content = file.readText()
            val className = file.nameWithoutExtension
            val fileExt = file.extension
            val packagePath = file.parentFile.toString()
                .replace("${path}\\", "")
                .replace(File.separator, ".")
            val fullyQualifiedClassName = "$packagePath.$className"
            val newClassName: String
            val newFileName: String
            if (mappingHandler.isRenamed(fullyQualifiedClassName)){
                println("$fullyQualifiedClassName is renamed!")
                return
            }
            if (mappingHandler.containsClass(fullyQualifiedClassName)) {
                val oldName = mappingHandler.getMappedClassName(fullyQualifiedClassName)!!
                newClassName = oldName.substringAfterLast('.')
                val na = newClassName.substringAfterLast('.')
                newFileName = "${na}.$fileExt"
//                println("contain class $fullyQualifiedClassName  ,  new name :$newFileName , class name : $newClassName")
            } else {
                newClassName = generateObfuscateClassName()
                newFileName = "${newClassName}.$fileExt"
                mappingHandler.addMapping(fullyQualifiedClassName, "$packagePath.$newClassName")
            }

            // Rename file
            val newFile = File(file.parentFile, newFileName)
            file.renameTo(newFile)
//            println("new name:"+newFile.name)
            // Update class name in content
            val classUsagePattern = Pattern.compile("""(?<!\.)\b$className\b""")
            content = classUsagePattern.matcher(content).replaceAll(newClassName)
            newFile.writeText(content)
        }
    }

    //生成混淆的类名
    private fun generateObfuscateClassName(): String {
        while (true) {
            val obfuscateClassName = (++classIdx).toUpperLetterStr()
            if (!obfuscateClassName.inClassNameBlackList()) //过滤黑名单
                return obfuscateClassName
        }
    }

    private fun updateReferences(dir:String,mappingHandler: MappingHandler) {
        val extensions = listOf(".java", ".kt", ".xml")
        val srcDir = File("${dir}/src")
//        println("srcDir:$srcDir")
        processDirectory(srcDir, mappingHandler, extensions)

//        val resDir = File("${dir}/res")
//        processDirectory(resDir, mappingHandler, extensions)
    }

    private fun processDirectory(dir: File, mappingHandler: MappingHandler, extensions: List<String>) {
        dir.walkTopDown().forEach { file ->
            if (extensions.any { ext -> file.name.endsWith(ext) }) {
                updateReferencesInFile(file, mappingHandler)
            }
        }
    }

    private fun updateReferencesInFile(file: File, mappingHandler: MappingHandler) {
//        println("file:$file")
        var content = file.readText()
        var updated = false

        // 处理 import 语句中的类名替换
        mappingHandler.getMapping().forEach { (originalClass, renamedClass) ->
            val packagePrefix = originalClass.substringBeforeLast('.')
            val oldClassName = originalClass.substringAfterLast('.')
            val newClassName = renamedClass.substringAfterLast('.')

            // 匹配 import 语句并替换
            val importPattern = Pattern.compile("""(?:\bimport\s+)?\b$packagePrefix\.$oldClassName\b""")
            val updatedContent = importPattern.matcher(content).replaceAll { result ->
                if (result.group().indexOf("import")>-1){
                    "import ${packagePrefix}.$newClassName"
                }else{
                    "${packagePrefix}.$newClassName"
                }
            }

            if (content != updatedContent) {
                content = updatedContent
                updated = true
            }
        }

        // 替换局部类的引用（处理没有全限定名的类引用，但排除内嵌类的情况）
        mappingHandler.getMapping().forEach { (originalClass, renamedClass) ->
            val oldClassName = originalClass.substringAfterLast('.')
            val newClassName = renamedClass.substringAfterLast('.')
            // 只替换外部类名，不替换内部类引用（A.B）
            val classUsagePattern = Pattern.compile("""(?<!\.)\b$oldClassName\b(?:\s*\.\.\.)?(?!\.)""")
            val matcher = classUsagePattern.matcher(content)
            val sb = StringBuffer()
            while (matcher.find()) {
                val end = if(matcher.group().endsWith("...")) "..." else ""
                if (isInsideInnerClassContext(matcher.start(), content, oldClassName)) {
                    matcher.appendReplacement(sb, oldClassName+end) // 保留内部类
                } else {
                    matcher.appendReplacement(sb, newClassName+end) // 替换外部类
                }
            }
            matcher.appendTail(sb)
            val updatedContent = sb.toString()
//            val updatedContent = classUsagePattern.matcher(content).replaceAll { matchResult ->
//                // 检查当前引用是否位于外部类或嵌套类的上下文中
//                if (isInsideInnerClassContext(matchResult.start(), content, oldClassName)) {
//                    oldClassName // 保留内部类
//                } else {
//                    newClassName // 替换外部类
//                }
//            }
            if (content != updatedContent) {
                content = updatedContent
                updated = true
            }
        }

        // 处理形如 A.B 的内部类引用替换（A -> D，A.B -> D.B）
        mappingHandler.getMapping().forEach { (originalClass, renamedClass) ->
            val oldClassName = originalClass.substringAfterLast('.')
            val newClassName = renamedClass.substringAfterLast('.')

            // 匹配嵌套类 A.B 的引用
            val nestedClassPattern = Pattern.compile("""\b$oldClassName\.(\w+)\b""")
            val updatedContent = nestedClassPattern.matcher(content).replaceAll {
                "$newClassName.${it.group(1)}"
            }

            if (content != updatedContent) {
                content = updatedContent
                updated = true
            }
        }

        if (updated) {
            file.writeText(content)
        }
    }
    //去除注释内容
    private fun removeComments(content: String): String {
        // 去除多行注释，使用非贪婪模式
        val noMultilineComments = content.replace(Regex("""/\*[\s\S]*?\*/"""), "")
        // 去除单行注释，避免误删包含 `http://` 的文本
        // 仅匹配以 `//` 开头的内容，确保 `http://` 这样的文本不会被误删
        val noSingleLineComments = noMultilineComments.replace(Regex("""(?<![\w\.])//[^\r\n]*"""), "")
        return noSingleLineComments
    }
    // 用于检查是否处于嵌套类上下文中
    private fun isInsideInnerClassContext(position: Int, content: String, oldClassName: String): Boolean {
//        // 检查是否在类的嵌套范围内，例如 A.B 中的 B 不应该被替换
//        val innerClassPattern = Pattern.compile("""\b(?:public\s+|private\s+|protected\s+|internal\s+)?(?:static\s+)?(?:class|data\s+class|object|enum\s+class|sealed\s+class)\s+\w+(?:\s*:\s*\w+)?(?:\s*,\s*\w+)*\s*\{\s*[^}]*\b$oldClassName\b""")
//        val matcher = innerClassPattern.matcher(content.substring(0, position))
//        return matcher.find()
//        val innerClassPattern = Pattern.compile("""\b(?:public\s+|protected\s+|private\s+)?(?:static\s+)?(?:data\s+|sealed\s+|inner\s+|object\s+)?class\s+$oldClassName\b""")
//        // 在完整的文件内容中匹配，但只考虑到当前位置之前的部分
//        val matcher = innerClassPattern.matcher(content)
//        while (matcher.find()) {
////            println("inner content:${matcher.start()} , $position , ${matcher.start() < position}")
//            // 仅检查匹配的类声明是否出现在当前引用之前
//            if (matcher.start() <= position) {
//                return true  // 在 oldClassName 类的定义之前找到匹配，说明是内部类
//            }
//        }
//        return false  // 如果没有匹配到，说明不在内部类的上下文中
        val classPattern = Pattern.compile("""\b(?:public\s+|protected\s+|private\s+)?(?:static\s+)?(?:data\s+|sealed\s+|inner\s+|object\s+)?class\s+(\w+)""")
        val matcher = classPattern.matcher(content)

        // Check all class declarations in the content
        while (matcher.find()) {
            val className = matcher.group(1)
            if (className == oldClassName) {
                // 如果匹配到的类名是内部类，返回 true
                return true
            }
        }

        return false
    }

    // 判断是否在匿名类上下文中（这个逻辑可以忽略，具体实现可能不适合你的场景）
    private fun isInsideAnonymousClassContext(referenceIndex: Int, content: String): Boolean {
        // 匹配 new K{} 这种匿名内部类的形式
        val pattern = Regex("""new\s+\w+\s*\{\s*""")
        return pattern.find(content.substring(0, referenceIndex)) != null
    }
}