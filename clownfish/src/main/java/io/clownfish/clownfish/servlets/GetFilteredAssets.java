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
package io.clownfish.clownfish.servlets;

import com.google.gson.Gson;
import static io.clownfish.clownfish.constants.ClownfishConst.AccessTypes.TYPE_ASSET;
import static io.clownfish.clownfish.constants.ClownfishConst.AccessTypes.TYPE_ASSETLIST;
import io.clownfish.clownfish.datamodels.AssetDataOutput;
import io.clownfish.clownfish.datamodels.AuthTokenClasscontent;
import io.clownfish.clownfish.datamodels.AuthTokenListClasscontent;
import io.clownfish.clownfish.dbentities.CfAsset;
import io.clownfish.clownfish.dbentities.CfAssetkeyword;
import io.clownfish.clownfish.dbentities.CfAssetlist;
import io.clownfish.clownfish.dbentities.CfAssetlistcontent;
import io.clownfish.clownfish.dbentities.CfKeyword;
import io.clownfish.clownfish.serviceinterface.CfAssetKeywordService;
import io.clownfish.clownfish.serviceinterface.CfAssetService;
import io.clownfish.clownfish.serviceinterface.CfAssetlistService;
import io.clownfish.clownfish.serviceinterface.CfAssetlistcontentService;
import io.clownfish.clownfish.serviceinterface.CfKeywordService;
import io.clownfish.clownfish.utils.AccessManagerUtil;
import io.clownfish.clownfish.utils.ApiKeyUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.list.SetUniqueList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author sulzbachr
 */
@WebServlet(name = "GetFilteredAssets", urlPatterns = {"/GetFilteredAssets"}, asyncSupported = true)
@Component
public class GetFilteredAssets extends HttpServlet {
    @Autowired transient CfAssetService cfassetService;
    @Autowired transient CfAssetlistService cfassetlistService;
    @Autowired transient CfAssetlistcontentService cfassetlistcontentService;
    @Autowired transient CfAssetKeywordService cfassetkeywordService;
    @Autowired transient CfKeywordService cfkeywordService;
    @Autowired ApiKeyUtil apikeyutil;
    @Autowired transient AuthTokenListClasscontent authtokenlist;
    @Autowired AccessManagerUtil accessmanager;
    
    private static transient @Getter @Setter String assetlibrary;
    private static transient @Getter @Setter String apikey;
    private static transient @Getter @Setter String token;
    private static transient @Getter @Setter String keywords;
    
    final transient Logger LOGGER = LoggerFactory.getLogger(GetFilteredAssets.class);
    
    public GetFilteredAssets() {
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String inst_assetlibrary = null;
        String inst_apikey = "";
        String inst_token = "";
        String inst_keywords = "";
        ArrayList<String> searchkeywords;
        List<CfAssetlistcontent> assetlistcontent = null;
        HashMap<String, String> outputmap;
        List<AssetDataOutput> outputlist = SetUniqueList.decorate(new ArrayList<AssetDataOutput>());
        outputmap = new HashMap<>();
        Map<String, String[]> parameters = request.getParameterMap();
        apikey = "";
        parameters.keySet().stream().filter((paramname) -> (paramname.compareToIgnoreCase("apikey") == 0)).map((paramname) -> parameters.get(paramname)).forEach((values) -> {
            apikey = values[0];
        });
        inst_apikey = apikey;
        token = "";
        parameters.keySet().stream().filter((paramname) -> (paramname.compareToIgnoreCase("token") == 0)).map((paramname) -> parameters.get(paramname)).forEach((values) -> {
            token = values[0];
        });
        inst_token = token;
        if (apikeyutil.checkApiKey(inst_apikey, "RestService")) {
            assetlibrary = "";
            parameters.keySet().stream().filter((paramname) -> (paramname.compareToIgnoreCase("assetlibrary") == 0)).map((paramname) -> parameters.get(paramname)).forEach((values) -> {
                assetlibrary = values[0];
            });
            inst_assetlibrary = assetlibrary;
            assetlistcontent = null;
            if ((null != inst_assetlibrary) && (!inst_assetlibrary.isEmpty())) {
                CfAssetlist assetList = cfassetlistService.findByName(inst_assetlibrary);
                // !ToDo: #95 check AccessManager
                if (accessmanager.checkAccess(token, TYPE_ASSETLIST.getValue(), BigInteger.valueOf(assetList.getId()))) {
                    assetlistcontent = cfassetlistcontentService.findByAssetlistref(assetList.getId());
                }
            }

            searchkeywords = new ArrayList<>();
            keywords = "";
            parameters.keySet().stream().filter((paramname) -> (paramname.compareToIgnoreCase("keywords") == 0)).map((paramname) -> parameters.get(paramname)).forEach((values) -> {
                keywords = values[0];
            });
            inst_keywords = keywords;
            if ((null != inst_keywords) && (!inst_keywords.isEmpty())) {
                String[] keys = inst_keywords.split("\\$");
                for (String key : keys) {
                    searchkeywords.add(key);
                }
            };

            boolean found = true;
            if (null != assetlistcontent) {
                for (CfAssetlistcontent assetcontent : assetlistcontent) {
                    CfAsset asset = cfassetService.findById(assetcontent.getCfAssetlistcontentPK().getAssetref());
                    // Only assets that are for public use and not scrapped
                    if ((!asset.isScrapped()) && (asset.isPublicuse())) {
                        // !ToDo: #95 check AccessManager
                        if (accessmanager.checkAccess(token, TYPE_ASSET.getValue(), BigInteger.valueOf(asset.getId()))) {
                            // Check the keyword filter (at least one keyword must be found (OR))
                            if (!searchkeywords.isEmpty()) {
                                ArrayList contentkeywords = getContentOutputKeywords(asset, true);
                                boolean dummyfound = false;
                                for (String keyword : searchkeywords) {
                                    if (contentkeywords.contains(keyword.toLowerCase())) {
                                        dummyfound = true;
                                    }
                                }
                                found = dummyfound;
                            } else {
                                found = true;
                            }
                        } else {
                            found = false;
                        }
                    }

                    if (found) {
                        AssetDataOutput ao = new AssetDataOutput();
                        ao.setAsset(asset);
                        ao.setKeywords(getContentOutputKeywords(asset, false));
                        outputlist.add(ao);
                    }
                }
            } else {
                // If Assetlist is empty but keywords are set
                if ((!searchkeywords.isEmpty()) && (inst_assetlibrary.isEmpty())) {
                    for (String searchkeyword : searchkeywords) {
                        CfKeyword keyword = cfkeywordService.findByName(searchkeyword);
                        if (null != keyword) {
                            List<CfAssetkeyword> assetkeywords = cfassetkeywordService.findByKeywordRef(keyword.getId());
                            for (CfAssetkeyword assetkeyword : assetkeywords) {
                                CfAsset asset = cfassetService.findById(assetkeyword.getCfAssetkeywordPK().getAssetref());
                                // Only assets that are for public use and not scrapped
                                if ((!asset.isScrapped()) && (asset.isPublicuse())) {
                                    // !ToDo: #95 check AccessManager
                                    if (accessmanager.checkAccess(token, TYPE_ASSET.getValue(), BigInteger.valueOf(asset.getId()))) {
                                        AssetDataOutput ao = new AssetDataOutput();
                                        ao.setAsset(asset);
                                        ao.setKeywords(getContentOutputKeywords(asset, false));
                                        if (!outputlist.contains(ao)) {
                                            outputlist.add(ao);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (inst_assetlibrary.isEmpty()) {
                        List<CfAsset> assetlist;
                        // !ToDo: #95 check AccessManager
                        if ((null != token) && (!token.isEmpty())) {
                            AuthTokenClasscontent classcontent = authtokenlist.getAuthtokens().get(token);
                            if (null != classcontent) {
                                assetlist = cfassetService.findByPublicuseAndScrappedNotInList(true, false, BigInteger.valueOf(classcontent.getUser().getId()));
                            } else {
                                assetlist = cfassetService.findByPublicuseAndScrappedNotInList(true, false, BigInteger.valueOf(0L));
                            }
                        } else {
                            assetlist = cfassetService.findByPublicuseAndScrappedNotInList(true, false, BigInteger.valueOf(0L));
                        }
                        for (CfAsset asset : assetlist) {
                            AssetDataOutput ao = new AssetDataOutput();
                            ao.setAsset(asset);
                            ao.setKeywords(getContentOutputKeywords(asset, false));
                            if (!outputlist.contains(ao)) {
                                outputlist.add(ao);
                            }
                        }
                        found = true;
                    } else {
                        found = false;
                    }
                }
            }

            if (!found) {
                outputmap.put("contentfound", "false");
            }
            Gson gson = new Gson(); 
            String json = gson.toJson(outputlist);
            response.setContentType("application/json;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.print(json);
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage());
            }
        } else {
            PrintWriter out = null;
            try {
                out = response.getWriter();
                out.print("Wrong API KEY");
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage());
            } finally {
                out.close();
            }
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private ArrayList getContentOutputKeywords(CfAsset asset, boolean toLower) {
        ArrayList<String> keywords = new ArrayList<>();
        List<CfAssetkeyword> keywordlist = cfassetkeywordService.findByAssetRef(asset.getId());
        if (!keywordlist.isEmpty()) {
            for (CfAssetkeyword ak : keywordlist) {
                if (toLower) {
                    keywords.add(cfkeywordService.findById(ak.getCfAssetkeywordPK().getKeywordref()).getName().toLowerCase());
                } else {
                    keywords.add(cfkeywordService.findById(ak.getCfAssetkeywordPK().getKeywordref()).getName());
                }
            }
        }
        return keywords;
    }
}
