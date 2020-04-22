// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.util
import com.intellij.ui.{CheckBoxList, ScrollPaneFactory}
import com.intellij.util.ui.{AsyncProcessIcon, JBUI}
import javax.swing.{BoxLayout, JComponent, JPanel, JScrollPane, SwingUtilities}

import scala.collection.JavaConverters._


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

  def setItems(value: util.Collection[String], selected: util.Collection[String]): Unit = {
    mainPanel.remove(0)
    mainPanel.add(myScrollPaneCheckbox)
    updateCheckboxList(value.asScala, selected.asScala.toSet)
  }

  def setLoading(): Unit = {
    updateTargetsListWithMessage(new AsyncProcessIcon(""))
  }
}