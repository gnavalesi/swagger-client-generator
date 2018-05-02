package com.navent.swagger.client.generator

import java.io.File
import javax.lang.model.element.Modifier

import com.google.common.base.CaseFormat._
import com.navent.swagger.client.generator.Generator.Config
import com.navent.swagger.client.implementation.config.{RestClientConfig, RestClientConfiguration}
import com.squareup.javapoet._
import io.swagger.models.Swagger
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean

import scala.collection.JavaConverters._

object ContextGenerator {

  def generate(swagger: Swagger)(implicit config: Config): Unit = {

    //    generateConfig(swagger)
    generateRestClientConfig(swagger)
    generateRestClientConfiguration(swagger)
  }

  private def generateRestClientConfig(swagger: Swagger)(implicit config: Config): Unit = {
    JavaFile.builder(config.configPackage, TypeSpec
      .classBuilder(getClassName("ClientConfig"))
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(AnnotationSpec.builder(classOf[ConfigurationProperties])
        .addMember("prefix", "$S", config.serviceName)
        .build)
      .superclass(classOf[RestClientConfig]).build)
      .build.writeTo(new File(config.codeOutput))
  }

  private def generateRestClientConfiguration(swagger: Swagger)(implicit config: Config): Unit = {
    JavaFile.builder(config.configPackage, TypeSpec
      .classBuilder(getClassName("ClientConfiguration"))
      .addModifiers(Modifier.PUBLIC)
      .superclass(classOf[RestClientConfiguration])
      .addMethod(MethodSpec.constructorBuilder() // Constructor
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(config.configPackage, getClassName("ClientConfig")), "config")
        .addStatement("super(config)")
        .build)
      .addMethod(MethodSpec // ClientConfig Bean
        .methodBuilder(UPPER_CAMEL.to(LOWER_CAMEL, getClassName("ClientConfig")))
        .returns(ClassName.get(config.configPackage, getClassName("ClientConfig")))
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(classOf[Bean])
        .addStatement("return new $T()", ClassName.get(config.configPackage, getClassName("ClientConfig")))
        .build)
      .addMethods(config.generatedControllers.map(c => c._2.name).map(c => { // Controllers Beans
        MethodSpec
          .methodBuilder(UPPER_CAMEL.to(LOWER_CAMEL, c))
          .returns(ClassName.get(config.controllerPackage, c))
          .addModifiers(Modifier.PROTECTED)
          .addAnnotation(classOf[Bean])
          .addStatement("return new $T(this)", ClassName.get(config.controllerPackage, c))
          .build
      }).asJava)
      .build)
      .build.writeTo(new File(config.codeOutput))
  }

  private def getClassName(suffix: String)(implicit config: Config): String = {
    LOWER_HYPHEN.to(UPPER_CAMEL, config.serviceName) + suffix
  }
}
