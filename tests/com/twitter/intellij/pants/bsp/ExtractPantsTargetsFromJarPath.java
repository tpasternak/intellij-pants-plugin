// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.testFramework.UsefulTestCase;

import java.nio.file.Paths;
import java.util.Optional;

public class ExtractPantsTargetsFromJarPath extends UsefulTestCase {
  public void testFlatDirectory () {
    Optional<String> actual = AmendEditorNotificationsProvider.jarPathToTarget(Paths.get("/tmp/directory.targetname.jar!/myFile"));
    assertEquals("directory:targetname", actual.get());
  }

  public void testDeepDirectory () {
    Optional<String> actual = AmendEditorNotificationsProvider.jarPathToTarget(Paths.get("/tmp/dirparent.dirchild.targetname.jar!/myFile"));
    assertEquals("dirparent/dirchild:targetname", actual.get());
  }

  public void testFlatDirectorySourcesJar () {
    Optional<String> actual = AmendEditorNotificationsProvider.jarPathToTarget(Paths.get("/tmp/directory.targetname-sources.jar!/myFile"));
    assertEquals("directory:targetname", actual.get());
  }

  public void testDeepDirectorySourcesJar () {
    Optional<String> actual = AmendEditorNotificationsProvider.jarPathToTarget(Paths.get("/tmp/dirparent.dirchild.targetname-sources.jar!/myFile"));
    assertEquals("dirparent/dirchild:targetname", actual.get());
  }
}
