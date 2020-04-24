// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.bsp.BSP;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

final public class PantsBspData {
  final private Path myBspPath;
  final private VirtualFile myPantsRoot;

  public PantsBspData(Path bspPath, VirtualFile pantsRoot) {
    myBspPath = bspPath;
    myPantsRoot = pantsRoot;
  }

  public Path getBspPath() {
    return myBspPath;
  }

  public VirtualFile getPantsRoot() {
    return myPantsRoot;
  }

  public static Collection<PantsBspData> importsFor(Project project) {


    return
      Arrays.stream(ModuleManager.getInstance(project).getModules())
        .filter(module ->
                  ExternalSystemModulePropertyManager.getInstance(module).getExternalSystemId().equals(BSP.ProjectSystemId().getId()) &&
                  FastpassUtils.pantsRoots(module).findFirst().isPresent()
        )
        .map(module -> {
          VirtualFile pantsRoots = FastpassUtils.pantsRoots(module).findFirst().get();
          Path bspRoot= Paths.get(ExternalSystemModulePropertyManager.getInstance(module).getLinkedProjectPath());
          return new PantsBspData(bspRoot, pantsRoots);
        })
        .collect(Collectors.toSet());
  }
}