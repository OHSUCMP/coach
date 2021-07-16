package edu.ohsu.cmp.htnu18app.cqfruler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CDSHookExecutorService {
    private static CDSHookExecutorService service = null;

    public static CDSHookExecutorService getInstance() {
        if (service == null) {
            service = new CDSHookExecutorService();
        }
        return service;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<String, CDSHookExecutor> map = new HashMap<>();
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final Object lock = new Object();

    private volatile String currentlyExecutingSessionId = null;

    private final Thread queueMonitor = new Thread() {
        @Override
        public void run() {
            while (true) {
                try {
                    String sessionId = queue.take();

                    if (map.containsKey(sessionId)) {
                        currentlyExecutingSessionId = sessionId;

                        CDSHookExecutor executor = map.remove(sessionId);
                        try {
                            logger.info("running executor for " + sessionId);
                            Thread t = new Thread(executor);
                            t.start();
                            t.join();
                            logger.info("completed executor for " + sessionId);

                        } catch (InterruptedException ie) {
                            logger.info("executor thread interrupted for " + sessionId);
                            throw ie;

                        } catch (Exception e) {
                            logger.error("caught " + e.getClass().getName() + " executing CDS Hooks for session " + sessionId, e);
                        }

                        currentlyExecutingSessionId = null;

                    } else {
                        logger.warn("attempted to get executor from map with sessionId=" + sessionId + " but no executor found - WTH?");
                    }

                } catch (InterruptedException e) {
                    logger.warn("queue monitor thread interrupted");
                    break;
                }
            }

            logger.info("shutdown complete");
        }
    };

    private CDSHookExecutorService() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("shutting down...");
                queueMonitor.interrupt();
            }
        });

        queueMonitor.start();
    }

    public void queue(CDSHookExecutor executor) throws InterruptedException {
        synchronized(lock) {
            String sessionId = executor.getSessionId();
            map.put(sessionId, executor);
            if ( ! queue.contains(sessionId) ) {
                queue.put(sessionId);
            }
        }
    }

    public void dequeue(String sessionId) {
        synchronized(lock) {
            queue.remove(sessionId);
            map.remove(sessionId);
        }
    }

    public int getPosition(String sessionId) {
        synchronized(lock) {
            if (currentlyExecutingSessionId != null && currentlyExecutingSessionId.equals(sessionId)) {
                return 0;

            } else if ( ! queue.contains(sessionId) ) {
                return -1;
            }

            int index = 1;

            Iterator<String> iter = queue.iterator();
            while (iter.hasNext()) {
                String s = iter.next();
                if (s.equals(sessionId)) break;
                index++;
            }

            return index;
        }
    }
}
