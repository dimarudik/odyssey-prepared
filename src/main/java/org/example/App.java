package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.jdbc.PgConnection;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException {
        String url = "jdbc:postgresql://localhost:8432/postgres";
        String user = "test";
        String password = "test";
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            PgConnection pgConnection = connection.unwrap(PgConnection.class);
            logger.info("PreferQueryMode: {}", pgConnection.getPreferQueryMode());
            logger.info("PrepareThreshold: {}", pgConnection.getPrepareThreshold());
            connection.setAutoCommit(false);
            String selectSQL = "select pid from pg_stat_activity where usename = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
            preparedStatement.setString(1, "test");
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                logger.info(rs.getInt(1));
                System.out.println(rs.getString(1));
            }
            Thread.sleep(10000);
            connection.commit();
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(sw);
        }
    }
}
