package in.sherinstephen.demo.service;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import java.util.List;

/**
 * @author Sherin (I073367)
 * @since 21/2/17
 */
public interface StudentDAO {
    EntityCollection readEntitySetData(EdmEntitySet edmEntitySet)throws ODataApplicationException;

    Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException;

    Entity createEntityData(EdmEntitySet edmEntitySet, Entity requestEntity);

    void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates, Entity requestEntity, HttpMethod httpMethod) throws ODataApplicationException;

    void deleteEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) throws ODataApplicationException;

    EntityCollection getRelatedEntityCollection(Entity entity, EdmEntityType expandEdmEntityType);

    Entity getRelatedEntity(Entity entity, EdmEntityType expandEdmEntityType);
}
