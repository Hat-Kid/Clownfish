/*
 * Copyright 2019 sulzbachr.
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
package io.clownfish.clownfish.converter;

import io.clownfish.clownfish.dbentities.CfSite;
import io.clownfish.clownfish.serviceinterface.CfSiteService;
import java.io.Serializable;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author sulzbachr
 */
@Scope("session")
@Named("siteConverterBean")
@FacesConverter(value = "siteConverter")
@Component
public class SiteConverter implements Converter, Serializable {
    @Autowired transient CfSiteService cfsiteservice;
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value.compareToIgnoreCase("-1") == 0) {
            return null;
        } else {
            try {
                Object o = cfsiteservice.findById(new Long(value));
                return o;
            } catch (NumberFormatException ex) {
                Object o = cfsiteservice.findByName(value);
                return o;
            }
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (null == value) {
            return "-1";
        } else {
            String returnname = ((CfSite) value).getName();
            return  returnname;
        }
    }
    
}
