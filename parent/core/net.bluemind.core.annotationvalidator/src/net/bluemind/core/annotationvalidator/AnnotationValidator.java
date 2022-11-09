/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.annotationvalidator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationProviderResolver;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.bootstrap.ProviderSpecificBootstrap;
import jakarta.validation.spi.ValidationProvider;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;

public class AnnotationValidator {

	public static class GenericValidatorFactory<T> implements IValidatorFactory<T> {

		private final Class<T> klass;
		private IValidator<T> validatorInstance;

		public GenericValidatorFactory(Class<T> klass) {
			this.klass = klass;
			validatorInstance = new IValidator<T>() {

				@Override
				public void create(T obj) throws ServerFault {
					INSTANCE.validate(obj);
				}

				@Override
				public void update(T oldValue, T newValue) throws ServerFault {
					INSTANCE.validate(newValue);
				}
			};
		}

		@Override
		public Class<T> support() {
			return klass;
		}

		@Override
		public IValidator<T> create(BmContext context) {
			return validatorInstance;
		}

	}

	private static class HibernateValidationProviderResolver implements ValidationProviderResolver {

		@Override
		public List<ValidationProvider<?>> getValidationProviders() {
			return Arrays.asList(new HibernateValidator());
		}
	}

	private static final AnnotationValidator INSTANCE = new AnnotationValidator();

	private Validator validator;
	private ProviderSpecificBootstrap<HibernateValidatorConfiguration> p;

	private AnnotationValidator() {
		p = Validation.byProvider(HibernateValidator.class);
		HibernateValidatorConfiguration c = p.providerResolver(new HibernateValidationProviderResolver()).configure();
		ValidatorFactory factory = c.messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();

		validator = factory.getValidator();

	}

	public void validate(Object o) {
		Set<ConstraintViolation<Object>> violations = validator.validate(o);
		if (!violations.isEmpty()) {
			Optional<String> msg = violations.stream().map(v -> v.getPropertyPath() + " : " + v.getMessage())
					.reduce((t, u) -> t + "," + u);
			throw new ServerFault(msg.isPresent() ? msg.get() : "<null>", ErrorCode.INVALID_PARAMETER);
		}
	}

	public static AnnotationValidator get() {
		return INSTANCE;
	}
}
