package nessusData.persistence;

import nessusData.entity.template.Pojo;
import nessusData.entity.template.NaturalIdPojo;

import java.util.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.json.JsonException;

import org.hibernate.Session;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ObjectLookupDao<POJO extends Pojo> extends Dao<POJO> {
    //whether to lookup the object by a (non-zero) id passed in the Pojo
    private final boolean naturalId;
    private final boolean getByIdWhenZero;
        // Typically, getByIdWhenZero will coincide with use of IdNullable deserializer

    public ObjectLookupDao(Class<POJO> pojoClass) {
        super(pojoClass);
        naturalId = NaturalIdPojo.class.isAssignableFrom(pojoClass);
        getByIdWhenZero = false;
    }

    public ObjectLookupDao(Class<POJO> pojoClass, boolean getByIdWhenZero) {
        super(pojoClass);
        this.naturalId = NaturalIdPojo.class.isAssignableFrom(pojoClass);

        if (getByIdWhenZero && !this.naturalId) {
                throw new IllegalArgumentException("Pojo class when getByIdWhenZero = true must be a NaturalIdPojo");
        }

        this.getByIdWhenZero = getByIdWhenZero;
    }

/*

        ObjectMapper mapper = new ObjectMapper();

        DeserializationConfig config = mapper.getDeserializationConfig();
        DeserializationContext context = mapper.getDeserializationContext();

        ObjectReader reader = mapper.readerFor(getPojoClass());

        ContextAttributes attributesBefore = reader.getAttributes();

        POJO pojo = reader.readValue(json);

        ContextAttributes attributesAfter = reader.getAttributes();




        MetamodelImplementor metamodel = (MetamodelImplementor)  sessionFactory.getMetamodel();
        // EntityTypeDescriptor<ScanInfo> entity = metamodel.entity(ScanInfo.class);
        ClassMetadata metadata = (ClassMetadata) metamodel.entityPersister(ScanInfo.class);

        metadata.getPropertyNames();

        String[] names = metadata.getPropertyNames();
        Type[] types = metadata.getPropertyTypes();

        System.out.println(names);
        System.out.println(types);

    */

    public POJO getOrCreate(POJO pojo) throws LookupException {
        if (pojo == null) {
            return null;
        }

        if (this.naturalId && (this.getByIdWhenZero || pojo.getId() != 0)) {
            POJO result = this.getById(pojo.getId());
            if (result != null && !pojo.equals(result)) {
                this.saveOrUpdate(pojo);
                result = this.getById(pojo.getId());

                if (!pojo.equals(result)) {
                    throw new LookupException("Unable to correctly assign object lookup Pojo to values:\n"
                            + pojo.toString(), this.getPojoClass());
                }
                return result;
            }
        }

        POJO result = this.findByExactPojo(pojo);

        if (result != null) {
            return result;
        }

        if (this.insert(pojo) != -1) {
            result = this.getById(pojo.getId());

            if (!pojo.equals(result)) {
                throw new LookupException("Unable to correctly assign object lookup Pojo to values:\n"
                        + pojo.toString(), this.getPojoClass());
            }

            return result;

        } else {
            throw new LookupException("Couldn't create pojo '" + pojo + "'",
                    this.getPojoClass());
        }
    }

    public static Map<String, Object> makeSearchMapFromJson(JsonNode searchMapNode) {
        Map<String, Object> searchMap = new HashMap();

        Iterator<Map.Entry<String, JsonNode>> iterator = searchMapNode.fields();
        // I guess you can't do a for loop over an iterator, only an iterable!!
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode nodeVal = entry.getValue();

            if (!nodeVal.isValueNode()) {
                throw new JsonException("Invalid non-primitive value in searchMap."
                        + key + "\n" + searchMapNode.toString());
            }

            Object value;

            switch (nodeVal.getNodeType()) {
                case NUMBER:
                    if (nodeVal.isFloatingPointNumber()) {
                        value = nodeVal.doubleValue();

                    } else if (nodeVal.isInt()) {
                        value = nodeVal.intValue();

                    } else if (nodeVal.isLong()) {
                        value = nodeVal.longValue();

                    } else {
                        throw new JsonException("Could not determine numeric type conversion for searchMap."
                                + key + "\n" + searchMapNode.toString());
                    }
                    break;

                case BOOLEAN:
                    value = nodeVal.booleanValue();
                    break;

                case STRING:
                    value = nodeVal.textValue();
                    break;

                case NULL:
                    value = null;
                    break;

                default:
                    throw new JsonException("Could not determine value type for JsonNode in searchMap"
                            + key + "\n" + searchMapNode.toString());

            }

            searchMap.put(key, value);

        }

        return searchMap;
    }


    public String toString() {
        return "[ObjectLookupDao for " + this.getPojoClass().getSimpleName() + "]";
    }

    public static <P extends Pojo, D extends Dao<P>> D get(Class<P> objectLookupPojoClass) {
        D dao = Dao.get(objectLookupPojoClass);
        if (dao != null && dao instanceof ObjectLookupDao) {
            return dao;

        } else {
            return null;
        }
    }
}