package com.navent.swagger.client.generator

import javax.lang.model.element.Modifier

import com.google.common.base.CaseFormat
import com.google.common.base.CaseFormat.{LOWER_HYPHEN, UPPER_CAMEL}
import com.navent.swagger.client.generator.Generator.Config
import com.navent.swagger.client.implementation.Controller
import com.squareup.javapoet.{ClassName, MethodSpec, TypeSpec}
import io.swagger.models.{HttpMethod, Operation, _}

import scala.collection.JavaConverters._

object PathGenerator {

  case class InternalOperation(url: String, method: HttpMethod, operation: Operation)

  def generate(swagger: Swagger)(implicit config: Config): Iterable[TypeSpec] = {
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
          createController(controllerName, operations)
      })
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
      .addMethod(MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(config.configPackage, LOWER_HYPHEN
          .to(UPPER_CAMEL, config.serviceName) + "ClientConfiguration"), "config")
        .addStatement("super(config)")
        .build)
      .build
}
