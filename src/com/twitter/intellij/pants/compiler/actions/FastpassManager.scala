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
import com.intellij.ui.{CheckBoxList, ScrollPaneFactory}
import com.intellij.util.ui.{AsyncProcessIcon, JBUI}
import javax.swing.{BoxLayout, JComponent, JLabel, JPanel, JScrollPane, SwingConstants, SwingUtilities}

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

  var checkboxPanel: CheckBoxList[String] = _

  var myFileSystemTree: FileSystemTreeImpl = _

  var mySelectedItems: Set[String] = initiallySelectedItems


  var myPanel: JPanel = _

  var myScrollPaneCheckbox: JScrollPane = _

  def selectedItems: Set[String] = mySelectedItems

  override def createCenterPanel(): JComponent = {
    setupCheckboxPanel
    myPanel = new JPanel()
    myPanel.setLayout(new BoxLayout(myPanel,BoxLayout.X_AXIS))
    myFileSystemTree = setupFileTree
    val scrollPaneFileTree = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree);
    scrollPaneFileTree.setPreferredSize(JBUI.size(300,500))
    myPanel.add(scrollPaneFileTree);

    myScrollPaneCheckbox = ScrollPaneFactory.createScrollPane(checkboxPanel)
    myPanel.add(myScrollPaneCheckbox, 1)
    myPanel.remove(1)
    myPanel.add (new JLabel(icons.DvcsImplIcons.CurrentBranchLabel), 1)
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

  private def setupCheckboxPanel() = {
    checkboxPanel = new CheckBoxList[String]()
    //checkboxPanel.setPreferredSize(JBUI.size(300, 500))
    checkboxPanel.setCheckBoxListListener(
      (index: Int, value: Boolean) => {
        val item = checkboxPanel.getItemAt(index)
        if (value) {
          mySelectedItems = mySelectedItems + item
        }
        else {
          mySelectedItems = mySelectedItems - item
        }
      }
      )
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

  var cache = new TargetListCache()

  private def updateCheckboxList(selectedFile: VirtualFile) = {
    val response = cache.get(selectedFile)
    if(!response.isDone) {
      myPanel.remove(1)
      myPanel.add (new AsyncProcessIcon(""), 1)
      myPanel.updateUI()
      //fillCheckboxList(List("Waiting"))
    }
    cache.get(selectedFile).whenComplete((value, error) =>
                                              SwingUtilities.invokeLater { () => {
                                                if (myFileSystemTree.getSelectedFile == selectedFile) {
                                                  if(error == null) {

                                                    myPanel.remove(1) // todo JAKOŚ ŁADNIEJ!!!!!
                                                    myPanel.add(myScrollPaneCheckbox, 1)
                                                    myPanel.updateUI()

                                                    fillCheckboxList(value)
                                                  } else {
                                                    myPanel.remove(1)
                                                    myPanel.add (new JLabel(icons.DvcsImplIcons.CurrentBranchLabel), 1)
                                                    myPanel.updateUI()

//                                                    fillCheckboxList(List("nothing")) // todo - jakiś ładniejszy komunikat
                                                  }
                                                }
                                              }})
  }

  private def fillCheckboxList(targets: Iterable[String]) = {
    checkboxPanel.setItems(targets.toList.asJava, x => x)
    targets.foreach(item => {
      checkboxPanel.setItemSelected(item, mySelectedItems.contains(item))
    })
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