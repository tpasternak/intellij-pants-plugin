// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FastpassManagerDialog extends DialogWrapper {
  public FastpassManagerDialog(
    @NotNull Project project,
    @NotNull PantsBspData importData,
    @NotNull CompletableFuture<Set<String>> importedTargets,
    @NotNull Function<VirtualFile, CompletableFuture<Collection<String>>> targetsListFetcher
  ) {
    super(project, false);
    setTitle(PantsBundle.message("pants.bsp.select.targets"));
    init();

    importedTargets.whenComplete((targets, error) ->
                               SwingUtilities.invokeLater(() -> {
                                   if (error == null) {
                                     mainPanel.removeAll();
                                     manager = new FastpassChooseTargetsPanel(project, importData, targets, targetsListFetcher);
                                     mainPanel.add(manager);
                                     setOKButtonText(CommonBundle.getOkButtonText());
                                     mainPanel.updateUI();
                                   }
                                   else {
                                     mainPanel.removeAll();
                                     mainPanel.add(new JLabel(PantsBundle.message("pants.bsp.error.failed.to.fetch.targets"),
                                                              PlatformIcons.ERROR_INTRODUCTION_ICON,
                                                              SwingConstants.CENTER
                                     ));
                                     logger.error(error);
                                     mainPanel.updateUI();
                                   }
                               }));

  }

  @NotNull
  static Logger logger = Logger.getInstance(FastpassManagerDialog.class);

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

  public Optional<Collection<String>> selectedItems() {
    return Optional.ofNullable(manager)
      .map(FastpassChooseTargetsPanel::selectedItems);
  }

  public static Optional<Set<String>> promptForTargetsToImport(
    Project project,
    PantsBspData importData,
    CompletableFuture<Set<String>> importedTargets,
    Function<VirtualFile, CompletableFuture<Collection<String>>> fetchTargetsList
  ) {
    try {
      FastpassManagerDialog dial =
        new FastpassManagerDialog(project, importData, importedTargets, fetchTargetsList);
      dial.show();
      return dial.isOK() ? dial.selectedItems().map(HashSet::new) : Optional.empty();
    }catch (Throwable e) {
      logger.error(e);
      return Optional.empty();
    }
  }
}
