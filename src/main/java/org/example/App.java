package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class App {
    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            try (Connection connection = DriverManager.getConnection(args[0])) {
                connection.setAutoCommit(false);
                String selectSQL = "select pg_backend_pid() where 1 = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
                preparedStatement.setInt(1, 1);
                logger.info("{}", "BEGIN");
                ResultSet rs = preparedStatement.executeQuery();
                logger.info("{}", "execute <unnamed>: select pg_backend_pid() where 1 = $1");
                if (rs.next()) {
                    logger.info("{}", rs.getString(1));
                }
                logger.info("{}", "Sleeping in transaction...");
                Thread.sleep(1000);
                connection.commit();
                logger.info("{}", "COMMIT");
                logger.info("{}", "Sleeping out of transaction...");
                Thread.sleep(3000);
            } catch (SQLException e) {
                logger.error("{}", e);
            }
        }
    }
}
