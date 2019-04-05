/*
 * Copyright Rainer Sulzbach
 */
package io.clownfish.clownfish.utils;

import io.clownfish.clownfish.beans.JsonFormParameter;
import io.clownfish.clownfish.dbentities.CfSitesaprfc;
import io.clownfish.clownfish.jdbc.DatatableCondition;
import io.clownfish.clownfish.jdbc.DatatableDeleteProperties;
import io.clownfish.clownfish.jdbc.DatatableDeleteValue;
import io.clownfish.clownfish.jdbc.DatatableNewProperties;
import io.clownfish.clownfish.jdbc.DatatableNewValue;
import io.clownfish.clownfish.jdbc.DatatableProperties;
import io.clownfish.clownfish.jdbc.DatatableUpdateProperties;
import io.clownfish.clownfish.mail.EmailProperties;
import io.clownfish.clownfish.sap.RFC_GET_FUNCTION_INTERFACE;
import io.clownfish.clownfish.sap.models.RfcFunctionParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author rawdog
 */
public class ClownfishUtil {
    
    /*
        getParametermap
        Übergibt die POST Parameter in eine Hashmap um
    */
    public Map getParametermap(List<JsonFormParameter> postmap) {
        Map parametermap = new HashMap<>();
        if (postmap != null) {
            for (JsonFormParameter jfp : postmap) {
                parametermap.put(jfp.getName(), jfp.getValue());
            }
        }
        return parametermap;
    }
    
    /*
        getSaprfcfunctionparamMap
    */
    public HashMap<String, List> getSaprfcfunctionparamMap(List<CfSitesaprfc> sitesaprfclist, RFC_GET_FUNCTION_INTERFACE rfc_get_function_interface) {
        HashMap<String, List> saprfcfunctionparamMap = new HashMap<>();
        for (CfSitesaprfc knsitesaprfc : sitesaprfclist) {
            List<RfcFunctionParam> rfcfunctionparamlist = new ArrayList<>();
            rfcfunctionparamlist.addAll(rfc_get_function_interface.getRfcFunctionsParamList(knsitesaprfc.getCfSitesaprfcPK().getRfcfunction()));
            saprfcfunctionparamMap.put(knsitesaprfc.getCfSitesaprfcPK().getRfcfunction(), rfcfunctionparamlist);
        }
        return saprfcfunctionparamMap;
    }
    
    /*
        getDatatableproperties
        Setzt die Properties für ein DB READ Aufruf
    */
    public HashMap<String, DatatableProperties> getDatatableproperties(List<JsonFormParameter> postmap) {
        HashMap<String, DatatableProperties> datatableproperties = new HashMap<>();
        if (postmap != null) {
            for (JsonFormParameter jfp : postmap) {
                // Datenbank READ Parameter
                if (jfp.getName().compareToIgnoreCase("db$table") == 0) {
                    if (datatableproperties.get(jfp.getValue()) == null ) {
                        DatatableProperties dtp = new DatatableProperties();
                        dtp.setTablename(jfp.getValue());
                        datatableproperties.put(jfp.getValue(), dtp);
                    }
                }
                if (jfp.getName().startsWith("db$table$")) {
                    String rest = jfp.getName().substring(9);
                    String[] values = rest.split("\\$");
                    if (values[1].compareToIgnoreCase("orderby") == 0) {
                        if (datatableproperties.isEmpty()) {
                            DatatableProperties dtp = new DatatableProperties();
                            dtp.setTablename(values[0]);
                            datatableproperties.put(values[0], dtp);
                        }
                        datatableproperties.get(values[0]).setOrderby(jfp.getValue());
                    }
                    if (values[1].compareToIgnoreCase("orderdir") == 0) {
                        if (datatableproperties.isEmpty()) {
                            DatatableProperties dtp = new DatatableProperties();
                            dtp.setTablename(values[0]);
                            datatableproperties.put(values[0], dtp);
                        }
                        datatableproperties.get(values[0]).setOrderdir(jfp.getValue());
                    }
                    if (values[1].compareToIgnoreCase("pagination") == 0) {
                        if (datatableproperties.isEmpty()) {
                            DatatableProperties dtp = new DatatableProperties();
                            dtp.setTablename(values[0]);
                            datatableproperties.put(values[0], dtp);
                        }
                        datatableproperties.get(values[0]).setPagination(Integer.parseInt(jfp.getValue()));
                    }
                    if (values[1].compareToIgnoreCase("page") == 0) {
                        if (datatableproperties.isEmpty()) {
                            DatatableProperties dtp = new DatatableProperties();
                            dtp.setTablename(values[0]);
                            datatableproperties.put(values[0], dtp);
                        }
                        datatableproperties.get(values[0]).setPage(Integer.parseInt(jfp.getValue()));
                    }
                    if (values[1].compareToIgnoreCase("groupbycount") == 0) {
                        if (datatableproperties.isEmpty()) {
                            DatatableProperties dtp = new DatatableProperties();
                            dtp.setTablename(values[0]);
                            datatableproperties.put(values[0], dtp);
                        }
                        datatableproperties.get(values[0]).setGroupbycount(jfp.getValue());
                    }
                    if (values[1].compareToIgnoreCase("groupby") == 0) {
                        if (datatableproperties.isEmpty()) {
                            DatatableProperties dtp = new DatatableProperties();
                            dtp.setTablename(values[0]);
                            datatableproperties.put(values[0], dtp);
                        }
                        datatableproperties.get(values[0]).getGroupbylist().add(jfp.getValue());
                    }
                    if (values[1].compareToIgnoreCase("condition") == 0) {
                        DatatableCondition dtc = new DatatableCondition();
                        dtc.setField(values[2]);
                        dtc.setOperand(values[3]);
                        dtc.setValue(jfp.getValue());
                        if (datatableproperties.isEmpty()) {
                            DatatableProperties dtp = new DatatableProperties();
                            dtp.setTablename(values[0]);
                            datatableproperties.put(values[0], dtp);
                        }
                        datatableproperties.get(values[0]).getConditionlist().add(dtc);
                    }
                }
            }
        }
        return datatableproperties;
    }
    
    /*
        getDatatablenewproperties
        Setzt die Properties für ein DB INSERT Aufruf
    */
    public HashMap<String, DatatableNewProperties> getDatatablenewproperties(List<JsonFormParameter> postmap) {  
        HashMap<String, DatatableNewProperties> datatablenewproperties = new HashMap<>();
        if (postmap != null) {
            for (JsonFormParameter jfp : postmap) {
                // Datenbank INSERT Parameter
                if (jfp.getName().compareToIgnoreCase("db$tablenew") == 0) {
                    DatatableNewProperties dtnp = new DatatableNewProperties();
                    dtnp.setTablename(jfp.getValue());
                    datatablenewproperties.put(jfp.getValue(), dtnp);
                }
                if (jfp.getName().startsWith("db$tablenew$")) {
                    String rest = jfp.getName().substring(12);
                    String[] values = rest.split("\\$");
                    DatatableNewValue dtnv = new DatatableNewValue();
                    dtnv.setField(values[1]);
                    dtnv.setValue(jfp.getValue());
                    datatablenewproperties.get(values[0]).getValuelist().add(dtnv);
                }
            }
        }
        return datatablenewproperties;
    }
    
    /*
        getDatatabledeleteproperties
        Setzt die Properties für ein DB DELETE Aufruf
    */
    public HashMap<String, DatatableDeleteProperties> getDatatabledeleteproperties(List<JsonFormParameter> postmap) {  
        HashMap<String, DatatableDeleteProperties> datatabledeleteproperties = new HashMap<>();
        if (postmap != null) {
            for (JsonFormParameter jfp : postmap) {
                // Datenbank DELETE Parameter
                if (jfp.getName().compareToIgnoreCase("db$tabledelete") == 0) {
                    DatatableDeleteProperties dtdp = new DatatableDeleteProperties();
                    dtdp.setTablename(jfp.getValue());
                    datatabledeleteproperties.put(jfp.getValue(), dtdp);
                }
                if (jfp.getName().startsWith("db$tabledelete$")) {
                    String rest = jfp.getName().substring(15);
                    String[] values = rest.split("\\$");
                    DatatableDeleteValue dtdv = new DatatableDeleteValue();
                    dtdv.setField(values[1]);
                    dtdv.setValue(jfp.getValue());
                    datatabledeleteproperties.get(values[0]).getValuelist().add(dtdv);
                }
            }
        }
        return datatabledeleteproperties;
    }
    
    /*
        getDatatableupdateproperties
        Setzt die Properties für ein DB UPDATE Aufruf
    */
    public HashMap<String, DatatableUpdateProperties> getDatatableupdateproperties(List<JsonFormParameter> postmap) {  
        HashMap<String, DatatableUpdateProperties> datatableupdateproperties = new HashMap<>();
        if (postmap != null) {
            for (JsonFormParameter jfp : postmap) {
                // Datenbank UPDATE Parameter
                if (jfp.getName().compareToIgnoreCase("db$tableupdate") == 0) {
                    DatatableUpdateProperties dtup = new DatatableUpdateProperties();
                    dtup.setTablename(jfp.getValue());
                    datatableupdateproperties.put(jfp.getValue(), dtup);
                }
                if (jfp.getName().startsWith("db$tableupdate$")) {
                    String rest = jfp.getName().substring(15);
                    String[] values = rest.split("\\$");
                    if (values[1].compareToIgnoreCase("condition") == 0) {
                        DatatableCondition dtc = new DatatableCondition();
                        dtc.setField(values[2]);
                        dtc.setOperand(values[3]);
                        dtc.setValue(jfp.getValue());
                        datatableupdateproperties.get(values[0]).getConditionlist().add(dtc);
                    } else {
                        DatatableNewValue dtnv = new DatatableNewValue();
                        dtnv.setField(values[1]);
                        dtnv.setValue(jfp.getValue());
                        datatableupdateproperties.get(values[0]).getValuelist().add(dtnv);
                    }
                }
            }
        }
        return datatableupdateproperties;
    }
                
    /*
        getEmailproperties
        Setzt die Properties für ein EMAIL send Aufruf
    */
    public EmailProperties getEmailproperties(List<JsonFormParameter> postmap) {  
        EmailProperties emailproperties = null;
        if (postmap != null) {
            for (JsonFormParameter jfp : postmap) {
                // EMAIL Parameter
                if (jfp.getName().compareToIgnoreCase("email$to") == 0) {
                    if (emailproperties == null) {
                        emailproperties = new EmailProperties();
                    }
                    emailproperties.setSendto(jfp.getValue());
                }
                if (jfp.getName().compareToIgnoreCase("email$subject") == 0) {
                    if (emailproperties == null) {
                        emailproperties = new EmailProperties();
                    }
                    emailproperties.setSubject(jfp.getValue());
                }
                if (jfp.getName().compareToIgnoreCase("email$body") == 0) {
                    if (emailproperties == null) {
                        emailproperties = new EmailProperties();
                    }
                    emailproperties.setBody(jfp.getValue());
                }
            }
        }
        return emailproperties;
    }
}