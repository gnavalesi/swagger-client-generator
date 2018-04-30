package com.navent.swagger.client.implementation.validators;

import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;

public class UniqueItemsValidator implements ConstraintValidator<UniqueItems, List> {

    private boolean unique = false;

    @Override
    public void initialize(UniqueItems annotation) {
        unique = annotation.value();
    }

    @Override
    public boolean isValid(List source, ConstraintValidatorContext cxt) {

        return !unique || CollectionUtils.isEmpty(source) || new HashSet(source).size() == source.size();

    }
}