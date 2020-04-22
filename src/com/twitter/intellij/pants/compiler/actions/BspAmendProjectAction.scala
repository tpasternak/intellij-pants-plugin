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
        val result = FastpassUtils.availableTargetsIn(file)
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

  override def actionPerformed(event: AnActionEvent): Unit = Try {
    for {
      project <- Try{ Option(event.getProject).get }
      targets <- selectedTargets(project.getBasePath).map(_.toSet)
      importedPantsRoots <- pantsRoots(project)
      targetsListCache = new TargetListCache
      newTargets <- FastpassManager.promptForTargetsToImport(project, importedPantsRoots.head, targets, importedPantsRoots, file => targetsListCache.getTargetsList(file))
      _ = newTargets.map {
        newTargets =>
          if(newTargets != targets) {
            refreshProjectsWithNewTargetsList(project, newTargets, event.getProject.getBasePath)
          }
      }
    } yield ()
  }.fold(logger.error, identity)

  private def refreshProjectsWithNewTargetsList(project: Project,
                                                newTargets: Set[String],
                                                basePath: String) = {
    amendAll(basePath, newTargets) // TODO złap błedy // a co jak jest więcej linked projektów?
    ExternalProjectUtil.refresh(project, BSP.ProjectSystemId)
  }
}

object FastpassUtils {
  implicit class OptionalExtension[T](optional: Optional[T]) {
    def toOption: Option[T] = Option(optional.orElseGet(null))
  }

  def amendAll(basePath: String, newTargets: Set[String]) = Try {
    newTargets.foreach(item => runAmend(basePath, item)) // todo jako jedna komenda
  }

  def pantsRoots(project: Project): Try[Set[VirtualFile]] = Try {
    ModuleManager.getInstance(project).getModules.toList.flatMap {
      module =>
        ModuleRootManager.getInstance(module).getSourceRoots.flatMap {
          sourceRoot => PantsUtil.findPantsExecutable(sourceRoot.getPath).toOption.map(_.getParent)
        }
    }.toSet
  }

  def runAmend(basePath: String, chosen: String): Int = {
    val builder = new ProcessBuilder("fastpass-amend", s"${basePath}/.bsp/bloop.json", chosen)// todo 1. tutaj ma być bardziej getlinkedproject 2. musi się wywalić jeżeli projekt pantsowy nie jest w BSP
    val process = builder.start()
    process.onExit().get() // todo handle cmd line output
    val exitCode = process.exitValue()
    exitCode
  }

  def availableTargetsIn(file: VirtualFile): CompletableFuture[Iterable[String]] = {
    CompletableFuture.supplyAsync(
      () => PantsUtil.listAllTargets(if (file.isDirectory) Paths.get(file.getPath, "BUILD").toString else file.getPath).asScala // todo użyj stałej zamiast BUILD
      )
  }

  def selectedTargets(basePath: String): Try[Array[String]] = Try {
    val builder = new ProcessBuilder("fastpass-get", s"${basePath}/.bsp/bloop.json")
    val process = builder.start()
    process.onExit().get() // todo handle cmd line output
    val list = IOUtils
      .toString(process.getInputStream, StandardCharsets.UTF_8)
      .split("\n")
    list
  }
}