package com.arpnetworking.utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.hooks.ConnectionHook;
import play.Application;
import play.Configuration;
import play.Logger;
import play.Plugin;

import javax.sql.DataSource;
import java.util.Iterator;

/**
 * Plugin that starts the standard metrics and stats collection
 *
 * @author barp
 */

public class ArpnetStandard extends Plugin {
    private final Application _Application;

    public ArpnetStandard(Application application) {
        _Application = application;
    }
    @Override
    public void onStart() {
        Logger.info("Starting Arpnet standard services");
        decorateDataSources(_Application);
        play.api.Logger logger = play.api.Logger.apply("query-log");
        org.slf4j.Logger slfLogger = logger.underlyingLogger();

        boolean directLog = false;
        if (slfLogger instanceof ch.qos.logback.classic.Logger) {
            final ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) slfLogger;
            LoggerContext context = logbackLogger.getLoggerContext();
            for (Appender<ILoggingEvent> appender : new Iterable<Appender<ILoggingEvent>>() {
                @Override
                public Iterator<Appender<ILoggingEvent>> iterator() {
                    return logbackLogger.iteratorForAppenders();
                }

                }) {
                directLog = true;
            }
            if (!directLog) {
                Logger.warn("It appears that there is not a logger setup for query logs.  For query logs to work, they must be directly written to an independent file.");
                Logger.warn("Add the following to your logger.xml file:");
                Logger.warn("\n" +
                        "    <appender name=\"QUERY-LOG-FILE\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">\n" +
                        "      <file>${application.home}/logs/query-log.log</file>\n" +
                        "      <rollingPolicy class=\"ch.qos.logback.core.rolling.TimeBasedRollingPolicy\">\n" +
                        "        <!-- hourly rollover -->\n" +
                        "        <fileNamePattern>${application.home}/logs/query-log.%d{yy-MM-dd_HH}.log</fileNamePattern>\n" +
                        "        <!-- keep 30 days of history -->\n" +
                        "        <maxHistory>720</maxHistory>\n" +
                        "      </rollingPolicy>\n" +
                        "      <encoder>\n" +
                        "        <pattern>%message%n</pattern>\n" +
                        "      </encoder>\n" +
                        "    </appender>" +
                        "    <appender name=\"ASYNCQL\" class=\"ch.qos.logback.classic.AsyncAppender\">\n" +
                        "      <appender-ref ref=\"QUERY-LOG-FILE\" />\n" +
                        "    </appender>" +
                        "    <logger name=\"query-log\" level=\"INFO\" additivity=\"false\">\n" +
                        "      <appender-ref ref=\"ASYNCQL\"/>\n" +
                        "    </logger>");
            }
        } else {
            Logger.warn("Unknown logging platform, cannot determine if query logs will work");
        }
    }

    @Override
    public void onStop() {
    }


    private void decorateDataSources(Application app) {
        Configuration dbConfig = app.configuration().getConfig("db");
        Configuration ebeanConf = app.configuration().getConfig("ebean");
        if (dbConfig == null) {
            return;
        }
        for (String dbName : dbConfig.subKeys()) {
            DataSource datasource = play.db.DB.getDataSource(dbName);
            if (datasource instanceof BoneCPDataSource) {
                BoneCPDataSource bcpDs = (BoneCPDataSource) datasource;
                ConnectionHook originalHook = bcpDs.getConnectionHook();
                bcpDs.setConnectionHook(new MetricLoggingHook(originalHook, dbName));
            }

            final String dbEbeanConf = ebeanConf.getString(dbName);
            boolean foundModel = false;
            if (dbEbeanConf != null) {
                if (dbEbeanConf.contains("com.avaje.ebean.meta.*") &&
                        (dbEbeanConf.contains("com.arpnetworking.utils.models.*") ||
                                dbEbeanConf.contains("com.arpnetworking.utils.models.QueryStatisticFinder"))) {
                    foundModel = true;
                }

                if (!foundModel) {
                    String modified = dbEbeanConf + ",com.avaje.ebean.meta.*,com.arpnetworking.utils.models.*";
                    Logger.warn("Database \"" + dbName + "\" models not loaded properly. ArpnetStandard plugin requires specific models to be loaded into Ebean.");
                    Logger.warn("Recommended model entry is: ebean." + dbName + "=\"" + modified + "\"");
                }
            }
        }
    }
}
