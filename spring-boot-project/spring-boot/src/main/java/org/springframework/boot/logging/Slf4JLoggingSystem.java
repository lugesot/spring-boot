/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations that utilize SLF4J.
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public abstract class Slf4JLoggingSystem extends AbstractLoggingSystem {

	private static final String BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

	public Slf4JLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public void beforeInitialize() {
		super.beforeInitialize();
		configureJdkLoggingBridgeHandler();
	}

	@Override
	public void cleanUp() {
		if (isBridgeHandlerAvailable()) {
			removeJdkLoggingBridgeHandler();
			reinstateConsoleHandlerIfNecessary();
		}
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext,
			String location, LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		if (initializationContext != null) {
			applySystemProperties(initializationContext.getEnvironment(), logFile);
		}
	}

	private void configureJdkLoggingBridgeHandler() {
		try {
			if (isBridgeJulIntoSlf4j()) {
				removeJdkLoggingBridgeHandler();
				SLF4JBridgeHandler.install();
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
	}

	protected final boolean isBridgeJulIntoSlf4j() {
		return isBridgeHandlerAvailable() && isJulUsingItsDefaultConfiguration();
	}

	protected final boolean isBridgeHandlerAvailable() {
		return ClassUtils.isPresent(BRIDGE_HANDLER, getClassLoader());
	}

	private boolean isJulUsingItsDefaultConfiguration() {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 1 && handlers[0] instanceof ConsoleHandler;
	}

	private void removeJdkLoggingBridgeHandler() {
		try {
			removeDefaultRootHandler();
			SLF4JBridgeHandler.uninstall();
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	private void removeDefaultRootHandler() {
		try {
			Logger rootLogger = LogManager.getLogManager().getLogger("");
			Handler[] handlers = rootLogger.getHandlers();
			if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
				rootLogger.removeHandler(handlers[0]);
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	private void reinstateConsoleHandlerIfNecessary() {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		if (rootLogger.getHandlers().length == 0) {
			rootLogger.addHandler(new ConsoleHandler());
		}
	}

}
