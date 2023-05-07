package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(args[0])) {
            connection.setAutoCommit(false);
//            String selectSQL = "select pg_backend_pid() where 1 = ?";
            String selectSQL = "select id from test where id = ?";

            int n = 10000;
            int backend = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
                preparedStatement.setInt(1, 1);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    backend = rs.getInt(1);
                }
                connection.commit();
            }
            long finish = System.currentTimeMillis();


            logger.info("pid = {}; time (sec) = {}; executions per second = {}",
                    backend, (finish - start) / 1000d, (n * 1d / ((finish - start) / 1000)));
        } catch (SQLException e) {
            logger.error("{}", e);
        }
    }
}
