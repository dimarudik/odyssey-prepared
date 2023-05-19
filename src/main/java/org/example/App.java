package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    private static final String selectSQL = "select id from test where id = ?";
    private static final int threads = 20;
    private static final int loopCount = 100000;

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(threads + 1);
        for (int i = 0; i < threads; i++) {
            executorService.submit(new CallableTask(args[0], selectSQL, loopCount));
        }

        executorService.shutdown();
    }

}

class CallableTask implements Callable<String> {
    private final String url;
    private final String statement;
    private final int loopCount;
    private static final Logger logger = LogManager.getLogger(CallableTask.class);

    public CallableTask(String url, String statement, int loopCount) {
        this.url = url;
        this.statement = statement;
        this.loopCount = loopCount;
    }

    @Override
    public String call() {
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement preparedStatement = connection.prepareStatement(statement);
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);

            int id = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; i < loopCount; i++) {
                preparedStatement.setInt(1, i % 2);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    id = resultSet.getInt(1);
                }
                resultSet.close();
                connection.commit();
            }
            long finish = System.currentTimeMillis();

            String selectSQL = "select pg_backend_pid()";
            ResultSet resultSet = statement.executeQuery(selectSQL);
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }
            resultSet.close();
            connection.commit();

            logger.info("\t backend = {}; \t\t executions per second = {};\t\t elapsed time (sec) = {}; ",
                    id, (loopCount * 1d / ((finish - start) / 1000)), (finish - start) / 1000d );
        } catch (SQLException e) {
            logger.error("{}", e);
        }
        return Thread.currentThread().getName();
    }
}