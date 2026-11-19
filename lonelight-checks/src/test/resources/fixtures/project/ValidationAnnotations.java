// 检查：AnnotationAttributeCheck
// 两套校验命名空间各有 22 种配置，两个 record 每个组件故意漏填 message
// 文件最后对照空白、拼接、转义、文本块、常量及与标准注解同名的声明
package project;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

class ValidationAnnotations {
    private static final String REQUIRED = "请填写内容";

    record JakartaForm(
            @AssertFalse
            boolean flagMustBeFalse,
            @AssertTrue
            boolean confirmed,
            @DecimalMax("99.99")
            BigDecimal maxPrice,
            @DecimalMin("0.01")
            BigDecimal minPrice,
            @Digits(integer = 4, fraction = 2)
            BigDecimal amount,
            @Email
            String email,
            @Future
            LocalDate futureDate,
            @FutureOrPresent
            LocalDate startDate,
            @Max(100)
            int maxCount,
            @Min(0)
            int minCount,
            @Negative
            int negative,
            @NegativeOrZero
            int negativeOrZero,
            @NotBlank
            String title,
            @NotEmpty
            String name,
            @NotNull
            String id,
            @Null
            String initialValue,
            @Past
            LocalDate birthDate,
            @PastOrPresent
            LocalDate createdOn,
            @Pattern(regexp = "[A-Z]+")
            String code,
            @Positive
            int positive,
            @PositiveOrZero
            int positiveOrZero,
            @Size(min = 1, max = 30)
            String description
    ) { }

    record JavaxForm(
            @javax.validation.constraints.AssertFalse
            boolean flagMustBeFalse,
            @javax.validation.constraints.AssertTrue
            boolean confirmed,
            @javax.validation.constraints.DecimalMax("99.99")
            BigDecimal maxPrice,
            @javax.validation.constraints.DecimalMin("0.01")
            BigDecimal minPrice,
            @javax.validation.constraints.Digits(integer = 4, fraction = 2)
            BigDecimal amount,
            @javax.validation.constraints.Email
            String email,
            @javax.validation.constraints.Future
            LocalDate futureDate,
            @javax.validation.constraints.FutureOrPresent
            LocalDate startDate,
            @javax.validation.constraints.Max(100)
            int maxCount,
            @javax.validation.constraints.Min(0)
            int minCount,
            @javax.validation.constraints.Negative
            int negative,
            @javax.validation.constraints.NegativeOrZero
            int negativeOrZero,
            @javax.validation.constraints.NotBlank
            String title,
            @javax.validation.constraints.NotEmpty
            String name,
            @javax.validation.constraints.NotNull
            String id,
            @javax.validation.constraints.Null
            String initialValue,
            @javax.validation.constraints.Past
            LocalDate birthDate,
            @javax.validation.constraints.PastOrPresent
            LocalDate createdOn,
            @javax.validation.constraints.Pattern(regexp = "[A-Z]+")
            String code,
            @javax.validation.constraints.Positive
            int positive,
            @javax.validation.constraints.PositiveOrZero
            int positiveOrZero,
            @javax.validation.constraints.Size(min = 1, max = 30)
            String description
    ) { }

    void messages(
            @NotNull(message = "") String empty,
            @NotBlank(message = " " + "\t") String whitespace,
            @NotEmpty(message = "\u0020") String escaped,
            @jakarta.validation.constraints.NotNull(message = """
                    """) String blankBlock,
            @Size(min = 1, message = "长度至少为一") String literal,
            @Email(message = "邮箱" + "格式不正确") String concatenated,
            @NotBlank(message = REQUIRED) String constant,
            @javax.validation.constraints.NotNull(message = "请填写内容") String legacy,
            @Local.NotNull String unrelated
    ) { }

    static class Local {
        @interface NotNull { }
    }
}
