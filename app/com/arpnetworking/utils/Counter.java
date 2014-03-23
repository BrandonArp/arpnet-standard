package com.arpnetworking.utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import play.Logger;
import play.Logger.ALogger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Query logs
 *
 * @author barp
 */
public class Counter {
    private static final ALogger QUERY_LOG = Logger.of("query-log");
    private HashMap<String, ArrayList<Long>> counters = new HashMap<>();
    private HashMap<String, ArrayList<Long>> timers = new HashMap<>();
    private HashMap<String, ArrayList<Double>> gauges = new HashMap<>();
    private HashMap<String, Long> startedTimers = new HashMap<String, Long>();
    private HashMap<String, String> annotations = new HashMap<String, String>();
    private Boolean hasDumped = false;
    private Boolean dumpLocked = false;
    private static final DecimalFormat timestampFormat = new DecimalFormat("##.000");

    public Counter() {
        annotations.put("initTimestamp",  timestampFormat.format(Double.valueOf(DateTime.now().getMillis()) / 1000));
    }

    public Long increment(String counterName, Long count) {
        ArrayList<Long> vals = initCounterKey(counterName);
        long currentValue = vals.get(vals.size() - 1);
        currentValue += count;
        vals.set(vals.size() - 1, currentValue);
        return currentValue;
    }

    public Long increment(String counterName) {
        return increment(counterName, 1l);
    }

    public Long decrement(String counterName, Long count) {
        return increment(counterName, count * -1);
    }

    public Long decrement(String counterName) {
        return decrement(counterName,  1l);
    }

    public void recordGauge(String gaugeName, double value) {
        ArrayList<Double> values = initGauge(gaugeName);
        values.add(value);
    }

    private ArrayList<Double> initGauge(final String gaugeName) {
        ArrayList<Double> values = gauges.get(gaugeName);
        if (values == null) {
            values = new ArrayList<>();
            gauges.put(gaugeName, values);
        }
        return values;
    }

    public void startTimer(String timerName) {
        initTimerKey(timerName);
        if (startedTimers.containsKey(timerName)) {
            throw new IllegalArgumentException("Cannot start a timer that is already started");
        }
        startedTimers.put(timerName, System.nanoTime());
    }

    public void stopTimer(String timerName) {
        if (!startedTimers.containsKey(timerName)) {
            throw new IllegalArgumentException("Cannot stop a timer that has not been started");
        }

        Long now = System.nanoTime();
        Long startTime = startedTimers.get(timerName);
        startedTimers.remove(timerName);

        Long elapsed = (now - startTime) / 1000;
        ArrayList<Long> times = timers.get(timerName);
        times.add(elapsed);
    }

    private ArrayList<Long> initCounterKey(String counterName) {
        ArrayList<Long> values = counters.get(counterName);
        if (values == null) {
            values = new ArrayList<>();
            values.add(0L);
            counters.put(counterName, values);
        }
        return values;
    }

    private void initTimerKey(String timerName) {
        if (!timers.containsKey(timerName)) {
            timers.put(timerName, new ArrayList<Long>());
        }
    }

    public void annotate(String key, String annotation) {
        annotations.put(key, annotation);
    }

    private String dumpCounters() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode ret = new ObjectNode(factory);

        //version number

        ObjectNode annotationsNode = new ObjectNode(factory);

        //Write the init and final timestamps first to be nice to splunk
        if (annotations.containsKey("initTimestamp")) {
            annotationsNode.put("initTimestamp", annotations.get("initTimestamp"));
            annotations.remove("initTimestamp");
        }

        if (annotations.containsKey("finalTimestamp")) {
            annotationsNode.put("finalTimestamp", annotations.get("finalTimestamp"));
            annotations.remove("finalTimestamp");
        }

        for (Map.Entry<String, String> annotation : annotations.entrySet()) {
            annotationsNode.put(annotation.getKey(), annotation.getValue());
        }
        ret.put("annotations", annotationsNode);

        ret.put("version", "2c");

        ret.put("counters", buildLongValuesNode(counters));
        ret.put("timers", buildLongValuesNode(timers));
        ret.put("gauges", buildDoubleValuesNode(gauges));


        return ret.toString();
    }

    private ObjectNode buildDoubleValuesNode(final HashMap<String, ArrayList<Double>> map) {
        ObjectNode countersNode = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, ArrayList<Double>> counterSet : map.entrySet()) {
            ArrayNode counterEntries = JsonNodeFactory.instance.arrayNode();
            for (Double timerEntry : counterSet.getValue()) {
                counterEntries.add(timerEntry);
            }
            countersNode.put(counterSet.getKey(), counterEntries);
        }
        return countersNode;
    }

    private ObjectNode buildLongValuesNode(final HashMap<String, ArrayList<Long>> map) {
        ObjectNode countersNode = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, ArrayList<Long>> counterSet : map.entrySet()) {
            ArrayNode counterEntries = JsonNodeFactory.instance.arrayNode();
            for (Long timerEntry : counterSet.getValue()) {
                counterEntries.add(timerEntry);
            }
            countersNode.put(counterSet.getKey(), counterEntries);
        }
        return countersNode;
    }


    public void lockSave() {
        dumpLocked = true;
    }

    public void unlockSave() {
        dumpLocked = false;
    }

    public void saveCounters() {
        if (!hasDumped && !dumpLocked) {
            hasDumped = true;
            annotations.put("finalTimestamp",  timestampFormat.format((double) DateTime.now().getMillis() / 1000));
            QUERY_LOG.info(dumpCounters());
        }
    }
}
