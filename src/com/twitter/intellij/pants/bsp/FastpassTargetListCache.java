// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;


import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.bsp.ui.PantsTargetAddress;

import java.util.Collection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class FastpassTargetListCache {

  ConcurrentHashMap<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> cache = new ConcurrentHashMap<>();

  // todo z jakiegoś cholernego powodu pada import `res:`
  // [x] todo coś w rodzaju putifabsent
  CompletableFuture<Collection<PantsTargetAddress>>  getTargetsList(VirtualFile file) {
    return cache.computeIfAbsent(file, FastpassUtils::availableTargetsIn);
  }
}