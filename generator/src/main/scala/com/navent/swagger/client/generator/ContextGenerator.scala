package com.navent.swagger.client.generator

import java.io.File
import javax.lang.model.element.Modifier

import com.google.common.base.CaseFormat
import com.navent.swagger.client.generator.Generator.Config
import com.squareup.javapoet.{AnnotationSpec, FieldSpec, JavaFile, TypeSpec}
import io.swagger.models.Swagger
import lombok.Data
import org.springframework.context.annotation.{ComponentScan, Configuration, PropertySource}

object ContextGenerator {

  def generate(swagger: Swagger)(implicit config: Config): Unit = {
    generateRestConfig(swagger)
    generateConfig(swagger)
  }

  private def generateRestConfig(swagger: Swagger)(implicit config: Config): Unit = {
    JavaFile.builder(config.configPackage, TypeSpec
      .classBuilder(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, config.serviceName) + "ClientRestConfig")
      .addModifiers(Modifier.PUBLIC).addAnnotation(classOf[Data])
      .addAnnotation(AnnotationSpec.builder(classOf[PropertySource]).addMember("prefix", "$S", config.serviceName)
        .build)
      .addField(FieldSpec.builder(classOf[String], "host", Modifier.PRIVATE).initializer("$S", swagger.getHost).build)
      .addField(FieldSpec.builder(classOf[String], "basePath", Modifier.PRIVATE).initializer("$S", swagger.getBasePath)
        .build).build).build.writeTo(new File(config.codeOutput))
  }

  private def generateConfig(swagger: Swagger)(implicit config: Config): Unit = {
    JavaFile.builder(config.configPackage, TypeSpec
      .classBuilder(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, config.serviceName) + "ClientConfig")
      .addModifiers(Modifier.PUBLIC).addAnnotation(classOf[Configuration])
      .addAnnotation(AnnotationSpec.builder(classOf[ComponentScan]).addMember("value", "$S", config.basePackage).build).build)
      .build.writeTo(new File(config.codeOutput))
  }
}
