package com.qoomon.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;

public class LoggingServer
{

    public static void main(final String[] args)
    {
        final LoggerContext lc = new LoggerContext();
        final SimpleSocketServer simpleSocketServer = new SimpleSocketServer(lc, 5516);
        simpleSocketServer.start();
    }
}
