// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;


import com.intellij.CommonBundle;
import com.intellij.history.core.Paths;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


class FastpassChooseTargetsPanel extends JPanel {
  public FastpassChooseTargetsPanel(
    @NotNull Project project,
    @NotNull VirtualFile dir,
    @NotNull Collection<String> importedTargets,
    @NotNull Collection<VirtualFile> importedPantsRoots,
    @NotNull Function<VirtualFile, CompletableFuture<Collection<String>>> targetsListFetcher
  ) {
    mySelectedTargets = new HashSet<>(importedTargets);

    myProject = project;
    myDir = dir;
    myImportedPantsRoots = importedPantsRoots;
    myTargetsListFetcher = targetsListFetcher;

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
    myFileSystemTree = createFileTree();
    JScrollPane fileSystemTreeScrollPane = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree());
    fileSystemTreeScrollPane.setPreferredSize(JBUI.size(400, 500));
    mainPanel.add(fileSystemTreeScrollPane);

    myTargetsListPanel = new FastpassTargetsCheckboxList(
      item -> mySelectedTargets.add(item),
      item -> mySelectedTargets.remove(item)
    );
    mainPanel.add(myTargetsListPanel);

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(mainPanel);
  }

  @NotNull
  Project myProject;

  @NotNull
  VirtualFile myDir;

  @NotNull
  Collection<VirtualFile> myImportedPantsRoots;

  @NotNull
  Function<VirtualFile, CompletableFuture<Collection<String>>> myTargetsListFetcher;

  @NotNull
  Set<String> mySelectedTargets;

  JPanel mainPanel = null;

  FileSystemTreeImpl myFileSystemTree = null;

  FastpassTargetsCheckboxList myTargetsListPanel = null;

  @NotNull
  public Collection<String> selectedItems() {
    return mySelectedTargets;
  }


  private FileSystemTreeImpl createFileTree() {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false,
                                                                 false, false,
                                                                 false, false
    );
    FileSystemTreeImpl fileSystemTree = new FileSystemTreeImpl(myProject, descriptor, new Tree(), null, null, null);
    fileSystemTree.select(myDir, null);
    fileSystemTree.expand(myDir, null);
    fileSystemTree.showHiddens(true);
    fileSystemTree.updateTree();
    fileSystemTree.getTree().getSelectionModel().addTreeSelectionListener(event -> handleTreeSelection(fileSystemTree));
    return fileSystemTree;
  }

  private void handleTreeSelection(FileSystemTreeImpl myFileSystemTree) {
    VirtualFile selectedFile = myFileSystemTree.getSelectedFile();
    if (selectedFile != null &&
        myImportedPantsRoots.stream().anyMatch(root -> belongsToImportedPantsProject(selectedFile, root))
    ) {
      updateCheckboxList(selectedFile);
    }
  }

  private boolean belongsToImportedPantsProject(
    VirtualFile selectedFile,
    VirtualFile root
  ) {
    return Paths.isParent(root.getPath(), selectedFile.getPath()) && !root.getPath().equals(selectedFile.getPath());
  }

  private void updateCheckboxList(VirtualFile selectedFile) {
    CompletableFuture<Collection<String>> targetsList = myTargetsListFetcher.apply(selectedFile);
    if (!targetsList.isDone()) {
      myTargetsListPanel.setLoading();
      mainPanel.updateUI();
    }
    targetsList.whenComplete((value, error) ->
                               SwingUtilities.invokeLater(() -> {
                                 if (myFileSystemTree.getSelectedFile().equals(selectedFile)) {
                                   if (error == null) {
                                     myTargetsListPanel.setItems(value, mySelectedTargets);
                                     mainPanel.updateUI();
                                   }
                                   else {
                                     myTargetsListPanel.setItems(Collections.emptyList(), Collections.emptySet());
                                     mainPanel.updateUI();
                                   }
                                 }
                               }));
  }
}
