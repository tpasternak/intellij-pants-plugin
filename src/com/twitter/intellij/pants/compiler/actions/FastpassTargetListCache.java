// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;


import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class FastpassTargetListCache {

  ConcurrentHashMap<VirtualFile, CompletableFuture<Collection<String>>> cache = new ConcurrentHashMap<>();

  // todo z jakiegoś cholernego powodu pada import `res:`
  CompletableFuture<Collection<String>>  getTargetsList(VirtualFile file) {
    CompletableFuture<Collection<String>> match = cache.get(file); // todo coś w rodzaju putifabsent
    if(match == null) {
      CompletableFuture<Collection<String>> result = FastpassUtils.availableTargetsIn(file);
      cache.put(file, result);
      return result;
    } else {
      return match;
    }
  }
}