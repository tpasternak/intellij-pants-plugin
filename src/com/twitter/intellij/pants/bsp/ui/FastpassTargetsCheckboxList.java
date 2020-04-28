// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class FastpassTargetsCheckboxList extends JComponent {

  final private BiConsumer<String, Collection<PantsTargetAddress>> myUpdate;

  public FastpassTargetsCheckboxList(BiConsumer<String, Collection<PantsTargetAddress>> update) {
    myUpdate = update;
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    checkboxPanel.setCheckBoxListListener ((index, value) -> {
      PantsTargetAddress item = checkboxPanel.getItemAt(index);
      List<PantsTargetAddress> selected = IntStream
        .range(0, checkboxPanel.getItemsCount())
        .filter(x -> checkboxPanel.isItemSelected(x))
        .mapToObj(x -> checkboxPanel.getItemAt(x))
        .collect(Collectors.toList());
      update.accept(item.getPath(), selected); // todo maybe store path in a different place ...
    });

    // todo show two special checkboxes at startup

    this.add(mainPanel);
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

  public void  setItems(Collection<PantsTargetAddress> value, Set<PantsTargetAddress> selected, Path path) {
    mainPanel.removeAll();
    mainPanel.add(myScrollPaneCheckbox);
    updateCheckboxList(value, selected);

    JCheckBox checkboxSelectAllFlat = new JCheckBox("all in dir (:)");
    checkboxSelectAllFlat.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.DESELECTED) {
        myUpdate.accept(path.toString(), Collections.emptyList());
      } else if(e.getStateChange() == ItemEvent.SELECTED) {
        myUpdate.accept(path.toString(), Collections.singletonList(new PantsTargetAddress(path.toString(), PantsTargetAddress.SelectionKind.ALL_TARGETS_FLAT, Optional.empty())));
      }
    });

    JCheckBox checkboxSelectAllDeep = new JCheckBox("recursive all in dir (::)");
    checkboxSelectAllDeep.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.DESELECTED) {
        myUpdate.accept(path.toString(), Collections.emptyList());
      } else if(e.getStateChange() == ItemEvent.SELECTED) {
        myUpdate.accept(path.toString(), Collections.singletonList(new PantsTargetAddress(path.toString(), PantsTargetAddress.SelectionKind.ALL_TARGETS_DEEP, Optional.empty())));
      }
    });

    mainPanel.add(checkboxSelectAllFlat);
    mainPanel.add(checkboxSelectAllDeep);
  }

  public void setLoading() {
    updateTargetsListWithMessage(new AsyncProcessIcon(""));
  }

  public void clear() {
    setItems(Collections.emptyList(), Collections.emptySet(), Paths.get("")); // todo no literal ""
  }
}