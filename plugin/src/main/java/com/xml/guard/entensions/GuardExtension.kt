package com.xml.guard.entensions

import java.io.File

/**
 * User: ljx
 * Date: 2022/3/2
 * Time: 12:46
 */
open class GuardExtension {

    /*
     * 是否查找约束布局的constraint_referenced_ids属性的值，并添加到AndResGuard的白名单中，
     * 是的话，要求你在XmlClassGuard前依赖AabResGuard插件，默认false
     */
    var findAndConstraintReferencedIds = false

    /*
     * 是否查找约束布局的constraint_referenced_ids属性的值，并添加到AabResGuard的白名单中，
     * 是的话，要求你在XmlClassGuard前依赖AabResGuard插件，默认false
     */
    var findAabConstraintReferencedIds = false

    /**
     * 是否查找约束布局的constraint_referenced_ids属性的值，并添加到resChiper的白名单中，
     * 是的话，要求你在XmlClassGuard前依赖resChiper插件，默认false
     */
    var findResChiperConstraintReferencedIds = false

    var mappingFile: File? = null

    var packageChange = HashMap<String, String>()

    var moveDir = HashMap<String, String>()

    /**
     * 白名单，不混淆的类全路径名称。
     */
    var whiteList: Set<String> = HashSet()
}