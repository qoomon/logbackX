package com.qoomon.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/**
 * Highlighting based on log event level
 *
 * @author bengtbrodersen
 *
 */
public class RelativeCompositeConverter extends CompositeConverter<ILoggingEvent>
{

    long   lastTimestamp = -1;
    String timesmapCache = null;




    public RelativeCompositeConverter()
    {
    }


    @Override
    public String transform(final ILoggingEvent event, final String in)
    {
        final long now = event.getTimeStamp();

        synchronized (this)
        {
            // update timesmapStrCache only if now != lastTimestamp
            if (now != this.lastTimestamp)
            {
                this.lastTimestamp = now;
                // this.timesmapCache = Long.toString(now - event.getLoggerContextVO().getBirthTime());
                this.timesmapCache = this.transform(now - event.getLoggerContextVO().getBirthTime());
            }
            return this.timesmapCache;
        }
    }




    private String transform(final long value)
    {

        final long ms = value % 1000;
        final long s = (value / 1000) % 60;
        final long m = (value / 1000 / 60) % 60;
        final long h = (value / 1000 / 60 / 60) % 24;
        final long d = (value / 1000 / 60 / 60 / 24) % 7;
        final long w = (value / 1000 / 60 / 60 / 24 / 7);

        final StringBuffer stringBuffer = new StringBuffer();

        if (w + d > 0)
        {
            stringBuffer.append("W").append(String.format("%02d", d))
                    .append("-").append(d).append(" ");
        }

        stringBuffer.append(String.format("%02d", h))
                .append(":").append(String.format("%02d", m))
                .append(":").append(String.format("%02d", s))
                .append(",").append(String.format("%03d", ms));

        return stringBuffer.toString();
    }


}
