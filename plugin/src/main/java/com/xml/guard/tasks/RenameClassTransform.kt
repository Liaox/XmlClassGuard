package com.xml.guard.tasks

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor

abstract class RenameClassTransform: AsmClassVisitorFactory<InstrumentationParameters.None> {
//    override fun isInstrumentable(classData: ClassData): Boolean {
//        return classData.className.startsWith("com/example/oldpackage");
//    }
//
//    override fun createClassVisitor(
//        classContext: ClassContext,
//        nextClassVisitor: ClassVisitor
//    ): ClassVisitor {
//        return
//    }
}