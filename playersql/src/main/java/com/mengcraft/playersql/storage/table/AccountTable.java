package com.mengcraft.playersql.storage.table;

public enum AccountTable {

    PLAYER_SQL_DATA(new DatabaseTable("playersql_data",
            new String[] {
                    "`uid` int NOT NULL PRIMARY KEY",
                    "`hand` int NOT NULL default 0",
                    "`locked` boolean NOT NULL",
                    "`lastUpdate` timestamp default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"
            })),

    PLAYER_INVENTORY(new DatabaseTable("playersql_inventory",
            new String[] {
                    "`uid` int not null",
                    "`slot` int not null",
                    "`item_id` varchar(64)",
                    "`item_amount` int default 0",
                    "`item_data` varchar(512)",
                    "`item_unique` varchar(128)",
                    "UNIQUE KEY `inventory` (`uid`,`slot`)",
            }
    ));

    private final DatabaseTable table;

    AccountTable(DatabaseTable table) {
        this.table = table;
    }

    public String getTableName() {
        return table.getTableName();
    }

    public String[] getColumns() {
        return table.getTableColumns();
    }

    public DatabaseTable getTable() {
        return table;
    }

    public void createTable() {
        table.createTable();
    }
}
