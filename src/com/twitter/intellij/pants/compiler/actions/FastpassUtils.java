package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Executable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FastpassUtils {
  public static int runAmend(String basePath, String chosen) throws IOException, InterruptedException, ExecutionException {
    ProcessBuilder builder = new ProcessBuilder("fastpass-amend", basePath + "/.bsp/bloop.json", chosen); // todo 1. tutaj ma być bardziej getlinkedproject 2. musi się wywalić jeżeli projekt pantsowy nie jest w BSP 3. slashe mają być lepsze
    Process process = builder.start();
    // todo handle cmd line output
    process.waitFor();
    int exitCode = process.exitValue();
    return exitCode;
  }

  public static void amendAll(String basePath, Collection<String> newTargets) throws InterruptedException, ExecutionException, IOException {
    for (String target : newTargets) {
      runAmend(basePath, target);
    } // todo jako jedna komendan

  }


  public static List<VirtualFile> pantsRoots(Project project)  {
    return  Stream.of(ModuleManager.getInstance(project).getModules()).flatMap (
      module ->
      Stream.of(ModuleRootManager.getInstance(module).getSourceRoots()).flatMap (
        sourceRoot -> {
          Optional<VirtualFile> pantsExecutable = PantsUtil.findPantsExecutable(sourceRoot.getPath()).map(VirtualFile::getParent);
          if(pantsExecutable.isPresent()) {
            return Stream.of(pantsExecutable.get());
          } else {
            return Stream.empty();
          }
        }
    )
    ).collect(Collectors.toList());
  }


  public static String[] selectedTargets(String basePath) throws IOException, ExecutionException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder("fastpass-get", basePath + ".bsp/bloop.json"); // todo slashes here
    Process process = builder.start();
    process.waitFor(); // todo handle cmd line output
    String[] list = IOUtils
      .toString(process.getInputStream(), StandardCharsets.UTF_8)
      .split("\n");
    return list;
  }

  public static CompletableFuture<Collection<String>> availableTargetsIn(VirtualFile file) {
    return CompletableFuture.supplyAsync(
      () -> PantsUtil.listAllTargets( (file.isDirectory())?  Paths.get(file.getPath(), "BUILD").toString() : file.getPath()) // todo użyj stałej zamiast BUILD
      );
  }
}
