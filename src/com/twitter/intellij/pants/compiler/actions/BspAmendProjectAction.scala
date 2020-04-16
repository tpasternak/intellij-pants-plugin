// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Optional

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

    val targets = selectedTargets(project.get.getBasePath)

    val dial = new FastpassManager(project.get, pantsRoots(project.get).head, targets)
    dial.show()

    val systemSettings = ExternalSystemApiUtil.getSettings(project.get, BSP.ProjectSystemId)
    systemSettings.getLinkedProjectsSettings.asScala

    val fileChooser = FileChooserFactory.getInstance().createFileChooser(
      new FileChooserDescriptor(true, true,true, true,true, true)
      , project.get
    , null
      )
    val file = fileChooser.choose(null).head

    val selectedFoldersPantsExecutable = PantsUtil.findPantsExecutable(file.getPath).toOption
    if(pantsRoots(project.get).exists(f => selectedFoldersPantsExecutable.contains(f))) {
      val list = availableTargets(file)
      val chosen =  Messages.showEditableChooseDialog("Msg", "Title", Icons.BSP_TARGET, list, list.head, null)
      runAmend(event.getProject.getBasePath, chosen)
      ExternalProjectUtil.refresh(project.get, BSP.ProjectSystemId) // todo no get here!
    }
  }.getOrElse(())
}

object FastpassUtils {
  implicit class OptionalExtension[T](optional: Optional[T]) {
    def toOption: Option[T] = Option(optional.orElseGet(null))
  }

  def pantsRoots(project: Project): Seq[VirtualFile] = {
    ModuleManager.getInstance(project).getModules.toList.flatMap {
      module =>
        ModuleRootManager.getInstance(module).getSourceRoots.flatMap {
          sourceRoot => PantsUtil.findPantsExecutable(sourceRoot.getPath).toOption
        }
    }
  }

  def runAmend(basePath: String, chosen: String): Int = {
    val builder = new ProcessBuilder("fastpass-amend", s"${basePath}/.bsp/bloop.json", chosen)// todo 1. tutaj ma być bardziej getlinkedproject 2. musi się wywalić jeżeli projekt pantsowy nie jest w BSP
    val process = builder.start()
    process.onExit().get() // todo handle cmd line output
    val exitCode = process.exitValue()
    exitCode
  }

  def availableTargets(file: VirtualFile) = {
    val pantsExecutable = PantsUtil.findPantsExecutable(file.getPath).get
    val pantsExecutablePath = Paths.get(pantsExecutable.getParent.getPath)
    val targetPath = Paths.get(file.getPath)
    val targetDirId = pantsExecutablePath.relativize(targetPath)
    val builderList = new ProcessBuilder("fastpass-list", pantsExecutable.getParent.getPath, targetDirId
      .toString)
    val processList = builderList.start().onExit().get()
    val list = IOUtils.toString(processList.getInputStream, StandardCharsets.UTF_8).split("\n")
    list
  }

  def selectedTargets(basePath: String): Array[String] = {
    val builder = new ProcessBuilder("fastpass-get", s"${basePath}/.bsp/bloop.json")
    val process = builder.start()
    process.onExit().get() // todo handle cmd line output
    val exitCode = process.exitValue()
    val list = IOUtils
      .toString(process.getInputStream, StandardCharsets.UTF_8)
      .split("\n")
    list
  }
}