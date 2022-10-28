package com.atguigu.common.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
@Documented
@Constraint(validatedBy = { ListValueConstraintValidator.class })
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface ListValue {
     String message() default "{com.atguigu.common.valid.ListValue.message}";

     Class<?>[] groups() default { };

     Class<? extends Payload>[] payload() default { };

     //预先准备的值 vals={0,1}
     int[] vals() default { };
}
