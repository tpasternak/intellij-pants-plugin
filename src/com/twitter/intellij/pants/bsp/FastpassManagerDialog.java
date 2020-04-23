// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;

import javax.swing.JComponent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class FastpassManagerDialog extends DialogWrapper {
  @NotNull private final VirtualFile myDir;
  @NotNull private final Collection<String> myTargets;
  @NotNull private final Collection<VirtualFile> myRoots;
  @NotNull private final Function<VirtualFile, CompletableFuture<Collection<String>>> myFetcher;
  @NotNull private final Project myProject;

  public FastpassManagerDialog(
    @NotNull Project project,
    @NotNull VirtualFile dir,
    @NotNull Collection<String> importedTargets,
    @NotNull Collection<VirtualFile> importedPantsRoots,
    @NotNull Function<VirtualFile, CompletableFuture<Collection<String>>> targetsListFetcher
  ) {
    super(project, false);
    manager = new FastpassChooseTargetsPanel(project, dir, importedTargets,importedPantsRoots, targetsListFetcher);
    setTitle(PantsBundle.message("pants.bsp.select.targets")); 
    setOKButtonText(CommonBundle.getOkButtonText());
    init();

  }

  @NotNull
  FastpassChooseTargetsPanel manager;

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return manager;
  }

  public Collection<String> selectedItems() {
    return manager.selectedItems();
  }

  public static Optional<Set<String>> promptForTargetsToImport(
    Project project,
    VirtualFile selectedDirectory,
    Set<String> importedTargets,
    Collection<VirtualFile> importedPantsRoots,
    Function<VirtualFile, CompletableFuture<Collection<String>>> fetchTargetsList
  ) {
    FastpassManagerDialog dial = new FastpassManagerDialog(project, selectedDirectory, importedTargets, importedPantsRoots, fetchTargetsList);
    dial.show();
    return dial.isOK() ? Optional.of(new HashSet<>(dial.selectedItems())) : Optional.empty();
  }
}
