// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final public class FastpassUtils {
  // todo document
  public static List<VirtualFile> pantsRoots(@NotNull Project project)  {
    return  Stream.of(ModuleManager.getInstance(project).getModules()).flatMap (
      module ->
        Stream.of(ModuleRootManager.getInstance(module).getSourceRoots()).flatMap (
          sourceRoot -> toStream(PantsUtil.findPantsExecutable(sourceRoot.getPath()).map(VirtualFile::getParent))
        )
    ).collect(Collectors.toList());
  }

  // todo document
  public static void amendAll(@NotNull Path basePath, Collection<String> newTargets) throws InterruptedException, IOException {
    // todo upewnić się, że amendowanie jest robione do dobrego projektu - może być zaimportowanych wiele BSP

    List<String> amendPart = Arrays.asList(
      "amend", basePath.getFileName().toString(),
      "--targets-list", String.join(",", newTargets)
    );
    String[] command = makeFastpassCommand(amendPart);
    Process process = fastpassProcess(command, basePath.getParent());
    process.waitFor();
    if(process.exitValue() != 0) {
      throw new RuntimeException(toString(process.getErrorStream()));
    }
  }

  // todo document
  public static Set<String> selectedTargets(Path basePath) throws IOException, InterruptedException {
    String[] fastpassCommand = makeFastpassCommand(Arrays.asList("info", basePath.getFileName().toString()));
    Process process = fastpassProcess(fastpassCommand, basePath.getParent());
    process.waitFor(); // todo handle cmd line output
    String stdout = toString(process.getInputStream());
    String[] list = stdout.equals("") ? new String[]{} : stdout.split("\n");
    return Stream.of(list).collect(Collectors.toSet());
  }


  private static List<String> coursierPart = Arrays.asList(
    "coursier", "launch", "org.scalameta:metals_2.12:0.8.5-SNAPSHOT", "-r", "ivy2local",
    "--main", "scala.meta.internal.pantsbuild.BloopPants", "--"
  );

  @NotNull
  private static String[] makeFastpassCommand(@NotNull  Collection<String> amendPart) {
    return Stream.concat(coursierPart.stream(), amendPart.stream()).toArray(String[]::new);
  }

  @NotNull
  private static Process fastpassProcess(String[] command, Path fastpassHome) throws IOException {
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.environment().put("FASTPASS_HOME", fastpassHome.toString());
    return builder.start();
  }

  @NotNull
  private static <T>  Stream<T> toStream(@NotNull Optional<T> pantsExecutable) {
    if(pantsExecutable.isPresent()) {
      return Stream.of(pantsExecutable.get());
    } else {
      return Stream.empty();
    }
  }

  @NotNull
  private static String toString(InputStream process) throws IOException {
    return IOUtils.toString(process, StandardCharsets.UTF_8);
  }

  // todo document
  public static CompletableFuture<Collection<String>> availableTargetsIn(VirtualFile file) {
    return CompletableFuture.supplyAsync(
      () ->
        // todo użyj stałej zamiast BUILD
        PantsUtil.listAllTargets( (file.isDirectory())?  Paths.get(file.getPath(), "BUILD").toString() : file.getPath())
      );
  }
}