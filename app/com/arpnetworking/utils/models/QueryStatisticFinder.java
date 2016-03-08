package com.arpnetworking.utils.models;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.common.BeanList;
import com.avaje.ebean.event.BeanFinder;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.meta.MetaQueryPlanStatistic;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.query.CQueryPlan;

import javax.persistence.PersistenceException;
import java.util.Iterator;
import java.util.List;

/**
 * BeanFinder for MetaQueryStatistic.
 */
public class QueryStatisticFinder implements BeanFinder<MetaQueryPlanStatistic> {


    public MetaQueryPlanStatistic find(BeanQueryRequest<MetaQueryPlanStatistic> request) {
        throw new RuntimeException("Not Supported yet");
    }

    /**
     * Only returns Lists at this stage.
     */
    public BeanCollection<MetaQueryPlanStatistic> findMany(BeanQueryRequest<MetaQueryPlanStatistic> request) {
        SpiQuery.Type queryType = ((SpiQuery<?>) request.getQuery()).getType();
        if (!queryType.equals(SpiQuery.Type.LIST)) {
            throw new PersistenceException("Only findList() supported at this stage.");
        }

        BeanList<MetaQueryPlanStatistic> list = new BeanList<MetaQueryPlanStatistic>();

        SpiEbeanServer server = (SpiEbeanServer) request.getEbeanServer();
        build(list, server);

        String orderBy = request.getQuery().order().toStringFormat();
        if (orderBy == null) {
            orderBy = "beanType, origQueryPlanHash, autofetchTuned";
        }
        server.sort(list, orderBy);

        return list;
    }

    private void build(List<MetaQueryPlanStatistic> list, SpiEbeanServer server) {
        for (BeanDescriptor<?> desc : server.getBeanDescriptors()) {
            build(list, desc);
        }
    }

    private void build(List<MetaQueryPlanStatistic> list, BeanDescriptor<?> desc) {
        Iterator<CQueryPlan> it = desc.queryPlans();
        while (it.hasNext()) {
            CQueryPlan queryPlan = it.next();
            list.add(queryPlan.getSnapshot(false));
        }
    }

}
