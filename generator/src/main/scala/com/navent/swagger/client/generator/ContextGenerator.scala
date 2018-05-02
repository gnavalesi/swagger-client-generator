package com.navent.swagger.client.generator

import java.io.File
import javax.lang.model.element.Modifier

import com.google.common.base.CaseFormat._
import com.navent.swagger.client.generator.Generator.Config
import com.navent.swagger.client.implementation.config.RestClientConfiguration
import com.squareup.javapoet._
import io.swagger.models.Swagger
import lombok.Getter
import org.springframework.beans.factory.annotation.{Autowired, Qualifier, Value}
import org.springframework.context.annotation.Bean
import org.zalando.riptide.Http

import scala.collection.JavaConverters._

object ContextGenerator {

  def generate(swagger: Swagger)(implicit config: Config): Unit = {

    //    generateConfig(swagger)
    //    generateRestClientConfig(swagger)
    generateRestClientConfiguration(swagger)
  }

  //  private def generateRestClientConfig(swagger: Swagger)(implicit config: Config): Unit = {
  //    JavaFile.builder(config.configPackage, TypeSpec
  //      .classBuilder(getClassName("ClientConfig"))
  //      .addModifiers(Modifier.PUBLIC)
  //      .addAnnotation(AnnotationSpec.builder(classOf[ConfigurationProperties])
  //        .addMember("prefix", "$S", config.serviceName)
  //        .build)
  //      .superclass(classOf[RestClientConfig]).build)
  //      .build.writeTo(new File(config.codeOutput))
  //  }

  private def generateRestClientConfiguration(swagger: Swagger)(implicit config: Config): Unit = {
    JavaFile.builder(config.configPackage, TypeSpec
      .classBuilder(getClassName("ClientConfiguration"))
      .addModifiers(Modifier.PUBLIC)
      .superclass(classOf[RestClientConfiguration])
      .addField(FieldSpec.builder(ClassName.get(classOf[Http]), "client", Modifier.PRIVATE) // Http Bean
        .addAnnotation(classOf[Autowired])
        .addAnnotation(AnnotationSpec.builder(classOf[Qualifier])
          .addMember("value", "$S", config.serviceName)
          .build())
        .build)
      .addField(FieldSpec.builder(TypeName.LONG, "futureTimeout", Modifier.PRIVATE) // Futures timeout
        .addAnnotation(classOf[Getter])
        .addAnnotation(AnnotationSpec.builder(classOf[Value])
        .addMember("value", "$S", "${riptide.clients." + config.serviceName + ".connection-time-to-live:30l}")
        .build())
        .build)
      .addMethod(MethodSpec.methodBuilder("http") // Http Method
        .returns(ClassName.get(classOf[Http]))
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(classOf[Override])
        .addStatement("return this.client")
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
