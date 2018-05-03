package com.navent.swagger.client.generator

import java.io.{File, IOException, PrintWriter}

import com.navent.swagger.client.implementation.Controllers
import com.squareup.javapoet.{JavaFile, TypeSpec}
import io.swagger.models.Swagger
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
    Option.apply(new SwaggerParser().read(config.specification))
      .foreach({
        case swagger: Swagger =>
          ModelGenerator.generate(swagger).foreach(t => {
            config.generatedModels += (t.name -> t)
            writeToFile(JavaFile.builder(config.modelPackage, t))
          })

          PathGenerator.generate(swagger).foreach(p => {
            config.generatedControllers += (p.name -> p)
            writeToFile(JavaFile.builder(config.controllerPackage, p)
              .addStaticImport(classOf[Controllers], "*"))
          })

          ContextGenerator.generate(swagger).foreach(c => {
            writeToFile(JavaFile.builder(config.configPackage, c))
          })

          writeToFile("gradle.build", GradleGenerator.generate)
        case _ =>
          throw new RuntimeException(s"Unable to parse ${config.specification}")
      })
  }

  case class Config(
                     // General Options
                     serviceName: String = "",

                     // Input Options
                     specification: String = "",

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
      .action((x, c) => c
        .copy(basePackage = x, modelPackage = s"$x.model", controllerPackage = s"$x.controller", configPackage = s"$x.config"))
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

    opt[String]('s', "specification").required().valueName("<specification>")
      .action((x, c) => c.copy(specification = x))
      .text("specification is a required string property")

    opt[String]('o', "output").required().valueName("<output>")
      .action((x, c) => {
        val corrected = if (x.endsWith("/")) x else x + "/"
        c.copy(output = corrected, codeOutput = s"${corrected}src/main/java")
      })
      .text("output is a required string property")
  }

  private def writeToFile(builder: JavaFile.Builder)(implicit config: Config): Unit = {
    try
      builder.indent("\t")
        .build.writeTo(new File(config.codeOutput))
    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }

  private def writeToFile(path: String, content: String)(implicit config: Config): Unit = {
    val pw = new PrintWriter(new File(config.output + path))
    pw.write(content)
    pw.close()
  }
}
