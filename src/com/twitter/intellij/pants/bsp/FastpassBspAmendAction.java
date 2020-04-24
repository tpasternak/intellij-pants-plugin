// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BSP;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FastpassBspAmendAction extends AnAction {

  private Logger logger = Logger.getInstance(FastpassBspAmendAction.class);

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // [x] todo: don't freeze UI at the beginning
    // todo: block for non-bsp project
    try {
      Project project = event.getProject(); // todo handle null

      Set<PantsBspData> linkedProjects = PantsBspData.importsFor(project);

      if(linkedProjects.size() != 1) {
        Messages.showErrorDialog(project,
                                 PantsBundle.message("pants.bsp.error.failed.more.than.one.bsp.project.not.supported.message"),
                                 PantsBundle.message("pants.bsp.error.failed.more.than.one.bsp.project.not.supported.title"));
        return;
      }

      PantsBspData firstProject = linkedProjects.stream().findFirst().get();
      VirtualFile importedPantsRoots = firstProject.getPantsRoot();
      Path bspPath = firstProject.getBspPath();

      // [x] TODO = raczej from getLinkedProjects powinno iść
      CompletableFuture<Set<String>> oldTargets = FastpassUtils.selectedTargets(firstProject);

      FastpassTargetListCache targetsListCache = new FastpassTargetListCache();

      // [x] todo co jak importedPantsRootsSize ==0?
      // todo handle "all in dir" targets selection (::)
      Optional<Set<String>> newTargets = FastpassManagerDialog
        .promptForTargetsToImport(project, firstProject, oldTargets,
                                  targetsListCache::getTargetsList
        );
      // todo freeze here!
      amendAndRefreshIfNeeded(project, firstProject, oldTargets, newTargets);
    }
    catch (Throwable e) {
      logger.error(e);
    }
  }

  private void amendAndRefreshIfNeeded(
    @NotNull Project project,
    @NotNull PantsBspData basePath,
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
    PantsBspData basePath
  ) throws InterruptedException, IOException {
    FastpassUtils.amendAll(basePath, newTargets); // TODO złap błedy // a co jak jest więcej linked projektów?
    ExternalProjectUtil.refresh(project, BSP.ProjectSystemId());
  }
}
