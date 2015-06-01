package infra;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

import com.exlibris.dps.api.rosetta.RosettaRestWS;

public class ManualEndPointApplication extends Application
{
    private Set<Object> singletons = new HashSet<Object>();
    private Set<Class<?>> empty = new HashSet<Class<?>>();

    public ManualEndPointApplication() {
        // ADD YOUR RESTFUL RESOURCES HERE
        this.singletons.add(new RosettaRestWS());
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        return this.empty;
    }

    @Override
    public Set<Object> getSingletons()
    {
        return this.singletons;
    }
}
