// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, ConcurrentMap}

import com.intellij.CommonBundle
import com.intellij.history.core.Paths
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.{CheckBoxList, CheckBoxListListener, ScrollPaneFactory}
import com.intellij.util.ui.{AsyncProcessIcon, JBUI}
import javax.swing.{BoxLayout, Icon, JComponent, JLabel, JPanel, JScrollPane, SwingConstants, SwingUtilities}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Try

class FastpassManager(project: Project,
                      dir: VirtualFile,
                      initiallySelectedItems: Set[String],
                      importedPantsRoots: Set[VirtualFile]
                     ) extends DialogWrapper(project, false) {
  setTitle("Fastpass manager")
  setOKButtonText(CommonBundle.getOkButtonText)
  init()

  var myFileSystemTree: FileSystemTreeImpl = _

  var cache = new TargetListCache()

  var mySelectedItems: Set[String] = initiallySelectedItems

  var myPanel: JPanel = _

  var targetsCheckboxList: TargetsCheckboxList = _

  def selectedItems: Set[String] = mySelectedItems

  override def createCenterPanel(): JComponent = {
    myPanel = new JPanel()
    myPanel.setLayout(new BoxLayout(myPanel,BoxLayout.X_AXIS))
    myFileSystemTree = setupFileTree
    val scrollPaneFileTree = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree);
    scrollPaneFileTree.setPreferredSize(JBUI.size(400,500))

    targetsCheckboxList = new TargetsCheckboxList(item => mySelectedItems = mySelectedItems + item, item => mySelectedItems = mySelectedItems - item)

    myPanel.add(scrollPaneFileTree);
    myPanel.add (targetsCheckboxList, 1)
    myPanel
  }

  private def setupFileTree: FileSystemTreeImpl = {
    val descriptor = new FileChooserDescriptor(true, false,
                                               false, false,
                                               false, false)
    val internalTree = new com.intellij.ui.treeStructure.Tree()
    val myFileSystemTree = new FileSystemTreeImpl(project, descriptor, internalTree, null, null, null)
    myFileSystemTree.select(dir, null) // todo show hidden files?
    myFileSystemTree.updateTree()
    myFileSystemTree.getTree.getSelectionModel.addTreeSelectionListener(_ => updateCombo(myFileSystemTree))
    myFileSystemTree
  }


  var running: Option[Process] = None


  private def updateCombo(myFileSystemTree: FileSystemTreeImpl) = {
    @volatile var selectedFile = myFileSystemTree.getSelectedFile
    if (selectedFile != null &&
        importedPantsRoots.exists(root => Paths.isParent(root.getPath, selectedFile.getPath)
                                          && root.getPath != selectedFile.getPath) // todo report this to the user
    ) {
      updateCheckboxList(selectedFile)
    }
  }

  private def updateCheckboxList(selectedFile: VirtualFile) = {
    val response = cache.get(selectedFile)
    if(!response.isDone) {
      targetsCheckboxList.updateTargetsListWithMessage(new AsyncProcessIcon("Loading targets list")) // todo do bundla
    }
    cache.get(selectedFile).whenComplete((value, error) =>
                                              SwingUtilities.invokeLater { () => {
                                                if (myFileSystemTree.getSelectedFile == selectedFile) {
                                                  if(error == null) {
                                                    targetsCheckboxList.updateTargetsList(value, mySelectedItems)
                                                    myPanel.updateUI()
                                                  } else {
                                                    targetsCheckboxList.updateTargetsList(List(), Set())
                                                    myPanel.updateUI()
                                                  }
                                                }
                                              }})
  }


}

import scala.collection.concurrent

class TargetListCache {
  var cache: concurrent.Map[VirtualFile, CompletableFuture[Iterable[String]]] =
    new ConcurrentHashMap[VirtualFile, CompletableFuture[Iterable[String]]]().asScala

  def get(file: VirtualFile): CompletableFuture[Iterable[String]] = {
    cache.get(file) match {
      case Some(targets) => targets
      case None => {
        val newFut=FastpassUtils.availableTargetsIn(file)
        cache.put(file, newFut)
        newFut
      }
    }
  }
}

class TargetsCheckboxList(onSelection: String => Unit,
                          onDeselection: String => Unit,
                         ) extends JPanel {

  var checkboxPanel: CheckBoxList[String] = new CheckBoxList[String]()
  var myTargetsContainer: JPanel = _
  var myScrollPaneCheckbox: JScrollPane = _

  checkboxPanel.setCheckBoxListListener ((index, value) => {
    val item = checkboxPanel.getItemAt(index)
    if (value) {
      onSelection(item)
    }
    else {
      onDeselection(item)
    }
  })

  myScrollPaneCheckbox = ScrollPaneFactory.createScrollPane(checkboxPanel)
  myTargetsContainer = new JPanel ()
  myTargetsContainer.setLayout(new BoxLayout(myTargetsContainer, BoxLayout.Y_AXIS))
  myTargetsContainer.add(new JLabel(icons.DvcsImplIcons.CurrentBranchLabel))
  this.setPreferredSize(JBUI.size(300, 500))
  this.add(myTargetsContainer)
  this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

  def fillCheckboxList(targets: Iterable[String], selected: Set[String]) = {
    checkboxPanel.setItems(targets.toList.asJava, x => x)
    targets.foreach(item => {
      checkboxPanel.setItemSelected(item, selected.contains(item))
    })
  }

  def updateTargetsListWithMessage(icon: JComponent) = {
    myTargetsContainer.remove(0)
    myTargetsContainer.add (icon)
    this.updateUI()
  }

  def updateTargetsList(value: Iterable[String], selected: Set[String]) = {
    myTargetsContainer.remove(0)
    myTargetsContainer.add(myScrollPaneCheckbox)
    fillCheckboxList(value, selected)
    this.updateUI()
  }

}