package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(args[0])) {
            connection.setAutoCommit(false);
            String selectSQL = "select id from test where id = ?";

            int n = 20000;
            int id = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
                preparedStatement.setInt(1, 1);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(1);
                }
                connection.commit();
            }
            long finish = System.currentTimeMillis();


            logger.info("\t\t executions per second = {};\t\t elapsed time (sec) = {}; ",
                    (n * 1d / ((finish - start) / 1000)), (finish - start) / 1000d );
        } catch (SQLException e) {
            logger.error("{}", e);
        }
    }
}
