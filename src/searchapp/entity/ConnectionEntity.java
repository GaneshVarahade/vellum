package searchapp.entity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import searchapp.util.storage.AbstractEntity;

/**
 *
 * @author evans
 */
public class ConnectionEntity extends AbstractEntity {

    private String connectionName;
    private String driver;
    private String url;
    private String user;
    private String password;

    public ConnectionEntity() {
    }
    
    public ConnectionEntity(String connectionName, String driver, String url, 
            String user, String password) {
        this.connectionName = connectionName;
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public String getKey() {
        return connectionName;
    }
    
    public String getConnectionName() {
        return connectionName;
    }

    public String getDriver() {
        return driver;
    }
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}