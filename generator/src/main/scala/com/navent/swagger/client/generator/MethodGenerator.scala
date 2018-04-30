package com.navent.swagger.client.generator

import com.google.common.base.CaseFormat
import com.navent.swagger.client.generator.Generator.Config
import com.squareup.javapoet.{ClassName, MethodSpec, ParameterSpec}
import io.swagger.models.parameters.{AbstractSerializableParameter, Parameter, PathParameter, QueryParameter}
import io.swagger.models.{HttpMethod, Operation}
import org.apache.commons.lang3.StringUtils
import org.zalando.riptide.Requester

import scala.collection.JavaConverters._

object MethodGenerator {
  def generate(url: String, method: HttpMethod, operation: Operation)(implicit config: Config): MethodSpec = {
    val methodName = operation.getOperationId.replace("Using" + method.name, "")
    val syncBuilder = MethodSpec.methodBuilder(methodName)

    val pathParameters: String = operation.getParameters.asScala.filter(p => p.getIn == "path")
      .map(p => p.asInstanceOf[PathParameter])
      .map(p => {
        val name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, p.getName)

//        syncBuilder.addParameter(getType(p), name)

        name
      })
      .foldLeft("")((a, b) => s"$a, $b")

    syncBuilder.addStatement("$T requester = $L($S" + pathParameters + ")", classOf[Requester], method.name().toLowerCase(), url)

    operation.getParameters.asScala.filter(p => p.getIn == "query")
      .map(p => p.asInstanceOf[QueryParameter])
      .foreach(p => {
        val name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, p.getName)
        syncBuilder.addStatement("requester.queryParam($S, $L)", name, name)

//        syncBuilder.addParameter(getType(p), name)
      })

    syncBuilder.build()
  }

  private def getMethodReturnType(operation: Operation)(implicit config: Config): Option[ClassName] =
    operation.getResponses.asScala.values
      .map(r => r.getResponseSchema)
      .filterNot(m => m == null)
      .map(m => m.getReference)
      .filterNot(m => StringUtils.isBlank(m))
      .map(d => ClassName.get(config.modelPackage, d.replace("#/definitions/", "")))
      .find(_ => true)

  private def getType(p: AbstractSerializableParameter[_]): Class[_] = {
    p.getType match {
      case "integer" =>
        p.getFormat match {
          case "int32" => classOf[Integer]
          case "int64" => classOf[Long]
        }
      case "boolean" => classOf[Boolean]
      case "string" =>
        p.getFormat match {
          case "" | null => classOf[String]
        }
    }
  }
}
