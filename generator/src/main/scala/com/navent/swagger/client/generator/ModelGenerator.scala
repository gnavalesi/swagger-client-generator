package com.navent.swagger.client.generator

import java.io.File
import javax.lang.model.element.Modifier

import com.navent.swagger.client.generator.Generator.Config
import com.squareup.javapoet._
import io.swagger.models._
import lombok.{Builder, Data}
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConverters._

object ModelGenerator {

  def generate(swagger: Swagger)(implicit config: Config): Iterable[TypeSpec] = {
    swagger.getDefinitions.asScala
      .map(p => generate(p._1, p._2))
  }

  def generate(name: String, model: Model)(implicit config: Config): TypeSpec = {
    val builder: TypeSpec.Builder = TypeSpec.classBuilder(name)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(classOf[Data])
      .addAnnotation(classOf[Builder])

    val javadocBuilder = CodeBlock.builder()

    if(StringUtils.isNotBlank(model.getTitle))
      javadocBuilder.add("Title: $L\n", model.getTitle)

    if(StringUtils.isNotBlank(model.getDescription))
      javadocBuilder.add("$L\n", model.getDescription)

    if(model.getExample != null)
      javadocBuilder.add("Example: $L\n", model.getExample)

    builder.addJavadoc(javadocBuilder.build())

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
