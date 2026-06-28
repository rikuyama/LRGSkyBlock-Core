package me.lrg.skyblock.core.database.table;


import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.Statement;



public class DatabaseSetup {


    private final DatabaseManager databaseManager;



    public DatabaseSetup(DatabaseManager databaseManager){

        this.databaseManager = databaseManager;

    }





    public void createTables(){


        try(Connection connection = databaseManager.getConnection();
            Statement statement = connection.createStatement()){



            statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS player_data (

                    uuid VARCHAR(36) PRIMARY KEY,

                    name VARCHAR(16),

                    coins DOUBLE DEFAULT 0,

                    health INT DEFAULT 100,

                    mana INT DEFAULT 100,

                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP

                    )
                    """
            );




            statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS skills (

                    uuid VARCHAR(36) PRIMARY KEY,

                    farming DOUBLE DEFAULT 0,

                    mining DOUBLE DEFAULT 0,

                    combat DOUBLE DEFAULT 0,

                    foraging DOUBLE DEFAULT 0,

                    fishing DOUBLE DEFAULT 0,

                    enchanting DOUBLE DEFAULT 0,

                    alchemy DOUBLE DEFAULT 0,

                    taming DOUBLE DEFAULT 0

                    )
                    """
            );





            statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS collections (

                    uuid VARCHAR(36),

                    collection VARCHAR(50),

                    amount BIGINT DEFAULT 0,

                    PRIMARY KEY(uuid, collection)

                    )
                    """
            );



            System.out.println("SkyBlockテーブル作成完了");


        }
        catch(Exception e){

            e.printStackTrace();

        }


    }


}