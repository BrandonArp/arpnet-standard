package com.arpnetworking.utils;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.meta.MetaQueryStatistic;

import java.util.List;

/**
 * Gets the query statistics for an Ebean server
 *
 * @author barp
 */
public class QueryStatistics {
    public static List<MetaQueryStatistic> getStatistics(String serverName) {
        return Ebean.getServer(serverName).find(MetaQueryStatistic.class).findList();
    }

    public static List<MetaQueryStatistic> getStatistics() {
        return Ebean.find(MetaQueryStatistic.class).findList();
    }
}
