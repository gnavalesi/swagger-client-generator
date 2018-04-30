package com.navent.swagger.client.generator

import java.io.File
import java.util.{Date, UUID}
import javax.lang.model.element.Modifier
import javax.validation.constraints._

import com.google.common.base.CaseFormat._
import com.navent.swagger.client.generator.Generator.Config
import com.navent.swagger.client.implementation.validators.{MultipleOf, UniqueItems}
import com.squareup.javapoet._
import io.swagger.models.properties._
import io.swagger.util.AllowableValues
import org.apache.commons.collections4.{CollectionUtils, MapUtils}
import org.apache.commons.lang3.{BooleanUtils, StringUtils}
import org.apache.commons.lang3.StringUtils._

import scala.collection.JavaConverters._
import scala.collection.mutable

object PropertyGenerator {

  case class FieldResult(theType: TypeName, field: FieldSpec, types: Seq[TypeSpec])

  case class FieldInternalResult(theType: TypeName, field: FieldSpec.Builder, types: Seq[TypeSpec]) {
    def build(): FieldResult = FieldResult(theType, field.build, types)
  }

  def generate(properties: mutable.Map[String, Property])(implicit config: Config): Iterable[FieldResult] = {
    properties.map({
      case (name: String, property: Property) => generate(name, property)
    })
  }

  def generate(name: String, property: Property)(implicit config: Config): FieldResult = {
    val result: FieldInternalResult = property match {
      case p: AbstractNumericProperty => abstractNumericBuilder(name, p)
      case p: ArrayProperty => arrayBuilder(name, p)
      case p: BinaryProperty => binaryBuilder(name, p)
      case p: BooleanProperty => booleanBuilder(name, p)
      case p: DateProperty => dateBuilder(name, p)
      case p: DateTimeProperty => dateTimeBuilder(name, p)
      case p: FileProperty => fileBuilder(name, p)
      case p: MapProperty => mapBuilder(name, p)
      case p: ObjectProperty => objectBuilder(name, p)
      case p: PasswordProperty => passwordBuilder(name, p)
      case p: RefProperty => refBuilder(name, p)
      case p: StringProperty => stringBuilder(name, p)
      case p: UntypedProperty => untypedBuilder(name, p)
      case p: UUIDProperty => uuidBuilder(name, p)
    }

    if(BooleanUtils.isTrue(property.getRequired))
      result.field.addAnnotation(classOf[NotNull])

    if(BooleanUtils.isTrue(property.getReadOnly))
      result.field.addModifiers(Modifier.FINAL)

    val javadocBuilder = CodeBlock.builder()

    if(StringUtils.isNotBlank(property.getTitle))
      javadocBuilder.add("Title: $L\n", property.getTitle)

    if(StringUtils.isNotBlank(property.getDescription))
      javadocBuilder.add("$L\n", property.getDescription)

    if(property.getExample != null)
      javadocBuilder.add("Example: $L\n", property.getExample)

    result.field.addJavadoc(javadocBuilder.build)

    result.build()
  }

  private def abstractNumericBuilder(name: String, property: AbstractNumericProperty)(implicit config: Config): FieldInternalResult = {
    val result: FieldInternalResult = property match {
      case p: BaseIntegerProperty => baseIntegerBuilder(name, p)
      case p: DecimalProperty => decimalBuilder(name, p)
    }

    if (property.getMinimum != null)
      result.field.addAnnotation(AnnotationSpec.builder(classOf[DecimalMin])
        .addMember("value", s"${property.getMinimum}")
        .build)
    else if (property.getExclusiveMinimum != null)
      result.field.addAnnotation(AnnotationSpec.builder(classOf[DecimalMin])
        .addMember("value", s"${property.getExclusiveMinimum}")
        .addMember("inclusive", "$L", Boolean.box(false))
        .build)

    if (property.getMaximum != null)
      result.field.addAnnotation(AnnotationSpec.builder(classOf[DecimalMax])
        .addMember("value", s"${property.getMaximum}")
        .build)
    else if (property.getExclusiveMaximum != null)
      result.field.addAnnotation(AnnotationSpec.builder(classOf[DecimalMax])
        .addMember("value", s"${property.getExclusiveMaximum}")
        .addMember("inclusive", "$L", Boolean.box(false))
        .build)

    if (property.getMultipleOf != null)
      result.field.addAnnotation(AnnotationSpec.builder(classOf[MultipleOf])
        .addMember("value", s"${property.getMultipleOf}")
        .build)

    result
  }

  private def baseIntegerBuilder(name: String, property: BaseIntegerProperty)(implicit config: Config): FieldInternalResult = {
    val builder: FieldInternalResult = property match {
      case p: IntegerProperty => integerBuilder(name, p)
      case p: LongProperty => longBuilder(name, p)
      case _ => FieldInternalResult(ClassName.get(classOf[Number]), FieldSpec
        .builder(classOf[Integer], name, Modifier.PRIVATE), Seq())
    }

    builder
  }

  private def integerBuilder(name: String, property: IntegerProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(TypeName.INT, name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    FieldInternalResult(TypeName.INT, builder, Seq())
  }

  private def longBuilder(name: String, property: LongProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(TypeName.LONG, name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    FieldInternalResult(TypeName.LONG, builder, Seq())
  }

  private def decimalBuilder(name: String, property: DecimalProperty)(implicit config: Config): FieldInternalResult = {
    val builder: FieldInternalResult = property match {
      case p: DoubleProperty => doubleBuilder(name, p)
      case p: FloatProperty => floatBuilder(name, p)
      case _ => FieldInternalResult(ClassName.get(classOf[BigDecimal]), FieldSpec
        .builder(classOf[BigDecimal], name, Modifier.PRIVATE), Seq())
    }

    builder
  }

  private def doubleBuilder(name: String, property: DoubleProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(TypeName.DOUBLE, name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    FieldInternalResult(TypeName.DOUBLE, builder, Seq())
  }

  private def floatBuilder(name: String, property: FloatProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(TypeName.FLOAT, name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    FieldInternalResult(TypeName.FLOAT, builder, Seq())
  }

  private def arrayBuilder(name: String, property: ArrayProperty)(implicit config: Config): FieldInternalResult = {
    val items = generate("items", property.getItems)

    val list = ClassName.get("java.util", "List")
    val arrayType = ParameterizedTypeName.get(list, items.theType)

    val builder = FieldSpec.builder(arrayType, name, Modifier.PRIVATE)

    if (property.getMaxItems != null || property.getMinItems != null) {
      val sizeSpecBuilder = AnnotationSpec.builder(classOf[Size])
      if (property.getMinItems != null) sizeSpecBuilder.addMember("min", s"${property.getMinItems}")
      if (property.getMaxItems != null) sizeSpecBuilder.addMember("max", s"${property.getMaxItems}")

      builder.addAnnotation(sizeSpecBuilder.build)
    }

    if (BooleanUtils.isTrue(property.getUniqueItems)) {
      builder.addAnnotation(classOf[UniqueItems])
    }

    FieldInternalResult(arrayType, builder, Seq())
  }

  private def booleanBuilder(name: String, property: BooleanProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(TypeName.BOOLEAN, name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    FieldInternalResult(TypeName.BOOLEAN, builder, Seq())
  }

  private def stringBuilder(name: String, property: StringProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[String], name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    if (property.getMaxLength != null || property.getMinLength != null) {
      val sizeSpecBuilder = AnnotationSpec.builder(classOf[Size])
      if (property.getMinLength != null) sizeSpecBuilder.addMember("min", s"${property.getMinLength}")
      if (property.getMaxLength != null) sizeSpecBuilder.addMember("max", s"${property.getMaxLength}")

      builder.addAnnotation(sizeSpecBuilder.build)
    }

    if (isNotBlank(property.getPattern)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[Pattern])
        .addMember("regexp", "$S", property.getPattern)
        .build)
    } else property match {
      case _: ByteArrayProperty =>
        builder.addAnnotation(AnnotationSpec.builder(classOf[Pattern])
          .addMember("regexp", "$S", "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")
          .build)
      case _: EmailProperty =>
      // FIXME: No fiable regexp for mails
      case _: StringProperty =>
    }

    FieldInternalResult(ClassName.get(classOf[String]), builder, Seq())
  }

  private def binaryBuilder(name: String, property: BinaryProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[String], name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    if (property.getMaxLength != null || property.getMinLength != null) {
      val sizeSpecBuilder = AnnotationSpec.builder(classOf[Size])
      if (property.getMinLength != null) sizeSpecBuilder.addMember("min", s"${property.getMinLength}")
      if (property.getMaxLength != null) sizeSpecBuilder.addMember("max", s"${property.getMaxLength}")

      builder.addAnnotation(sizeSpecBuilder.build)
    }

    if (isNotBlank(property.getPattern)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[Pattern])
        .addMember("regexp", "$S", property.getPattern)
        .build)
    }

    FieldInternalResult(ClassName.get(classOf[String]), builder, Seq())
  }

  private def dateBuilder(name: String, property: DateProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[Date], name, Modifier.PRIVATE)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    FieldInternalResult(ClassName.get(classOf[Date]), builder, Seq())
  }

  private def dateTimeBuilder(name: String, property: DateTimeProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[Date], name, Modifier.PRIVATE)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    FieldInternalResult(ClassName.get(classOf[Date]), builder, Seq())
  }

  private def fileBuilder(name: String, property: FileProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[File], name, Modifier.PRIVATE)

    FieldInternalResult(ClassName.get(classOf[File]), builder, Seq())
  }

  private def mapBuilder(name: String, property: MapProperty)(implicit config: Config): FieldInternalResult = {
    val properties = generate("items", property.getAdditionalProperties)

    val fieldType = ParameterizedTypeName
      .get(ClassName.get("java.util", "Map"), ClassName.get(classOf[String]), properties.theType)

    val builder = FieldSpec.builder(fieldType, name, Modifier.PRIVATE)

    if (property.getMinProperties != null || property.getMaxProperties != null) {
      val sizeSpecBuilder = AnnotationSpec.builder(classOf[Size])
      if (property.getMinProperties != null) sizeSpecBuilder.addMember("min", s"${property.getMinProperties}")
      if (property.getMaxProperties != null) sizeSpecBuilder.addMember("max", s"${property.getMaxProperties}")

      builder.addAnnotation(sizeSpecBuilder.build)
    }

    FieldInternalResult(fieldType, builder, properties.types)
  }

  private def objectBuilder(name: String, property: ObjectProperty)(implicit config: Config): FieldInternalResult = {
    val typeName = LOWER_UNDERSCORE.to(UPPER_CAMEL, name)
    val typeBuilder = TypeSpec.classBuilder(typeName)
    if (MapUtils.isNotEmpty(property.getProperties)) {
      property.getProperties.asScala
        .map({
          case (propertyName: String, internalProperty: Property) => generate(propertyName, internalProperty)
        })
        .map({
          case FieldResult(_: TypeName, field: FieldSpec, types: Seq[TypeSpec]) =>
            if (types.nonEmpty) typeBuilder.addTypes(types.asJava)

            if (CollectionUtils.isNotEmpty(property.getRequiredProperties) && property.getRequiredProperties
              .contains(field.name))
              field.toBuilder.addAnnotation(classOf[NotNull]).build()
            else field
        })
        .foreach(typeBuilder.addField)
    }

    val fieldBuilder = FieldSpec.builder(ClassName.get("", typeName), name, Modifier.PRIVATE)

    FieldInternalResult(ClassName.get("", typeName), fieldBuilder, Seq(typeBuilder.build()))
  }

  private def passwordBuilder(name: String, property: PasswordProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[String], name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    if (property.getMaxLength != null || property.getMinLength != null) {
      val sizeSpecBuilder = AnnotationSpec.builder(classOf[Size])
      if (property.getMinLength != null) sizeSpecBuilder.addMember("min", s"${property.getMinLength}")
      if (property.getMaxLength != null) sizeSpecBuilder.addMember("max", s"${property.getMaxLength}")

      builder.addAnnotation(sizeSpecBuilder.build)
    }

    if (isNotBlank(property.getPattern)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[Pattern])
        .addMember("regexp", "$S", property.getPattern)
        .build)
    }

    FieldInternalResult(ClassName.get(classOf[String]), builder, Seq())
  }

  private def refBuilder(name: String, property: RefProperty)(implicit config: Config): FieldInternalResult = ???

  private def untypedBuilder(name: String, property: UntypedProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[String], name, Modifier.PRIVATE)

    FieldInternalResult(ClassName.get(classOf[String]), builder, Seq())
  }

  private def uuidBuilder(name: String, property: UUIDProperty)(implicit config: Config): FieldInternalResult = {
    val builder = FieldSpec.builder(classOf[UUID], name, Modifier.PRIVATE)
    if (property.getDefault != null) builder.initializer("$L", property.getDefault)
    if (CollectionUtils.isNotEmpty(property.getEnum)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[AllowableValues])
        .addMember("values", s"{ ${property.getEnum.asScala.mkString(", ")} }")
        .build)
    }

    if (property.getMaxLength != null || property.getMinLength != null) {
      val sizeSpecBuilder = AnnotationSpec.builder(classOf[Size])
      if (property.getMinLength != null) sizeSpecBuilder.addMember("min", s"${property.getMinLength}")
      if (property.getMaxLength != null) sizeSpecBuilder.addMember("max", s"${property.getMaxLength}")

      builder.addAnnotation(sizeSpecBuilder.build)
    }

    if (isNotBlank(property.getPattern)) {
      builder.addAnnotation(AnnotationSpec.builder(classOf[Pattern])
        .addMember("regexp", "$S", property.getPattern)
        .build)
    }

    FieldInternalResult(ClassName.get(classOf[UUID]), builder, Seq())
  }
}
