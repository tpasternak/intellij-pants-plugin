// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Todo testme
public class PantsTargetAddress {

  public enum SelectionKind {
    ALL_TARGETS_FLAT,
    ALL_TARGETS_RECURSIVE,
    SINGLE_TARGETS
  }

  @NotNull private String myPath;
  @NotNull private SelectionKind myKind;
  @NotNull private Optional<String> myTargets;

  public PantsTargetAddress(@NotNull String path, @NotNull SelectionKind kind, @NotNull Optional<String> targets) {
    myPath = path;
    myKind = kind;
    myTargets = targets;
  }

  public String toAddressString() {
    switch (myKind)  {
      case SINGLE_TARGETS: return myPath + ":" + myTargets.get();
      case ALL_TARGETS_FLAT: return myPath + ":";
      case ALL_TARGETS_RECURSIVE: return myPath + "::";
    }
    throw new RuntimeException("Very bad"); //todo better
  }

  public static PantsTargetAddress fromString(String s) {
    String[] strings = s.split(":");

    if (strings.length == 2) {
      return new PantsTargetAddress(strings[0], SelectionKind.SINGLE_TARGETS, Optional.of(strings[1]));
    } else if (s.endsWith("::")) {
      return new PantsTargetAddress(strings[0], SelectionKind.ALL_TARGETS_RECURSIVE, Optional.empty());
    } else if (s.endsWith(":")) {
      return new PantsTargetAddress(strings[0], SelectionKind.ALL_TARGETS_FLAT, Optional.empty());
    } else {
      throw new RuntimeException("");//todo better
    }
  }

  public String getPath() {
    return myPath;
  }

  public SelectionKind getKind() {
    return myKind;
  }

  public Optional<String> getTargets() {
    return myTargets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PantsTargetAddress selection = (PantsTargetAddress) o;
    return Objects.equals(getPath(), selection.getPath()) &&
           getKind() == selection.getKind() &&
           Objects.equals(getTargets(), selection.getTargets());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPath(), getKind(), getTargets());
  }
}

