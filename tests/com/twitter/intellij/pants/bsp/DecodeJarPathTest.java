// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.testFramework.UsefulTestCase;

import java.nio.file.Paths;
import java.util.Optional;

public class DecodeJarPathTest extends UsefulTestCase {
  public void testDecodeRandomDir() {
    assertEquals(Optional.empty(), AmendEditorNotificationsProvider.decodeJarPath(Paths.get("/tmp/abc")));
  }

  public void testRegularJar() {
    assertEquals(Optional.empty(), AmendEditorNotificationsProvider.decodeJarPath(Paths.get("/tmp/main.jar!")));
  }

  public void testBloopJar() {
    assertEquals(Optional.of("dir:target"), AmendEditorNotificationsProvider.decodeJarPath(Paths.get("/tmp/bloop-jars/dir.target.jar!")));
  }
}
