package com.navent.swagger.client.implementation.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueItemsValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueItems {
    boolean value() default true;

    String message() default "{UniqueItems}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
