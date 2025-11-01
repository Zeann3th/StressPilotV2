package dev.zeann3th.stresspilot.common.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = EndpointRequestValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEndpointRequest {
    String message() default "Invalid endpoint request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

