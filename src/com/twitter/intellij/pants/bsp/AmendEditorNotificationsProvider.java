// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.EditorNotificationPanel;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.bsp.BspUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class AmendEditorNotificationsProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  Logger logger = Logger.getInstance(AmendEditorNotificationsProvider.class);
  private static final Key<EditorNotificationPanel> KEY = Key.create("fastpass.amend.notification");

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(
    @NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project
  ) {
    if(!BspUtil.isBspProject(project)){
      return null;
    }
    Path path = Paths.get(file.getPath());
    Optional<PantsTargetAddress> targetName = decodeJarPath(path).flatMap(PantsTargetAddress::tryParse);
    if (targetName.isPresent()) {
      EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.createActionLabel(PantsBundle.message("pants.bsp.editor.convert.button"), () -> {
        try {
          FastpassBspAmendAction.bspAmendWithDialog(project, Collections.singleton(targetName.get().toAddressString()));
        } catch (Throwable e) {
          logger.error(e);
        }
      });
      return panel.text(PantsBundle.message(
        "pants.bsp.file.editor.amend.notification.title",
        targetName.get().toAddressString()
      ));
    }
    else {
      return null;
    }
  }

  @NotNull
  public static Optional<String> decodeJarPath(@NotNull Path p) {
    if(!p.toString().contains("bloop-jars")) {
      return Optional.empty();
    }
    List<Path> allParentNames =
      Stream.iterate(p, x -> x != null ? x.getParent() : null)
        .limit(100)
        .filter(Objects::nonNull)
        .map(Path::getFileName)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    Optional<Path> jarName = allParentNames.stream().filter(x -> x.toString().endsWith(".jar!")).findFirst();
    return jarName.flatMap(AmendEditorNotificationsProvider::decodeJarName);
  }

  @NotNull
  public static Optional<String> decodeJarName(@NotNull Path x) {
    String sourceFileSuffix = "-sources.jar!";
    String classFileSuffix = ".jar!";
    String stem = x.toString().contains("-sources")
                         ? x.toString().substring(0, x.toString().length() - sourceFileSuffix.length())
                         : x.toString().substring(0, x.toString().length() - classFileSuffix.length());
    String[] jarFilePartitioned = stem.split("\\.");
    Stream<String> allButLast = Arrays.stream(jarFilePartitioned).limit(jarFilePartitioned.length - 1);
    Optional<String> lastElementOption = Arrays.stream(jarFilePartitioned).skip(jarFilePartitioned.length - 1).findFirst();
    return lastElementOption.map(last -> allButLast.collect(Collectors.joining("/")) + ":" + last);
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }
}