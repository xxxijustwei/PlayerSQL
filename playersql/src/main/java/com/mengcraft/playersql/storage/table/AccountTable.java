package com.mengcraft.playersql.storage.table;

public enum AccountTable {

    PLAYER_SQL_DATE(new DatabaseTable("playersql_date",
            new String[] {
                    "`uid` int NOT NULL PRIMARY KEY",
                    "`hand` int NOT NULL default 0",
                    "`inventory` longtext",
                    "`armor` text",
                    "`locked` boolean NOT NULL",
                    "`lastUpdate` timestamp default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"
            }));

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
