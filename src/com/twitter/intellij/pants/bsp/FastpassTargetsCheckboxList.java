// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;


import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

class FastpassTargetsCheckboxList extends JComponent {

  public FastpassTargetsCheckboxList(Consumer<String> onSelection,
                             Consumer<String> onDeselection) {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    checkboxPanel.setCheckBoxListListener ((index, value) -> {
      String item = checkboxPanel.getItemAt(index);
      if (value) onSelection.accept(item); else onDeselection.accept(item);
    });
    mainPanel.add(myScrollPaneCheckbox);
    this.add(mainPanel);
  }

  private JPanel createMainPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setPreferredSize(JBUI.size(300, 500));
    return mainPanel;
  }

  @NotNull
  CheckBoxList<String> checkboxPanel =  new CheckBoxList<>();

  @NotNull
  JPanel mainPanel = createMainPanel();

  JScrollPane myScrollPaneCheckbox = ScrollPaneFactory.createScrollPane(checkboxPanel);




  private void updateCheckboxList(Collection<String> targets, Set<String> selected) {
    checkboxPanel.setItems(new ArrayList<>(targets), x -> x);
    for (String target : targets) {
      checkboxPanel.setItemSelected(target, selected.contains(target));
    }
  }

  private void updateTargetsListWithMessage(JComponent icon){
    mainPanel.remove(0);
    mainPanel.add(icon);
  }

  public void  setItems(Collection<String> value, Set<String> selected) {
    mainPanel.remove(0);
    mainPanel.add(myScrollPaneCheckbox);
    updateCheckboxList(value, selected);
  }

  public void setLoading() {
    updateTargetsListWithMessage(new AsyncProcessIcon(""));
  }

  public void clear() {
    setItems(Collections.emptyList(), Collections.emptySet());
  }
}