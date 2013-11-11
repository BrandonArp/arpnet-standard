package com.arpnetworking.utils;

import play.Logger;
import play.api.mvc.ChunkedResult;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import scala.runtime.AbstractFunction1;

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
        public Result call(Http.Context ctx) {
            Result result = null;
            final Counter counter;
            if (ctx.args.containsKey("query-log")) {
                counter = (Counter)ctx.args.get("query-log");
            }
            else {
                counter = new Counter();
                ctx.args.put("query-log", counter);
            }
            boolean async = false;
            try {
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
                counter.startTimer(finalCounterName);
                result = delegate.call(ctx);
                if (result instanceof AsyncResult) {
                    async = true;
                    AsyncResult asyncResult = (AsyncResult)result;
                    asyncResult.transform(new F.Function<Result, Result>() {
                        @Override
                        public Result apply(final Result result) throws Throwable {
                            counter.stopTimer(finalCounterName);
                            counter.saveCounters();
                            return result;
                        }
                    });
                } else if (result instanceof ChunkedResult) {
                    async = true;
                    ChunkedResult chunkedResult = (ChunkedResult)result;
                    chunkedResult.chunks().andThen(new AbstractFunction1<Result, Result>(){
                        @Override
                        public Result apply(Result result) {
                            counter.stopTimer(finalCounterName);
                            counter.saveCounters();
                            return result;
                        }
                    });
                } else {
                    counter.stopTimer(finalCounterName);
                }
            } catch(RuntimeException e) {
                throw e;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            } finally {
                if (!async) {
                    counter.saveCounters();
                }
            }
            return result;
        }
    }
}
