package name.kropp.intellij.makefile

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import com.intellij.util.getOrCreate
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import org.jdom.Element
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class MakefileRunConfigurationType : ConfigurationType {
  override fun getDisplayName() = "Makefile"
  override fun getIcon() = MakefileIcon
  override fun getConfigurationTypeDescription() = "Makefile Target"

  override fun getId() = "MAKEFILE_TARGET_RUN_CONFIGURATION"

  override fun getConfigurationFactories() = arrayOf(MakefileRunConfigurationFactory(this))
}

class MakefileRunConfigurationFactory(runConfigurationType: MakefileRunConfigurationType) : ConfigurationFactory(runConfigurationType) {
  override fun createTemplateConfiguration(project: Project) = MakefileRunConfiguration(project, this, "name")
  override fun isConfigurationSingletonByDefault() = true
}

class MakefileRunConfiguration(project: Project, factory: MakefileRunConfigurationFactory, name: String) : RunConfigurationBase(project, factory, name) {
  var filename: String = ""
  var target: String = ""

  override fun checkConfiguration() {
  }

  override fun getConfigurationEditor() = MakefileRunConfigurationEditor(project)

  override fun writeExternal(element: Element?) {
    super.writeExternal(element)
    val child = element!!.getOrCreate("makefile")
    child.setAttribute("filename", filename)
    child.setAttribute("target", target)
  }

  override fun readExternal(element: Element?) {
    super.readExternal(element)
    val child = element?.getChild("makefile")
    filename = child?.getAttributeValue("filename") ?: ""
    target = child?.getAttributeValue("target") ?: ""
  }

  override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
    return null
  }
}

class MakefileRunConfigurationEditor(private val project: Project) : SettingsEditor<MakefileRunConfiguration>() {
  private val filenameField = TextFieldWithBrowseButton()
  private val targetCompletionProvider = MakefileTextFieldAutocompletionTargetProvider()
  private val targetField = TextFieldWithAutoCompletion<String>(project, targetCompletionProvider, true, "")

  private val panel: JPanel by lazy {
    FormBuilder.createFormBuilder()
        .setAlignLabelOnRight(false)
        .setHorizontalGap(UIUtil.DEFAULT_HGAP)
        .setVerticalGap(UIUtil.DEFAULT_VGAP)
        .addLabeledComponent("&Makefile", filenameField)
        .addLabeledComponent("&Target", targetField)
        .panel
  }

  init {
    filenameField.addBrowseFolderListener("Makefile", "Makefile path", project, MakefileFileChooserDescriptor())
    filenameField.textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(event: DocumentEvent) {
        updateTargetCompletion(filenameField.text)
      }
    })
  }

  fun updateTargetCompletion(filename: String) {
    val file = LocalFileSystem.getInstance().findFileByPath(filename)
    if (file != null) {
      val psiFile = PsiManager.getInstance(project).findFile(file)
      if (psiFile != null) {
        targetCompletionProvider.setItems(findTargets(psiFile).map { it.name })
        return
      }
    }
    targetCompletionProvider.setItems(emptyList())
  }

  override fun createEditor() = panel

  override fun applyEditorTo(configuration: MakefileRunConfiguration) {
    configuration.filename = filenameField.text
    configuration.target = targetField.text
  }

  override fun resetEditorFrom(configuration: MakefileRunConfiguration) {
    filenameField.text = configuration.filename
    targetField.text = configuration.target

    updateTargetCompletion(configuration.filename)
  }
}

class MakefileTextFieldAutocompletionTargetProvider : TextFieldWithAutoCompletionListProvider<String>(emptyList()) {
  override fun getLookupString(str: String) = str
}