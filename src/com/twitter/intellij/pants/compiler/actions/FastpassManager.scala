// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.awt.{BorderLayout, Dimension, FlowLayout}

import com.intellij.CommonBundle
import com.intellij.history.core.Paths
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.{CheckBoxList, ScrollPaneFactory}
import com.intellij.util.ui.JBUI
import javax.swing.{BoxLayout, JComponent, JPanel, SwingConstants}

import scala.collection.JavaConverters._
import scala.util.Try

class FastpassManager(project: Project,
                      dir: VirtualFile,
                      initiallySelectedItems: Set[String],
                      importedPantsRoots: Set[VirtualFile]
                     ) extends DialogWrapper(project, false) {
  setTitle("Fastpass manager")
  //setButtonsAlignment(SwingConstants.CENTER)
  setOKButtonText(CommonBundle.getOkButtonText)
  init()

  var checkboxPanel: CheckBoxList[String] = _
  var mySelectedItems: Set[String] = initiallySelectedItems

  def selectedItems: Set[String] = mySelectedItems

  override def createCenterPanel(): JComponent = {
    setupCheckboxPanel
    val panel = new JPanel()
    panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS))
//    panel.setPreferredSize(JBUI.size(500,500))

    val myFileSystemTree = setupFileTree
    val scrollPaneFileTree = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree);
    scrollPaneFileTree.setPreferredSize(JBUI.size(300,500))
    panel.add(scrollPaneFileTree);

    val scrollPaneCheckbox = ScrollPaneFactory.createScrollPane(checkboxPanel)
    panel.add(scrollPaneCheckbox)
    panel
  }

  private def setupFileTree: FileSystemTreeImpl = {
    val descriptor = new FileChooserDescriptor(true, false,
                                               false, false,
                                               false, false)
    val internalTree = new com.intellij.ui.treeStructure.Tree()
    val myFileSystemTree = new FileSystemTreeImpl(project, descriptor, internalTree, null, null, null)
    myFileSystemTree.select(dir, null) // todo hiddens?
    myFileSystemTree.updateTree()
    myFileSystemTree.getTree.getSelectionModel.addTreeSelectionListener(_ => UpdateCombo(myFileSystemTree))
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

  private def UpdateCombo(myFileSystemTree: FileSystemTreeImpl) = {
    val selectedFile = myFileSystemTree.getSelectedFile
    if (selectedFile != null &&
       importedPantsRoots.exists(root => Paths.isParent(root.getPath, selectedFile.getPath)
                                         && root.getPath != selectedFile.getPath)
    ) {
      val checkboxPanelItems = Try {
        FastpassUtils.availableTargets(selectedFile).toList
      }.getOrElse(List())
      checkboxPanel.clear()
      checkboxPanelItems.foreach(item => {
        checkboxPanel.addItem(item, item, mySelectedItems.contains(item))
      })
    } else {
      checkboxPanel.clear()
    }
  }
}
