// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.testFramework.UsefulTestCase;

import java.util.Optional;

public class PantsTargetAddressTest extends UsefulTestCase {
  public void testDirectEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project:target");
    assertEquals(t, new PantsTargetAddress("project", PantsTargetAddress.AddressKind.SINGLE_TARGETS, Optional.of("target")));
  }

  public void testRecursiveEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project::");
    assertEquals(t, new PantsTargetAddress("project", PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP, Optional.empty()));
  }

  public void testFlatEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project:");

    assertEquals(t, new PantsTargetAddress("project", PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT, Optional.empty()));
  }
}
