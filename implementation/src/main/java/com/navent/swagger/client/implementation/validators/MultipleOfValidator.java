package com.navent.swagger.client.implementation.validators;

import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class MultipleOfValidator implements ConstraintValidator<MultipleOf, String> {

    private BigDecimal multipleOf = BigDecimal.ZERO;

    @Override
    public void initialize(MultipleOf annotation) {
        multipleOf = new BigDecimal(annotation.value());
    }

    @Override
    public boolean isValid(String source, ConstraintValidatorContext cxt) {

        if (multipleOf == null) {
            return false;
        }

        if (StringUtils.isEmpty(source))
            return true;

        BigDecimal value = new BigDecimal(source);

        if(value.remainder(multipleOf).compareTo(BigDecimal.ZERO) != 0)
            return false;

        return true;
    }
}