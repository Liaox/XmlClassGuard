package com.xml.guard

import com.xml.guard.tasks.RenameSourceFilesWithReferencesTask
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.xml.guard.entensions.GuardExtension
import com.xml.guard.model.aabResGuard
import com.xml.guard.model.andResGuard
import com.xml.guard.model.resChiper
import com.xml.guard.tasks.FindConstraintReferencedIdsTask
import com.xml.guard.tasks.ImageChangeMd5Task
import com.xml.guard.tasks.MoveDirTask
import com.xml.guard.tasks.PackageChangeTask
import com.xml.guard.tasks.XmlClassGuardTask
import com.xml.guard.tasks.XmlClassGuardWithMappingTask
import com.xml.guard.utils.AgpVersion
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import kotlin.reflect.KClass

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:03
 */
class XmlClassGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        println("XmlClassGuard version is $version, agpVersion=${AgpVersion.agpVersion}")
        val guardExt = project.extensions.create("xmlClassGuard", GuardExtension::class.java)
        val android = project.extensions.getByName("android") as AppExtension
        project.afterEvaluate {
            android.applicationVariants.all { variant ->
                it.createTasks(guardExt, variant)
            }
        }

//        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
//        androidComponents.onVariants {  variant ->
//            variant.instrumentation.transformClassesWith(
//                StringFogTransform::class.java,
//                InstrumentationScope.ALL) {
//            }
//            variant.instrumentation.setAsmFramesComputationMode(
//                FramesComputationMode.COPY_FRAMES
//            )
//        }
    }

    private fun Project.createTasks(guardExt: GuardExtension, variant: ApplicationVariant) {
        val variantName = variant.name.capitalize()
        createTask("xmlClassGuard$variantName", XmlClassGuardTask::class, guardExt, variantName)
        createTask("xmlClassWithMapping$variantName", XmlClassGuardWithMappingTask::class, guardExt, variantName)
//        createTask("packageChange$variantName", PackageChangeTask::class, guardExt, variantName)
//        createTask("moveDir$variantName", MoveDirTask::class, guardExt, variantName)
        createTask("renameModels$variantName", RenameSourceFilesWithReferencesTask::class, guardExt, variantName)
        if (guardExt.findAndConstraintReferencedIds) {
            createAndFindConstraintReferencedIdsTask(variantName)
        }
        if (guardExt.findAabConstraintReferencedIds) {
            createAabFindConstraintReferencedIdsTask(variantName)
        }
//        if (guardExt.findResChiperConstraintReferencedIds) {
//            createResChiperFindConstraintReferencedIdsTask(variantName)
//        }
        //需要修改图片文件md5.目前仅支持修改jpg或png。
        if (guardExt.changeImageMD5Count>0){
            val taskName = "imageChangeMD5$variantName"
            createTask(taskName, ImageChangeMd5Task::class, guardExt, variantName)
        }
    }

    private fun Project.createAndFindConstraintReferencedIdsTask(variantName: String) {
        val andResGuardTaskName = "resguard$variantName"
        val andResGuardTask = project.tasks.findByName(andResGuardTaskName)
            ?: throw GradleException("AndResGuard plugin required")
        val taskName = "andFindConstraintReferencedIds$variantName"
        val task =
            createTask(taskName, FindConstraintReferencedIdsTask::class, andResGuard, variantName)
        andResGuardTask.dependsOn(task)
    }

    private fun Project.createAabFindConstraintReferencedIdsTask(variantName: String) {
        val aabResGuardTaskName = "aabresguard$variantName"
        val aabResGuardTask = project.tasks.findByName(aabResGuardTaskName)
            ?: throw GradleException("AabResGuard plugin required")
        val taskName = "aabFindConstraintReferencedIds$variantName"
        val task =
            createTask(taskName, FindConstraintReferencedIdsTask::class, aabResGuard, variantName)
        aabResGuardTask.dependsOn(task)
    }
    private fun Project.createResChiperFindConstraintReferencedIdsTask(variantName: String) {
        val resChiperTaskName = "resChiper$variantName"
//        println("resChiper Task name : ${resChiperTaskName} , project.tasks : ${project.tasks.names}")
        val resChiperTask = project.tasks.findByName(resChiperTaskName)
            ?: throw GradleException("ResChiper plugin required")
        val taskName = "resChiperFindConstraintReferencedIds$variantName"
        val task =
            createTask(taskName, FindConstraintReferencedIdsTask::class, resChiper, variantName)
        resChiperTask.dependsOn(task)
    }

    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Android Application plugin required")
        }
    }

    private fun <T : Task> Project.createTask(
        taskName: String,
        taskClass: KClass<T>,
        vararg params: Any
    ): Task = tasks.findByName(taskName) ?: tasks.create(taskName, taskClass.java, *params)
}