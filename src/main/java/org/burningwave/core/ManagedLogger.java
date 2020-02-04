/*
 * This file is part of Burningwave Core.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/core
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Roberto Gentili
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.burningwave.core;

public interface ManagedLogger {
		
	@SuppressWarnings("unchecked")
	public default <T extends ManagedLogger> T disableLogging() {
		Repository.INSTANCE.disableLogging(this.getClass());
		return (T) this;
	}
	
	@SuppressWarnings("unchecked")
	public default <T extends ManagedLogger> T enableLogging() {
		Repository.INSTANCE.enableLogging(this.getClass());
		return (T) this;
	}
	
	default void logError(String message, Throwable exc) {
		Repository.INSTANCE.logError(this.getClass(), message, exc);
	}
	
	default void logError(String message) {
		Repository.INSTANCE.logError(this.getClass(), message);
	}
	
	default void logDebug(String message) {
		Repository.INSTANCE.logDebug(this.getClass(), message);
	}
	
	default void logDebug(String message, Object... arguments) {
		Repository.INSTANCE.logDebug(this.getClass(), message, arguments);
	}
	
	default void logInfo(String message) {
		Repository.INSTANCE.logInfo(this.getClass(), message);
	}
	
	default void logInfo(String message, Object... arguments) {
		Repository.INSTANCE.logInfo(this.getClass(), message, arguments);
	}
	
	default void logWarn(String message) {
		Repository.INSTANCE.logWarn(this.getClass(), message);
	}
	
	default void logWarn(String message, Object... arguments) {
		Repository.INSTANCE.logWarn(this.getClass(), message, arguments);
	}
	
	
	public static interface Repository {
		final static Repository INSTANCE = newInstance();
		
		static Repository newInstance() {
			try {
				String className = (String)org.burningwave.core.iterable.Properties.getGlobalProperty("managed-logger.repository");
				return (Repository)Class.forName(className).getConstructor().newInstance();
			} catch (Throwable exc) {
				try {
					Class.forName("org.slf4j.Logger");
					return new SLF4JManagedLoggerRepository();
				} catch (Throwable exc2) {
					return new SimpleManagedLoggerRepository();
				}
			}			
		}
		
		public static Repository getInstance() {
			return INSTANCE;
		}
		
		public void disableLogging(Class<?> client);
		
		public void enableLogging(Class<?> client);
		
		public void logError(Class<?> client, String message, Throwable exc);
		
		public void logError(Class<?> client, String message);
		
		public void logDebug(Class<?> client, String message);
		
		public void logDebug(Class<?> client, String message, Object... arguments);
		
		public void logInfo(Class<?> client, String message);
		
		public void logInfo(Class<?> client, String message, Object... arguments);
		
		public void logWarn(Class<?> client, String message);
		
		public void logWarn(Class<?> client, String message, Object... arguments);
	}
}
