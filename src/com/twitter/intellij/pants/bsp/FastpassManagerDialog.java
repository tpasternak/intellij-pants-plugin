// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import scala.reflect.internal.Trees;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class FastpassManagerDialog extends DialogWrapper {
  public FastpassManagerDialog(
    @NotNull Project project,
    @NotNull VirtualFile dir,
    @NotNull CompletableFuture<Set<String>> importedTargets,
    @NotNull Collection<VirtualFile> importedPantsRoots,
    @NotNull Function<VirtualFile, CompletableFuture<Collection<String>>> targetsListFetcher
  ) {
    super(project, false);
    setTitle(PantsBundle.message("pants.bsp.select.targets"));
    setOKButtonText(CommonBundle.getOkButtonText());
    init();

    importedTargets.whenComplete((targets, error) ->
                               SwingUtilities.invokeLater(() -> {
                                   if (error == null) {
                                     mainPanel.remove(0);
                                     manager = new FastpassChooseTargetsPanel(project, dir, targets,importedPantsRoots, targetsListFetcher);
                                     mainPanel.add(manager);
                                     mainPanel.updateUI();
                                   }
                                   else {
                                     mainPanel.updateUI();
                                   }
                               }));

  }

  @NotNull
  static Logger logger = Logger.getInstance(FastpassManagerDialog.class);

  @NotNull
  FastpassChooseTargetsPanel manager;

  @NotNull JPanel mainPanel = new JPanel();

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    mainPanel.setLayout(new BorderLayout());
    mainPanel.setPreferredSize(JBUI.size(800, 600));
    mainPanel.add(new AsyncProcessIcon(""), BorderLayout.CENTER);
    return mainPanel;
  }

  public Collection<String> selectedItems() {
    return manager.selectedItems();
  }

  public static Optional<Set<String>> promptForTargetsToImport(
    Project project,
    VirtualFile selectedDirectory,
    CompletableFuture<Set<String>> importedTargets,
    Collection<VirtualFile> importedPantsRoots,
    Function<VirtualFile, CompletableFuture<Collection<String>>> fetchTargetsList
  ) {
    try {
      FastpassManagerDialog dial =
        new FastpassManagerDialog(project, selectedDirectory, importedTargets, importedPantsRoots, fetchTargetsList);
      dial.show();
      return dial.isOK() ? Optional.of(new HashSet<>(dial.selectedItems())) : Optional.empty();
    }catch (Throwable e) {
      logger.error(e);
      return Optional.empty();
    }
  }
}
