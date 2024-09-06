import com.xml.guard.utils.MappingHandler
import com.xml.guard.utils.allDependencyAndroidProjects
import com.xml.guard.utils.inClassNameBlackList
import com.xml.guard.utils.javaDirs
import com.xml.guard.utils.toUpperLetterStr
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.regex.Pattern

open class RenameSourceFilesWithReferencesTask : DefaultTask() {

    @Input
    var targetPackages: Array<String> = arrayOf("com/example/myapp")

    @Input
    var whitelist: Set<String> = emptySet()

    @OutputFile
    val mappingFile: File = File("${project.projectDir}/rename_mapping.txt")
    //类名索引
    private var classIdx = -1L

    @TaskAction
    fun renameSourceFilesAndUpdateReferences() {
        val mappingHandler = MappingHandler(mappingFile)
        println("new rename class whiteList:$whitelist")
        val androidProjects = allDependencyAndroidProjects()
        androidProjects.forEach {
            val dirs = it.javaDirs("")
            dirs.forEach { dir->
                targetPackages.forEach { targetPackage->
                    val sourceDir = File("${dir.path}/${targetPackage.replace(".", "/")}")
                    if (sourceDir.isDirectory){
                        println(sourceDir)
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
        classIdx = mappingHandler.getMapping().size.toLong()
        dir.walkTopDown().forEach { file ->
            if (file.name.endsWith(".java") || file.name.endsWith(".kt")) {
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

        val resDir = File("${dir}/res")
        processDirectory(resDir, mappingHandler, extensions)
    }

    private fun processDirectory(dir: File, mappingHandler: MappingHandler, extensions: List<String>) {
        dir.walkTopDown().forEach { file ->
            if (extensions.any { ext -> file.name.endsWith(ext) }) {
                updateReferencesInFile(file, mappingHandler)
            }
        }
    }

    private fun updateReferencesInFile(file: File, mappingHandler: MappingHandler) {
        var content = file.readText()
        var updated = false

        // 处理 import 语句中的类名替换
        mappingHandler.getMapping().forEach { (originalClass, renamedClass) ->
            val packagePrefix = originalClass.substringBeforeLast('.')
            val oldClassName = originalClass.substringAfterLast('.')
            val newClassName = renamedClass.substringAfterLast('.')

            // 匹配 import 语句并替换
            val importPattern = Pattern.compile("""import\s+($packagePrefix\.)?\b$oldClassName\b""")
            val updatedContent = importPattern.matcher(content).replaceAll { result ->
                "import ${packagePrefix}.$newClassName"
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

            // 只替换外部类名，不替换内嵌类引用（A.B）
            val classUsagePattern = Pattern.compile("""(?<!\.)\b$oldClassName\b(?:\s*\.\.\.)?(?!\.)""")
            val updatedContent = classUsagePattern.matcher(content).replaceAll { matchResult ->
                // 检查当前引用是否位于外部类或嵌套类的上下文中
                if (isInsideInnerClassContext(matchResult.start(), content, oldClassName)) {
                    oldClassName // 保留内部类
                } else {
                    newClassName // 替换外部类
                }
            }

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

    // 用于检查是否处于嵌套类上下文中
    private fun isInsideInnerClassContext(position: Int, content: String, oldClassName: String): Boolean {
        // 检查是否在类的嵌套范围内，例如 A.B 中的 B 不应该被替换
        val innerClassPattern = Pattern.compile("""\b(\w+)\.$oldClassName\b""") // 找到 class 声明
        val matcher = innerClassPattern.matcher(content.substring(0, position))
        return matcher.find()
    }

    // 判断是否在匿名类上下文中（这个逻辑可以忽略，具体实现可能不适合你的场景）
    private fun isInsideAnonymousClassContext(referenceIndex: Int, content: String): Boolean {
        // 匹配 new K{} 这种匿名内部类的形式
        val pattern = Regex("""new\s+\w+\s*\{\s*""")
        return pattern.find(content.substring(0, referenceIndex)) != null
    }
}