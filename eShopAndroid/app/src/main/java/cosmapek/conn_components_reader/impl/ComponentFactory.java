package cosmapek.conn_components_reader.impl;


import cosmapek.conn_components_reader.prov.IManager;

/**
 * Created by ArchyWin on 5/25/2015.
 */
public class ComponentFactory {
    private static IManager manager = null;

    private ComponentFactory() {
    }

    public static synchronized IManager createInstance() {
        if (manager == null)
            manager = new Manager();
        return manager;
    }
}
