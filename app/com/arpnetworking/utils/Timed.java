package com.arpnetworking.utils;

import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.SimpleResult;
import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Action to intercept and time (query log) requests
 *
 * @author barp
 */
public class Timed {
    @With(TimedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Logged {
        String counterName() default "";
    }

    public static class TimedAction extends Action<Logged> {

        @SuppressWarnings("unchecked")
        public F.Promise<SimpleResult> call(Http.Context ctx) {
            F.Promise<SimpleResult> result = null;
            final Counter counter;
            if (ctx.args.containsKey("query-log")) {
                counter = (Counter)ctx.args.get("query-log");
            }
            else {
                counter = new Counter();
                ctx.args.put("query-log", counter);
            }
            String counterName = configuration.counterName();
            if (counterName.equals("")) {
                counterName = ctx.request().path();

                //remove any integer based resource identifiers
                String oldCounter = counterName;
                counterName = counterName.replaceAll("/\\d+", "");
                if (!oldCounter.equals(counterName)) {
                    Logger.debug("Counter name changed to " + counterName + " from " + oldCounter);
                }
            }
            final String finalCounterName = counterName;
            try {
                counter.startTimer(finalCounterName);
                result = delegate.call(ctx);
                result.map(new F.Function<SimpleResult, Void>() {
                    @Override
                    public Void apply(SimpleResult simpleResult) throws Throwable {
                        counter.stopTimer(finalCounterName);
                        counter.saveCounters();

                        return null;
                    }
                }).onFailure(new F.Callback<Throwable>() {
                       @Override
                       public void invoke(Throwable throwable) throws Throwable {
                           counter.stopTimer(finalCounterName);
                           counter.saveCounters();
                       }
                });
                return result;
            } catch(RuntimeException e) {
                counter.stopTimer(finalCounterName);
                throw e;
            } catch(Throwable t) {
                counter.stopTimer(finalCounterName);
                throw new RuntimeException(t);
            }
        }
    }
}
