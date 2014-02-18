package com.qoomon.logback;

/**
 * http://pueblo.sourceforge.net/doc/manual/ansi_color_codes.html
 *
 * @author bengtbrodersen
 *
 */
public final class ANSIUtil
{

    public final static String ESC_START         = "\033[";
    public final static String ESC_END           = "m";

    public final static String RESET             = "0";

    public final static String BOLD_ON           = "1";
    public final static String ITALICS_ON        = "3";
    public final static String UNDERLINE_ON      = "4";
    public final static String INVERSE_ON        = "7";
    public final static String STRIKETHROUGH_ON  = "9";

    public final static String BOLD_OFF          = "22";
    public final static String ITALICS_OFF       = "23";
    public final static String UNDERLINE_OFF     = "24";
    public final static String INVERSE_OFF       = "27";
    public final static String STRIKETHROUGH_OFF = "29";

    public final static String BLACK_FG          = "30";
    public final static String RED_FG            = "31";
    public final static String GREEN_FG          = "32";
    public final static String YELLOW_FG         = "33";
    public final static String BLUE_FG           = "34";
    public final static String MAGENTA_FG        = "35";
    public final static String CYAN_FG           = "36";
    public final static String WHITE_FG          = "37";
    public final static String DEFAULT_FG        = "39";

    public final static String BLACK_BG          = "40";
    public final static String RED_BG            = "41";
    public final static String GREEN_BG          = "42";
    public final static String YELLOW_BG         = "43";
    public final static String BLUE_BG           = "44";
    public final static String MAGENTA_BG        = "45";
    public final static String CYAN_BG           = "46";
    public final static String WHITE_BG          = "47";
    public final static String DEFAULT_BG        = "49";




    public static String set(final String... codes)
    {
        final StringBuffer builder = new StringBuffer();
        for (final String code : codes)
        {
            builder.append(set(code));
        }
        return builder.toString();

    }




    private static StringBuffer set(final String code)
    {
        final StringBuffer builder = new StringBuffer()
                .append(ESC_START)
                .append(code)
                .append(ESC_END);

        return builder;

    }
}
