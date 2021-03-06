/*
 * Copyright 2016 Martin Winandy
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

package org.tinylog.pattern;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;

/**
 * Token for outputting the text message of a log entry.
 */
final class MessageToken implements Token {

	private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\r\n|\n|\r");
	private static final String NEW_LINE = System.getProperty("line.separator");

	/** */
	MessageToken() {
	}

	@Override
	public Collection<LogEntryValue> getRequiredLogEntryValues() {
		return Collections.singleton(LogEntryValue.MESSAGE);
	}

	@Override
	public void render(final LogEntry logEntry, final StringBuilder builder) {
		String message = logEntry.getMessage();
		if (message != null) {
			builder.append(NEW_LINE_PATTERN.matcher(message).replaceAll(NEW_LINE));
		}
	}

	@Override
	public void apply(final LogEntry logEntry, final PreparedStatement statement, final int index) throws SQLException {
		statement.setString(index, logEntry.getMessage());
	}

}
