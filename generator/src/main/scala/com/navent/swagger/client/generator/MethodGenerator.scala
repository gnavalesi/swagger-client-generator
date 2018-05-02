package com.navent.swagger.client.generator

import java.util.concurrent.{ExecutionException, TimeoutException}
import javax.lang.model.element.Modifier

import com.navent.swagger.client.generator.Generator.Config
import com.navent.swagger.client.generator.ParameterGenerator.ParameterResult
import com.navent.swagger.client.generator.PathGenerator.InternalOperation
import com.squareup.javapoet.{CodeBlock, _}
import io.swagger.models._
import io.swagger.models.parameters._
import org.apache.commons.collections4.CollectionUtils
import org.springframework.http.MediaType

import scala.collection.JavaConverters._

object MethodGenerator {

  case class ReturnType(typeName: TypeName, isList: Boolean = false)

  def generate(internal: InternalOperation)(implicit config: Config): Seq[MethodSpec] = {
    val methodName = getMethodName(internal)
    val returnType = getReturnType(internal)
    val parameters = internal.operation.getParameters.asScala
      .map(a => ParameterGenerator.generate(a)).toList

    generateAsync(internal, methodName + "Async", returnType, parameters) ++ generateSync(internal, methodName, returnType, parameters)
  }

  def generateSync(internal: InternalOperation, name: String, returnType: Option[ReturnType], parameters: List[ParameterResult])(implicit config: Config): Seq[MethodSpec] = {
    val allParametersBuilder = MethodSpec.methodBuilder(name)
      .addModifiers(Modifier.PUBLIC)
      .addParameters(parameters.map(p => p.spec).asJava)

    val syncReturnType = returnType.map(r => r
      .copy(typeName =
        if (r.isList) ParameterizedTypeName.get(ClassName.get("java.util", "List"), r.typeName)
        else r.typeName))

    allParametersBuilder
      .addStatement(returnType.map(_ => "return ").getOrElse("") + "resolveFuture($L(" +
        parameters.map(p => p.spec.name).mkString(", ") + "))", name + "Async")

    val requiredParametersBuilder = createRequiredParametersBuilder(name, returnType, parameters)


    val exceptions = Seq(classOf[InterruptedException], classOf[ExecutionException], classOf[TimeoutException])
      .map(c => ClassName.get(c))
      .asJava

    allParametersBuilder.addExceptions(exceptions)
    requiredParametersBuilder.map(r => r.addExceptions(exceptions))

    syncReturnType.map(t => {
      allParametersBuilder.returns(t.typeName)
      requiredParametersBuilder.map(r => r.returns(t.typeName))
    })

    Seq(Option(allParametersBuilder), requiredParametersBuilder)
      .filter(p => p.isDefined)
      .map(p => p.get.build)
  }

  def generateAsync(internal: InternalOperation, name: String, returnType: Option[ReturnType], parameters: List[ParameterResult])(implicit config: Config): Seq[MethodSpec] = {
    val allParametersBuilder = MethodSpec.methodBuilder(name)
      .addModifiers(Modifier.PUBLIC)
      .addParameters(parameters.map(p => p.spec).asJava)

    val isListReturn = returnType.exists(r => r.isList)
    val concreteReturnType = returnType.map(r => r.typeName).getOrElse(ClassName.get(classOf[java.lang.Void]))
    val futureType = ClassName.get("java.util.concurrent", "Future")
    val concreteFutureType = ParameterizedTypeName.get(futureType,
      if (isListReturn) ParameterizedTypeName.get(ClassName.get("java.util", "List"), concreteReturnType)
      else concreteReturnType)

    allParametersBuilder
      .addStatement({
        CodeBlock.builder()
          .add("return dispatch").add(if (isListReturn) "List" else "").add("($T.class, ", concreteReturnType) // Return
          .add(getQueryCall(internal, parameters))
          .indent()
          .add(getContentType(internal))
          .add(getQueryParameters(parameters))
          .add(getHeaderParameters(parameters))
          .add(getBodyParameter(parameters))
          .add(")")
          .unindent()
          .build()
      })

    allParametersBuilder.returns(concreteFutureType)

    Seq(Option(allParametersBuilder), createRequiredParametersBuilder(name, returnType, parameters)
      .map(r => r.returns(concreteFutureType)))
      .filter(p => p.isDefined)
      .map(p => p.get.build)
  }

  private def getQueryCall(internal: InternalOperation, parameters: List[ParameterResult]): CodeBlock = {
    CodeBlock.builder().add("http().$L($S" + (parameters.filter(p => p.parameter.isInstanceOf[PathParameter])
      .map(p => p.spec.name)
      .mkString(", ") match {
      case "" => ""
      case s => ", " + s
    }) + ")", internal.method.name().toLowerCase, internal.url).build()
  }

  private def getContentType(internal: InternalOperation) = {
    Option(internal.operation.getConsumes) // Content Type
      .filter(CollectionUtils.isNotEmpty)
      .map(cs => CodeBlock.builder().add("$T.parseMediaType($S)", classOf[MediaType], cs.get(0)).build())
      .map(c => CodeBlock.builder()
        .add("\n.accept(").add(c).add(")")
        .add("\n.contentType(").add(c).add(")")
        .build)
      .getOrElse(emptyCodeBlock)
  }

  private def getQueryParameters(parameters: List[ParameterResult]) = {
    parameters // Query Parameters
      .filter(p => p.parameter.isInstanceOf[QueryParameter])
      .map(p => CodeBlock.builder()
        .add("\n.queryParam($S, toQueryParameter($L, $L.getClass()))", p.parameter.getName, p.spec.name, p.spec
          .name)
        .build())
      .foldLeft(CodeBlock.builder())((acc, c) => acc.add(c))
      .build()
  }

  private def getHeaderParameters(parameters: List[ParameterResult]) = {
    parameters // Header Parameters
      .filter(p => p.parameter.isInstanceOf[HeaderParameter])
      .map(p => CodeBlock.builder()
        .add("\n.header($S, toHeaderParameter($L, $L.getClass()))", p.parameter.getName, p.spec.name, p.spec.name)
        .build())
      .foldLeft(CodeBlock.builder())((acc, c) => acc.add(c))
      .build()
  }

  private def getBodyParameter(parameters: List[ParameterResult]) = {
    parameters // Body Parameter
      .filter(p => p.parameter.isInstanceOf[BodyParameter])
      .find(_ => true)
      .map(p => CodeBlock.builder().add("\n.body($L)", p.spec.name).build())
      .getOrElse(emptyCodeBlock)
  }

  private def emptyCodeBlock: CodeBlock = CodeBlock.builder().build()

  private def getMethodName(internal: InternalOperation): String =
    internal.operation.getOperationId
      .substring(0, internal.operation.getOperationId.indexOf("Using" + internal.method.name))

  private def createRequiredParametersBuilder(methodName: String, returnType: Option[ReturnType], parameters: Seq[ParameterResult]) = {
    val requiredParameters = parameters.filter(p => p.parameter.getRequired)
    val nonRequiredParameters = parameters.filterNot(p => p.parameter.getRequired)

    val requiredParametersBuilder =
      if (nonRequiredParameters.isEmpty) Option.empty
      else {
        val sb: StringBuilder = new StringBuilder(returnType.map(_ => "return ").getOrElse(""))
        sb.append(s"$methodName(${requiredParameters.map(p => p.spec.name).mkString(", ")}, ${
          nonRequiredParameters.map(p => {
            if (p.parameter.isInstanceOf[AbstractSerializableParameter[_ <: Parameter]]) {
              val par = p.parameter.asInstanceOf[AbstractSerializableParameter[_ <: Parameter]]
              par.getDefaultValue match {
                case s: String => "\"" + s + "\""
                case null => "null"
                case a => a
              }
            } else "null"
          }).mkString(", ")
        })")

        Option(MethodSpec.methodBuilder(methodName)
          .addModifiers(Modifier.PUBLIC)
          .addParameters(requiredParameters.map(p => p.spec).asJava)
          .addStatement(sb.toString()))
      }
    requiredParametersBuilder
  }

  private def getReturnType(internal: InternalOperation)(implicit config: Config): Option[ReturnType] = {
    internal.operation.getResponses.asScala.values
      .map(_.getResponseSchema)
      .filter(_ != null)
      .map({
        case m: ArrayModel =>
          val itemsType = PropertyGenerator.generate("items", m.getItems).theType
          ReturnType(itemsType, isList = true)
        case m: RefModel =>
          ReturnType(ClassName.get(config.modelPackage, m.getSimpleRef))
        case m: ModelImpl => ReturnType(m.getType match {
          case "integer" =>
            m.getFormat match {
              case "int32" => ClassName.get(classOf[java.lang.Integer])
              case "int64" => ClassName.get(classOf[java.lang.Long])
              case "" | null => ClassName.get(classOf[Number])
            }
          case "string" =>
            m.getFormat match {
              case "" | null => ClassName.get(classOf[String])
            }
        })
        case _: ComposedModel => println("NotImplemented ComposedModel")
          ???
      })
      .find(_ => true)
  }
}
