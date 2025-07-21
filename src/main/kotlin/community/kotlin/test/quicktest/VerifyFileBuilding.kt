package community.kotlin.test.quicktest

import java.io.File
import kompile.*
import kompiler.effects.DiagnosticEffect
import kompiler.effects.DiagnosticSeverity
import kotlinx.algebraiceffects.Effective
import kotlinx.algebraiceffects.NotificationEffect




fun main() {


    val workspace = Workspace(File("src/test/resources/ExampleTestProjectWithBuildScriptAndTests/"))

    val command = "org.example.buildMaven()"
    val output = File("delete-me")

    output.mkdir()

    Effective {
        handler { e: NotificationEffect ->

            //println("Handling ${e.message} of type ${e.javaClass.canonicalName}")

            if(e.javaClass.canonicalName.equals("tools.kotlin.build.runtime.BuildingToCacheLocationEffect")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("tools.kotlin.build.runtime.StartingBuildRuleEffect")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("tools.kotlin.build.runtime.FinishedBuildRuleEffect")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("tools.kotlin.build.runtime.BuildingToCacheLocationEffect")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("tools.kotlin.build.runtime.FinishedBuildingToCacheLocationEffect")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("kompile.buildscript.StartingKotlinCompile")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("kompile.buildscript.FinishedKotlinCompile")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("build.kotlin.jvm.JarBuiltEffect")) {
                // ignore
            }

            else if(e.javaClass.canonicalName.equals("kompile.buildscript.NoEmbeddedBuildRulesEffect")) {
                // ignore
            }

            else if(e is DiagnosticEffect) {

                if(e.diagnostic.severity.equals(DiagnosticSeverity.ERROR) || e.diagnostic.severity.equals(DiagnosticSeverity.WARNING)) {
                    e.printTinyTrace() // TODO: toss(e)
                }
            }

            else {
                println("[ERROR] Unhandled effect: ${e.javaClass.canonicalName}")
                e.printStackTrace()
            }
        }

        workspace.execute(command, output)
    }
}

