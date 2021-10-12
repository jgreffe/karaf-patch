package net.nanthrax.example.patch.log;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

public class PatchLogger {

    Logger log;

    public PatchLogger(Logger log) {
        this.log = log;
    }

    private String addCommonPrefixAndResolvePlaceHolders(String message) {
        return "[PATCH] " + message;
    }

    public void console(String messagePattern, Object... args) {
        String resolvedMessagePattern = addCommonPrefixAndResolvePlaceHolders(messagePattern);
        System.out.println(MessageFormatter.arrayFormat(resolvedMessagePattern, args).getMessage());
    }

    public void trace(String messagePattern, Object... args) {
        if (log.isTraceEnabled()) {
            String resolvedMessagePattern = addCommonPrefixAndResolvePlaceHolders(messagePattern);
            log.trace(resolvedMessagePattern, args);
            System.out.println(MessageFormatter.arrayFormat(resolvedMessagePattern, args).getMessage());
        }
    }

    public void debug(String messagePattern, Object... args) {
        if (log.isDebugEnabled()) {
            String resolvedMessagePattern = addCommonPrefixAndResolvePlaceHolders(messagePattern);
            log.debug(resolvedMessagePattern, args);
            System.out.println(MessageFormatter.arrayFormat(resolvedMessagePattern, args).getMessage());
        }
    }

    public void info(String messagePattern, Object... args) {
        String resolvedMessagePattern = addCommonPrefixAndResolvePlaceHolders(messagePattern);
        log.info(resolvedMessagePattern, args);
        System.out.println(MessageFormatter.arrayFormat(resolvedMessagePattern, args).getMessage());
    }

    public void warn(String messagePattern, Object... args) {
        String resolvedMessagePattern = addCommonPrefixAndResolvePlaceHolders(messagePattern);
        log.warn(resolvedMessagePattern, args);
        System.err.println(MessageFormatter.arrayFormat(resolvedMessagePattern, args).getMessage());
    }

    public void error(String messagePattern, Object... args) {
        String resolvedMessagePattern = addCommonPrefixAndResolvePlaceHolders(messagePattern);
        log.error(resolvedMessagePattern, args);
        System.err.println(MessageFormatter.arrayFormat(resolvedMessagePattern, args).getMessage());
    }
}
