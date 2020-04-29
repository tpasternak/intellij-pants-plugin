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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
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
  @NotNull final JLabel statusLabel;
  @NotNull boolean blockedByParent;

  public FastpassImportedAddressesEditor(
    @NotNull Collection<PantsTargetAddress> availableTargetsInPath,
    @NotNull Set<PantsTargetAddress> allSelectedAddresses,
    @NotNull Path path,
    @NotNull Consumer<Collection<PantsTargetAddress>> update) {

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setAlignmentX(Component.LEFT_ALIGNMENT);


    checkBoxList = createCheckboxList(availableTargetsInPath, allSelectedAddresses, update);
    checkboxListScroll = ScrollPaneFactory.createScrollPane(checkBoxList);

    checkboxSelectAllFlat = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.flat"));
    checkboxSelectAllFlat.addItemListener(e -> updateEnablement(update, path));
    checkboxSelectAllFlat.setHorizontalAlignment(SwingConstants.LEFT);

    checkboxSelectAllDeep = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.recursive"));
    checkboxSelectAllDeep.addItemListener(e -> updateEnablement(update, path));
    checkboxSelectAllDeep.setHorizontalAlignment(SwingConstants.LEFT);

    statusLabel = new JLabel(" ");

    setupInitialCheckboxesSelection(allSelectedAddresses, path);

    if(blockedByParent(allSelectedAddresses, path)) {
      checkBoxList.setEnabled(false);
      checkboxSelectAllFlat.setEnabled(false);
      checkboxSelectAllDeep.setEnabled(false);
      statusLabel.setText("Selected by parent");
    }

    this.add(checkboxListScroll);
    this.add(checkboxSelectAllFlat);
    this.add(checkboxSelectAllDeep);
  }

  private boolean blockedByParent(
    @NotNull Set<PantsTargetAddress> allSelectedAddresses,
    @NotNull Path path
  ) {
    return allSelectedAddresses.stream().anyMatch(x -> path.startsWith(x.getPath())
                                                   && !x.getPath().equals(path)
                                                   && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP
    );
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
    return selected.stream().noneMatch(x -> x.getPath().equals(path));
  }

  private boolean singleTargetsSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream()
      .filter(x -> x.getPath().equals(path))
      .allMatch(x -> x.getKind() == PantsTargetAddress.AddressKind.SINGLE_TARGETS);
  }

  private boolean deepAllInDirSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().anyMatch(x -> x.getPath().equals(path) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP);
  }

  private boolean flatAllInDirSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().anyMatch(x -> x.getPath().equals(path) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT);
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

    checkboxPanel.setItems(new ArrayList<>(targets), PantsTargetAddress::toAddressString);
    for (PantsTargetAddress target : targets) {
      checkboxPanel.setItemSelected(target, selected.contains(target));
    }
    return checkboxPanel;
  }

  // todo handle parent deep selected here
  void updateEnablement(Consumer<Collection<PantsTargetAddress>> update, Path path) {
    if(checkboxSelectAllDeep.isSelected()) {
      update.accept(
        // TODO zrób jakieś konstruktory do tego
        Collections.singletonList(new PantsTargetAddress(path, PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP, Optional.empty())));

      checkBoxList.setEnabled(false);
      checkboxSelectAllFlat.setEnabled(false);
      checkboxSelectAllDeep.setEnabled(true);
    } else if(checkboxSelectAllFlat.isSelected()){
      update.accept(Collections.singletonList(new PantsTargetAddress(path, PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT, Optional.empty())));

      checkBoxList.setEnabled(false);
      checkboxSelectAllFlat.setEnabled(true);
      checkboxSelectAllDeep.setEnabled(false);
    } else {
      update.accept(Collections.emptyList());

      checkBoxList.setEnabled(true);
      checkboxSelectAllDeep.setEnabled(true);
      checkboxSelectAllFlat.setEnabled(true);
    }
  };

}
