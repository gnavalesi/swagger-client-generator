package com.navent.swagger.client.generator

import java.io.{File, IOException}
import javax.lang.model.element.Modifier

import com.google.common.base.CaseFormat
import com.navent.swagger.client.generator.Generator.Config
import com.navent.swagger.client.implementation.Controller
import com.squareup.javapoet.{JavaFile, TypeSpec}
import io.swagger.models.{HttpMethod, Operation, _}

import scala.collection.JavaConverters._

object PathGenerator {

  case class InternalOperation(url: String, method: HttpMethod, operation: Operation)

  def generate(swagger: Swagger)(implicit config: Config): Unit = {
    swagger.getPaths.asScala
      .flatMap({
        case (url: String, path: Path) =>
          mapToOperations(url, path)
      })
      .groupBy({
        case InternalOperation(url: String, method: HttpMethod, operation: Operation) =>
          getControllerName(url, method, operation)
      })
      .map({
        case (controllerName: String, operations: Iterable[InternalOperation]) =>
          val generatedController = createController(controllerName, operations)
          config.generatedControllers(controllerName) = generatedController
          generatedController
      })
      .foreach(typeSpec => writeToFile(typeSpec))
  }

  private def mapToOperations(url: String, path: Path): Iterable[InternalOperation] =
    path.getOperationMap.asScala.map({
      case (method: HttpMethod, operation: Operation) => InternalOperation(url, method, operation)
    })

  private def getControllerName(url: String, method: HttpMethod, operation: Operation) =
    operation.getTags.asScala
      .find(_.endsWith("-controller"))
      .getOrElse("default-controller")

  private def createController(controllerName: String, operations: Iterable[InternalOperation])(implicit config: Config): TypeSpec =
    TypeSpec
      .classBuilder(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, controllerName))
      .superclass(classOf[Controller])
      .addModifiers(Modifier.PUBLIC)
      .addMethods(operations.flatMap({
        e: InternalOperation => MethodGenerator.generate(e)
      }).asJava)
      .build

  private def writeToFile(t: TypeSpec)(implicit config: Config): Unit = {
    try
      JavaFile.builder(config.controllerPackage, t)
        .indent("\t")
        .build.writeTo(new File(config.codeOutput))
    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }
}
