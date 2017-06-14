package conn_MasterCardA_MasterCardASensor.prov;

import java.util.Map;

/**
 * Created by COSMAPEK-Feature IDE on 2016-jul-11.
 * Feature: MasterCardA
 * Component: Conn_MasterCardA_MasterCardASensor
 *
 * This interface is also used by the Cosmapek Executor component to manage 
 * the components at runtime.
 */
public interface IManager {
    
    public void setRequiredInterface(String name, Object facade); //external use (important)

    public Object getProvidedInterface(String name); //external use (important)

    public Object getRequiredInterface(String name); //internal use (important)

    public String[] getProvidedInterfaceNames(); //external use

    public String[] getRequiredInterfaceNames(); //internal and external use

    public Map<String, Class<?>> getProvidedInterfaceTypes(); //external use

    public Map<String, Class<?>> getRequiredInterfaceTypes(); //internal and external use

}