package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.Driver;
import org.postgresql.PGConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException, UnknownHostException {
        InetAddress[] addr = InetAddress.getAllByName("rda-pcidb0045.pgsql.tcsbank.ru");
        for (InetAddress inetAddress : addr) {
            System.out.println(inetAddress.getHostAddress());
        }
        String url = args[0];

        Properties properties = Driver.parseURL(url, null);
        assert properties != null;
        properties.forEach((k, v) -> System.out.println(k + " " + v));

        int threadCount = Integer.parseInt(args[1]);
        int SQLStatementsCapacity = Integer.parseInt(args[2]);
        int loopCount = Integer.parseInt(args[3]);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount + 1);
        List<Future<LogMessage>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(executorService.submit(new CallableTask(url, SQLStatementsCapacity, loopCount)));
            Thread.sleep(10);
        }

        Iterator<Future<LogMessage>> futureIterator = tasks.listIterator();
        double elapsedTime = 0;
        while (futureIterator.hasNext()) {
            Future<LogMessage> future = futureIterator.next();
            if (future.isDone()) {
                LogMessage logMessage = future.get();
                if (logMessage != null) {
                    logger.info("thread = {} \t backend = {} \t executions per second = {}\t elapsed time (sec) = {}",
                            logMessage.threadName(), logMessage.backend(), logMessage.execsPerSecond(), logMessage.elapsedTime());
                    elapsedTime += logMessage.elapsedTime();
                }
                futureIterator.remove();
            }
            if (!futureIterator.hasNext()) {
                futureIterator = tasks.listIterator();
            }
            Thread.sleep(5);
        }

        logger.info("Average elapsed time: {}", elapsedTime / threadCount);
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
            Thread.sleep(2000);
        } catch (SQLException e) {
            logger.error(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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