// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileChooser.{FileChooserDescriptor, FileChooserFactory}
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.twitter.intellij.pants.util.{ExternalProjectUtil, PantsUtil}
import org.apache.commons.io.IOUtils
import org.jetbrains.bsp.{BSP, Icons}

import scala.collection.JavaConverters._
import scala.util.Try
import FastpassUtils._

class BspAmendProjectAction extends AnAction{
  class Error(msg: String) extends RuntimeException


  type Result[T] = Either[Error, T]

  implicit class OptionalExtension[T](optional: Optional[T]) {
    def toOption: Option[T] = Option(optional.orElseGet(null))
  }

  override def actionPerformed(event: AnActionEvent): Unit = Try {
    val project = Option(event.getProject)
    val targets = selectedTargets(project.get.getBasePath).toSet
    val importedPantsRoots =  pantsRoots(project.get)
    val dial = new FastpassManager(project.get, pantsRoots(project.get).head, targets, importedPantsRoots)
    dial.show()
    if(dial.isOK && dial.selectedItems != targets) {
      dial.selectedItems.foreach(item => runAmend(event.getProject.getBasePath, item)) // todo Złap błędy!!!!!!
      ExternalProjectUtil.refresh(project.get, BSP.ProjectSystemId)
    }
  }.getOrElse(())
}

object FastpassUtils {
  implicit class OptionalExtension[T](optional: Optional[T]) {
    def toOption: Option[T] = Option(optional.orElseGet(null))
  }

  def pantsRoots(project: Project): Set[VirtualFile] = {
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
      () => PantsUtil.listAllTargets( if (file.isDirectory) file.getPath + File.separator + "BUILD" else file.getPath).asScala //todo użyj stałej zamiast BUILD
      )
  }

  def selectedTargets(basePath: String): Array[String] = {
    val builder = new ProcessBuilder("fastpass-get", s"${basePath}/.bsp/bloop.json")
    val process = builder.start()
    process.onExit().get() // todo handle cmd line output
    val list = IOUtils
      .toString(process.getInputStream, StandardCharsets.UTF_8)
      .split("\n")
    list
  }
}