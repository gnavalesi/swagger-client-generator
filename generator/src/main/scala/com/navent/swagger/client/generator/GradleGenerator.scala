package com.navent.swagger.client.generator

import java.io.{File, PrintWriter}

import com.hubspot.jinjava.Jinjava
import com.navent.swagger.client.generator.Generator.Config

import scala.collection.JavaConverters._
import scala.io.Source

object GradleGenerator {
  def generate(implicit config: Config): String = {
    val buildGradleTemplate = Source.fromResource("build.gradle.j2").mkString

    val parameters = Map("group" -> config.group, "version" -> config.version)

    new Jinjava().render(buildGradleTemplate, parameters.asJava)




  }
}
