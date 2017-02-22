package in.sherinstephen.demo.odata;

import in.sherinstephen.demo.service.StudentDAO;
import in.sherinstephen.demo.service.Util;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Sherin (I073367)
 * @since 21/2/17
 */
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {
    private OData odata;
    private ServiceMetadata serviceMetadata;
    private StudentDAO studentDAO;

    public DemoEntityCollectionProcessor(StudentDAO studentDAO) {

        this.studentDAO = studentDAO;
    }

    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

        EdmEntitySet responseEdmEntitySet = null; // we'll need this to build the ContextURL
        EntityCollection responseEntityCollection = null; // we'll need this to set the response body

        try {
            // 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
            List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
            EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            int segmentCount = resourceParts.size();

            UriResource uriResource = resourceParts.get(0); // in our example, the first segment is the EntitySet
            if (!(uriResource instanceof UriResourceEntitySet)) {
                throw new ODataApplicationException("Only EntitySet is supported",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }


            if (segmentCount == 1) { // this is the case for: DemoService/DemoService.svc/Categories
                responseEdmEntitySet = edmEntitySet; // the response body is built from the first (and only) entitySet

                // 2nd: fetch the data from backend for this requested EntitySetName and deliver as EntitySet
                responseEntityCollection = studentDAO.readEntitySetData(edmEntitySet);
            } else if (segmentCount == 2) { // in case of navigation: DemoService.svc/Categories(3)/Students

                UriResource lastSegment = resourceParts.get(1); // in our example we don't support more complex URIs
                if (lastSegment instanceof UriResourceNavigation) {
                    UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
                    EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                    EdmEntityType targetEntityType = edmNavigationProperty.getType();
                    // from Categories(1) to Students
                    responseEdmEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);

                    // 2nd: fetch the data from backend
                    // first fetch the entity where the first segment of the URI points to
                    List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                    // e.g. for Categories(3)/Students we have to find the single entity: Department with ID 3
                    Entity sourceEntity = studentDAO.readEntityData(edmEntitySet, keyPredicates);
                    // error handling for e.g. DemoService.svc/Categories(99)/Students
                    if (sourceEntity == null) {
                        throw new ODataApplicationException("Entity not found.",
                                HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                    }
                    // then fetch the entity collection where the entity navigates to
                    // note: we don't need to check uriResourceNavigation.isCollection(),
                    // because we are the EntityCollectionProcessor
                    responseEntityCollection = studentDAO.getRelatedEntityCollection(sourceEntity, targetEntityType);
                }
            } else { // this would be the case for e.g. Students(1)/Department/Students
                throw new ODataApplicationException("Not supported",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }

            // 3rd: apply system query options
            SelectOption selectOption = uriInfo.getSelectOption();
            ExpandOption expandOption = uriInfo.getExpandOption();

            // handle $expand
            // in our example: http://localhost:8080/DemoService/DemoService.svc/Categories/$expand=Students
            // or http://localhost:8080/DemoService/DemoService.svc/Students?$expand=Department
            if (expandOption != null) {
                // retrieve the EdmNavigationProperty from the expand expression
                // Note: in our example, we have only one NavigationProperty, so we can directly access it
                EdmNavigationProperty edmNavigationProperty = null;
                ExpandItem expandItem = expandOption.getExpandItems().get(0);
                if (expandItem.isStar()) {
                    List<EdmNavigationPropertyBinding> bindings = edmEntitySet.getNavigationPropertyBindings();
                    // we know that there are navigation bindings
                    // however normally in this case a check if navigation bindings exists is done
                    if (!bindings.isEmpty()) {
                        // can in our case only be 'Department' or 'Students', so we can take the first
                        EdmNavigationPropertyBinding binding = bindings.get(0);
                        EdmElement property = edmEntitySet.getEntityType().getProperty(binding.getPath());
                        // we don't need to handle error cases, as it is done in the Olingo library
                        if (property instanceof EdmNavigationProperty) {
                            edmNavigationProperty = (EdmNavigationProperty) property;
                        }
                    }
                } else {
                    // can be 'Department' or 'Students', no path supported
                    UriResource uriResource1 = expandItem.getResourcePath().getUriResourceParts().get(0);
                    // we don't need to handle error cases, as it is done in the Olingo library
                    if (uriResource instanceof UriResourceNavigation) {
                        edmNavigationProperty = ((UriResourceNavigation) uriResource1).getProperty();
                    }
                }

                // can be 'Department' or 'Students', no path supported
                // we don't need to handle error cases, as it is done in the Olingo library
                if (edmNavigationProperty != null) {
                    String navPropName = edmNavigationProperty.getName();
                    EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();

                    List<Entity> entityList = responseEntityCollection.getEntities();
                    for (Entity entity : entityList) {
                        Link link = new Link();
                        link.setTitle(navPropName);
                        link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
                        link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

                        if (edmNavigationProperty.isCollection()) { // in case of Categories/$expand=Students
                            // fetch the data for the $expand (to-many navigation) from backend
                            EntityCollection expandEntityCollection = studentDAO.getRelatedEntityCollection(entity, expandEdmEntityType);
                            link.setInlineEntitySet(expandEntityCollection);
                            link.setHref(expandEntityCollection.getId().toASCIIString());
                        } else { // in case of Student?$expand=Department
                            // fetch the data for the $expand (to-one navigation) from backend
                            // here we get the data for the expand
                            Entity expandEntity = studentDAO.getRelatedEntity(entity, expandEdmEntityType);
                            link.setInlineEntity(expandEntity);
                            link.setHref(expandEntity.getId().toASCIIString());
                        }

                        // set the link - containing the expanded data - to the current entity
                        entity.getNavigationLinks().add(link);
                    }
                }
            }

            // 4th: serialize
            EdmEntityType edmEntityType = edmEntitySet.getEntityType();
            // we need the property names of the $select, in order to build the context URL
            String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, expandOption, selectOption);
            ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).selectList(selectList).build();

            // adding the selectOption to the serializerOpts will actually tell the lib to do the job
            final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
            EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
                    .contextURL(contextUrl)
                    .select(selectOption)
                    .expand(expandOption)
                    .id(id)
                    .build();

            ODataSerializer serializer = odata.createSerializer(responseFormat);
            SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, responseEntityCollection, opts);

            // 5th: configure the response object: set the body, headers and status code
            response.setContent(serializerResult.getContent());
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {

        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }
}
