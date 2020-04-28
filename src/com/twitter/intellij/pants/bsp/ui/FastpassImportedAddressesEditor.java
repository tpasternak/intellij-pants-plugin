// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

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
  @NotNull final CheckBoxList<PantsTargetAddress> checkBoxList;
  @NotNull final JScrollPane checkboxListScroll;
  @NotNull final JCheckBox checkboxSelectAllFlat;
  @NotNull final JCheckBox checkboxSelectAllDeep;

  public FastpassImportedAddressesEditor(
    @NotNull Collection<PantsTargetAddress> value,
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path,
    @NotNull Consumer<Collection<PantsTargetAddress>> update) {

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    checkBoxList = createCheckboxList(value, selected, update);
    checkboxListScroll = ScrollPaneFactory.createScrollPane(checkBoxList);

    checkboxSelectAllFlat = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.flat"));
    checkboxSelectAllFlat.addItemListener(e -> updateEnablement(update, path));

    checkboxSelectAllDeep = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.recursive"));
    checkboxSelectAllDeep.addItemListener(e -> updateEnablement(update, path));

    setupInitialCheckboxesSelection(selected, path);

    this.add(checkboxListScroll);
    this.add(checkboxSelectAllFlat);
    this.add(checkboxSelectAllDeep);
  }

  private void setupInitialCheckboxesSelection(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    if(flatAllInDirSelected(selected, path)) {
      checkboxSelectAllFlat.setSelected(true);
    } else if(deepAllInDirSelected(selected, path)) {
      checkboxSelectAllDeep.setSelected(true);
    } else if(singleTargetsSelected(selected, path)) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    } else if(nothingSelected(selected, path)) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    }
  }

  private boolean nothingSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().noneMatch(x -> x.getPath().equals(path.toString()));
  }

  private boolean singleTargetsSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream()
      .filter(x -> x.getPath().equals(path.toString()))
      .allMatch(x -> x.getKind() == PantsTargetAddress.AddressKind.SINGLE_TARGETS);
  }

  private boolean deepAllInDirSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().anyMatch(x -> x.getPath().equals(path.toString()) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP);
  }

  private boolean flatAllInDirSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().anyMatch(x -> x.getPath().equals(path.toString()) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT);
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


  void updateEnablement(Consumer<Collection<PantsTargetAddress>> update, Path path) {
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

}
