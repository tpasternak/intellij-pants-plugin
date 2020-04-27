// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.CheckBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Checkbox;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class FastpassTargetsCheckboxList extends JComponent {

  public FastpassTargetsCheckboxList(Consumer<PantsTargetAddress> onSelection,
                             Consumer<PantsTargetAddress> onDeselection) {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    checkboxPanel.setCheckBoxListListener ((index, value) -> {
      PantsTargetAddress item = checkboxPanel.getItemAt(index);
      if (value) onSelection.accept(item); else onDeselection.accept(item);
    });
//    mainPanel.add(myScrollPaneCheckbox);

    JCheckBox c = new JCheckBox("all in dir (:)");

    mainPanel.add(c);

    this.add(mainPanel);
//    CheckBox aggregateAddressesCheckboxList = new CheckBox("all in dir :", mainPanel, "prop");
  }

  private JPanel createMainPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setPreferredSize(JBUI.size(300, 500));
    return mainPanel;
  }

  @NotNull
  CheckBoxList<PantsTargetAddress> checkboxPanel =  new CheckBoxList<>();

  @NotNull
  JPanel mainPanel = createMainPanel();

  JScrollPane myScrollPaneCheckbox = ScrollPaneFactory.createScrollPane(checkboxPanel);


  private void updateCheckboxList(Collection<PantsTargetAddress> targets, Set<PantsTargetAddress> selected) {
    checkboxPanel.setItems(new ArrayList<>(targets), x -> x.toAddressString());
    for (PantsTargetAddress target : targets) {
      checkboxPanel.setItemSelected(target, selected.contains(target));
    }
  }

  private void updateTargetsListWithMessage(JComponent icon){
    mainPanel.removeAll();
    mainPanel.add(icon);
  }

  public void  setItems(Collection<PantsTargetAddress> value, Set<PantsTargetAddress> selected) {
    mainPanel.removeAll();
    mainPanel.add(myScrollPaneCheckbox);
    updateCheckboxList(value, selected);

    JCheckBox c = new JCheckBox("all in dir (:)");
    JCheckBox d = new JCheckBox("recursive all in dir (::)");
    mainPanel.add(c);
    mainPanel.add(d);
  }

  public void setLoading() {
    updateTargetsListWithMessage(new AsyncProcessIcon(""));
  }

  public void clear() {
    setItems(Collections.emptyList(), Collections.emptySet());
  }
}