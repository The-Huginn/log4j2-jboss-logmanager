/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.log4j;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

/**
 * A status logger which logs to a JBoss Log Manager Logger.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JBossStatusListener implements StatusListener {
    private static final String NAME = "org.jboss.logmanager.log4j.status";
    private static final Logger.AttachmentKey<StatusListener> STATUS_LISTENER_KEY = new Logger.AttachmentKey<>();
    private final Logger logger;
    private final LevelTranslator levelTranslator = LevelTranslator.getInstance();
    private final Level level;

    private JBossStatusListener(final Logger logger) {
        this.logger = logger;
        level = StatusLogger.getLogger().getLevel();
    }

    /**
     * Registers a status listener with the log context if one does not already exist.
     *
     * @param logContext the log context to possibly register the status listener with
     */
    static void registerIfAbsent(final LogContext logContext) {
        final Logger logger = logContext.getLogger(NAME);
        StatusListener listener = logger.getAttachment(STATUS_LISTENER_KEY);
        if (listener == null) {
            listener = new JBossStatusListener(logger);
            if (attachIfAbsent(logger, listener) == null) {
                StatusLogger.getLogger().registerListener(listener);
            }
        }
    }

    /**
     * Removes the status listener from the log context.
     *
     * @param logContext the log context to remove the status listener from
     */
    static void remove(final LogContext logContext) {
        final Logger logger = logContext.getLoggerIfExists(NAME);
        if (logger != null) {
            detach(logger);
        }
    }

    @Override
    public void log(final StatusData data) {
        logger.log(
                levelTranslator.translateLevel(data.getLevel()),
                data.getMessage().getFormattedMessage(),
                data.getThrowable());
    }

    @Override
    public Level getStatusLevel() {
        return level;
    }

    @Override
    public void close() {
        detach(logger);
    }

    private static StatusListener attachIfAbsent(final Logger logger, final StatusListener value) {
        if (System.getSecurityManager() == null) {
            return logger.attachIfAbsent(STATUS_LISTENER_KEY, value);
        }
        return AccessController
                .doPrivileged((PrivilegedAction<StatusListener>) () -> logger.attachIfAbsent(STATUS_LISTENER_KEY, value));
    }

    private static void detach(final Logger logger) {
        if (System.getSecurityManager() == null) {
            logger.detach(STATUS_LISTENER_KEY);
        } else {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                logger.detach(STATUS_LISTENER_KEY);
                return null;
            });
        }
    }

}
