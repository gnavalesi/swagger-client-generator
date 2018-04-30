package com.navent.swagger.client.generator

import java.io.{File, IOException}

import com.google.common.base.CaseFormat
import com.navent.swagger.client.generator.Generator.Config
import com.navent.swagger.client.implementation.Controller
import com.squareup.javapoet.{JavaFile, TypeSpec}
import io.swagger.models.{HttpMethod, Operation, _}

import scala.collection.JavaConverters._

object PathGenerator {

  def generate(swagger: Swagger)(implicit config: Config): Unit = {
    swagger.getPaths.asScala
      .flatMap({
        case (url: String, path: Path) =>
          mapToOperations(url, path)
      })
      .groupBy({
        case (url: String, method: HttpMethod, operation: Operation) =>
          getControllerName(url, method, operation)
      })
      .map({
        case (controllerName: String, operations: Iterable[(String, HttpMethod, Operation)]) =>
          val generatedController = createController(controllerName, operations)
          config.generatedControllers(controllerName) = generatedController
          generatedController
      })
      .foreach(typeSpec => writeToFile(typeSpec))
  }

  private def mapToOperations(url: String, path: Path): Iterable[(String, HttpMethod, Operation)] =
    path.getOperationMap.asScala.map({
      case (method: HttpMethod, operation: Operation) => (url, method, operation)
    })

  private def getControllerName(url: String, method: HttpMethod, operation: Operation) =
    operation.getTags.asScala
      .find(_.endsWith("-controller"))
      .getOrElse("default-controller")


  private def createController(controllerName: String, operations: Iterable[(String, HttpMethod, Operation)])(implicit config: Config) =
    TypeSpec
      .classBuilder(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, controllerName))
      .superclass(classOf[Controller])
      .addMethods(operations.map({
        case (url: String, method: HttpMethod, operation: Operation) => MethodGenerator.generate(url, method, operation)
      }).asJava)
      .build

  private def writeToFile(t: TypeSpec)(implicit config: Config): Unit = {
    try
      JavaFile.builder(config.controllerPackage, t).build.writeTo(new File(config.codeOutput))
    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }
}
