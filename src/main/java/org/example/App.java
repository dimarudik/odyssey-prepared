package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class App {
    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException, SQLException {
        try (Connection connection = DriverManager.getConnection(args[0])) {
            while (true) {
                connection.setAutoCommit(false);
                String selectSQL = "select pid from pg_stat_activity where usename = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
                preparedStatement.setString(1, "test");
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    logger.info("{}", rs.getString(1));
                }
                Thread.sleep(1000);
                connection.commit();
            }
        }
    }
}
