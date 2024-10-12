package test

import java.util.regex.Pattern

object TestUtil {
    fun match(content: String, oldClassName: String,newClassName:String){
        // 只替换外部类名，不替换内部类引用（A.B）
        val classUsagePattern = Pattern.compile("""(?<!\.)\b($oldClassName)\b(?:\s*\.\.\.)?(?!\.\w)""")
        val matcher = classUsagePattern.matcher(content)
        val sb = StringBuffer()
        while (matcher.find()) {
            println(matcher.group() + " ---------")
            val end = if(matcher.group(0).endsWith("...")) "..." else ""
            if (isInsideInnerClassContext(matcher.start(), content, oldClassName)) {
                matcher.appendReplacement(sb, oldClassName+end) // 保留内部类
            } else {
                matcher.appendReplacement(sb, newClassName+end) // 替换外部类
            }
        }
        matcher.appendTail(sb)
        val updatedContent = sb.toString()
        println("old:$content")
        println("new : $updatedContent")
    }
    // 用于检查是否处于嵌套类上下文中
    fun isInsideInnerClassContext(position: Int, content: String, oldClassName: String): Boolean {
        // 检查是否在类的嵌套范围内，例如 A.B 中的 B 不应该被替换
//        val innerClassPattern = Pattern.compile("""\b(?:public\s+)?(?:static\s+)?class\s+$oldClassName\b""")
//        val innerClassPattern = Pattern.compile("""\b(?:public\s+|protected\s+|private\s+)?(?:static\s+)?(?:data\s+|sealed\s+|inner\s+|object\s+)?class\s+$oldClassName\b""")
//        // 在完整的文件内容中匹配，但只考虑到当前位置之前的部分
//        val matcher = innerClassPattern.matcher(content)
//
//        while (matcher.find()) {
//            println("inner content:${matcher.start()} , $position , ${matcher.start() < position}")
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
}