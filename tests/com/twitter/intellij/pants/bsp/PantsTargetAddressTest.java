// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.testFramework.UsefulTestCase;
import com.twitter.intellij.pants.bsp.ui.PantsTargetAddress;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public class PantsTargetAddressTest extends UsefulTestCase {
  public void testDirectEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project:target");
    assertEquals(t, new PantsTargetAddress("project", PantsTargetAddress.SelectionKind.SINGLE_TARGETS, Optional.of("target")));
  }

  public void testRecursiveEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project::");
    assertEquals(t, new PantsTargetAddress("project", PantsTargetAddress.SelectionKind.ALL_TARGETS_RECURSIVE, Optional.empty()));
  }

  public void testFlatEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project:");

    assertEquals(t, new PantsTargetAddress("project", PantsTargetAddress.SelectionKind.ALL_TARGETS_FLAT, Optional.empty()));
  }
}
