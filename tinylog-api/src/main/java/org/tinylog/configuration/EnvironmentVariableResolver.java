/*
 * Copyright 2018 Martin Winandy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.tinylog.configuration;

/**
 * Resolver for environment variables.
 */
final class EnvironmentVariableResolver implements Resolver {

	static final EnvironmentVariableResolver INSTANCE = new EnvironmentVariableResolver();
	
	/** */
	private EnvironmentVariableResolver() {
	}

	@Override
	public String getName() {
		return "environment variables";
	}

	@Override
	public char getPrefix() {
		return '$';
	}

	@Override
	public String resolve(final String name) {
		return System.getenv(name);
	}

}