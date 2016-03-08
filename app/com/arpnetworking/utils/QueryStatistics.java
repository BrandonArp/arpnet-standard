package com.arpnetworking.utils;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.meta.MetaQueryPlanStatistic;

import java.util.List;

/**
 * Gets the query statistics for an Ebean server
 *
 * @author barp
 */
public class QueryStatistics {
    public static List<MetaQueryPlanStatistic> getStatistics(String serverName) {
        return Ebean.getServer(serverName).find(MetaQueryPlanStatistic.class).findList();
    }

    public static List<MetaQueryPlanStatistic> getStatistics() {
        return Ebean.find(MetaQueryPlanStatistic.class).findList();
    }
}
