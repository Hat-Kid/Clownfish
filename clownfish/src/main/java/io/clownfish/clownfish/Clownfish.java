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
package io.clownfish.clownfish;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import de.destrukt.sapconnection.SAPConnection;
import io.clownfish.clownfish.templatebeans.DatabaseTemplateBean;
import io.clownfish.clownfish.beans.JsonFormParameter;
import io.clownfish.clownfish.beans.PropertyList;
import static io.clownfish.clownfish.beans.SiteTreeBean.SAPCONNECTION;
import io.clownfish.clownfish.constants.ClownfishConst;
import static io.clownfish.clownfish.constants.ClownfishConst.ViewModus.DEVELOPMENT;
import static io.clownfish.clownfish.constants.ClownfishConst.ViewModus.STAGING;
import io.clownfish.clownfish.datamodels.ClownfishResponse;
import io.clownfish.clownfish.dbentities.CfJavascript;
import io.clownfish.clownfish.dbentities.CfSite;
import io.clownfish.clownfish.dbentities.CfSitecontent;
import io.clownfish.clownfish.dbentities.CfSitedatasource;
import io.clownfish.clownfish.dbentities.CfSitesaprfc;
import io.clownfish.clownfish.dbentities.CfStylesheet;
import io.clownfish.clownfish.dbentities.CfTemplate;
import io.clownfish.clownfish.interceptor.GzipSwitch;
import io.clownfish.clownfish.jdbc.DatatableDeleteProperties;
import io.clownfish.clownfish.jdbc.DatatableNewProperties;
import io.clownfish.clownfish.jdbc.DatatableProperties;
import io.clownfish.clownfish.jdbc.DatatableUpdateProperties;
import io.clownfish.clownfish.mail.EmailProperties;
import io.clownfish.clownfish.sap.RPY_TABLE_READ;
import io.clownfish.clownfish.serviceimpl.CfTemplateLoaderImpl;
import io.clownfish.clownfish.serviceinterface.CfDatasourceService;
import io.clownfish.clownfish.serviceinterface.CfJavascriptService;
import io.clownfish.clownfish.serviceinterface.CfJavascriptversionService;
import io.clownfish.clownfish.serviceinterface.CfSiteService;
import io.clownfish.clownfish.serviceinterface.CfSitecontentService;
import io.clownfish.clownfish.serviceinterface.CfSitedatasourceService;
import io.clownfish.clownfish.serviceinterface.CfSitesaprfcService;
import io.clownfish.clownfish.serviceinterface.CfStylesheetService;
import io.clownfish.clownfish.serviceinterface.CfStylesheetversionService;
import io.clownfish.clownfish.serviceinterface.CfTemplateService;
import io.clownfish.clownfish.serviceinterface.CfTemplateversionService;
import io.clownfish.clownfish.templatebeans.EmailTemplateBean;
import io.clownfish.clownfish.templatebeans.SAPTemplateBean;
import io.clownfish.clownfish.utils.ClownfishUtil;
import io.clownfish.clownfish.utils.DatabaseUtil;
import io.clownfish.clownfish.utils.MailUtil;
import io.clownfish.clownfish.utils.SiteUtil;
import io.clownfish.clownfish.utils.TemplateUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sulzbachr
 */
@RestController
@EnableAutoConfiguration(exclude = HibernateJpaAutoConfiguration.class)
@Component
public class Clownfish {
    @Autowired CfSiteService cfsiteService;
    @Autowired CfSitecontentService cfsitecontentService;
    @Autowired CfSitedatasourceService cfsitedatasourceService;
    @Autowired CfTemplateService cftemplateService;
    @Autowired CfTemplateversionService cftemplateversionService;
    @Autowired CfStylesheetService cfstylesheetService;
    @Autowired CfStylesheetversionService cfstylesheetversionService;
    @Autowired CfJavascriptService cfjavascriptService;
    @Autowired CfJavascriptversionService cfjavascriptversionService;
    @Autowired CfSitesaprfcService cfsitesaprfcService;
    @Autowired TemplateUtil templateUtil;
    @Autowired PropertyList propertylist;
    @Autowired CfTemplateLoaderImpl freemarkerTemplateloader;
    @Autowired SiteUtil siteutil;
    @Autowired DatabaseUtil databaseUtil;
    @Autowired CfDatasourceService cfdatasourceService;
    @Autowired DatabaseTemplateBean databasebean;
    @Autowired EmailTemplateBean emailbean;
    @Autowired SAPTemplateBean sapbean;
    
    @Context
    protected HttpServletResponse response;
    @Context 
    protected HttpServletRequest request;
    
    private GzipSwitch gzipswitch;
    private freemarker.template.Configuration freemarkerCfg;
    //private RFC_GET_FUNCTION_INTERFACE rfc_get_function_interface = null;
    private RPY_TABLE_READ rpytableread = null;
    private static SAPConnection sapc = null;
    private boolean sapSupport = false;
    private @Getter @Setter Map<String, String> propertymap = null;
    private HttpSession userSession;
    private ClownfishConst.ViewModus modus = STAGING;
    private ClownfishUtil clownfishutil;
    private @Getter @Setter String characterEncoding;
    private @Getter @Setter String contentType;
    private @Getter @Setter Locale locale;
    private @Getter @Setter Map sitecontentmap;
    private @Getter @Setter List<CfSitedatasource> sitedatasourcelist;
    
    final Logger logger = LoggerFactory.getLogger(Clownfish.class);
    
    @RequestMapping("/")
    String home() {
        return "Welcome to Clownfish Content Management System";
    }
    
    @PostConstruct
    public void init() {
        logger.info("INIT");
        // Set default values
        modus = STAGING;    // 1 = Staging mode (fetch sourcecode from commited repository) <= default
                            // 0 = Development mode (fetch sourcecode from database)
        characterEncoding = "UTF-8";
        contentType = "text/html";
        locale = new Locale("de");

        // read all System Properties of the property table
        propertymap = propertylist.fillPropertyMap();
        clownfishutil = new ClownfishUtil();
        String sapSupportProp = propertymap.get("sap.support");
        if (sapSupportProp.compareToIgnoreCase("true") == 0) {
            sapSupport = true;
        }
        if (sapSupport) {
            //Class<?> clazz = Class.forName("KNSAPTools.SAPConnection");
            //Object sapcinstance = clazz.newInstance();
            sapc = new SAPConnection(SAPCONNECTION, "Clownfish1");
            //rfc_get_function_interface = new RFC_GET_FUNCTION_INTERFACE(sapc);
            rpytableread = new RPY_TABLE_READ(sapc);
        }
        // Override default values with system properties
        String systemContentType = propertymap.get("response.contenttype");
        String systemCharacterEncoding = propertymap.get("response.characterencoding");
        String systemLocale = propertymap.get("response.locale");
        if (!systemCharacterEncoding.isEmpty()) {
            characterEncoding = systemCharacterEncoding;
        }
        if (!systemContentType.isEmpty()) {
            contentType = systemContentType;
        }
        if (!systemLocale.isEmpty()) {
            locale = new Locale(systemLocale);
        }
        this.gzipswitch = new GzipSwitch();
    }
    
    public Clownfish() {
    }

    @GetMapping(path = "/{name}")
    public void universalGet(@PathVariable("name") String name, @Context HttpServletRequest request, @Context HttpServletResponse response) {
        //logger.info("START universal GET: " + name);
        try {
            userSession = request.getSession();
            this.request = request;
            this.response = response;
            Map<String, String[]> querymap = request.getParameterMap();
            
            ArrayList queryParams = new ArrayList();
            for (Object key : querymap.keySet()) {
                JsonFormParameter jfp = new JsonFormParameter();
                jfp.setName((String) key);
                String[] values = querymap.get(key);
                jfp.setValue(values[0]);
                queryParams.add(jfp);
            }
            
            Future<ClownfishResponse> cfResponse = makeResponse(name, queryParams);
            if (cfResponse.get().getErrorcode() == 0) {
                response.setContentType(this.response.getContentType());
                response.setCharacterEncoding(this.response.getCharacterEncoding());
                PrintWriter outwriter = response.getWriter();
                outwriter.println(cfResponse.get().getOutput());
            } else {
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                PrintWriter outwriter = response.getWriter();
                outwriter.println(cfResponse.get().getOutput());
            }
        } catch (IOException | InterruptedException | ExecutionException ex) {
            logger.error(ex.getMessage());
        }
        //logger.info("END universal GET: " + name);
    }
    
    @PostMapping("/{name}")
    public void universalPost(@PathVariable("name") String name, @Context HttpServletRequest request, @Context HttpServletResponse response) {
        //logger.info("START universal POST:" + name);
        try {
            userSession = request.getSession();
            this.request = request;
            this.response = response;
            String content = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            
            Gson gson = new Gson(); 
            List<JsonFormParameter> map;
            map = (List<JsonFormParameter>) gson.fromJson(content, new TypeToken<List<JsonFormParameter>>() {}.getType());
            
            Future<ClownfishResponse> cfResponse = makeResponse(name, map);
            if (cfResponse.get().getErrorcode() == 0) {
                response.setContentType(this.response.getContentType());
                response.setCharacterEncoding(this.response.getCharacterEncoding());
                PrintWriter outwriter = response.getWriter();
                outwriter.println(cfResponse.get().getOutput());
            } else {
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                PrintWriter outwriter = response.getWriter();
                outwriter.println(cfResponse.get().getOutput());
            }
        } catch (IOException | InterruptedException | ExecutionException ex) {
            logger.error(ex.getMessage());
        }
        //logger.info("END universal POST:" + name);
    }
    
    @Async
    public Future<ClownfishResponse> makeResponse(String name, List<JsonFormParameter> postmap) {
        /*
        logger.info("START makeResponse: " + name);
        for (JsonFormParameter param : postmap) {
            logger.info("PARAM: " + param.getName() + " VALUE:" + param.getValue());
        }
        */
        ClownfishResponse cfresponse = new ClownfishResponse();
        try {
            // Freemarker Template
            freemarker.template.Template fmTemplate = null;
            Map fmRoot = null;
            
            // Velocity Template
            org.apache.velocity.VelocityContext velContext = null;
            org.apache.velocity.Template velTemplate = null;

            // fetch parameter list
            Map parametermap = clownfishutil.getParametermap(postmap);
            if (parametermap.containsKey("modus")) {    // check mode for display (stageing or dev)
                if (parametermap.get("modus").toString().compareToIgnoreCase("dev") == 0) {
                    modus = DEVELOPMENT;
                }
            }
            
            // fetch site by name or aliasname
            CfSite cfsite = null;
            try {
                cfsite = cfsiteService.findByName(name);
            } catch (Exception ex) {
                cfsite = cfsiteService.findByAliaspath(name);
            }
            if ((cfsite.getContenttype() != null)) {
                if (!cfsite.getContenttype().isEmpty()) this.response.setContentType(cfsite.getContenttype());
            }
            if ((cfsite.getCharacterencoding() != null)) {
                if (!cfsite.getCharacterencoding().isEmpty()) this.response.setCharacterEncoding(cfsite.getCharacterencoding());
            }
            if ((cfsite.getLocale() != null)) {
                if (!cfsite.getLocale().isEmpty()) this.response.setLocale(new Locale(cfsite.getLocale()));
            }
            
            try {
                CfTemplate cftemplate = cftemplateService.findById(cfsite.getTemplateref().longValue());
                // fetch the dependend template 
                if (0 == cftemplate.getScriptlanguage()) {  // Freemarker Template
                    fmRoot = new LinkedHashMap();
                    freemarkerTemplateloader.setModus(modus);
                    
                    freemarkerCfg = new freemarker.template.Configuration();
                    freemarkerCfg.setDefaultEncoding("UTF-8");
                    freemarkerCfg.setTemplateLoader(freemarkerTemplateloader);
                    freemarkerCfg.setLocalizedLookup(false);
                    freemarkerCfg.setLocale(Locale.GERMANY);
                    
                    fmTemplate = freemarkerCfg.getTemplate(cftemplate.getName());
                } else {                                    // Velocity Template
                    velContext = new org.apache.velocity.VelocityContext();
                    
                    velTemplate = new org.apache.velocity.Template();
                    org.apache.velocity.runtime.RuntimeServices runtimeServices = org.apache.velocity.runtime.RuntimeSingleton.getRuntimeServices();
                    String templateContent;
                    if (DEVELOPMENT == modus) {
                        templateContent = cftemplate.getContent();
                    } else {
                        long currentTemplateVersion;
                        try {
                            currentTemplateVersion = cftemplateversionService.findMaxVersion(cftemplate.getId());
                        } catch (NullPointerException ex) {
                            currentTemplateVersion = 0;
                        }
                        templateContent = templateUtil.getVersion(cftemplate.getId(), currentTemplateVersion);
                    }
                    templateContent = templateUtil.fetchIncludes(templateContent, modus);
                    StringReader reader = new StringReader(templateContent);
                    velTemplate.setRuntimeServices(runtimeServices);
                    velTemplate.setData(runtimeServices.parse(reader, cftemplate.getName()));
                    velTemplate.initDocument();
                }
                
                // fetch the dependend styleshett, if available
                String cfstylesheet = "";
                if (cfsite.getStylesheetref() != null) {
                    cfstylesheet = ((CfStylesheet) cfstylesheetService.findById(cfsite.getStylesheetref().longValue())).getContent();
                }

                // fetch the dependend javascript, if available
                String cfjavascript = "";
                if (cfsite.getJavascriptref()!= null) {
                    cfjavascript = ((CfJavascript) cfjavascriptService.findById(cfsite.getJavascriptref().longValue())).getContent();
                }
                
                // fetch the dependend content
                List<CfSitecontent> sitecontentlist = new ArrayList<>();
                sitecontentlist.addAll(cfsitecontentService.findBySiteref(cfsite.getId()));
                sitecontentmap = siteutil.getSitecontentmapList(sitecontentlist);
                
                // fetch the dependend datalists, if available
                siteutil.getSitelist_list(cfsite, sitecontentmap);

                // manage parameters 
                HashMap<String, DatatableProperties> datatableproperties = clownfishutil.getDatatableproperties(postmap);
                EmailProperties emailproperties = clownfishutil.getEmailproperties(postmap);
                HashMap<String, DatatableNewProperties> datatablenewproperties = clownfishutil.getDatatablenewproperties(postmap);
                HashMap<String, DatatableDeleteProperties> datatabledeleteproperties = clownfishutil.getDatatabledeleteproperties(postmap);
                HashMap<String, DatatableUpdateProperties> datatableupdateproperties = clownfishutil.getDatatableupdateproperties(postmap);
                manageSessionVariables(postmap);
                writeSessionVariables(parametermap);
                
                // fetch the dependend datasources
                sitedatasourcelist = new ArrayList<>();
                sitedatasourcelist.addAll(cfsitedatasourceService.findBySiteref(cfsite.getId()));

                HashMap<String, HashMap> dbexport = databaseUtil.getDbexport(sitedatasourcelist, datatableproperties, datatablenewproperties, datatabledeleteproperties, datatableupdateproperties);
                sitecontentmap.put("db", dbexport);

                // send a mail, if email properties are set
                if (emailproperties != null) {
                    try {
                        sendRespondMail(emailproperties.getSendto(), emailproperties.getSubject(), emailproperties.getBody());
                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                    }
                }

                // write the output
                Writer out = new StringWriter();
                if (0 == cftemplate.getScriptlanguage()) {  // Freemarker template
                    emailbean.init(propertymap);
                    fmRoot.put("emailBean", emailbean);
                    fmRoot.put("css", cfstylesheet);
                    fmRoot.put("js", cfjavascript);
                    fmRoot.put("sitecontent", sitecontentmap);
                    if (sapSupport) {
                        List<CfSitesaprfc> sitesaprfclist = new ArrayList<>();
                        sitesaprfclist.addAll(cfsitesaprfcService.findBySiteref(cfsite.getId()));
                        sapbean.init(sapc, sitesaprfclist, rpytableread, postmap);
                    }
                    fmRoot.put("sapBean", sapbean);
                    databasebean.init(sitedatasourcelist);
                    fmRoot.put("databaseBean", databasebean);
                    
                    fmRoot.put("parameter", parametermap);
                    fmRoot.put("property", propertymap);
                    try {
                        freemarker.core.Environment env = fmTemplate.createProcessingEnvironment(fmRoot, out);
                        env.process();
                    } catch (freemarker.template.TemplateException ex) {
                        logger.error(ex.getMessage());
                    }
                } else {                                    // Velocity template
                    emailbean.init(propertymap);
                    velContext.put("emailBean", emailbean);
                    velContext.put("css", cfstylesheet);
                    velContext.put("js", cfjavascript);
                    velContext.put("sitecontent", sitecontentmap); 
                    if (sapSupport) {
                        List<CfSitesaprfc> sitesaprfclist = new ArrayList<>();
                        sitesaprfclist.addAll(cfsitesaprfcService.findBySiteref(cfsite.getId()));
                        sapbean.init(sapc, sitesaprfclist, rpytableread, postmap);
                    }
                    velContext.put("sapBean", sapbean);
                    databasebean.init(sitedatasourcelist);
                    velContext.put("databaseBean", databasebean);
                    
                    velContext.put("parameter", parametermap);
                    velContext.put("property", propertymap);
                    
                    velTemplate.merge(velContext, out);
                }
                String gzip;
                gzip = propertymap.get("html.gzip");
                if (gzip == null) {
                     gzip = "off";
                }
                switch (cfsite.getGzip()) {
                    case 1:
                        gzip = "on";
                        break;
                    case 2:
                        gzip = "off";
                        break;    
                }
                if (gzip.compareToIgnoreCase("on") == 0) {
                    gzipswitch.setGzipon(true);
                }
                
                String htmlcompression;
                htmlcompression = propertymap.get("html.compression");
                if (htmlcompression == null) {
                     htmlcompression = "off";
                }
                switch (cfsite.getHtmlcompression()) {
                    case 1:
                        htmlcompression = "on";
                        break;
                    case 2:
                        htmlcompression = "off";
                        break;    
                }
                if (htmlcompression.compareToIgnoreCase("on") == 0) {
                    HtmlCompressor htmlcompressor = new HtmlCompressor();
                    htmlcompressor.setRemoveSurroundingSpaces(HtmlCompressor.ALL_TAGS);
                    htmlcompressor.setPreserveLineBreaks(false);
                    htmlcompressor.setCompressCss(false);

                    cfresponse.setErrorcode(0);
                    cfresponse.setOutput(htmlcompressor.compress(out.toString()));
                    //logger.info("END makeResponse: " + name);
                    return new AsyncResult<ClownfishResponse>(cfresponse);
                } else {
                    cfresponse.setErrorcode(0);
                    cfresponse.setOutput(out.toString());
                    //logger.info("END makeResponse: " + name);
                    return new AsyncResult<ClownfishResponse>(cfresponse);
                }
            } catch (NoResultException ex) {
                cfresponse.setErrorcode(1);
                cfresponse.setOutput("No template");
                //logger.info("END makeResponse: " + name);
                return new AsyncResult<ClownfishResponse>(cfresponse);
            }     
        } catch (IOException | org.apache.velocity.runtime.parser.ParseException ex) {
            cfresponse.setErrorcode(1);
            cfresponse.setOutput(ex.getMessage());
            //logger.info("END makeResponse: " + name);
            return new AsyncResult<ClownfishResponse>(cfresponse);
        } 
    }
    
    private void sendRespondMail(String mailto, String subject, String mailbody) throws Exception {
        MailUtil mailutil = new MailUtil(propertymap.get("mail.smtp.host"), propertymap.get("mail.transport.protocol"), propertymap.get("mail.user"), propertymap.get("mail.password"), propertymap.get("mail.sendfrom"));
        mailutil.sendRespondMail(mailto, subject, mailbody);
    }

    private void manageSessionVariables(List<JsonFormParameter> postmap) {
        if (postmap != null) {
            for (JsonFormParameter jfp : postmap) {
                if (jfp.getName().startsWith("session")) {
                    userSession.setAttribute(jfp.getName(), jfp.getValue());
                }
            }
        }
    }
    
    private void writeSessionVariables(Map parametermap) {
        for (String key : Collections.list(userSession.getAttributeNames())) {
            if (key.startsWith("session")) {
                String attributevalue = (String) userSession.getAttribute(key);
                parametermap.put(key, attributevalue);
            }
        }
    }
}
