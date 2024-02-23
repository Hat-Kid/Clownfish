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
package io.clownfish.clownfish.utils;

import io.clownfish.clownfish.dbentities.CfAsset;
import io.clownfish.clownfish.dbentities.CfAssetlist;
import io.clownfish.clownfish.dbentities.CfAssetlistcontent;
import io.clownfish.clownfish.dbentities.CfAttribut;
import io.clownfish.clownfish.dbentities.CfAttributcontent;
import io.clownfish.clownfish.dbentities.CfClasscontent;
import io.clownfish.clownfish.dbentities.CfKeyword;
import io.clownfish.clownfish.dbentities.CfKeywordlist;
import io.clownfish.clownfish.dbentities.CfKeywordlistcontent;
import io.clownfish.clownfish.dbentities.CfLayoutcontent;
import io.clownfish.clownfish.dbentities.CfList;
import io.clownfish.clownfish.dbentities.CfListcontent;
import io.clownfish.clownfish.dbentities.CfSite;
import io.clownfish.clownfish.dbentities.CfSiteassetlist;
import io.clownfish.clownfish.dbentities.CfSitecontent;
import io.clownfish.clownfish.dbentities.CfSitekeywordlist;
import io.clownfish.clownfish.dbentities.CfSitelist;
import io.clownfish.clownfish.serviceinterface.CfAssetService;
import io.clownfish.clownfish.serviceinterface.CfAssetlistService;
import io.clownfish.clownfish.serviceinterface.CfAssetlistcontentService;
import io.clownfish.clownfish.serviceinterface.CfAttributcontentService;
import io.clownfish.clownfish.serviceinterface.CfClassService;
import io.clownfish.clownfish.serviceinterface.CfClasscontentService;
import io.clownfish.clownfish.serviceinterface.CfKeywordService;
import io.clownfish.clownfish.serviceinterface.CfKeywordlistService;
import io.clownfish.clownfish.serviceinterface.CfKeywordlistcontentService;
import io.clownfish.clownfish.serviceinterface.CfLayoutcontentService;
import io.clownfish.clownfish.serviceinterface.CfListService;
import io.clownfish.clownfish.serviceinterface.CfListcontentService;
import io.clownfish.clownfish.serviceinterface.CfSiteService;
import io.clownfish.clownfish.serviceinterface.CfSiteassetlistService;
import io.clownfish.clownfish.serviceinterface.CfSitekeywordlistService;
import io.clownfish.clownfish.serviceinterface.CfSitelistService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author sulzbachr
 */
@Component
public class SiteUtil {
    @Autowired CfSiteService cfsiteService;
    @Autowired CfSitelistService cfsitelistService;
    @Autowired CfClasscontentService cfclasscontentService;
    @Autowired CfClassService cfclassService;
    @Autowired CfListService cflistService;
    @Autowired CfListcontentService cflistcontentService;
    @Autowired CfAttributcontentService cfattributcontentService;
    @Autowired CfAssetService cfassetService;
    @Autowired CfAssetlistService cfassetlistService;
    @Autowired CfSiteassetlistService cfsiteassetlistService;
    @Autowired CfAssetlistcontentService cfassetlistcontentService;
    @Autowired CfKeywordService cfkeywordService;
    @Autowired CfSitekeywordlistService cfsitekeywordlistService;
    @Autowired CfKeywordlistService cfkeywordlistService;
    @Autowired CfKeywordlistcontentService cfkeywordlistcontentService;
    @Autowired CfLayoutcontentService cflayoutcontentService;
    @Autowired ClassUtil classutil;
    @Autowired HibernateUtil hibernateutil;
    @Autowired private PropertyUtil propertyUtil;
    @Autowired transient FolderUtil folderUtil;
    @Autowired DatabaseUtil databaseUtility;
    final transient Logger LOGGER = LoggerFactory.getLogger(SiteUtil.class);
    
    @Value("${hibernate.use:0}") int useHibernate;
    
    public SiteUtil() {
    }
    
    @PostConstruct
    public void init() {
        if (null != classutil) {
            classutil.setSiteutil(this);
            databaseUtility.setSiteutil(this);
        }
    }
    
    public Map getSitelist_list(CfSite cfsite, Map sitecontentmap) {
        List<CfSitelist> sitelist_list = new ArrayList<>();
        sitelist_list.addAll(cfsitelistService.findBySiteref(cfsite.getId()));
        if (!sitelist_list.isEmpty()) {
            for (CfSitelist sitelist : sitelist_list) {
                CfList cflist = cflistService.findById(sitelist.getCfSitelistPK().getListref());
                Map listcontentmap = new LinkedHashMap();

                List<CfListcontent> contentlist = cflistcontentService.findByListref(cflist.getId());
                for (CfListcontent listcontent : contentlist) {
                    CfClasscontent classcontent = cfclasscontentService.findById(listcontent.getCfListcontentPK().getClasscontentref());
                    if (null != classcontent) {
                        cfclassService.findById(classcontent.getClassref().getId());
                        List<CfAttributcontent> attributcontentlist = new ArrayList<>();
                        attributcontentlist.addAll(cfattributcontentService.findByClasscontentref(classcontent));
                        if (0 == useHibernate) {
                            listcontentmap.put(classcontent.getName(), classutil.getattributmap(classcontent));
                        } else {
                            listcontentmap.put(classcontent.getName(), hibernateutil.getContent(classcontent.getClassref().getName(), classcontent.getId(), null, null));
                        }
                    } else {
                        LOGGER.warn("LISTENTRY MISSING: " + listcontent.getCfListcontentPK().getClasscontentref());
                    }
                }
                sitecontentmap.put(cflist.getName(), listcontentmap);
            }
        }
        return sitecontentmap;
    }
    
    public Map getSitelist_list(List<CfList> sitelist, Map sitecontentmap) {
        if (!sitelist.isEmpty()) {
            for (CfList cflist : sitelist) {
                Map listcontentmap = new LinkedHashMap();

                List<CfListcontent> contentlist = cflistcontentService.findByListref(cflist.getId());
                for (CfListcontent listcontent : contentlist) {
                    CfClasscontent classcontent = cfclasscontentService.findById(listcontent.getCfListcontentPK().getClasscontentref());
                    cfclassService.findById(classcontent.getClassref().getId());
                    List<CfAttributcontent> attributcontentlist = new ArrayList<>();
                    attributcontentlist.addAll(cfattributcontentService.findByClasscontentref(classcontent));
                    if (0 == useHibernate) {
                        listcontentmap.put(classcontent.getName(), classutil.getattributmap(classcontent));
                    } else {
                        listcontentmap.put(classcontent.getName(), hibernateutil.getContent(classcontent.getClassref().getName(), classcontent.getId(), null, null));
                    }
                }
                sitecontentmap.put(cflist.getName(), listcontentmap);
            }
        }
        return sitecontentmap;
    }
    
    public Map getSitecontentmapList(List<CfSitecontent> sitecontentlist) {
        Map sitecontentmapdummy = new LinkedHashMap();
        for (CfSitecontent sitecontent : sitecontentlist) {
            CfClasscontent classcontent = cfclasscontentService.findById(sitecontent.getCfSitecontentPK().getClasscontentref());
            fillSitecontentmap(classcontent, sitecontentmapdummy);
        }
        return sitecontentmapdummy;
    }
    
    public Map getClasscontentmapList(List<CfClasscontent> classcontentlist) {
        Map sitecontentmapdummy = new LinkedHashMap();
        for (CfClasscontent classcontent : classcontentlist) {
            fillSitecontentmap(classcontent, sitecontentmapdummy);
        }
        return sitecontentmapdummy;
    }
    
    public Map getSiteAssetlibrary(CfSite cfsite, Map sitecontentmap) {
        List<CfSiteassetlist> siteassetlibrary = new ArrayList<>();
        siteassetlibrary.addAll(cfsiteassetlistService.findBySiteref(cfsite.getId()));
        
        HashMap<String, ArrayList> assetlibraryMap = new HashMap<>();
        for (CfSiteassetlist siteassetlist : siteassetlibrary) {
            CfAssetlist cfassetlist = cfassetlistService.findById(siteassetlist.getCfSiteassetlistPK().getAssetlistref());
            List<CfAssetlistcontent> assetlist = new ArrayList<>();
            assetlist.addAll(cfassetlistcontentService.findByAssetlistref(cfassetlist.getId()));
            ArrayList<CfAsset> dummyassetlist = new ArrayList<>();
            for (CfAssetlistcontent assetcontent : assetlist) {
                CfAsset asset = cfassetService.findById(assetcontent.getCfAssetlistcontentPK().getAssetref());
                if (null != asset) {
                    dummyassetlist.add(asset);
                } else {
                    LOGGER.warn("ASSET NOT FOUND (deleted or on scrapyard): " + assetcontent.getCfAssetlistcontentPK().getAssetref());
                }
            }
            assetlibraryMap.put(cfassetlist.getName(), dummyassetlist);
        }
        sitecontentmap.put("AssetLibrary", assetlibraryMap);
        return sitecontentmap;
    }
    

    public Map getAssetlibrary(List<CfAssetlist> assetlibrary_list, Map sitecontentmap) {
        HashMap<String, ArrayList> assetlibraryMap = new HashMap<>();
        for (CfAssetlist cfassetlist : assetlibrary_list) {
            List<CfAssetlistcontent> assetlist = new ArrayList<>();
            assetlist.addAll(cfassetlistcontentService.findByAssetlistref(cfassetlist.getId()));
            ArrayList<CfAsset> dummyassetlist = new ArrayList<>();
            for (CfAssetlistcontent assetcontent : assetlist) {
                CfAsset asset = cfassetService.findById(assetcontent.getCfAssetlistcontentPK().getAssetref());
                if (null != asset) {
                    dummyassetlist.add(asset);
                } else {
                    LOGGER.warn("ASSET NOT FOUND (deleted or on scrapyard): " + assetcontent.getCfAssetlistcontentPK().getAssetref());
                }
            }
            assetlibraryMap.put(cfassetlist.getName(), dummyassetlist);
        }
        sitecontentmap.put("AssetLibrary", assetlibraryMap);
        return sitecontentmap;
    }
    
    public Map getSiteKeywordlibrary(CfSite cfsite, Map sitecontentmap) {
        List<CfSitekeywordlist> sitekeywordlibrary = new ArrayList<>();
        sitekeywordlibrary.addAll(cfsitekeywordlistService.findBySiteref(cfsite.getId()));
        
        HashMap<String, ArrayList> keywordlibraryMap = new HashMap<>();
        for (CfSitekeywordlist sitekeywordlist : sitekeywordlibrary) {
            CfKeywordlist cfkeywordlist = cfkeywordlistService.findById(sitekeywordlist.getCfSitekeywordlistPK().getKeywordlistref());
            List<CfKeywordlistcontent> keywordlist = new ArrayList<>();
            keywordlist.addAll(cfkeywordlistcontentService.findByKeywordlistref(cfkeywordlist.getId()));
            ArrayList<CfKeyword> dummykeywordlist = new ArrayList<>();
            for (CfKeywordlistcontent keywordcontent : keywordlist) {
                CfKeyword keyword = cfkeywordService.findById(keywordcontent.getCfKeywordlistcontentPK().getKeywordref());
                dummykeywordlist.add(keyword);
            }
            keywordlibraryMap.put(cfkeywordlist.getName(), dummykeywordlist);
        }
        sitecontentmap.put("KeywordLibrary", keywordlibraryMap);
        return sitecontentmap;
    }
    
    public Map getSiteKeywordlibrary(List<CfKeywordlist> keywordlibrary_list, Map sitecontentmap) {
        HashMap<String, ArrayList> keywordlibraryMap = new HashMap<>();
        for (CfKeywordlist cfkeywordlist : keywordlibrary_list) {
            List<CfKeywordlistcontent> keywordlist = new ArrayList<>();
            keywordlist.addAll(cfkeywordlistcontentService.findByKeywordlistref(cfkeywordlist.getId()));
            ArrayList<CfKeyword> dummykeywordlist = new ArrayList<>();
            for (CfKeywordlistcontent keywordcontent : keywordlist) {
                CfKeyword keyword = cfkeywordService.findById(keywordcontent.getCfKeywordlistcontentPK().getKeywordref());
                dummykeywordlist.add(keyword);
            }
            keywordlibraryMap.put(cfkeywordlist.getName(), dummykeywordlist);
        }
        sitecontentmap.put("KeywordLibrary", keywordlibraryMap);
        return sitecontentmap;
    }
    
    private String getAttributValue(List<CfAttributcontent> attributcontentlist, String key) {
        for (CfAttributcontent ac : attributcontentlist) {
            if ((0 == ac.getAttributref().getName().compareToIgnoreCase(key)) && (!ac.getAttributref().getIdentity()) && (isEncryptable(ac.getAttributref()))) {
                return EncryptUtil.decrypt(ac.getContentString(), propertyUtil.getPropertyValue("aes_key")) ;
            }
        }
        return null;
    }
    
    private boolean isEncryptable(CfAttribut attribut) {
        switch (attribut.getAttributetype().getName()) {
            case "string":
            case "text":
            case "htmltext":
            case "markdown":
                return true;
            default:
                return false;
        }
    }
    
    public String generateShorturl() {
        String shorturl = "";
        boolean notfound = true;
        while (notfound) {
            for (int i = 1; i <= 5; i++) {
                shorturl += getRandomChar();
            }
            try {
                cfsiteService.findByShorturl(shorturl);
                notfound = true;
            } catch (Exception ex) {
                notfound = false;
            }
        }
        return shorturl;
    }
    
    private char getRandomChar() {
        Random rand = new Random();
        int i = rand.nextInt(62);
        int j = 0;
        if (i < 10) {
                j = i + 48;
        } else if (i > 9 && i <= 35) {
                j = i + 55;
        } else {
                j = i + 61;
        }
        return (char) j;
    }
    
    public void publishSite(CfSite site, boolean feedback) {
        if (null != folderUtil.getStatic_folder()) {
            File file = new File(folderUtil.getStatic_folder() + File.separator + site.getName());
            try {
                Files.deleteIfExists(file.toPath());
                if (feedback) {
                    FacesMessage message = new FacesMessage("Deleted static site for " + site.getName());
                    FacesContext.getCurrentInstance().addMessage(null, message);
                }
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage());
            }
        }
        
        List<CfLayoutcontent> layoutcontentlist = cflayoutcontentService.findBySiteref(site.getId());
        for (CfLayoutcontent layoutcontent : layoutcontentlist) {
            layoutcontent.setContentref(layoutcontent.getPreview_contentref());
            cflayoutcontentService.edit(layoutcontent);
        }
        if (feedback) {
            FacesMessage message = new FacesMessage("Published " + site.getName());
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }
    
    private void fillSitecontentmap(CfClasscontent classcontent, Map sitecontentmapdummy) {
        if (null != classcontent) {
            List<CfAttributcontent> attributcontentlist = new ArrayList<>();
            attributcontentlist.addAll(cfattributcontentService.findByClasscontentref(classcontent));
            if (0 == useHibernate) {
                sitecontentmapdummy.put(classcontent.getName(), classutil.getattributmap(classcontent));
            } else {
                sitecontentmapdummy.put(classcontent.getName(), hibernateutil.getContent(classcontent.getClassref().getName(), classcontent.getId(), null, null));
            }

            if (classcontent.getClassref().isEncrypted()) {
                HashMap contentmap = (HashMap) sitecontentmapdummy.get(classcontent.getName());
                for (Object key : contentmap.keySet()) {
                    if (null != getAttributValue(attributcontentlist, key.toString())) {
                        HashMap am = (HashMap) sitecontentmapdummy.get(classcontent.getName());
                        am.put(key, getAttributValue(attributcontentlist, key.toString()));
                    }
                }
            }
            // Add entries for 1:n and n:m relations to sitecontentmap
            for (CfAttributcontent attributcontent : attributcontentlist) {
                if (null != attributcontent.getAttributref().getRelationref()) {
                    if (1 == attributcontent.getAttributref().getRelationtype()) {
                        CfClasscontent refcontent = cfclasscontentService.findById(attributcontent.getContentInteger().longValue());

                        if (0 == useHibernate) {
                            HashMap entry2 = (HashMap) sitecontentmapdummy.get(classcontent.getName());
                            entry2.put(attributcontent.getAttributref().getName(), classutil.getattributmap(refcontent));
                        } else {
                            HashMap entry2 = (HashMap) sitecontentmapdummy.get(classcontent.getName());
                            entry2.put(attributcontent.getAttributref().getName(), hibernateutil.getContent(refcontent.getClassref().getName(), refcontent.getId(), null, null));
                        }
                    } else {
                        List<CfListcontent> attrlist = cflistcontentService.findByListref(attributcontent.getClasscontentlistref().getId());
                        ArrayList<Map> contentlist = new ArrayList<>();
                        for (CfListcontent listcontent : attrlist) {
                            if (0 == useHibernate) {
                                CfClasscontent cc = cfclasscontentService.findById(listcontent.getCfListcontentPK().getClasscontentref());
                                contentlist.add(classutil.getattributmap(cc));
                            } else {
                                CfClasscontent cc = cfclasscontentService.findById(listcontent.getCfListcontentPK().getClasscontentref());
                                contentlist.add(hibernateutil.getContent(cc.getClassref().getName(), cc.getId(), null, null));
                            }
                        }
                        HashMap entry2 = (HashMap) sitecontentmapdummy.get(classcontent.getName());
                        entry2.put(attributcontent.getAttributref().getName(), contentlist);
                    }
                }
            }
        } else {
            LOGGER.warn("CLASSCONTENT NOT FOUND (deleted or on scrapyard): " + classcontent.getId());
        }
    }
    
    public String getUniqueName(String name) {
        int i = 1;
        boolean found = false;
        do {
            try {
                cfsiteService.findByName(name+"_"+i);
                i++;
            } catch(Exception ex) {
                found = true;
            }
        } while (!found);
        return name+"_"+i;
    }
}
