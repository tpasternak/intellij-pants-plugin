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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class FastpassTargetsCheckboxList extends JComponent {

  public FastpassTargetsCheckboxList() {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    // [x] todo show two special checkboxes at startup
    this.add(mainPanel);
  }

  private JPanel createMainPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setPreferredSize(JBUI.size(300, 500));
    return mainPanel;
  }

  @NotNull
  JPanel mainPanel = createMainPanel();

  private CheckBoxList<PantsTargetAddress> updateCheckboxList(Collection<PantsTargetAddress> targets, Set<PantsTargetAddress> selected,
                                                              Path path, Consumer<Collection<PantsTargetAddress>> update) {
    CheckBoxList<PantsTargetAddress> checkboxPanel =  new CheckBoxList<>();
    CheckBoxList<PantsTargetAddress> cb = checkboxPanel;
    cb.setCheckBoxListListener ((index, value) -> {
      List<PantsTargetAddress> newSelected = IntStream
        .range(0, cb.getItemsCount())
        .filter(cb::isItemSelected)
        .mapToObj(cb::getItemAt)
        .collect(Collectors.toList());
      update.accept(newSelected); // [x] todo maybe store path in a different place ...
    });


    checkboxPanel.setPreferredSize(JBUI.size(300,500));
    checkboxPanel.setItems(new ArrayList<>(targets), x -> x.toAddressString());
    for (PantsTargetAddress target : targets) {
      checkboxPanel.setItemSelected(target, selected.contains(target));
    }
    return checkboxPanel;
  }

  private void updateTargetsListWithMessage(JComponent icon){
    mainPanel.removeAll();
    mainPanel.add(icon);
  }

  public void  setItems(Collection<PantsTargetAddress> value,
                        Set<PantsTargetAddress> selected,
                        Path path,
                        Consumer<Collection<PantsTargetAddress>> update
  ) {
    mainPanel.removeAll();

    CheckBoxList<PantsTargetAddress> cbList = updateCheckboxList(value, selected, path, update);

    JScrollPane cbScroll = ScrollPaneFactory.createScrollPane(cbList);

    JCheckBox checkboxSelectAllFlat = new JCheckBox("all in dir (:)");

    JCheckBox checkboxSelectAllDeep = new JCheckBox("recursive all in dir (::)");

    checkboxSelectAllFlat.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.DESELECTED) {
        update.accept(Collections.emptyList());
        cbList.setEnabled(true);
        checkboxSelectAllDeep.setEnabled(true);
      } else if(e.getStateChange() == ItemEvent.SELECTED) {
        update.accept(Collections.singletonList(new PantsTargetAddress(path.toString(), PantsTargetAddress.SelectionKind.ALL_TARGETS_FLAT, Optional.empty())));
        cbList.setEnabled(false);
        checkboxSelectAllDeep.setEnabled(false);
      }
    });

    checkboxSelectAllDeep.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.DESELECTED) {
        update.accept(Collections.emptyList());
        cbList.setEnabled(true);
        checkboxSelectAllFlat.setEnabled(true);
      } else if(e.getStateChange() == ItemEvent.SELECTED) {
        update.accept(Collections.singletonList(new PantsTargetAddress(path.toString(), PantsTargetAddress.SelectionKind.ALL_TARGETS_DEEP, Optional.empty())));
        cbList.setEnabled(false);
        checkboxSelectAllFlat.setEnabled(false);
      }
    });

    mainPanel.add(cbScroll);
    mainPanel.add(checkboxSelectAllFlat);
    mainPanel.add(checkboxSelectAllDeep);


    if(selected.stream().anyMatch(x -> x.getPath().equals(path.toString()) && x.getKind() == PantsTargetAddress.SelectionKind.ALL_TARGETS_FLAT)) {
      checkboxSelectAllFlat.setSelected(true);
    }

    if(selected.stream().anyMatch(x -> x.getPath().equals(path.toString()) && x.getKind() == PantsTargetAddress.SelectionKind.ALL_TARGETS_DEEP)) {
      checkboxSelectAllDeep.setSelected(true);
    }

    if(selected.stream()
      .filter(x -> x.getPath().equals(path.toString()))
      .allMatch(x -> x.getKind() == PantsTargetAddress.SelectionKind.SINGLE_TARGETS)) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    }

    if(selected.stream().noneMatch(x -> x.getPath().equals(path.toString()))) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    }
  }


  public void setLoading() {
    updateTargetsListWithMessage(new AsyncProcessIcon(""));
  }


  public void clear() {
    mainPanel.removeAll(); // [x] todo no literal "", clear should not all "SetItems"
  }
}