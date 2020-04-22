// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.util
import java.util.Optional
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap}

import com.intellij.CommonBundle
import com.intellij.history.core.Paths
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.{CheckBoxList, ScrollPaneFactory}
import com.intellij.util.ui.{AsyncProcessIcon, JBUI}
import javax.swing.{BoxLayout, JComponent, JPanel, JScrollPane, SwingUtilities}

import scala.collection.JavaConverters._
import scala.util.Try

object FastpassManager{
  def promptForTargetsToImport(
                                project: Project,
                                selectedDirectory: VirtualFile,
                                importedTargets: util.Collection[String],
                                importedPantsRoots: util.Collection[VirtualFile],
                                fetchTargetsList: VirtualFile => CompletableFuture[util.Collection[String]]
                              ): Optional[util.Collection[String]] = {
    val dial = new FastpassManager(project, selectedDirectory, importedTargets, importedPantsRoots, fetchTargetsList)
    dial.show()
    if(dial.isOK) Optional.of(dial.selectedItems.asJava) else Optional.empty()
  }
}

sealed class FastpassManager(project: Project,
                             dir: VirtualFile,
                             importedTargets: util.Collection[String],
                             importedPantsRoots: util.Collection[VirtualFile],
                             targetsListFetcher: VirtualFile => CompletableFuture[util.Collection[String]]
                     ) extends DialogWrapper(project, false) {
  setTitle("Fastpass manager")
  setOKButtonText(CommonBundle.getOkButtonText)
  init()

  var mainPanel: JPanel = _

  var myFileSystemTree: FileSystemTreeImpl = _

  var myTargetsListPanel: TargetsCheckboxList = _

  var mySelectedTargets: Set[String] = importedTargets.asScala.toSet

  def selectedItems: Set[String] = mySelectedTargets

  override def createCenterPanel(): JComponent = {
    mainPanel = new JPanel()
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS))

    myFileSystemTree = createFileTree
    val fileSystemTreeScrollPane = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree);
    fileSystemTreeScrollPane.setPreferredSize(JBUI.size(400,500))
    mainPanel.add(fileSystemTreeScrollPane);

    myTargetsListPanel = new TargetsCheckboxList(item => mySelectedTargets = mySelectedTargets + item, item => mySelectedTargets = mySelectedTargets - item)
    mainPanel.add(myTargetsListPanel)

    mainPanel
  }

  private def createFileTree: FileSystemTreeImpl = {
    val descriptor = new FileChooserDescriptor(true, false,
                                               false, false,
                                               false, false)
    val fileSystemTree = new FileSystemTreeImpl(project, descriptor,  new Tree(), null, null, null)
    fileSystemTree.select(dir, null)
    fileSystemTree.expand(dir, null)
    fileSystemTree.showHiddens(true)
    fileSystemTree.updateTree()
    fileSystemTree.getTree.getSelectionModel.addTreeSelectionListener(_ => handleTreeSelection(fileSystemTree))
    fileSystemTree
  }

  private def handleTreeSelection(myFileSystemTree: FileSystemTreeImpl) = {
    val selectedFile = myFileSystemTree.getSelectedFile
    if (selectedFile != null &&
        importedPantsRoots.asScala.exists(root => belongsToImportedPantsProject(selectedFile, root))
    ) {
      updateCheckboxList(selectedFile)
    }
  }

  private def belongsToImportedPantsProject(selectedFile: VirtualFile,
                                            root: VirtualFile): Boolean = {
    Paths.isParent(root.getPath, selectedFile.getPath) && root.getPath != selectedFile.getPath
  }

  private def updateCheckboxList(selectedFile: VirtualFile): Unit= {
    val targetsList = targetsListFetcher(selectedFile)
    if(!targetsList.isDone) {
      myTargetsListPanel.setLoading()
      mainPanel.updateUI()
    }
    targetsList
      .whenComplete{(value, error) =>
        SwingUtilities.invokeLater { () => {
          if (myFileSystemTree.getSelectedFile == selectedFile) {
            if(error == null) {
              myTargetsListPanel.setItems(value.asScala, mySelectedTargets)
              mainPanel.updateUI()
            } else {
              myTargetsListPanel.setItems(List(), Set())
              mainPanel.updateUI()
            }
          }
        }}}
  }
}

sealed class TargetsCheckboxList(onSelection: String => Unit,
                          onDeselection: String => Unit,
                         ) extends JComponent {
  this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

  var checkboxPanel: CheckBoxList[String] = new CheckBoxList[String]()
  var mainPanel: JPanel = _
  var myScrollPaneCheckbox: JScrollPane = _

  checkboxPanel.setCheckBoxListListener ((index, value) => {
    val item = checkboxPanel.getItemAt(index)
    if (value) onSelection(item) else onDeselection(item)
  })
  myScrollPaneCheckbox = ScrollPaneFactory.createScrollPane(checkboxPanel)

  mainPanel = new JPanel ()
  mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS))

  mainPanel.add(myScrollPaneCheckbox)
  mainPanel.setPreferredSize(JBUI.size(300, 500))
  this.add(mainPanel)


  private def updateCheckboxList(targets: Iterable[String], selected: Set[String]) = {
    checkboxPanel.setItems(targets.toList.asJava, x => x)
    targets.foreach(item => {
      checkboxPanel.setItemSelected(item, selected.contains(item))
    })
  }

  private def updateTargetsListWithMessage(icon: JComponent): Unit = {
    mainPanel.remove(0)
    mainPanel.add(icon)
    ()
  }

  def setItems(value: Iterable[String], selected: Set[String]): Unit = {
    mainPanel.remove(0)
    mainPanel.add(myScrollPaneCheckbox)
    updateCheckboxList(value, selected)
  }

  def setLoading(): Unit = {
    updateTargetsListWithMessage(new AsyncProcessIcon(""))
  }
}