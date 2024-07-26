/*
 * Copyright 2024 SulzbachR.
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

import io.clownfish.clownfish.datamodels.AuthResponse;
import io.clownfish.clownfish.datamodels.AuthTokenClasscontent;
import io.clownfish.clownfish.datamodels.AuthTokenListClasscontent;
import io.clownfish.clownfish.dbentities.CfAttribut;
import io.clownfish.clownfish.dbentities.CfAttributcontent;
import io.clownfish.clownfish.dbentities.CfClass;
import io.clownfish.clownfish.dbentities.CfClasscontent;
import io.clownfish.clownfish.serviceinterface.CfAttributService;
import io.clownfish.clownfish.serviceinterface.CfAttributcontentService;
import io.clownfish.clownfish.serviceinterface.CfClassService;
import io.clownfish.clownfish.serviceinterface.CfClasscontentService;
import io.clownfish.clownfish.utils.MailUtil;
import io.clownfish.clownfish.utils.PropertyUtil;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author SulzbachR
 */
@WebServlet(name = "SendConfirmDeleteMail", urlPatterns = {"/SendConfirmDeleteMail"})
@Component
public class ClownfishSendConfirmDeleteMail extends HttpServlet {
    @Autowired transient CfClassService cfclassService;
    @Autowired transient CfClasscontentService cfclasscontentservice;
    @Autowired transient CfAttributcontentService cfattributcontentService;
    @Autowired transient CfAttributService cfattributService;
    @Autowired transient AuthTokenListClasscontent confirmtokenlist;
    @Autowired PropertyUtil propertyUtil;
    
    @Value("${server.port:9000}")
    int serverPortHttp;
    
    String klasse = "";
    String email = "";
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String[]> parameters = request.getParameterMap();
        klasse = "";
        parameters.keySet().stream().filter((paramname) -> (paramname.compareToIgnoreCase("class") == 0)).map((paramname) -> parameters.get(paramname)).forEach((values) -> {
            klasse = values[0];
        });
        email = "";
        parameters.keySet().stream().filter((paramname) -> (paramname.compareToIgnoreCase("email") == 0)).map((paramname) -> parameters.get(paramname)).forEach((values) -> {
            email = values[0];
        });
        
        CfClass clazz = cfclassService.findByName(klasse);
        if (null != clazz) {
            if (clazz.isLoginclass()) {
                List<CfClasscontent> classcontentlist = cfclasscontentservice.findByClassref(clazz);
                CfAttribut attributEmail = cfattributService.findByNameAndClassref("email", clazz);
                for (CfClasscontent cc : classcontentlist) {
                    CfAttributcontent attributContentEmail = cfattributcontentService.findByAttributrefAndClasscontentref(attributEmail, cc);
                    if (0 == attributContentEmail.getContentString().compareToIgnoreCase(email)) {
                        sendEmail(cc);
                        break;
                    }
                }
            }
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

    }
    
    private void sendEmail(CfClasscontent user) {
        AuthResponse ar = new AuthResponse();
        ar.setStatus(true);
        ar.setToken("");
        ar.setValiduntil(null);
        
        CfAttribut attributPassword = cfattributService.findByNameAndClassref("password", user.getClassref());
        CfAttributcontent attributContent = cfattributcontentService.findByAttributrefAndClasscontentref(attributPassword, user);
        ar.setToken(AuthTokenClasscontent.generateToken(attributContent.getContentString(), attributContent.getSalt()));
        String accountconfirmtime = propertyUtil.getPropertyValue("accountconfirmtime");
        int act = Integer.parseInt(accountconfirmtime);
        AuthTokenClasscontent at = new AuthTokenClasscontent(ar.getToken(), new DateTime().plusMinutes(act), user, "");      // Tokens valid for 60 minutes
        ar.setValiduntil(at.getValiduntil());
        confirmtokenlist.setConfirmation(false);
        confirmtokenlist.getAuthtokens().put(ar.getToken(), at);

        CfAttribut attributEmail = cfattributService.findByNameAndClassref("email", user.getClassref());
        CfAttributcontent attributContentEmail = cfattributcontentService.findByAttributrefAndClasscontentref(attributEmail, user);

        MailUtil mailutil = new MailUtil(propertyUtil);
        try {
            StringBuilder domain = new StringBuilder();
            String strdomain = propertyUtil.getPropertyValue("domain");
            String http = "http://";
            if ((strdomain.startsWith("http://"))) {
                strdomain = strdomain.replaceFirst("http://", "");
            }
            if ((strdomain.startsWith("https://"))) {
                strdomain = strdomain.replaceFirst("https://", "");
                http = "https://";
            }
            String port = "";
            if (strdomain.lastIndexOf(":") > 0) {
                port = strdomain.substring(strdomain.lastIndexOf(":")+1);
                strdomain = strdomain.substring(0, strdomain.lastIndexOf(":"));
            }
            domain.append(http);
            domain.append(strdomain);
            if ((80 != serverPortHttp) && (port.isBlank())) {
                domain.append(":");
                domain.append(serverPortHttp);
            } else {
                if (0 != port.compareToIgnoreCase("80")) {
                    domain.append(":");
                    domain.append(port);
                }
            }
            mailutil.sendRespondMail(attributContentEmail.getContentString(), "Löschung des Accounts", "Mit folgendem Link bestätigen Sie die Löschung Ihres Accounts:</br>" + domain.toString() + "/ConfirmDelete?token=" + URLEncoder.encode(ar.getToken(), "UTF-8") + "</br>Falls Sie diesen Link innerhalb der nächsten " + act + " Minuten nicht anwählen, bleibt der Account weiterhin bestehen.");
        } catch (Exception ex) {
            Logger.getLogger(ClownfishSendConfirmDeleteMail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
