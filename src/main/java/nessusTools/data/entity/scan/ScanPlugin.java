package nessusTools.data.entity.scan;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import nessusTools.data.deserialize.*;
import nessusTools.data.entity.objectLookup.*;
import nessusTools.data.entity.response.*;
import nessusTools.data.persistence.*;
import org.hibernate.annotations.*;

import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.util.*;

@Entity(name = "ScanPlugin")
@Table(name = "scan_plugin")
public class ScanPlugin extends ScanResponse.MultiChildLookup<ScanPlugin> {

    public static final ObjectLookupDao<ScanPlugin>
            dao = new ObjectLookupDao<ScanPlugin>(ScanPlugin.class);

    @ManyToOne
    @JoinColumn(name = "plugin_id")
    @Access(AccessType.PROPERTY)
    @JsonUnwrapped
    Plugin plugin;

    @Column(name = "host_count")
    @JsonProperty("host_count")
    Integer hostCount;

    @ManyToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Fetch(value = FetchMode.SUBSELECT)
    @Access(AccessType.PROPERTY)
    @JoinTable(
            name = "scan_plugin_host",
            joinColumns = { @JoinColumn(name = "scan_plugin_id") },
            inverseJoinColumns = { @JoinColumn(name = "plugin_host_id") }
    )
    @OrderColumn(name = "__order_for_scan_plugin_host")
    List<PluginHost> hosts;


    @Override
    public void _set(ScanPlugin o) {
        this.__set(o);
        this.plugin = o.plugin;
        this.hosts = o.hosts;
    }

    @Override
    public boolean _lookupMatch(ScanPlugin other) {
        return this.__lookupMatch(other)
                && this.plugin != null && other.plugin != null
                && (this.plugin.getId() != 0 && other.plugin.getId() != 0
                    ? this.plugin.getId() == other.plugin.getId()
                    : Objects.equals(this, other));
    }

    @Override
    @Transient
    @JsonIgnore
    public Map<String, Object> _getSearchMap() {
        return Map.of("response", this.getResponse(), "plugin", this.plugin);
    }


    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = Plugin.dao.getOrCreate(plugin);
    }

    public Integer getHostCount() {
        return hostCount;
    }

    public void setHostCount(Integer hostCount) {
        this.hostCount = hostCount;
    }

    public List<PluginHost> getHosts() {
        return hosts;
    }

    public void setHosts(List<PluginHost> hosts) {
        this.hosts = PluginHost.dao.getOrCreate(hosts);
    }
}