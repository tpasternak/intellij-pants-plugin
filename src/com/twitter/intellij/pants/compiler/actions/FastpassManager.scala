// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.awt.BorderLayout

import com.intellij.CommonBundle
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.{CheckBoxList, ScrollPaneFactory}
import com.intellij.util.ui.JBUI
import javax.swing.event.TreeSelectionEvent
import javax.swing.{JComponent, JPanel, SwingConstants}

import scala.collection.JavaConverters._
import scala.util.Try

class FastpassManager(project: Project, dir: VirtualFile, selectedItems: Seq[String]) extends DialogWrapper(project, false){
  setTitle("Fastpass manager")
  setButtonsAlignment(SwingConstants.CENTER)
  setOKButtonText(CommonBundle.getOkButtonText)
  init()

  var checkboxPanel: CheckBoxList[String] = _
  var mySelectedItems = selectedItems

  override def createCenterPanel(): JComponent = {
    val panel = new JPanel()

    val descriptor = new FileChooserDescriptor(true, false,
                                               false, false,
                                               false, false)
    val internalTree = new com.intellij.ui.treeStructure.Tree()
    val myFileSystemTree = new FileSystemTreeImpl(project, descriptor, internalTree, null, null, null)
    myFileSystemTree.select(dir, new Runnable {
      override def run(): Unit = ()
    })
    val scrollPane = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree());
    panel.add(scrollPane, BorderLayout.CENTER);
    panel.setPreferredSize(JBUI.size(400))
    checkboxPanel = new CheckBoxList[String]()
    checkboxPanel.addItem("Nothing", "Nothing", true)
    panel.add(checkboxPanel, BorderLayout.CENTER)

    myFileSystemTree.getTree.getSelectionModel().addTreeSelectionListener(
      (e: TreeSelectionEvent) => {
        val f = myFileSystemTree.getSelectedFile

        val checkboxPanelItems = Try{FastpassUtils.availableTargets(f).toList}.getOrElse(List())
        checkboxPanel.clear()
        checkboxPanelItems.foreach(item => {
          checkboxPanel.addItem(item, item, mySelectedItems.contains(item))
        })
      }
    )


    panel
  }
}
