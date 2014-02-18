package com.qoomon.logback;


import java.net.InetAddress;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Sends {@link ILoggingEvent} objects to a remote a log server, usually a {@link SocketNode}.
 *
 * For more information on this appender, please refer to the online manual at http://logback.qos.ch/manual/appenders.html#SocketAppender
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */

public class SocketAppender extends AbstractSocketAppender<ILoggingEvent>
{


    private boolean                                                 includeCallerData = false;




    public SocketAppender()
    {
    }




    /**
     * Connects to remote server at <code>host</code> and <code>port</code>.
     */
    @Deprecated
    public SocketAppender(final String host, final int port)
    {
        super(host, port);
    }




    /**
     * Connects to remote server at <code>address</code> and <code>port</code>.
     */
    @Deprecated
    public SocketAppender(final InetAddress address, final int port)
    {
        super(address.getHostAddress(), port);
    }




    @Override
    protected void postProcessEvent(final ILoggingEvent event)
    {
        if (this.includeCallerData)
        {
            event.getCallerData();
    }
    }




    public void setIncludeCallerData(final boolean includeCallerData)
    {
        this.includeCallerData = includeCallerData;
    }


}
