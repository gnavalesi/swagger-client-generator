package com.navent.swagger.client.generator

import com.google.common.base.CaseFormat
import com.navent.swagger.client.generator.Generator.Config
import com.squareup.javapoet._
import io.swagger.models.parameters._
import io.swagger.models.{ArrayModel, ComposedModel, ModelImpl, RefModel}

object ParameterGenerator {

  case class ParameterResult(theType: TypeName, parameter: Parameter, spec: ParameterSpec)

  def generate(parameter: Parameter)(implicit config: Config): ParameterResult = {
    parameter match {
      case p: PathParameter => ParameterGenerator.generate(p)
      case p: QueryParameter => ParameterGenerator.generate(p)
      case p: HeaderParameter => ParameterGenerator.generate(p)
      case p: FormParameter => ParameterGenerator.generate(p)
      case p: CookieParameter => ParameterGenerator.generate(p)
      case p: RefParameter => ParameterGenerator.generate(p)
      case p: BodyParameter => ParameterGenerator.generate(p)
    }
  }

  def generate(parameter: AbstractSerializableParameter[_ <: Parameter])(implicit config: Config): ParameterResult = {
    val paramType = getAbstractParameterType(parameter)

    val paramBuilder = ParameterSpec.builder(paramType, createName(parameter))

    ParameterResult(paramType, parameter, paramBuilder.build)
  }

  private def getAbstractParameterType(parameter: AbstractSerializableParameter[_ <: Parameter])(implicit config: Config): TypeName = {
    parameter.getType match {
      case "integer" =>
        parameter.getFormat match {
          case "int32" => ClassName.get(classOf[java.lang.Integer])
          case "int64" => ClassName.get(classOf[java.lang.Long])
          case "" | null => ClassName.get(classOf[Number])
        }
      case "string" =>
        parameter.getFormat match {
          case "" | null => ClassName.get(classOf[String])
        }
      case "array" =>
        val items = PropertyGenerator.generate("items", parameter.getItems)
        val list = ClassName.get("java.util", "List")
        ParameterizedTypeName.get(list, items.theType)
    }
  }

  def createName(parameter: Parameter): String =
    if (parameter.getName.contains("_")) CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, parameter.getName)
    else parameter.getName

  def generate[T](parameter: BodyParameter)(implicit config: Config): ParameterResult = {

    val paramType = parameter.getSchema match {
      case e: RefModel => ClassName.get(config.modelPackage, e.getSimpleRef)
      case e: ArrayModel => println(s"Pending implementation ArrayModel")
        ??? // TODO
      case e: ComposedModel => println(s"Pending implementation ComposedModel")
        ??? // TODO
      case e: ModelImpl => println(s"Pending implementation ModelImpl")
        ??? // TODO
    }

    val paramBuilder = ParameterSpec.builder(paramType, createName(parameter))

    ParameterResult(paramType, parameter, paramBuilder.build)
  }

  def generate[T](parameter: RefParameter)(implicit config: Config): ParameterResult = {

    val paramBuilder = ParameterSpec.builder(ClassName.get(classOf[String]), createName(parameter))
    // TODO
    println("Pening implementation RefParameter")
    ParameterResult(ClassName.get(classOf[String]), parameter, paramBuilder.build)
  }
}
