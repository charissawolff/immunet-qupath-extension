package org.computational_immunology.ext.ImmuNet.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImmuNetLog {
    // Define the logger at the top of your class
    private static final Logger logger = LoggerFactory.getLogger(ImmuNetLog.class);


    // Static shorthand for INFO
    public static void log(String message) {
        logger.info(message);
    }

    // Static shorthand for INFO with placeholders (useful for variables)
    public static void log(String format, Object... args) {
        logger.info(format, args);
    }

    // Static shorthand for ERROR
    public static void error(String message, Throwable t) {
        logger.error(message, t);
    }

    public static void error(String format, Object... arguments) {
        logger.error(format,arguments);
    }

    private ImmuNetLog() {
        /* This utility class should not be instantiated */
    }
}
