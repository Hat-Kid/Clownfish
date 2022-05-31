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
package io.clownfish.clownfish.lucene;

import io.clownfish.clownfish.dbentities.CfAttributcontent;
import io.clownfish.clownfish.dbentities.CfDatasource;
import io.clownfish.clownfish.dbentities.CfSearchdatabase;
import io.clownfish.clownfish.jdbc.JDBCUtil;
import io.clownfish.clownfish.jdbc.TableField;
import io.clownfish.clownfish.jdbc.TableFieldStructure;
import io.clownfish.clownfish.serviceinterface.CfDatasourceService;
import io.clownfish.clownfish.serviceinterface.CfSearchdatabaseService;
import io.clownfish.clownfish.utils.DatabaseUtil;
import io.clownfish.clownfish.utils.EncryptUtil;
import io.clownfish.clownfish.utils.PropertyUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import org.apache.lucene.index.IndexWriter;
import javax.inject.Named;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.CorruptIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author sulzbachr
 */
@Named("databasetableindexerservice")
@Scope("singleton")
@Component
public class DatabasetableIndexer implements Runnable {
    private final IndexWriter writer;
    private final CfSearchdatabaseService cfsearchdatabaseService;
    private final CfDatasourceService cfdatasourceService;
    private final DatabaseUtil databaseUtil;
    @Autowired private PropertyUtil propertyUtil;
    
    final transient Logger LOGGER = LoggerFactory.getLogger(DatabasetableIndexer.class);
    
    public DatabasetableIndexer(CfSearchdatabaseService cfsearchdatabaseService, CfDatasourceService cfdatasourceService, DatabaseUtil databaseUtil, IndexService indexService) throws IOException {
        this.cfsearchdatabaseService = cfsearchdatabaseService;
        this.cfdatasourceService = cfdatasourceService;
        this.databaseUtil = databaseUtil;
        writer = indexService.getWriter();
    }

    public void close() throws CorruptIndexException, IOException {
        writer.close();
    }

    /*
        getDocument makes the IndexDocument for a content element with following fields:
        id
        classcontent_ref
        content-type (always Clownfish/DBContent)
        content
    */
    private Document getDocument(CfAttributcontent attributcontent) throws IOException {
        if (attributcontent.getClasscontentref().getClassref().isSearchrelevant()) {
            Document document = new Document();
            document.add(new StoredField(LuceneConstants.CONTENT_TYPE, "Clownfish/DBContent"));
            document.add(new StoredField(LuceneConstants.ID, attributcontent.getId()));
            document.add(new StoredField(LuceneConstants.CLASSCONTENT_REF, attributcontent.getClasscontentref().getId()));
            switch (attributcontent.getAttributref().getAttributetype().getName()) {
                case "string":
                    if (null != attributcontent.getContentString()) {
                        if ((attributcontent.getClasscontentref().getClassref().isEncrypted()) && (!attributcontent.getAttributref().getIdentity())) {
                            document.add(new TextField(LuceneConstants.CONTENT_STRING, EncryptUtil.decrypt(attributcontent.getContentString(), propertyUtil.getPropertyValue("aes_key")), Field.Store.YES));
                        } else {
                            document.add(new TextField(LuceneConstants.CONTENT_STRING, attributcontent.getContentString(), Field.Store.YES));
                        }
                    }
                    break;
                case "text":
                case "htmltext":
                case "markdown":
                    if (null != attributcontent.getContentText()) {
                        if ((attributcontent.getClasscontentref().getClassref().isEncrypted()) && (!attributcontent.getAttributref().getIdentity())) {
                            document.add(new TextField(LuceneConstants.CONTENT_TEXT, EncryptUtil.decrypt(attributcontent.getContentText(), propertyUtil.getPropertyValue("aes_key")), Field.Store.YES));
                        } else {
                            document.add(new TextField(LuceneConstants.CONTENT_TEXT, attributcontent.getContentText(), Field.Store.YES));
                        }
                    }
                    break;
            }
            return document;
        } else {
            return null;
        }
    }

    /*
    private void indexAttributContent(CfAttributcontent attributcontent) throws IOException {
        if (attributcontent.getAttributref().getAttributetype().isSearchrelevant()) {
            attributcontent.setIndexed(true);
            cfattributcontentService.edit(attributcontent);
            Document document = getDocument(attributcontent);
            if (null != document) {
                writer.addDocument(document);
            }
        }
    }
    */

    public long createIndex(CfSearchdatabase searchdb) throws IOException {
        Statement stmt = null;
        ResultSet result = null;
        CfDatasource cfdatasource = cfdatasourceService.findById(searchdb.getCfSearchdatabsePK().getDatasourceRef());
        JDBCUtil jdbcutil = new JDBCUtil(cfdatasource.getDriverclass(), cfdatasource.getUrl(), cfdatasource.getUser(), cfdatasource.getPassword());
        Connection con = jdbcutil.getConnection();
        
        if (null != con) {
            try {
                DatabaseMetaData dmd = con.getMetaData();

                TableFieldStructure tfs = databaseUtil.getTableFieldsList(dmd, searchdb.getCfSearchdatabsePK().getTablename(), "");
                String statement = databaseUtil.getSQLSelect(con, dmd, searchdb.getCfSearchdatabsePK().getTablename(), tfs);
                stmt = con.createStatement();
                result = stmt.executeQuery(statement);
                ArrayList<HashMap> tablevalues = new ArrayList<>();
                while (result.next()) {
                    HashMap<String, String> dbexportvalues = new HashMap<>();
                    for (TableField tf : tfs.getTableFieldsList()) {
                        try {
                            String value = result.getString(tf.getName());
                            dbexportvalues.put(tf.getName(), value);
                        } catch (java.sql.SQLException ex) {

                        }
                    }
                    tablevalues.add(dbexportvalues);
                }
                writer.commit();
                writer.forceMerge(10);
                return writer.numRamDocs();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
                return -1;
            } finally {
                try {
                    con.close();
                    return -1;
                }
                catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                    return -1;
                }
            }
        }
        return -1;
    }

    @Override
    public void run() {
        try {
            List<CfSearchdatabase> searchdblist = cfsearchdatabaseService.findAll();
            for (CfSearchdatabase searchdb : searchdblist) {
                createIndex(searchdb);
            }
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }
    }
}
