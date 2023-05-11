package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        String selectSQL = "select id from test where id = ?";
        try (Connection connection = DriverManager.getConnection(args[0]);
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
            Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);

            int n = 100000;
            int id = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                preparedStatement.setInt(1, i % 2);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    id = resultSet.getInt(1);
                }
                resultSet.close();
                connection.commit();
            }
            long finish = System.currentTimeMillis();

            selectSQL = "select pg_backend_pid()";
            ResultSet resultSet = statement.executeQuery(selectSQL);
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }
            resultSet.close();
            connection.commit();


            logger.info("\t backend = {}; \t\t executions per second = {};\t\t elapsed time (sec) = {}; ",
                    id, (n * 1d / ((finish - start) / 1000)), (finish - start) / 1000d );
        } catch (SQLException e) {
            logger.error("{}", e);
        }
    }
}
