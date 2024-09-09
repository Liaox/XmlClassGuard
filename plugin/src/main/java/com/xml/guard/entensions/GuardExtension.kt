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

    /**
     * 修改图片md5时数据结尾添加0的个数。0代表不修改，其他数字代表添加多少个。
     * 目前只支持jpg或png。
     */
    var changeImageMD5Count = 0

    /**
     * 修改图片数量百分比，最小0，最大1.
     */
    var changeImagePercent = 1f

    /**
     * 修改图片md5的白名单。图片名称。
     */
    var imageWhiteList:Set<String> = HashSet()

    /**
     * 重命名实体的包名集合。
     */
    var renameModelPackages :Set<String> = HashSet()
    /**
     * 重命名实体类包名白名单，暂无逻辑。
     */
    var renameModelWhiteList:Set<String> = HashSet()

    /**
     * 重命名实体类的名称偏移值，影响起始字母选择。各包应不同。
     */
    var renameNameOffsetIdx :Long = 0L
}