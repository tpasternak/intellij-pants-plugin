// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BSP;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FastpassBspAmendAction extends AnAction {

  private Logger logger = Logger.getInstance(FastpassBspAmendAction.class);

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // [x] todo: don't freeze UI at the beginning
    // todo: block for non-bsp project
    try {
      Project project = event.getProject(); // todo handle null
      Path basePath = Paths.get(project.getBasePath()); // TODO = raczej from getLinkedProjects powinno iść
      CompletableFuture<Set<String>> oldTargets = FastpassUtils.selectedTargets(basePath);
      List<VirtualFile> importedPantsRoots = FastpassUtils.pantsRoots(project);
      FastpassTargetListCache targetsListCache = new FastpassTargetListCache();
      // todo co jak importedPantsRootsSize ==0?
      // todo handle "all in dir" targets selection (::)

      Optional<Set<String>> newTargets = FastpassManagerDialog
        .promptForTargetsToImport(project, importedPantsRoots.get(0), oldTargets, importedPantsRoots,
                                  targetsListCache::getTargetsList
        );

      amendAndRefreshIfNeeded(project, basePath, oldTargets, newTargets);
    }
    catch (Throwable e) {
      logger.error(e);
    }
  }

  private void amendAndRefreshIfNeeded(
    @NotNull Project project,
    @NotNull Path basePath,
    @NotNull CompletableFuture<Set<String>> oldTargets,
    @NotNull Optional<Set<String>> newTargets
  ) {
    oldTargets.thenAccept(
      oldTargetsVal -> {
        newTargets.ifPresent(newTargetsVal -> {
          if (!newTargets.get().equals(oldTargetsVal)) {
            try {
              refreshProjectsWithNewTargetsList(project, newTargets.get(), basePath);
            }
            catch (Throwable e) {
              logger.error(e);
            }
          }
        });
      }
    );
  }

  private void refreshProjectsWithNewTargetsList(
    Project project,
    Collection<String> newTargets,
    Path basePath
  ) throws InterruptedException, IOException {
    FastpassUtils.amendAll(basePath, newTargets); // TODO złap błedy // a co jak jest więcej linked projektów?
    ExternalProjectUtil.refresh(project, BSP.ProjectSystemId());
  }
}
