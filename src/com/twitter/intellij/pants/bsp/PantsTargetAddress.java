// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

// Todo testme
public class PantsTargetAddress {

  public enum AddressKind {
    ALL_TARGETS_FLAT,
    ALL_TARGETS_DEEP,
    SINGLE_TARGETS
  }

  @NotNull private Path myPath;
  @NotNull private AddressKind myKind;
  @NotNull private Optional<String> myTargets;

  public PantsTargetAddress(@NotNull Path path, @NotNull AddressKind kind, @NotNull Optional<String> targets) {
    myPath = path;
    myKind = kind;
    myTargets = targets;
  }

  // todo testme
  public String toAddressString() {
    switch (myKind)  {
      case SINGLE_TARGETS: return myPath + ":" + myTargets.get();
      case ALL_TARGETS_FLAT: return myPath + ":";
      case ALL_TARGETS_DEEP: return myPath + "::";
    }
    throw new RuntimeException("Very bad"); //todo better
  }

  public static PantsTargetAddress fromString(String s) {
    String[] strings = s.split(":");

    if (strings.length == 2) {
      return new PantsTargetAddress(Paths.get(strings[0]), AddressKind.SINGLE_TARGETS, Optional.of(strings[1]));
    } else if (s.endsWith("::")) {
      return new PantsTargetAddress(Paths.get(strings[0]), AddressKind.ALL_TARGETS_DEEP, Optional.empty());
    } else if (s.endsWith(":")) {
      return new PantsTargetAddress(Paths.get(strings[0]), AddressKind.ALL_TARGETS_FLAT, Optional.empty());
    } else {
      throw new RuntimeException("PantsTargetAddress: could not parse string '" + s + "'");// [x] todo better
    }
  }

  public Path getPath() {
    return myPath;
  }

  public AddressKind getKind() {
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

