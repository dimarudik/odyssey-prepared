package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        ExecutorService executorService = Executors.newFixedThreadPool(threads + 1);
        List<Future<LogMessage>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(executorService.submit(new CallableTask(url)));
            Thread.sleep(10);
        }

        Iterator<Future<LogMessage>> futureIterator = tasks.listIterator();
        while (futureIterator.hasNext()) {
            Future<LogMessage> future = futureIterator.next();
            if (future.isDone()) {
                LogMessage logMessage = future.get();
                if (logMessage != null) {
                    logger.info("thread = {} \t backend = {} \t elapsed time (sec) = {}",
                            logMessage.threadName(), logMessage.backend(), logMessage.elapsedTime());
                }
                futureIterator.remove();
            }
            if (!futureIterator.hasNext()) {
                futureIterator = tasks.listIterator();
            }
            Thread.sleep(1);
        }

        executorService.shutdown();
    }
}

class CallableTask implements Callable<LogMessage> {
    private final String url;
    private static final Logger logger = LogManager.getLogger(CallableTask.class);

    public CallableTask(String url) {
        this.url = url;
    }

    @Override
    public LogMessage call() {
        LogMessage logMessage = null;
        long start = System.currentTimeMillis();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            int id = 0;
            long finish = System.currentTimeMillis();

            String selectSQL = "select pg_backend_pid()";
            ResultSet resultSet = statement.executeQuery(selectSQL);
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }
            resultSet.close();

            logMessage = new LogMessage(Thread.currentThread().getName(), id, (finish - start) / 1000d);
            Thread.sleep(1000);
        } catch (SQLException | InterruptedException e) {
            logger.error("{}", e);
        }
        return logMessage;
    }

}

record LogMessage (String threadName, int backend, double elapsedTime) {}