package com.qoomon.logback;

import static com.qoomon.logback.ANSIUtil.BLUE_BG;
import static com.qoomon.logback.ANSIUtil.BLUE_FG;
import static com.qoomon.logback.ANSIUtil.BOLD_ON;
import static com.qoomon.logback.ANSIUtil.DEFAULT_BG;
import static com.qoomon.logback.ANSIUtil.DEFAULT_FG;
import static com.qoomon.logback.ANSIUtil.RED_BG;
import static com.qoomon.logback.ANSIUtil.RESET;
import static com.qoomon.logback.ANSIUtil.WHITE_FG;
import static com.qoomon.logback.ANSIUtil.YELLOW_BG;
import static com.qoomon.logback.ANSIUtil.YELLOW_FG;
import static com.qoomon.logback.ANSIUtil.set;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/**
 * Highlighting based on log event level
 *
 * @author bengtbrodersen
 *
 */
public class HighlightingCompositeConverter extends CompositeConverter<ILoggingEvent>
{

    final private String             defaultStyle = set(RESET);
    final private Map<Level, String> levelStyles  = new HashMap<Level, String>();




    public HighlightingCompositeConverter()
    {
        this.setStyleTemplate(0);
    }




    @Override
    protected String transform(final ILoggingEvent event, final String in)
    {
        final Level level = event.getLevel();
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getStyle(level));
        if (!in.isEmpty())
        {
            builder.append(in);
            builder.append(this.getDefaultStyle());
        }
        return builder.toString();
    }




    /**
     * Derived classes return the style specific to the derived level.
     *
     * @return the style for level
     */
    public String getStyle(final Level level)
    {
        final String style = this.levelStyles.get(level);
        return style != null ? style : this.getDefaultStyle();

    }




    public String getDefaultStyle()
    {
        return this.defaultStyle;
    }




    public void setStyleTemplate(final String templateId)
    {
        this.setStyleTemplate(Integer.parseInt(templateId));
    }




    public void setStyleTemplate(final int templateId)
    {

        if (templateId == 0)
        {
            this.levelStyles.put(Level.ERROR, set(WHITE_FG, RED_BG, BOLD_ON));
            this.levelStyles.put(Level.WARN, set(YELLOW_FG, BOLD_ON));
            this.levelStyles.put(Level.INFO, set(BLUE_FG));
            this.levelStyles.put(Level.DEBUG, set(DEFAULT_FG));
            this.levelStyles.put(Level.TRACE, set(DEFAULT_FG));
            return;
        }

        if (templateId == 1)
        {
            this.levelStyles.put(Level.ERROR, set(WHITE_FG, RED_BG));
            this.levelStyles.put(Level.WARN, set(WHITE_FG, YELLOW_BG));
            this.levelStyles.put(Level.INFO, set(WHITE_FG, BLUE_BG));
            this.levelStyles.put(Level.DEBUG, set(DEFAULT_BG));
            this.levelStyles.put(Level.TRACE, set(DEFAULT_BG));
            return;
        }

        if (templateId == 2)
        {
            this.levelStyles.put(Level.ERROR, set(WHITE_FG, RED_BG, BOLD_ON));
            this.levelStyles.put(Level.WARN, set(YELLOW_BG));
            this.levelStyles.put(Level.INFO, set(BLUE_FG));
            this.levelStyles.put(Level.DEBUG, set(DEFAULT_FG));
            this.levelStyles.put(Level.TRACE, set(DEFAULT_FG));
            return;
        }

    }




    public void setErrorStyle(final String style)
    {
        this.levelStyles.put(Level.ERROR, set(style.split(";")));
    }




    public void setWarnStyle(final String style)
    {
        this.levelStyles.put(Level.WARN, set(style.split(";")));
    }




    public void setInfoStyle(final String style)
    {
        this.levelStyles.put(Level.INFO, set(style.split(";")));
    }




    public void setDebugStyle(final String style)
    {
        this.levelStyles.put(Level.DEBUG, set(style.split(";")));
    }




    public void setTraceStyle(final String style)
    {
        this.levelStyles.put(Level.TRACE, set(style.split(";")));
    }

}
