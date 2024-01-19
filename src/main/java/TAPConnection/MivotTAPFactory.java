package TAPConnection;

import tap.ServiceConnection;
import tap.TAPException;
import tap.config.ConfigurableTAPFactory;
import tap.db.JDBCConnection;

import java.util.Optional;
import java.util.Properties;

public class MivotTAPFactory extends ConfigurableTAPFactory {

    protected static MivotTAPFactory instance = null;

    public MivotTAPFactory(ServiceConnection service, Properties tapConfig) throws NullPointerException, TAPException {
        super(service, tapConfig);
        instance = this;
        System.out.println(instance);
    }

    public static Optional<JDBCConnection> getJDBCConnection() throws TAPException {
        if (instance != null)
            return Optional.of((JDBCConnection)(instance.getConnection("MIVOT")));
        return Optional.empty();
    }

    public static void freeJDBCConnection(final JDBCConnection conn){
        if (instance != null)
            instance.freeConnection(conn);
    }
}
