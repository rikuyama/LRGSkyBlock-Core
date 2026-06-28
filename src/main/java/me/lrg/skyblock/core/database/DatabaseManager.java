package me.lrg.skyblock.core.database;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;


public class DatabaseManager {


    private HikariDataSource dataSource;


    public void connect() {


        HikariConfig config = new HikariConfig();


        config.setJdbcUrl(
                "jdbc:mysql://127.0.0.1:3306/LRGSkyBlock?useSSL=false&serverTimezone=UTC"
        );


        config.setUsername("rikuyamagorou");


        config.setPassword("peka2305");


        config.setMaximumPoolSize(10);


        dataSource = new HikariDataSource(config);


        System.out.println("MySQL接続成功");


    }



    public Connection getConnection() throws SQLException {

        return dataSource.getConnection();

    }



    public void disconnect() {


        if(dataSource != null){

            dataSource.close();

        }


    }


}