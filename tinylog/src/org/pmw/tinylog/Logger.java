/*
 * Copyright 2012 Martin Winandy
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

package org.pmw.tinylog;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static class to create log entries.
 * 
 * The default logging level is {@link org.pmw.tinylog.ELoggingLevel#INFO} which ignores trace and debug log entries.
 * 
 * An {@link org.pmw.tinylog.ILoggingWriter} must be set to create any output.
 */
public final class Logger {

	private static final String DEFAULT_LOGGING_FORMAT = "{date} [{thread}] {class}.{method}()\n{level}: {message}";
	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final boolean USE_SUN_REFLECTION_FOR_CALLER = isCallerClassReflectionAvailable();

	private static volatile int maxLoggingStackTraceElements = 40;
	private static volatile ILoggingWriter loggingWriter = new ConsoleLoggingWriter();
	private static volatile ELoggingLevel loggingLevel = ELoggingLevel.INFO;
	private static final Map<String, ELoggingLevel> packageLoggingLevels = Collections.synchronizedMap(new HashMap<String, ELoggingLevel>());
	private static volatile String loggingFormat = DEFAULT_LOGGING_FORMAT;
	private static volatile Locale locale = Locale.getDefault();
	private static volatile List<Token> loggingEntryTokens = Tokenizer.parse(loggingFormat, locale);

	static {
		PropertiesLoader.reload();
	}

	private Logger() {
	}

	/**
	 * Returns the current logging level.
	 * 
	 * @return The current logging level
	 */
	public static ELoggingLevel getLoggingLevel() {
		return loggingLevel;
	}

	/**
	 * Change the logging level. The logger creates only log entries for the current logging level or higher.
	 * 
	 * @param level
	 *            New logging level
	 */
	public static void setLoggingLevel(final ELoggingLevel level) {
		loggingLevel = level;
	}

	/**
	 * Returns the logging level of a package.
	 * 
	 * @param packageName
	 *            Name of package
	 * 
	 * @return The logging level
	 */
	public static ELoggingLevel getLoggingLevel(final String packageName) {
		return getLoggingLevelOfPackage(packageName);
	}

	/**
	 * Sets the logging level of a package.
	 * 
	 * @param packageName
	 *            Name of package
	 * @param level
	 *            The logging level
	 */
	public static void setLoggingLevel(final String packageName, final ELoggingLevel level) {
		packageLoggingLevels.put(packageName, level);
	}

	/**
	 * Resets the logging level of a package to default (to use the current logging level again).
	 * 
	 * @param packageName
	 *            Name of package
	 */
	public static void resetLoggingLevel(final String packageName) {
		packageLoggingLevels.remove(packageName);
	}

	/**
	 * Resets all logging level for packages to default (to use the current logging level again).
	 */
	public static void resetAllLoggingLevel() {
		packageLoggingLevels.clear();
	}

	/**
	 * Returns the format pattern for log entries.
	 * 
	 * @return Format pattern for log entries.
	 */
	public static String getLoggingFormat() {
		return loggingFormat;
	}

	/**
	 * Sets the format pattern for log entries.
	 * <code>"{date:yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method}()\n{level}: {message}"</code> is the default format
	 * pattern. The date format pattern is compatible with {@link SimpleDateFormat}.
	 * 
	 * @param format
	 *            Format pattern for log entries (or <code>null</code> to reset to default)
	 * 
	 * @see SimpleDateFormat
	 */
	public static void setLoggingFormat(final String format) {
		if (format == null) {
			loggingFormat = DEFAULT_LOGGING_FORMAT;
		} else {
			loggingFormat = format;
		}
		loggingEntryTokens = Tokenizer.parse(loggingFormat, locale);
	}

	/**
	 * Gets the locale for message format.
	 * 
	 * @return Locale for message format
	 * 
	 * @see MessageFormat#getLocale()
	 */
	public static Locale getLocale() {
		return locale;
	}

	/**
	 * Sets the locale for message format.
	 * 
	 * @param locale
	 *            Locale for message format
	 * 
	 * @see MessageFormat#setLocale(Locale)
	 */
	public static void setLocale(final Locale locale) {
		if (locale == null) {
			Logger.locale = Locale.getDefault();
		} else {
			Logger.locale = locale;
		}
		loggingEntryTokens = Tokenizer.parse(loggingFormat, Logger.locale);
	}

	/**
	 * Returns the limit of stack traces for exceptions.
	 * 
	 * @return The limit of stack traces
	 */
	public static int getMaxStackTraceElements() {
		return maxLoggingStackTraceElements;
	}

	/**
	 * Sets the limit of stack traces for exceptions (default is 40). Set "-1" for no limitation and "0" to disable any
	 * stack traces.
	 * 
	 * @param maxStackTraceElements
	 *            Limit of stack traces
	 */
	public static void setMaxStackTraceElements(final int maxStackTraceElements) {
		if (maxStackTraceElements < 0) {
			Logger.maxLoggingStackTraceElements = Integer.MAX_VALUE;
		} else {
			Logger.maxLoggingStackTraceElements = maxStackTraceElements;
		}
	}

	/**
	 * Returns the current logging writer.
	 * 
	 * @return The current logging writer
	 */
	public static ILoggingWriter getWriter() {
		return loggingWriter;
	}

	/**
	 * Register a logging writer to output the created log entries.
	 * 
	 * @param writer
	 *            Logging writer to add (can be <code>null</code> to disable any output)
	 */
	public static void setWriter(final ILoggingWriter writer) {
		loggingWriter = writer;
	}

	/**
	 * Create a trace log entry.
	 * 
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void trace(final String message, final Object... arguments) {
		output(ELoggingLevel.TRACE, null, message, arguments);
	}

	/**
	 * Create a trace log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void trace(final Throwable exception, final String message, final Object... arguments) {
		output(ELoggingLevel.TRACE, exception, message, arguments);
	}

	/**
	 * Create a trace log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 */
	public static void trace(final Throwable exception) {
		output(ELoggingLevel.TRACE, exception, null);
	}

	/**
	 * Create a debug log entry.
	 * 
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void debug(final String message, final Object... arguments) {
		output(ELoggingLevel.DEBUG, null, message, arguments);
	}

	/**
	 * Create a debug log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void debug(final Throwable exception, final String message, final Object... arguments) {
		output(ELoggingLevel.DEBUG, exception, message, arguments);
	}

	/**
	 * Create a debug log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 */
	public static void debug(final Throwable exception) {
		output(ELoggingLevel.DEBUG, exception, null);
	}

	/**
	 * Create an info log entry.
	 * 
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void info(final String message, final Object... arguments) {
		output(ELoggingLevel.INFO, null, message, arguments);
	}

	/**
	 * Create an info log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void info(final Throwable exception, final String message, final Object... arguments) {
		output(ELoggingLevel.INFO, exception, message, arguments);
	}

	/**
	 * Create an info log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 */
	public static void info(final Throwable exception) {
		output(ELoggingLevel.INFO, exception, null);
	}

	/**
	 * Create a warning log entry.
	 * 
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void warn(final String message, final Object... arguments) {
		output(ELoggingLevel.WARNING, null, message, arguments);
	}

	/**
	 * Create a warning log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void warn(final Throwable exception, final String message, final Object... arguments) {
		output(ELoggingLevel.WARNING, exception, message, arguments);
	}

	/**
	 * Create a warning log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 */
	public static void warn(final Throwable exception) {
		output(ELoggingLevel.WARNING, exception, null);
	}

	/**
	 * Create an error log entry.
	 * 
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void error(final String message, final Object... arguments) {
		output(ELoggingLevel.ERROR, null, message, arguments);
	}

	/**
	 * Create an error log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 * @param message
	 *            Formated text for the log entry
	 * @param arguments
	 *            Arguments for the text message
	 * 
	 * @see MessageFormat#format(String, Object...)
	 */
	public static void error(final Throwable exception, final String message, final Object... arguments) {
		output(ELoggingLevel.ERROR, exception, message, arguments);
	}

	/**
	 * Create an error log entry.
	 * 
	 * @param exception
	 *            Exception to log
	 */
	public static void error(final Throwable exception) {
		output(ELoggingLevel.ERROR, exception, null);
	}

	private static void output(final ELoggingLevel level, final Throwable exception, final String message, final Object... arguments) {
		ILoggingWriter currentWriter = loggingWriter;

		String className;
		StackTraceElement stackTraceElement = null;
		if (USE_SUN_REFLECTION_FOR_CALLER) {
			try {
				className = getCallerClassName();
			} catch (Exception ex) {
				stackTraceElement = getStackTraceElement();
				className = stackTraceElement.getClassName();
			}
		} else {
			stackTraceElement = getStackTraceElement();
			className = stackTraceElement.getClassName();
		}

		if (currentWriter != null && getLoggingLevelOfClass(className).ordinal() <= level.ordinal()) {
			try {
				StringBuilder builder = new StringBuilder();

				String threadName = null;
				Date now = null;

				for (Token token : loggingEntryTokens) {
					switch (token.getType()) {
						case THREAD:
							if (threadName == null) {
								threadName = Thread.currentThread().getName();
							}
							builder.append(threadName);
							break;

						case CLASS:
							builder.append(className);
							break;

						case METHOD:
							if (stackTraceElement == null) {
								stackTraceElement = getStackTraceElement();
							}
							builder.append(stackTraceElement.getMethodName());
							break;

						case FILE:
							if (stackTraceElement == null) {
								stackTraceElement = getStackTraceElement();
							}
							builder.append(stackTraceElement.getFileName());
							break;

						case LINE_NUMBER:
							if (stackTraceElement == null) {
								stackTraceElement = getStackTraceElement();
							}
							builder.append(stackTraceElement.getLineNumber());
							break;

						case LOGGING_LEVEL:
							builder.append(level);
							break;

						case DATE:
							if (now == null) {
								now = new Date();
							}
							DateFormat formatter = (DateFormat) token.getData();
							String format;
							synchronized (formatter) {
								format = formatter.format(now);
							}
							builder.append(format);
							break;

						case MESSAGE:
							if (message != null) {
								builder.append(new MessageFormat(message, locale).format(arguments));
							}
							if (exception != null) {
								if (message != null) {
									builder.append(": ");
								}
								int countLoggingStackTraceElements = maxLoggingStackTraceElements;
								if (countLoggingStackTraceElements == 0) {
									builder.append(exception.getClass().getName());
									String exceptionMessage = exception.getMessage();
									if (exceptionMessage != null) {
										builder.append(": ");
										builder.append(exceptionMessage);
									}
								} else {
									builder.append(getPrintedException(exception, countLoggingStackTraceElements));
								}
							}
							break;

						default:
							builder.append(token.getData());
							break;
					}
				}
				builder.append(NEW_LINE);

				currentWriter.write(level, builder.toString());
			} catch (Exception ex) {
				error(ex, "Could not create log entry");
			}
		}
	}

	private static ELoggingLevel getLoggingLevelOfClass(final String className) {
		if (!packageLoggingLevels.isEmpty()) {
			int index = className.lastIndexOf('.');
			if (index > 0) {
				return getLoggingLevel(className.substring(0, index));
			}
		}
		return loggingLevel;
	}

	private static ELoggingLevel getLoggingLevelOfPackage(final String packageName) {
		String packageKey = packageName;
		while (true) {
			ELoggingLevel level = packageLoggingLevels.get(packageKey);
			if (level != null) {
				return level;
			}
			int index = packageKey.lastIndexOf('.');
			if (index > 0) {
				packageKey = packageKey.substring(0, index);
			} else {
				return loggingLevel;
			}
		}
	}

	private static StackTraceElement getStackTraceElement() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		if (stackTraceElements.length > 4) {
			return stackTraceElements[4];
		} else {
			return new StackTraceElement("<unknown>", "<unknown>", "<unknown>", -1);
		}
	}

	private static String getPrintedException(final Throwable exception, final int countStackTraceElements) {
		StringBuilder builder = new StringBuilder();
		builder.append(exception.getClass().getName());

		String message = exception.getMessage();
		if (message != null) {
			builder.append(": ");
			builder.append(message);
		}

		StackTraceElement[] stackTrace = exception.getStackTrace();
		int length = Math.max(1, Math.min(stackTrace.length, countStackTraceElements));
		for (int i = 0; i < length; ++i) {
			builder.append(NEW_LINE);
			builder.append('\t');
			builder.append("at ");
			builder.append(stackTrace[i]);
		}

		if (stackTrace.length > length) {
			builder.append(NEW_LINE);
			builder.append('\t');
			builder.append("...");
			return builder.toString();
		}

		Throwable cause = exception.getCause();
		if (cause != null) {
			builder.append(NEW_LINE);
			builder.append("Caused by: ");
			builder.append(getPrintedException(cause, countStackTraceElements - length));
		}

		return builder.toString();
	}

	private static boolean isCallerClassReflectionAvailable() {
		try {
			Class<?> reflectionClass = Class.forName("sun.reflect.Reflection");
			return reflectionClass.getDeclaredMethod("getCallerClass", int.class) != null;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("restriction")
	private static String getCallerClassName() {
		return sun.reflect.Reflection.getCallerClass(4).getName();
	}

}
