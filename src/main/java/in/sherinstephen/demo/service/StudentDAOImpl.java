package in.sherinstephen.demo.service;

import in.sherinstephen.demo.odata.DemoEdmProvider;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Sherin (I073367)
 * @since 21/2/17
 */
public class StudentDAOImpl implements StudentDAO {

    private List<Entity> studentList;
    private List<Entity> departmentList;

    public StudentDAOImpl() {
        this.studentList = new ArrayList<>();
        this.departmentList = new ArrayList<>();
        initSampleData();
    }

    @Override
    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {

        // actually, this is only required if we have more than one Entity Sets
        if (edmEntitySet.getName().equals(DemoEdmProvider.ES_STUDENTS_NAME)) {
            return getStudents();
        }

        return null;
    }

    @Override
    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ES_STUDENTS_NAME)) {
            return getStudent(edmEntityType, keyParams);
        }

        return null;
    }

    @Override
    public Entity createEntityData(EdmEntitySet edmEntitySet, Entity entityToCreate) {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_STUDENT_NAME)) {
            return createStudent(edmEntityType, entityToCreate);
        }

        return null;
    }

    private Entity createStudent(EdmEntityType edmEntityType, Entity entityToCreate) {


        Integer newId = -1;
        Property idProperty = entityToCreate.getProperty("id");
        if (idProperty != null) {
            if (idProperty.getValue() != null) {
                newId = studentIdExists((Integer) idProperty.getValue()) ? getNextId() : (Integer) idProperty.getValue();
            } else {
                newId = getNextId();
            }
            idProperty.setValue(ValueType.PRIMITIVE, newId);
        } else {
            // as of OData v4 spec, the key property can be omitted from the POST request body
            entityToCreate.getProperties().add(new Property(null, "id", ValueType.PRIMITIVE, newId));
        }
        this.studentList.add(entityToCreate);

        return entityToCreate;
    }

    private boolean studentIdExists(int newId) {
        boolean found = false;
        for (Entity entity : this.studentList) {
            Integer existingID = (Integer) entity.getProperty("id").getValue();
            if (existingID == newId) {
                found = true;
                break;
            }
        }

        return found;
    }

    @Override
    public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates, Entity requestEntity, HttpMethod httpMethod) throws ODataApplicationException {

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_STUDENT_NAME)) {
            updateStudent(edmEntityType, keyPredicates, requestEntity, httpMethod);
        }
    }

    @Override
    public void deleteEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) throws ODataApplicationException {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_STUDENT_NAME)) {
            deleteStudent(edmEntityType, keyPredicates);
        }
    }

    @Override
    public EntityCollection getRelatedEntityCollection(Entity sourceEntity, EdmEntityType targetEntityType) {
        EntityCollection navigationTargetEntityCollection = new EntityCollection();

        FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
        String sourceEntityFqn = sourceEntity.getType();

        if (sourceEntityFqn.equals(DemoEdmProvider.ET_STUDENT_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(DemoEdmProvider.ET_DEPARTMENT_FQN)) {
            navigationTargetEntityCollection.setId(createId(sourceEntity, "ID", DemoEdmProvider.NAV_TO_DEP));
            // relation Products->Category (result all categories)
            int studentId = (Integer) sourceEntity.getProperty("id").getValue();
            if (studentId == 1 || studentId == 2) {
                navigationTargetEntityCollection.getEntities().add(departmentList.get(0));
            } else if (studentId == 3 || studentId == 4) {
                navigationTargetEntityCollection.getEntities().add(departmentList.get(1));
            } else if (studentId == 5 || studentId == 6) {
                navigationTargetEntityCollection.getEntities().add(departmentList.get(2));
            }
        } else if (sourceEntityFqn.equals(DemoEdmProvider.ET_DEPARTMENT_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(DemoEdmProvider.ET_STUDENT_FQN)) {
            navigationTargetEntityCollection.setId(createId(sourceEntity, "id", DemoEdmProvider.NAV_TO_STUDENTS));
            // relation Category->Products (result all products)
            int categoryID = (Integer) sourceEntity.getProperty("ID").getValue();
            if (categoryID == 1) {
                // the first 2 products are notebooks
                navigationTargetEntityCollection.getEntities().addAll(studentList.subList(0, 2));
            } else if (categoryID == 2) {
                // the next 2 products are organizers
                navigationTargetEntityCollection.getEntities().addAll(studentList.subList(2, 4));
            } else if (categoryID == 3) {
                // the first 2 products are monitors
                navigationTargetEntityCollection.getEntities().addAll(studentList.subList(4, 6));
            }
        }

        if (navigationTargetEntityCollection.getEntities().isEmpty()) {
            return null;
        }

        return navigationTargetEntityCollection;
    }

    @Override
    public Entity getRelatedEntity(Entity entity, EdmEntityType expandEdmEntityType) {
        return null;
    }

    private void deleteStudent(EdmEntityType edmEntityType, List<UriParameter> keyPredicates) throws ODataApplicationException {
        Entity productEntity = getStudent(edmEntityType, keyPredicates);
        if (productEntity == null) {
            throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        this.studentList.remove(productEntity);
    }

    private void updateStudent(EdmEntityType edmEntityType, List<UriParameter> keyPredicates, Entity requestEntity, HttpMethod httpMethod) throws ODataApplicationException {
        Entity studentEntity = getStudent(edmEntityType, keyPredicates);
        if (studentEntity == null) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // loop over all properties and replace the values with the values of the given payload
        // Note: ignoring ComplexType, as we don't have it in our odata model
        List<Property> existingProperties = studentEntity.getProperties();
        for (Property existingProp : existingProperties) {
            String propName = existingProp.getName();

            // ignore the key properties, they aren't updateable
            if (isKey(edmEntityType, propName)) {
                continue;
            }

            Property updateProperty = requestEntity.getProperty(propName);
            // the request payload might not consider ALL properties, so it can be null
            if (updateProperty == null) {
                // if a property has NOT been added to the request payload
                // depending on the HttpMethod, our behavior is different
                if (httpMethod.equals(HttpMethod.PATCH)) {
                    // in case of PATCH, the existing property is not touched
                    continue; // do nothing
                } else if (httpMethod.equals(HttpMethod.PUT)) {
                    // in case of PUT, the existing property is set to null
                    existingProp.setValue(existingProp.getValueType(), null);
                    continue;
                }
            }

            // change the value of the properties
            existingProp.setValue(existingProp.getValueType(), updateProperty.getValue());
        }
    }


    private boolean isKey(EdmEntityType edmEntityType, String propertyName) {
        List<EdmKeyPropertyRef> keyPropertyRefs = edmEntityType.getKeyPropertyRefs();
        for (EdmKeyPropertyRef propRef : keyPropertyRefs) {
            String keyPropertyName = propRef.getName();
            if (keyPropertyName.equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    private Entity getStudent(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException {
        // the list of entities at runtime
        EntityCollection entitySet = getStudents();

        /*  generic approach  to find the requested entity */
        Entity requestedEntity = Util.findEntity(edmEntityType, entitySet, keyParams);

        if (requestedEntity == null) {
            // this variable is null if our data doesn't contain an entity for the requested key
            // Throw suitable exception
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }

    private EntityCollection getStudents() {
        EntityCollection retEntitySet = new EntityCollection();
        retEntitySet.getEntities().addAll(this.studentList);
        return retEntitySet;
    }

    private void initSampleData() {


        // add some sample product entities
        final Entity e1 = new Entity()
                .addProperty(new Property(null, "id", ValueType.PRIMITIVE, 1))
                .addProperty(new Property(null, "firstName", ValueType.PRIMITIVE, "Morgan"))
                .addProperty(new Property(null, "lastName", ValueType.PRIMITIVE,
                        "Freeman"))
                .addProperty(new Property(null, "dateOfBirth", ValueType.PRIMITIVE, new Date()));
        e1.setType(DemoEdmProvider.ET_STUDENT_FQN.getFullQualifiedNameAsString());
        e1.setId(createId(e1, "id"));

        studentList.add(e1);

        final Entity e2 = new Entity()
                .addProperty(new Property(null, "id", ValueType.PRIMITIVE, 2))
                .addProperty(new Property(null, "firstName", ValueType.PRIMITIVE, "Tom"))
                .addProperty(new Property(null, "lastName", ValueType.PRIMITIVE,
                        "Hanks"))
                .addProperty(new Property(null, "dateOfBirth", ValueType.PRIMITIVE, new Date()));
        e2.setType(DemoEdmProvider.ET_STUDENT_FQN.getFullQualifiedNameAsString());
        e2.setId(createId(e2, "id"));
        studentList.add(e2);

        final Entity e3 = new Entity()
                .addProperty(new Property(null, "id", ValueType.PRIMITIVE, 3))
                .addProperty(new Property(null, "firstName", ValueType.PRIMITIVE, "Russell"))
                .addProperty(new Property(null, "lastName", ValueType.PRIMITIVE,
                        "Crowe"))
                .addProperty(new Property(null, "dateOfBirth", ValueType.PRIMITIVE, new Date()));
        e3.setType(DemoEdmProvider.ET_STUDENT_FQN.getFullQualifiedNameAsString());
        e3.setId(createId(e3, "id"));
        studentList.add(e3);
    }

    private URI createId(Entity entity, String idPropertyName, String navigationName) {
        try {
            StringBuilder sb = new StringBuilder(getEntitySetName(entity)).append("(");
            final Property property = entity.getProperty(idPropertyName);
            sb.append(property.asPrimitive()).append(")");
            if (navigationName != null) {
                sb.append("/").append(navigationName);
            }
            return new URI(sb.toString());
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create (Atom) id for entity: " + entity, e);
        }
    }

    private String getEntitySetName(Entity entity) {
        if (DemoEdmProvider.ET_DEPARTMENT_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return DemoEdmProvider.ES_DEPARTMENT_NAME;
        } else if (DemoEdmProvider.ET_STUDENT_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return DemoEdmProvider.ES_STUDENTS_NAME;
        }
        return entity.getType();
    }

    private URI createId(Entity entity, String id) {
        return createId(entity, id, null);
    }

    public int getNextId() {
        int nextId = (int) (Math.random() * 1000);
        while (studentIdExists(nextId)) {
            nextId = getNextId();
        }
        return nextId;
    }
}
