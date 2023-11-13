package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.PGConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        String url = args[0];
        int threads = Integer.parseInt(args[1]);
        int lifeTime = Integer.parseInt(args[2]);
        ExecutorService executorService = Executors.newFixedThreadPool(threads + 1);
        List<Future<LogMessage>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(executorService.submit(new CallableTask(url, lifeTime)));
            Thread.sleep(10);
        }

        Iterator<Future<LogMessage>> futureIterator = tasks.listIterator();
        double elapsedTime = 0;
        while (futureIterator.hasNext()) {
            Future<LogMessage> future = futureIterator.next();
            if (future.isDone()) {
                LogMessage logMessage = future.get();
                if (logMessage != null) {
                    logger.info("thread = {} \t backend = {} \t elapsed time (sec) = {}",
                            logMessage.threadName(), logMessage.backend(), logMessage.elapsedTime());
                }
                futureIterator.remove();
                assert logMessage != null;
                elapsedTime += logMessage.elapsedTime();
            }
            if (!futureIterator.hasNext()) {
                futureIterator = tasks.listIterator();
            }
            Thread.sleep(1);
        }

        executorService.shutdown();
        System.out.println(elapsedTime / threads);
    }
}

class CallableTask implements Callable<LogMessage> {
    private final String url;
    private final int lifeTime;
    private static final Logger logger = LogManager.getLogger(CallableTask.class);

    public CallableTask(String url, int lifeTime) {
        this.url = url;
        this.lifeTime = lifeTime;
    }

    @Override
    public LogMessage call() {
        LogMessage logMessage;
        long start = System.currentTimeMillis();
        try (Connection connection = DriverManager.getConnection(url)) {
            long finish = System.currentTimeMillis();
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            logMessage = new LogMessage(Thread.currentThread().getName(), pgConnection.getBackendPID(), (finish - start) / 1000d);
            Thread.sleep(lifeTime);
        } catch (SQLException | InterruptedException e) {
            logMessage = new LogMessage(Thread.currentThread().getName(), -1, 0);
            logger.error("{}", e);
        }
        return logMessage;
    }
}

record LogMessage (String threadName, int backend, double elapsedTime) {}