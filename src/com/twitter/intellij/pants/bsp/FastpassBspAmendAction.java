// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.ui.FastpassManagerDialog;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BSP;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FastpassBspAmendAction extends AnAction {

  private Logger logger = Logger.getInstance(FastpassBspAmendAction.class);

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // [x] todo: don't freeze UI at the beginning
    // [x] todo: block for non-bsp project
    try {
      Project project = event.getProject(); // [x] todo handle null
      if (project != null) {
        Set<PantsBspData> linkedProjects = PantsBspData.importsFor(project);
        if (linkedProjects.size() > 1) {
          Messages.showErrorDialog(
            PantsBundle.message("pants.bsp.error.failed.more.than.one.bsp.project.not.supported.message"),
            PantsBundle.message("pants.bsp.error.action.not.supported.title")
          );
        }
        else if (linkedProjects.size() < 1) {
          Messages.showErrorDialog(
            PantsBundle.message("pants.bsp.error.failed.not.a.bsp.pants.project.message"),
            PantsBundle.message("pants.bsp.error.action.not.supported.title")
          );
        }
        else {
          PantsBspData importData = linkedProjects.stream().findFirst().get();
          startAmendProcedure(project, importData);
        }
      }
      else {
        Messages.showErrorDialog(
          PantsBundle.message("pants.bsp.error.no.project.found"),
          PantsBundle.message("pants.bsp.error.action.not.supported.title")
        );
      }
    } catch (Throwable e) {
      logger.error(e);
    }
  }

  private void startAmendProcedure(Project project, PantsBspData firstProject) {
    // [x] TODO = raczej from getLinkedProjects powinno iść
    CompletableFuture<Set<String>> oldTargets = FastpassUtils.selectedTargets(firstProject);

    FastpassTargetListCache targetsListCache = new FastpassTargetListCache();

    // [x] todo co jak importedPantsRootsSize ==0?
    // todo handle "all in dir" targets selection (::)
    Optional<Set<String>> newTargets = FastpassManagerDialog
      .promptForTargetsToImport(project, firstProject, oldTargets,
                                targetsListCache::getTargetsList
      );
    // [x] todo freeze here!
    amendAndRefreshIfNeeded(project, firstProject, oldTargets, newTargets);
  }

  private void amendAndRefreshIfNeeded(
    @NotNull Project project,
    @NotNull PantsBspData basePath,
    @NotNull CompletableFuture<Set<String>> oldTargets,
    @NotNull Optional<Set<String>> newTargets
  ) {
    oldTargets.thenAccept(
      oldTargetsVal -> newTargets.ifPresent(newTargetsVal -> {
        if (!newTargets.get().equals(oldTargetsVal)) {
          try {
            refreshProjectsWithNewTargetsList(project, newTargets.get(), basePath);
          }
          catch (Throwable e) {
            logger.error(e);
          }
        }
      })
    );
  }

  private void refreshProjectsWithNewTargetsList(
    @NotNull Project project,
    Collection<String> newTargets,
    PantsBspData basePath
  ) throws InterruptedException, IOException {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        // [x] TODO złap błedy // a co jak jest więcej linked projektów?
        FastpassUtils.amendAll(basePath, newTargets).get();
      } catch (Throwable e){
        logger.error(e);
      }
    },"Amending", false, project );
    ExternalProjectUtil.refresh(project, BSP.ProjectSystemId());
  }
}
