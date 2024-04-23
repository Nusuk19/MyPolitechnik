package com.mysql.cj.jdbs;

import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class Driver implements java.sql.Driver {
    /**
     * @param url  the URL of the database to which to connect
     * @param info a list of arbitrary string tag/value pairs as
     *             connection arguments. Normally at least a "user" and
     *             "password" property should be included.
     * @return
     * @throws SQLException
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return null;
    }

    /**
     * @param url the URL of the database
     * @return
     * @throws SQLException
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return false;
    }

    /**
     * @param url  the URL of the database to which to connect
     * @param info a proposed list of tag/value pairs that will be sent on
     *             connect open
     * @return
     * @throws SQLException
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    /**
     * @return
     */
    @Override
    public int getMajorVersion() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public int getMinorVersion() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    /**
     * @return
     * @throws SQLFeatureNotSupportedException
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
