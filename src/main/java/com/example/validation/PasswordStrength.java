package com.example.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {
    String message() default "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one digit";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
} 