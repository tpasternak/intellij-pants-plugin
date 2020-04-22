// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BSP;
import scala.Option;
import scala.collection.immutable.Set;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FastpassBspAmendAction extends AnAction {

  private Logger logger = Logger.getInstance(FastpassBspAmendAction.class);

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    try {
      Project project = event.getProject(); // todo handle null
      String[] targets = FastpassUtils.selectedTargets(project.getBasePath());
      List<VirtualFile> importedPantsRoots = FastpassUtils.pantsRoots(project);
      FastpassTargetListCache targetsListCache = new FastpassTargetListCache();
      // todo co jak importedPantsRootsSize ==0?
      Optional<Collection<String>> newTargets = FastpassManager
        .promptForTargetsToImport(project, importedPantsRoots.get(0), Stream.of(targets).collect(Collectors.toList()), importedPantsRoots,
                                  targetsListCache::getTargetsList
        );

      if (newTargets.isPresent() && newTargets.get() != Stream.of(targets)) {
          refreshProjectsWithNewTargetsList(project, newTargets.get(), event.getProject().getBasePath());
      }
    }
    catch (Throwable e) {
      logger.error(e);
    }
  }


  private void refreshProjectsWithNewTargetsList(
    Project project,
    Collection<String> newTargets,
    String basePath
  ) throws InterruptedException, ExecutionException, IOException {
    FastpassUtils.amendAll(basePath, newTargets); // TODO złap błedy // a co jak jest więcej linked projektów?
    ExternalProjectUtil.refresh(project, BSP.ProjectSystemId());
  }
}
