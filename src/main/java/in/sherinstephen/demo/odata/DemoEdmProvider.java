package in.sherinstephen.demo.odata;

import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A class that declares the metadata of our OData service.
 * Here we declare the main elements of an OData service: EntityType, EntitySet, EntityContainer and Schema
 * (with the corresponding Olingo classes {@link CsdlEntityType}, {@link CsdlEntitySet}, {@link CsdlEntityContainer} and {@link CsdlSchema}).
 *
 * @author Sherin (I073367)
 * @since 21/2/17
 */
public class DemoEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "OData.Demo";

    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types Names
    public static final String ET_STUDENT_NAME = "Students";
    public static final FullQualifiedName ET_STUDENT_FQN = new FullQualifiedName(NAMESPACE, ET_STUDENT_NAME);

    public static final String ET_DEPARTMENT_NAME = "Departments";
    public static final FullQualifiedName ET_DEPARTMENT_FQN = new FullQualifiedName(NAMESPACE, ET_DEPARTMENT_NAME);

    // Entity Set Names
    public static final String ES_STUDENTS_NAME = "Students";
    public static final String ES_DEPARTMENT_NAME = "Departments";


    // this method is called for one of the EntityTypes that are configured in the Schema
    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        if (entityTypeName.equals(ET_STUDENT_FQN)) {
            CsdlProperty id = new CsdlProperty().setName("id").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty fname = new CsdlProperty().setName("firstName").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty lname = new CsdlProperty().setName("lastName").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty dob = new CsdlProperty().setName("dateOfBirth").setType(EdmPrimitiveTypeKind.Date.getFullQualifiedName());
            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                    .setName("Department")
                    .setType(ET_DEPARTMENT_FQN)
                    .setNullable(false)
                    .setPartner("Students");

            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("id");

            List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);

            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_STUDENT_NAME);
            entityType.setProperties(Arrays.asList(id, fname, lname, dob));
            entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setNavigationProperties(navPropList);

            return entityType;
        } else if (entityTypeName.equals(ET_DEPARTMENT_FQN)) {
            CsdlProperty id = new CsdlProperty().setName("id").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // navigation property: one-to-many
            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                    .setName("Students")
                    .setType(ET_STUDENT_FQN)
                    .setCollection(true)
                    .setPartner("Department");

            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("id");

            List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);

            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_DEPARTMENT_NAME);
            entityType.setProperties(Arrays.asList(id, name));
            entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setNavigationProperties(navPropList);

            return entityType;
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if(entityContainer.equals(CONTAINER)) {
            if(entitySetName.contains(ES_STUDENTS_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(ES_STUDENTS_NAME);
                entitySet.setType(ET_STUDENT_FQN);

                return entitySet;
            }
        }
        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        // This method is invoked when displaying the Service Document at e.g. http://localhost:8080/DemoService/DemoService.svc
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }

        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // add EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
        entityTypes.add(getEntityType(ET_STUDENT_FQN));
        schema.setEntityTypes(entityTypes);

        // add EntityContainer
        schema.setEntityContainer(getEntityContainer());

        List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
        schemas.add(schema);

        return schemas;

    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        // create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        entitySets.add(getEntitySet(CONTAINER, ES_STUDENTS_NAME));

        // create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }
}
