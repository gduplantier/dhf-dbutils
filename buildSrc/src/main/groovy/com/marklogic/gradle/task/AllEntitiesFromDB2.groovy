package com.marklogic.gradle.task

import java.util.Properties;
import groovy.sql.Sql
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask

import com.marklogic.util.StringUtil
import com.marklogic.util.DbUtils

/***********************************************************************************
 * Read a text file with an entity name on each line 
 * and create entity descriptor for each
 * TODO Use DBUtils functions
 ***********************************************************************************/

public class AllEntitiesFromDB2 extends DefaultTask {
    @Input
    String inputFilename

    @Input
    String outputDir
	
	@TaskAction
	void createAllEntities() {
    	def sql = DbUtils.createDbInstance(project);

        File inFile = new File(inputFilename)
            def line, noOfLines = 0;
            inFile.withReader { reader ->
                while ((line = reader.readLine()) != null) {
                    createEntity(line.trim(), sql)
			    }
		}
        sql.close()
	}

    void createEntity(String tableName, Sql sql) {
        def outfileName = "${outputDir}\\${tableName}.entity.json"
        File outFile = new File(outfileName)
    
        def sqlString = "select distinct colno, name, coltype, nulls, keyseq from sysibm.syscolumns where tbname = ${tableName} order by colno"
        def requiredList = []
		def primaryKey = null;
        def noOfLines = 0;

        outFile.write "{\n"
		outFile <<    "  \"info\": {\n"
		outFile <<    "    \"title\": \"${tableName}\", \n"
		outFile <<    "    \"version\": \"1.0\",\n"
		outFile <<    "    \"baseUri\": \"http://myorg.com/myproject\" \n"
		outFile <<    "  },"
		outFile <<    "  \"definitions\": {\n"
		outFile <<    "    \"${tableName}\": {\n"
		outFile <<    "      \"properties\": {\n"

        sql.eachRow(sqlString) { rs ->
            String propName = rs.name;
            String colType = rs.coltype.replace("\"", "").trim();
            String nullAllowed = rs.nulls.replace("\"", "").trim();
            int keySeq = (rs.keyseq == null) ? 0 : rs.keyseq;
            
            if(nullAllowed == "N") requiredList.add("\"${propName}\"");
            if(primaryKey == null && keySeq != null && keySeq > 0) primaryKey = propName;
            
            if(noOfLines > 0) {
                outFile << "        },\n"
            }

            outFile << "        \"${propName}\": {\n";

            if(colType == 'CHAR' || colType == 'VARCHAR') {
                outFile << "          \"datatype\": \"string\",  \n";
                outFile << "          \"collation\": \"http://marklogic.com/collation/codepoint\" \n"
            } else if(colType == 'TIMESTMP') {
                outFile << "          \"datatype\": \"dateTime\"  \n";
            } else if(colType == 'DATE') {
                outFile << "          \"datatype\": \"date\"  \n";
            } else if(colType == 'INT' || colType == 'INTEGER' || colType == 'BIGINT' || colType == 'SMALLINT') {
                outFile << "          \"datatype\": \"integer\"  \n";
            } else if(colType == 'DECIMAL' || colType == 'NUMERIC' || colType == 'DECFLOAT' || colType == 'REAL' || colType == 'DOUBLE') {
                outFile << "          \"datatype\": \"decimal\"  \n";
            } else {
                outFile << "          \"datatype\": \"unknown\" \n";
            }
            
            noOfLines++;
        }

        //Handle last closing bracket of property
        if(noOfLines > 0) {
            outFile << "        }\n"
        }
        outFile << "      },\n"
		outFile << "      \"required\": ${requiredList},\n"
		outFile << "      \"primaryKey\": \"${primaryKey}\"\n"
		outFile << "    }\n"
		outFile << "  }\n"
		outFile << "}\n"
        		
        //println groovy.json.JsonOutput.prettyPrint(outFile.text);
    }
}