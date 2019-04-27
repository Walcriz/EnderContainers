package fr.utarwyn.endercontainers.database;

import fr.utarwyn.endercontainers.Config;
import fr.utarwyn.endercontainers.EnderContainers;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Dump all tables (included data) into a generated string.
 *
 * You can use, modify and freely distribute this file as long as you credit Isocra Ltd.
 * There is no explicit or implied guarantee of functionality associated with this file, use it at your own risk.
 * Adapted for EnderContainers.
 *
 * @since 2.0.0
 * @author Isocra Ltd
 * @author Utarwyn <maxime.malgorn@laposte.net>
 */
public class MysqlDumper {

	/**
	 * Database server linked
	 */
	private Database database;

	/**
	 * The plugin logger
	 */
	private Logger logger;

	MysqlDumper(Database database) {
		this.database = database;
		this.logger = EnderContainers.getInstance().getLogger();
	}

	/**
	 * Dump all tables into a string
	 * @return String of the data dumped from the connected database.
	 */
	public String dump() {
		Connection dbConn;
		DatabaseMetaData dbMetaData;

		String columnNameQuote = "\"";

		try {
			dbConn = this.database.getConnection();
			assert dbConn != null;

			dbMetaData = dbConn.getMetaData();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

		try {
			StringBuffer result = new StringBuffer();
			ResultSet rs = dbMetaData.getTables(null, null, null, null);

			result.append("-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - --\n");
			result.append("-- MySQL dump generated by EnderContainers plugin at ").append(String.format("%-23s", new Timestamp(System.currentTimeMillis()).toString())).append(" --\n");
			result.append("-- Migration from 1.X to 2.X                                                 --\n");
			result.append("-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - --\n");

			if (!rs.next()) {
				rs.close();
			} else {
				do {
					String tableName = rs.getString("TABLE_NAME");
					String tableType = rs.getString("TABLE_TYPE");

					if ("TABLE".equalsIgnoreCase(tableType)) {
						result.append("\n\n-- ").append(tableName);
						result.append("\nCREATE TABLE ").append(tableName).append(" (\n");
						ResultSet tableMetaData = dbMetaData.getColumns(null, null, tableName, "%");
						boolean firstLine = true;

						while (tableMetaData.next()) {
							if (firstLine)
								firstLine = false;
							else
								result.append(",\n");

							String columnName = tableMetaData.getString("COLUMN_NAME");
							String columnType = tableMetaData.getString("TYPE_NAME");
							int columnSize = tableMetaData.getInt("COLUMN_SIZE");
							String nullable = tableMetaData.getString("IS_NULLABLE");
							String nullString = "NULL";

							if ("NO".equalsIgnoreCase(nullable))
								nullString = "NOT NULL";

							result.append("    ").append(columnNameQuote).append(columnName).
									append(columnNameQuote).append(" ").append(columnType).
									append(" (").append(columnSize).append(")").append(" ").
									append(nullString);
						}

						tableMetaData.close();

						try {
							ResultSet primaryKeys = dbMetaData.getPrimaryKeys(null, null, tableName);
							String primaryKeyName = null;
							StringBuffer primaryKeyColumns = new StringBuffer();

							while (primaryKeys.next()) {
								String thisKeyName = primaryKeys.getString("PK_NAME");
								if (thisKeyName != null && primaryKeyName == null || thisKeyName == null &&
										primaryKeyName != null || thisKeyName != null && !thisKeyName.equals(primaryKeyName)) {

									if (primaryKeyColumns.length() > 0) {
										result.append(",\n    PRIMARY KEY ");
										if (primaryKeyName != null) { result.append(primaryKeyName); }
										result.append("(").append(primaryKeyColumns.toString()).append(")");
									}

									primaryKeyColumns = new StringBuffer();
									primaryKeyName = thisKeyName;
								}

								if (primaryKeyColumns.length() > 0)
									primaryKeyColumns.append(", ");

								primaryKeyColumns.append(primaryKeys.getString("COLUMN_NAME"));
							}

							if (primaryKeyColumns.length() > 0) {
								result.append(",\n    PRIMARY KEY ");

								if (primaryKeyName != null)
									result.append(primaryKeyName);

								result.append(" (").append(primaryKeyColumns.toString()).append(")");
							}
						} catch (SQLException e) {
							this.logger.warning("Unable to get primary keys for table " + tableName + "!");
							if (Config.debug) {
								e.printStackTrace();
							}
						}

						result.append("\n);\n");

						this.dumpTable(dbConn, result, tableName);
					}
				} while (rs.next());
				rs.close();
			}
			dbConn.close();
			return result.toString();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Dump a specific data of a table into a given string buffer.
	 *
	 * Copyright Isocra Ltd 2004
	 * You can use, modify and freely distribute this file as long as you credit Isocra Ltd.
	 * There is no explicit or implied guarantee of functionality associated with this file, use it at your own risk.
	 * Adapted for EnderContainers.
	 *
	 * @param dbConn SQL Connection used to perform requests to get data
	 * @param result Result buffer which stores the generated dump
	 * @param tableName Name of the table to dump
	 */
	private void dumpTable(Connection dbConn, StringBuffer result, String tableName) {
		try {
			PreparedStatement stmt = dbConn.prepareStatement("SELECT * FROM "+tableName);
			ResultSet rs = stmt.executeQuery();
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();

			result.append("\n\n-- Data for ").append(tableName).append("\n");

			while (rs.next()) {
				result.append("INSERT INTO ").append(tableName).append(" VALUES (");

				for (int i = 0; i < columnCount; i++) {
					if (i > 0)
						result.append(", ");

					Object value = rs.getObject(i + 1);

					if (value == null)
						result.append("NULL");
					else {
						String outputValue = value.toString();
						outputValue = outputValue.replaceAll("'","\\'");
						result.append("'").append(outputValue).append("'");
					}
				}

				result.append(");\n");
			}

			rs.close();
			stmt.close();
		} catch (SQLException e) {
			this.logger.warning("Unable to dump table " + tableName + "!");
			if (Config.debug) {
				e.printStackTrace();
			}
		}
	}

}