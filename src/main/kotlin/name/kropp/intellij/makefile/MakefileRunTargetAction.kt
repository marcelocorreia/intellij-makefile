package name.kropp.intellij.makefile

import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManagerEx
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import name.kropp.intellij.makefile.psi.MakefileTarget

class MakefileRunTargetAction(target: MakefileTarget) : AnAction("make ${target.name}", "make ${target.name}", MakefileTargetIcon) {
  override fun actionPerformed(event: AnActionEvent) {
    val context = ConfigurationContext.getFromContext(event.dataContext)

    val producer = MakefileRunConfigurationProducer()
    val configuration = producer.findOrCreateConfigurationFromContext(context)?.configurationSettings ?: return

    (context.runManager as RunManagerEx).setTemporaryConfiguration(configuration)
    ExecutionUtil.runConfiguration(configuration, ExecutorRegistry.getInstance().registeredExecutors.first())
  }
}