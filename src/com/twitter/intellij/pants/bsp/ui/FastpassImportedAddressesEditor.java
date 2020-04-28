// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FastpassImportedAddressesEditor extends JPanel {
  private Collection<PantsTargetAddress> myValue;
  private Set<PantsTargetAddress> mySelected;
  private Path myPath;
  private Consumer<Collection<PantsTargetAddress>> myUpdate;

  public FastpassImportedAddressesEditor(
    Collection<PantsTargetAddress> value,
    Set<PantsTargetAddress> selected,
    Path path,
    Consumer<Collection<PantsTargetAddress>> update) {
    myValue = value;
    mySelected = selected;
    myPath = path;
    myUpdate = update;

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    CheckBoxList<PantsTargetAddress> checkBoxList = createCheckboxList(value, selected, update);
    JScrollPane cbScroll = ScrollPaneFactory.createScrollPane(checkBoxList);
    JCheckBox checkboxSelectAllFlat = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.flat"));
    JCheckBox checkboxSelectAllDeep = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.recursive"));

    Runnable updateEnablement = () -> {
      if(checkboxSelectAllDeep.isSelected()) {
        update.accept(
          Collections.singletonList(new PantsTargetAddress(path.toString(), PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP, Optional.empty())));
        checkBoxList.setEnabled(false);
        checkboxSelectAllFlat.setEnabled(false);
      } else if(checkboxSelectAllFlat.isSelected()){
        update.accept(Collections.singletonList(new PantsTargetAddress(path.toString(), PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT, Optional.empty())));
        checkBoxList.setEnabled(false);
        checkboxSelectAllDeep.setEnabled(false);
      } else {
        checkBoxList.setEnabled(true);
        checkboxSelectAllDeep.setEnabled(true);
        checkboxSelectAllFlat.setEnabled(true);
      }
    };

    checkboxSelectAllFlat.addItemListener(e -> updateEnablement.run());
    checkboxSelectAllDeep.addItemListener(e -> updateEnablement.run());

    if(selected.stream().anyMatch(x -> x.getPath().equals(path.toString()) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT)) {
      checkboxSelectAllFlat.setSelected(true);
    } else if(selected.stream().anyMatch(x -> x.getPath().equals(path.toString()) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP)) {
      checkboxSelectAllDeep.setSelected(true);
    } else if(selected.stream()
      .filter(x -> x.getPath().equals(path.toString()))
      .allMatch(x -> x.getKind() == PantsTargetAddress.AddressKind.SINGLE_TARGETS)) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    } else if(selected.stream().noneMatch(x -> x.getPath().equals(path.toString()))) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    }

    this.add(cbScroll);
    this.add(checkboxSelectAllFlat);
    this.add(checkboxSelectAllDeep);
  }

  private CheckBoxList<PantsTargetAddress> createCheckboxList(Collection<PantsTargetAddress> targets, Set<PantsTargetAddress> selected,
                                                              Consumer<Collection<PantsTargetAddress>> update) {
    CheckBoxList<PantsTargetAddress> checkboxPanel =  new CheckBoxList<>();
    checkboxPanel.setCheckBoxListListener ((index, value) -> {
      List<PantsTargetAddress> newSelected = IntStream
        .range(0, checkboxPanel.getItemsCount())
        .filter(checkboxPanel::isItemSelected)
        .mapToObj(checkboxPanel::getItemAt)
        .collect(Collectors.toList());
      update.accept(newSelected); // [x] todo maybe store path in a different place ...
    });


    checkboxPanel.setPreferredSize(JBUI.size(300,500));
    checkboxPanel.setItems(new ArrayList<>(targets), PantsTargetAddress::toAddressString);
    for (PantsTargetAddress target : targets) {
      checkboxPanel.setItemSelected(target, selected.contains(target));
    }
    return checkboxPanel;
  }
}
