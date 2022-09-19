/*
 * Copyright 2022 raine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.clownfish.clownfish.odata;

import io.clownfish.clownfish.dbentities.CfAttribut;
import io.clownfish.clownfish.dbentities.CfClass;
import io.clownfish.clownfish.serviceinterface.CfAttributService;
import io.clownfish.clownfish.serviceinterface.CfClassService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author raine
 */
@Component
public class GenericEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "OData.Generic";

    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types Names
    //public static final String ET_PRODUCT_NAME = "Product";
    //public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

    // Entity Set Names
    public static final String ES_PRODUCTS_NAME = "Products";
    
    @Autowired private CfClassService cfclassservice;
    @Autowired private CfAttributService cfattributservice;

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        // create Schema
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // add EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        for (CfClass clazz : cfclassservice.findAll()) {
            entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, clazz.getName())));
        }
        schema.setEntityTypes(entityTypes);

        // add EntityContainer
        schema.setEntityContainer(getEntityContainer());

        // finally
        List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
        schemas.add(schema);

        return schemas;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        // create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        for (CfClass clazz : cfclassservice.findAll()) {
            entitySets.add(getEntitySet(CONTAINER, clazz.getName()));
        }
        //entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));

        // create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        System.out.println(entityTypeName.getFullQualifiedNameAsString());
        CfClass classref = cfclassservice.findByName(entityTypeName.getName());
        CsdlPropertyRef propertyRef = new CsdlPropertyRef();
        List propsList = new ArrayList();
        for (CfAttribut attribut : cfattributservice.findByClassref(classref)) {
            CsdlProperty prop = new CsdlProperty().setName(attribut.getName()).setType(getODataType(attribut.getAttributetype().getName()));
            propsList.add(prop);
            if (attribut.getIdentity()) {
                propertyRef.setName(attribut.getName());
            }
        }
        CsdlEntityType entityType = new CsdlEntityType();
        entityType.setName(entityTypeName.getName());
        entityType.setProperties(propsList);
        entityType.setKey(Collections.singletonList(propertyRef));

        return entityType;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if (entityContainer.equals(CONTAINER)) {
            CsdlEntitySet entitySet = new CsdlEntitySet();
            entitySet.setName(entitySetName + "Set");
            entitySet.setType(new FullQualifiedName(NAMESPACE, entitySetName));

            return entitySet;
        }

        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }
        return null;
    }
    
    private FullQualifiedName getODataType(String attributetype) {
        switch (attributetype) {
            case "string":
            case "hashstring":
            case "htmltext":
            case "markdown":
            case "datetime":
            case "text":
                return EdmPrimitiveTypeKind.String.getFullQualifiedName();
            case "integer":
            case "media":
            case "classref":
            case "assetref":
                return EdmPrimitiveTypeKind.Int32.getFullQualifiedName();
            case "real":
                return EdmPrimitiveTypeKind.Double.getFullQualifiedName();
            case "boolean":
                return EdmPrimitiveTypeKind.Boolean.getFullQualifiedName();
            default:
                return null;
        }
    }
}
