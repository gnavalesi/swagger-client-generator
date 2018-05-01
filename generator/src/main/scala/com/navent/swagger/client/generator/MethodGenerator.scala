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
        val codeBlockBuilder = CodeBlock.builder()
          .add("return dispatch").add(if (isListReturn) "List" else "").add("($T.class, ", concreteReturnType)
          .add(parameters.filter(p => p.parameter.isInstanceOf[PathParameter]).map(p => p.spec.name)
            .mkString(", ") match {
            case "" => "http.$L($S)"
            case s => "http.$L($S, " + s + ")"
          }, internal.method.name().toLowerCase, internal.url)

        if (CollectionUtils.isNotEmpty(internal.operation.getConsumes)) {
          val mediaTypeBlock = CodeBlock.builder()
            .add("$T.parseMediaType($S)", classOf[MediaType], internal.operation.getConsumes.asScala.head).build

          codeBlockBuilder
            .add("\n.accept(").add(mediaTypeBlock).add(")")
            .add("\n.contentType(").add(mediaTypeBlock).add(")")
        }

//        parameters.filter(p => p.parameter.isInstanceOf[QueryParameter]).foreach(p =>
//          codeBlockBuilder.add("\n.queryParam($S, $L" + (
//            if (!p.theType.equals(ClassName.get(classOf[String]))) ".toString()" else "") + ")", p.parameter
//            .getName, p.spec.name)
//        )

//        parameters.filter(p => p.parameter.isInstanceOf[HeaderParameter]).foreach(p =>
//          codeBlockBuilder.add(".header($S, $L" + (
//            if (!p.theType.equals(ClassName.get(classOf[String]))) ".toString()" else "") + ")", p.parameter
//            .getName, p.spec.name)
//        )

        //        parameters.filter(p => p.parameter.isInstanceOf[BodyParameter]).find(_ => true).foreach(p =>
        //          codeBlockBuilder.add("\n.body($L)", p.spec.name)
        //        )
        codeBlockBuilder
          .add(parameters // Query Parameters
            .filter(p => p.parameter.isInstanceOf[QueryParameter])
            .map(p => CodeBlock.builder()
              .add("\n.queryParam($S, toQueryParameter($L, $L.getClass()))", p.parameter.getName, p.spec.name, p.spec.name)
              .build())
            .foldLeft(CodeBlock.builder())((acc, c) => acc.add(c))
            .build())
          .add(parameters // Header Parameters
            .filter(p => p.parameter.isInstanceOf[HeaderParameter])
            .map(p => CodeBlock.builder()
              .add("\n.header($S, toHeaderParameter($L, $L.getClass()))", p.parameter.getName, p.spec.name, p.spec.name)
              .build())
            .foldLeft(CodeBlock.builder())((acc, c) => acc.add(c))
            .build())
          .add(parameters // Body Parameter
            .filter(p => p.parameter.isInstanceOf[BodyParameter])
            .find(_ => true)
            .map(p => CodeBlock.builder().add("\n.body($L)", p.spec.name).build())
            .getOrElse(emptyCodeBlock))
          .add(")")
          .build()
      })


    val requiredParametersBuilder = createRequiredParametersBuilder(name, returnType, parameters)

    allParametersBuilder
      .returns(concreteFutureType)

    requiredParametersBuilder.map(r =>
      r.returns(concreteFutureType))

    Seq(Option(allParametersBuilder), requiredParametersBuilder)
      .filter(p => p.isDefined)
      .map(p => p.get.build)
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
          ReturnType(itemsType, true)
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
        case m: ComposedModel => println("NotImplemented ComposedModel")
          ???
      })
      .find(_ => true)
  }
}
