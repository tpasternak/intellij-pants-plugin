// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.Computable;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.python.facet.PythonFacet;
import com.jetbrains.python.facet.PythonFacetType;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.jetbrains.python.testing.TestRunnerService;
import com.twitter.intellij.pants.service.project.model.PythonInterpreterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PantsPythonSetupDataService implements ProjectDataService<PythonSetupData, Module> {
  @NotNull
  @Override
  public Key<PythonSetupData> getTargetDataKey() {
    return PythonSetupData.KEY;
  }

  @Override
  public void importData(
    @NotNull final Collection<DataNode<PythonSetupData>> toImport,
    @Nullable ProjectData projectData,
    @NotNull Project project,
    @NotNull final IdeModifiableModelsProvider modelsProvider
  ) {
    final Set<PythonInterpreterInfo> interpreters = ContainerUtil.map2Set(
      toImport,
      node -> node.getData().getInterpreterInfo()
    );

    if (interpreters.isEmpty()) {
      return;
    }

    final Map<PythonInterpreterInfo, Sdk> interpreter2sdk = new HashMap<>();
    final List<Sdk> createdSdks = new ArrayList<>();

    final PythonSdkType pythonSdkType = PythonSdkType.getInstance();

    ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(project) {
      @Override
      public void execute() {
        for (final PythonInterpreterInfo interpreterInfo : interpreters) {
          final String interpreter = interpreterInfo.getBinary();
          Sdk pythonSdk = PythonSdkUtil.findSdkByPath(interpreter);
          if (pythonSdk == null) {
            final ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
            pythonSdk = jdkTable.createSdk(PathUtil.getFileName(interpreter), pythonSdkType);
            jdkTable.addJdk(pythonSdk);
            final SdkModificator modificator = pythonSdk.getSdkModificator();
            modificator.setHomePath(interpreter);
            modificator.commitChanges();
            createdSdks.add(pythonSdk);
          }
          interpreter2sdk.put(interpreterInfo, pythonSdk);
        }
      }
    });

    createdSdks.forEach(pythonSdkType::setupSdkPaths);

    for (DataNode<PythonSetupData> dataNode : toImport) {
      final PythonSetupData pythonSetupData = dataNode.getData();
      final Sdk pythonSdk = interpreter2sdk.get(pythonSetupData.getInterpreterInfo());
      final Module module = modelsProvider.findIdeModule(pythonSetupData.getOwnerModuleData());
      if (module == null) {
        return;
      }
      FacetManager facetManager = FacetManager.getInstance(module);
      PythonFacet facet = facetManager.getFacetByType(PythonFacetType.getInstance().getId());
      if (facet == null) {
        facet = facetManager.createFacet(PythonFacetType.getInstance(), "Python", null);
        facet.getConfiguration().setSdk(pythonSdk);

        final ModifiableFacetModel facetModel = modelsProvider.getModifiableFacetModel(module);
        facetModel.addFacet(facet);
        TestRunnerService.getInstance(module).setProjectConfiguration("py.test");
      }
    }
  }

  @NotNull
  @Override
  public Computable<Collection<Module>> computeOrphanData(
    @NotNull Collection<DataNode<PythonSetupData>> toImport,
    @NotNull ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {
    return new Computable<Collection<Module>>() {
      @Override
      public Collection<Module> compute() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public void removeData(
    @NotNull Computable<Collection<Module>> toRemove,
    @NotNull Collection<DataNode<PythonSetupData>> toIgnore,
    @NotNull ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {

  }
}
