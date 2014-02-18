package com.qoomon.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

public class LogbackTestUtil
{

  private static final Logger LOG = LoggerFactory.getLogger(LogbackTestUtil.class);

  public static void main(final String[] args) throws Exception
  {
    SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

    while (true)
    {
      MDC.put("first", "Dorothy");
      LOG.error("error", new Exception("ACH WAS"));
      System.err.println("info from System.err");
      LOG.warn("warn");
      LOG.info("info");
      System.out.println("info from System.out");
      LOG.debug("debug");
      MDC.remove("first");
      LOG.trace("trace");

      Thread.sleep(1000);
    }

  }
}
