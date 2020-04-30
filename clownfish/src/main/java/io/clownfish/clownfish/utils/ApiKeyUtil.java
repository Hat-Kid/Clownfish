/*
 * Copyright 2020 SulzbachR.
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
package io.clownfish.clownfish.utils;

import io.clownfish.clownfish.dbentities.CfWebserviceauth;
import io.clownfish.clownfish.serviceinterface.CfWebserviceauthService;
import io.clownfish.clownfish.servlets.GetAssetPreview;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.faces.bean.ViewScoped;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author SulzbachR
 */
@ViewScoped
@Component
public class ApiKeyUtil {
    @Autowired CfWebserviceauthService cfwebserviceauthService;

    public ApiKeyUtil() {
    }
    
    public boolean checkApiKey(String apikey, String webservicename) {
        try {
            CfWebserviceauth webserviceauth = cfwebserviceauthService.findByHash(apikey);
            if (webserviceauth.getCfWebserviceauthPK().getWebserviceRef().getName().compareToIgnoreCase(webservicename) == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
    }
}
