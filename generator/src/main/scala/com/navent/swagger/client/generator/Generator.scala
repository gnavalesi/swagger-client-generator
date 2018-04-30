package com.navent.swagger.client.generator

import java.io.File

import com.squareup.javapoet.TypeSpec
import io.swagger.parser.SwaggerParser

import scala.collection.mutable

object Generator {

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) => generate(config)

      case None =>
    }
  }

  private def generate(implicit config: Config): Unit = {
    val swagger = new SwaggerParser().read(config.specification.getPath)

    ContextGenerator.generate(swagger)
    ModelGenerator.generate(swagger)
    PathGenerator.generate(swagger)
    GradleGenerator.generate
  }

  case class Config(
                     // General Options
                     serviceName: String = "",

                     // Input Options
                     specification: File = new File("."),

                     // Java Options
                     basePackage: String = "",
                     modelPackage: String = "",
                     controllerPackage: String = "",
                     configPackage: String = "",

                     // Maven Options
                     group: String = "",
                     version: String = "",

                     // Output Options
                     output: String = "",
                     codeOutput: String = ""
                   ) {
    val generatedModels: mutable.Map[String, TypeSpec] = mutable.Map()
    val generatedControllers: mutable.Map[String, TypeSpec] = mutable.Map()
  }

  private val parser = new scopt.OptionParser[Config]("generator") {
    head("generator")

    opt[String]('b', "basePackage").required().valueName("<basePackage>")
      .action((x, c) => c.copy(basePackage = x, modelPackage = s"$x.model", controllerPackage = s"$x.controller", configPackage = s"$x.config"))
      .text("basePackage is a required string property")

    opt[String]('n', "serviceName").required().valueName("<serviceName>")
      .action((x, c) => c.copy(serviceName = x))
      .text("serviceName is a required string property")

    opt[String]('g', "group").required().valueName("<group>")
      .action((x, c) => c.copy(group = x))
      .text("group is a required string property")

    opt[String]('v', "version").required().valueName("<version>")
      .action((x, c) => c.copy(version = x))
      .text("version is a required string property")

    opt[File]('s', "specification").required().valueName("<specification>")
      .action((x, c) => c.copy(specification = x))
      .text("specification is a required file property")

    opt[String]('o', "output").required().valueName("<output>")
      .action((x, c) => {
        val corrected = if (x.endsWith("/")) x else x + "/"
        c.copy(output = corrected, codeOutput = s"${corrected}src/main/java")
      })
      .text("output is a required string property")
  }
}
