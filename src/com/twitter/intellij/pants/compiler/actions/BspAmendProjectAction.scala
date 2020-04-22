// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Optional
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap}

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.twitter.intellij.pants.util.{ExternalProjectUtil, PantsConstants, PantsUtil}
import org.apache.commons.io.IOUtils
import org.jetbrains.bsp.BSP

import scala.collection.JavaConverters._
import scala.util.Try
import FastpassUtils._
import com.intellij.openapi.diagnostic.Logger

import scala.collection.concurrent

sealed class TargetListCache {
  var cache: concurrent.Map[VirtualFile, CompletableFuture[Iterable[String]]] =
    new ConcurrentHashMap[VirtualFile, CompletableFuture[Iterable[String]]]().asScala

  // todo z jakiegoś cholernego powodu pada import `res:`
  def getTargetsList(file: VirtualFile): CompletableFuture[Iterable[String]] = {
    cache.get(file) match {
      case Some(targets) => targets
      case None => {
        val result = FastpassUtils2.availableTargetsIn(file).thenApply(_.asScala)
        cache.put(file, result)
        result
      }
    }
  }
}

sealed class BspAmendProjectAction extends AnAction{
  private val logger = Logger.getInstance(classOf[BspAmendProjectAction])

  case class Error(msg: String) extends RuntimeException

  implicit class OptionalExtension[T](optional: Optional[T]) {
    def toOption: Option[T] = Option(optional.orElseGet(null))
  }

  override def actionPerformed(event: AnActionEvent): Unit = try {
      val project = Option(event.getProject).get
      val targets = FastpassUtils2.selectedTargets(project.getBasePath).toSet
      val importedPantsRoots = FastpassUtils2.pantsRoots(project).asScala.toSet
      val targetsListCache = new TargetListCache
      val newTargets = FastpassManager.promptForTargetsToImport(project, importedPantsRoots.head, targets, importedPantsRoots, file => targetsListCache.getTargetsList(file))
      newTargets.foreach {
        newTargets =>
          if(newTargets != targets) {
            refreshProjectsWithNewTargetsList(project, newTargets, event.getProject.getBasePath)
          }
      }
  } catch {
    case e: Throwable => logger.error(e)
  }

  private def refreshProjectsWithNewTargetsList(project: Project,
                                                newTargets: Set[String],
                                                basePath: String) = {
    FastpassUtils2.amendAll(basePath, newTargets.toList.asJava) // TODO złap błedy // a co jak jest więcej linked projektów?
    ExternalProjectUtil.refresh(project, BSP.ProjectSystemId)
  }
}

object FastpassUtils {
  implicit class OptionalExtension[T](optional: Optional[T]) {
    def toOption: Option[T] = Option(optional.orElseGet(null))
  }



}