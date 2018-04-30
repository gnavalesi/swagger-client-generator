package com.navent.swagger.client.implementation.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.math.BigDecimal;

@Documented
@Constraint(validatedBy = MultipleOfValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MultipleOf {
    String value();

    String message() default "{MultipleOf}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
