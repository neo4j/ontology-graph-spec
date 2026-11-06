package org.neo4j.spec.graph.javagen

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

/**
 * Loads [sourceRoots] into a standalone Analysis API session with [classpath] as library dependencies.
 * The Kotlin common sources are analysed as JVM code, which is how the existing jvm target compiles them.
 */
class Session(sourceRoots: List<Path>, classpath: List<Path>) : AutoCloseable {
    private val disposable: Disposable = Disposer.newDisposable("javagen")
    val files: List<KtFile>

    init {
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                val libraries = buildKtLibraryModule {
                    platform = JvmPlatforms.defaultJvmPlatform
                    libraryName = "classpath"
                    classpath.forEach { addBinaryRoot(it) }
                }
                addModule(
                    buildKtSourceModule {
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "commonMain"
                        sourceRoots.forEach { addSourceRoot(it) }
                        addRegularDependency(libraries)
                    }
                )
            }
        }
        files = session.modulesWithFiles.values.flatten().filterIsInstance<KtFile>().sortedBy { it.virtualFilePath }
    }

    override fun close() = Disposer.dispose(disposable)
}
