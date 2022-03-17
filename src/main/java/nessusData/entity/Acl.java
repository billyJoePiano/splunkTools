package nessusData.entity;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.*;

import nessusData.entity.template.*;
import nessusData.persistence.ObjectLookupDao;
import nessusData.serialize.*;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;

@Entity(name = "Acl")
@Table(name = "acl")
public class Acl extends NullableIdPojo {
	public static final ObjectLookupDao<Acl> dao
			= new ObjectLookupDao<Acl>(Acl.class, true);

	private Integer owner;

	private int permissions;

	private String name;

	@Column(name = "display_name")
	@JsonProperty("display_name")
	private String displayName;

	private String type;

	public Integer getOwner() {
		return owner;
	}

	public void setOwner(Integer owner) {
		this.owner = owner;
	}

	public int getPermissions() {
		return permissions;
	}

	public void setPermissions(int permissions) {
		this.permissions = permissions;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
