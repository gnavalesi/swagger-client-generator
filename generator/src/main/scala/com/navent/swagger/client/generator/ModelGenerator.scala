package com.navent.swagger.client.generator

import java.io.File
import javax.lang.model.element.Modifier

import com.navent.swagger.client.generator.Generator.Config
import com.squareup.javapoet._
import io.swagger.models.{Model, Swagger}
import lombok.{Builder, Data}
import org.apache.commons.lang3.StringUtils._

import scala.collection.JavaConverters._

object ModelGenerator {

  def generate(swagger: Swagger)(implicit config: Config): Unit = {
    swagger.getDefinitions.asScala
      .map({
        case (name: String, model: Model) =>
          val generatedModel = generateModel(name, model)
          config.generatedModels(name) = generatedModel
          generatedModel
      })
      .foreach(t => writeToFile(t))
  }

  private def generateModel(name: String, model: Model)(implicit config: Config): TypeSpec = {
    val builder: TypeSpec.Builder = TypeSpec.classBuilder(name)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(classOf[Data])
      .addAnnotation(classOf[Builder])
      .addJavadoc(if (isNotBlank(model.getDescription)) model.getDescription else "")

    PropertyGenerator.generate(model.getProperties.asScala).foreach(p => {
      builder.addField(p.field)
      builder.addTypes(p.types.asJava)
    })

    builder.build
  }

  private def writeToFile(t: TypeSpec)(implicit config: Config): Unit = {
    JavaFile.builder(config.modelPackage, t).build.writeTo(new File(config.codeOutput))
  }
}
