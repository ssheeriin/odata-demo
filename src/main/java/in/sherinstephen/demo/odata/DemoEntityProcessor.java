package in.sherinstephen.demo.odata;

import in.sherinstephen.demo.service.StudentDAO;
import in.sherinstephen.demo.service.Util;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * @author Sherin (I073367)
 * @since 21/2/17
 */
public class DemoEntityProcessor implements EntityProcessor {

    private StudentDAO studentDAO;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public DemoEntityProcessor(StudentDAO studentDAO) {

        this.studentDAO = studentDAO;
    }

    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    /**
     * pattern of http://localhost:8080/DemoService/DemoService.svc/Student(3)
     *
     * @param request
     * @param response
     * @param uriInfo
     * @param responseFormat
     * @throws ODataApplicationException
     * @throws ODataLibraryException
     */
    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

        EdmEntityType responseEdmEntityType = null; // we'll need this to build the ContextURL
        Entity responseEntity = null; // required for serialization of the response body
        EdmEntitySet responseEdmEntitySet = null; // we need this for building the contextUrl


        try {
            // 1. retrieve the Entity Type
            List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);

            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            int segmentCount = resourceParts.size();


            EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();


            // 2. retrieve the data from backend
            // Analyze the URI segments
            if (segmentCount == 1) { // no navigation
                responseEdmEntityType = startEdmEntitySet.getEntityType();
                responseEdmEntitySet = startEdmEntitySet; // since we have only one segment

                // 2. step: retrieve the data from backend
                List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                responseEntity = studentDAO.readEntityData(startEdmEntitySet, keyPredicates);
                for (String navProp : responseEdmEntityType.getNavigationPropertyNames()) {
                    EdmNavigationProperty edmNavigationProperty = responseEdmEntityType.getNavigationProperty(navProp);
                    processNavigationLinks(responseEntity, edmNavigationProperty, false);
                }

            } else if (segmentCount == 2) { // navigation
                UriResource navSegment = resourceParts.get(1); // in our example we don't support more complex URIs
                if (navSegment instanceof UriResourceNavigation) {
                    UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) navSegment;
                    EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                    responseEdmEntityType = edmNavigationProperty.getType();
                    // contextURL displays the last segment
                    responseEdmEntitySet = Util.getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);

                    // 2nd: fetch the data from backend.
                    // e.g. for the URI: Students(1)/Department we have to find the correct Department entity
                    List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                    // e.g. for Students(1)/Department we have to find first the Students(1)
                    Entity sourceEntity = studentDAO.readEntityData(startEdmEntitySet, keyPredicates);

                    // now we have to check if the navigation is
                    // a) to-one: e.g. Students(1)/Department
                    // b) to-many with key: e.g. Departments(3)/Students(5)
                    // the key for nav is used in this case: Departments(3)/Students(5)
                    List<UriParameter> navKeyPredicates = uriResourceNavigation.getKeyPredicates();

                    if (navKeyPredicates.isEmpty()) { // e.g. DemoService.svc/Students(1)/Department
                        responseEntity = studentDAO.getRelatedEntity(sourceEntity, responseEdmEntityType);
                    } else { // e.g. DemoService.svc/Departments(3)/Students(5)
                        responseEntity = studentDAO.getRelatedEntity(sourceEntity, responseEdmEntityType, navKeyPredicates);
                    }
                }
            } else {
                // this would be the case for e.g. Students(1)/Department/Students(1)/Department
                throw new ODataApplicationException("Not supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }

            if (responseEntity == null) {
                // this is the case for e.g. DemoService.svc/Departments(4) or DemoService.svc/Departments(3)/Students(999)
                throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
            }


            // apply system query options

            // handle $select
            SelectOption selectOption = uriInfo.getSelectOption();
            // in our example, we don't have performance issues, so we can rely upon the handling in the Olingo lib
            // nothing else to be done

            // handle $expand
            ExpandOption expandOption = uriInfo.getExpandOption();
            // in our example: http://localhost:8080/DemoService/DemoService.svc/Departments(1)/$expand=Students
            // or http://localhost:8080/DemoService/DemoService.svc/Students(1)?$expand=Department
            if (expandOption != null) {
                // retrieve the EdmNavigationProperty from the expand expression
                // Note: in our example, we have only one NavigationProperty, so we can directly access it
                EdmNavigationProperty edmNavigationProperty = null;
                ExpandItem expandItem = expandOption.getExpandItems().get(0);
                if (expandItem.isStar()) {
                    List<EdmNavigationPropertyBinding> bindings = responseEdmEntitySet.getNavigationPropertyBindings();
                    // we know that there are navigation bindings
                    // however normally in this case a check if navigation bindings exists is done
                    if (!bindings.isEmpty()) {
                        // can in our case only be 'Department' or 'Students', so we can take the first
                        EdmNavigationPropertyBinding binding = bindings.get(0);
                        EdmElement property = responseEdmEntitySet.getEntityType().getProperty(binding.getPath());
                        // we don't need to handle error cases, as it is done in the Olingo library
                        if (property instanceof EdmNavigationProperty) {
                            edmNavigationProperty = (EdmNavigationProperty) property;
                        }
                    }
                } else {
                    // can be 'Department' or 'Students', no path supported
                    UriResource uriResource = expandItem.getResourcePath().getUriResourceParts().get(0);
                    // we don't need to handle error cases, as it is done in the Olingo library
                    if (uriResource instanceof UriResourceNavigation) {
                        edmNavigationProperty = ((UriResourceNavigation) uriResource).getProperty();
                    }
                }
                processNavigationLinks(responseEntity, edmNavigationProperty, true);


            }


            // 4. serialize
            EdmEntityType edmEntityType = responseEdmEntitySet.getEntityType();
            // we need the property names of the $select, in order to build the context URL
            String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, expandOption, selectOption);
            ContextURL contextUrl = ContextURL.with().entitySet(responseEdmEntitySet)
                    .selectList(selectList)
                    .suffix(ContextURL.Suffix.ENTITY).build();

            // make sure that $expand and $select are considered by the serializer
            // adding the selectOption to the serializerOpts will actually tell the lib to do the job
            EntitySerializerOptions opts = EntitySerializerOptions.with()
                    .contextURL(contextUrl)
                    .select(selectOption)
                    .expand(expandOption)
                    .build();

            ODataSerializer serializer = this.odata.createSerializer(responseFormat);
            SerializerResult serializerResult = serializer.entity(serviceMetadata, responseEdmEntityType , responseEntity, opts);

            // 5. configure the response object
            response.setContent(serializerResult.getContent());
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processNavigationLinks(Entity responseEntity, EdmNavigationProperty edmNavigationProperty, boolean expand) {
        // can be 'Department' or 'Students', no path supported
        // we don't need to handle error cases, as it is done in the Olingo library
        if (edmNavigationProperty != null) {
            EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();
            String navPropName = edmNavigationProperty.getName();

            // build the inline data
            Link link = new Link();
            link.setTitle(navPropName);
            link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
            link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

            if (edmNavigationProperty.isCollection()) { // in case of Departments(1)/$expand=Students
                // fetch the data for the $expand (to-many navigation) from backend
                // here we get the data for the expand
                EntityCollection expandEntityCollection = studentDAO.getRelatedEntityCollection(responseEntity, expandEdmEntityType);
                if (expand) {
                    link.setInlineEntitySet(expandEntityCollection);
                    link.setBindingLink("test link");
                }
                link.setHref(expandEntityCollection.getId().toASCIIString());
            } else {  // in case of Students(1)?$expand=Department
                // fetch the data for the $expand (to-one navigation) from backend
                // here we get the data for the expand
                Entity expandEntity = studentDAO.getRelatedEntity(responseEntity, expandEdmEntityType);
                link.setInlineEntity(expandEntity);
                link.setHref(expandEntity.getId().toASCIIString());
            }

            // set the link - containing the expanded data - to the current entity
            responseEntity.getNavigationLinks().add(link);
            responseEntity.getNavigationBindings().add(link);
        }
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity type from the URI
        EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. create the data in backend
        // 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();

        // 2.2 do the creation in backend, which returns the newly created entity
        Entity createdEntity = studentDAO.createEntityData(edmEntitySet, requestEntity);


        // 3. serialize the response (we have to return the created entity)
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
        // expand and select currently not supported
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        SerializerResult serializedResponse = serializer.entity(serviceMetadata, edmEntityType, createdEntity, options);

        //4. configure the response object
        response.setContent(serializedResponse.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity set which belongs to the requested entity
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // Note: only in our example we can assume that the first segment is the EntitySet
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. update the data in backend

        // 2.1. retrieve the payload from the PUT request for the entity to be updated
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();

        // 2.2 do the modification in backend
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

        // Note that this updateEntity()-method is invoked for both PUT or PATCH operations
        HttpMethod httpMethod = request.getMethod();
        studentDAO.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity set which belongs to the requested entity
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // Note: only in our example we can assume that the first segment is the EntitySet
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2. delete the data in backend
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        studentDAO.deleteEntityData(edmEntitySet, keyPredicates);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());

    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }
}
