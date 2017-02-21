package in.sherinstephen.demo.odata;

import in.sherinstephen.demo.service.StudentDAO;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
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
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

/**
 * @author Sherin (I073367)
 * @since 21/2/17
 */
public class StudentEntityCollectionProcessor implements EntityCollectionProcessor {
    private OData odata;
    private ServiceMetadata serviceMetadata;
    private StudentDAO studentDAO;

    public StudentEntityCollectionProcessor(StudentDAO studentDAO) {

        this.studentDAO = studentDAO;
    }

    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

        try {
            // 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
            List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
            EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

            // 2nd: fetch the data from backend for this requested EntitySetName
            // it has to be delivered as EntitySet object
            EntityCollection entityCollection = studentDAO.readEntitySetData(edmEntitySet);

            ODataSerializer dataSerializer = odata.createSerializer(responseFormat);

            EdmEntityType entityType = edmEntitySet.getEntityType();
            ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet).build();

            final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();

            EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextURL).build();
            SerializerResult serializerResult = dataSerializer.entityCollection(serviceMetadata, entityType, entityCollection, opts);
            InputStream serializedContent = serializerResult.getContent();

            response.setContent(serializedContent);
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
