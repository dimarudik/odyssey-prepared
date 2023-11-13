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
        int SQLStatementsCapacity = Integer.parseInt(args[2]);
        int loopCount = Integer.parseInt(args[3]);
        ExecutorService executorService = Executors.newFixedThreadPool(threads + 1);
        List<Future<LogMessage>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(executorService.submit(new CallableTask(url, SQLStatementsCapacity, loopCount)));
            Thread.sleep(10);
        }

        Iterator<Future<LogMessage>> futureIterator = tasks.listIterator();
        while (futureIterator.hasNext()) {
            Future<LogMessage> future = futureIterator.next();
            if (future.isDone()) {
                LogMessage logMessage = future.get();
                if (logMessage != null) {
                    logger.info("thread = {} \t backend = {} \t executions per second = {}\t elapsed time (sec) = {}",
                            logMessage.threadName(), logMessage.backend(), logMessage.execsPerSecond(), logMessage.elapsedTime());
                }
                futureIterator.remove();
            }
            if (!futureIterator.hasNext()) {
                futureIterator = tasks.listIterator();
            }
            Thread.sleep(5);
        }

        executorService.shutdown();
    }
}

class CallableTask implements Callable<LogMessage> {
    private final String url;
    private final int SQLStatementCapacity;
    private final int loopCount;
    private static final Logger logger = LogManager.getLogger(CallableTask.class);

    public CallableTask(String url, int SQLStatementCapacity, int loopCount) {
        this.url = url;
        this.SQLStatementCapacity = SQLStatementCapacity;
        this.loopCount = loopCount;
    }

    @Override
    public LogMessage call() {
        LogMessage logMessage = null;
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);

            long start = System.currentTimeMillis();
            List<String> listOfSQLStatements = getListOfSQLStatements(SQLStatementCapacity);
            for (int i = 0; i < loopCount; i++) {
                PreparedStatement preparedStatement =
                        connection.prepareStatement(listOfSQLStatements
                                .get((int) (Math.random() * SQLStatementCapacity)));
                preparedStatement.setInt(1, i % 2);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int id = resultSet.getInt(1);
                }
                resultSet.close();
                preparedStatement.close();
                connection.commit();
            }
            long finish = System.currentTimeMillis();
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            logMessage = new LogMessage(Thread.currentThread().getName(), pgConnection.getBackendPID(),
                    (int)(loopCount * 1d / ((double) (finish - start) / 1000d)), (finish - start) / 1000d);
        } catch (SQLException e) {
            logger.error(e);
        }
        return logMessage;
    }

    private List<String> getListOfSQLStatements(int SQLStatementsCapacity) {
        List<String> listOfSQLStatements = new ArrayList<>(SQLStatementsCapacity);
        for (int i = 0; i < SQLStatementsCapacity; i++) {
            String SQLStatementPart1 = "select id from test t";
            String SQLStatementPart2 = " where id = ?";
            listOfSQLStatements.add(SQLStatementPart1 + i + SQLStatementPart2);
        }
        return listOfSQLStatements;
    }
}

record LogMessage (String threadName, int backend, int execsPerSecond, double elapsedTime) {}