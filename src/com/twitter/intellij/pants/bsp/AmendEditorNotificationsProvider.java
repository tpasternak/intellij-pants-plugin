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
import com.twitter.intellij.pants.bsp.ui.FastpassManagerDialog;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.bsp.BSP;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
    Path p = Paths.get(file.getPath());
    Optional<String> targetName = jarPathToTarget(p);
    if (targetName.isPresent()) {
      EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.createActionLabel(PantsBundle.message("pants.bsp.editor.convert.button"), () -> {
        try {
          FastpassBspAmendAction.bspAmendWithDialog(project, Collections.singleton(targetName.get()));
        } catch (Throwable e) {
          logger.error(e);
        }
      });
      return panel.text(PantsBundle.message(
        "pants.bsp.file.editor.amend.notification.title",
        targetName.get()
      ));
    }
    else {
      return null;
    }
  }

  @NotNull
  public static Optional<String> jarPathToTarget(@NotNull Path p) {
    List<Path> allParentNames =
      Stream.iterate(p, x -> x != null ? x.getParent() : null)
        .limit(100)
        .filter(Objects::nonNull)
        .map(x -> x.getFileName())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    Optional<Path> jarName = allParentNames.stream().filter(x -> x.toString().endsWith(".jar!")).findFirst();
    return jarName.map(x -> {
      String cleanString = x.toString().contains("-sources")
                           ? x.toString().substring(0, x.toString().length() - 13)
                           : x.toString().substring(0, x.toString().length() - 5);
      String[] col = cleanString.split("\\.");
      return Arrays.stream(col).limit(col.length - 1).collect(Collectors.joining("/"))
             + ":"
             + Arrays.stream(col).skip(col.length - 1).findFirst().get(); // todo zrób to parsowanie jak człowiek
    });
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }
}