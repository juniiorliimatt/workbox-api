package br.com.workbox.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

/**
 * Aplicação é só pt-BR — sem negociação de locale por request, um {@link Locale} fixo
 * evita repetir {@code Locale.of("pt", "BR")} em cada chamada de {@link MessageSource}.
 * {@link LocalValidatorFactoryBean} aponta pro mesmo {@code messages.properties} (via
 * {@code setValidationMessageSource}) pra Bean Validation resolver {@code message =
 * "{chave}"} nas anotações sem precisar de um {@code ValidationMessages.properties}
 * separado — uma fonte só de mensagens pro projeto inteiro.
 *
 * <p>{@link FixedLocaleResolver}: sem isso, uma constraint sem {@code message=} custom
 * (ex.: {@code @Size}) cai no texto padrão em inglês do bundle interno do Hibernate
 * Validator, porque a interpolação usa {@code LocaleContextHolder.getLocale()} — que sem
 * {@code Accept-Language} no request cai no locale default da JVM (inglês no container),
 * não no {@link Locale} fixo do {@link MessageSourceAccessor} acima (que só cobre nossas
 * próprias chaves, resolvidas via {@code messages.getMessage(...)} explícito).
 */
@Configuration
public class MessageConfig {

    @Bean
    public MessageSourceAccessor messageSourceAccessor(final MessageSource messageSource) {
        return new MessageSourceAccessor(messageSource, Locale.of("pt", "BR"));
    }

    @Bean
    public LocalValidatorFactoryBean getValidator(final MessageSource messageSource) {
        final var validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(messageSource);
        return validatorFactoryBean;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.of("pt", "BR"));
    }
}
