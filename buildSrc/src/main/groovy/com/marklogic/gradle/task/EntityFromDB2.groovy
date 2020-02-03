package com.marklogic.gradle.task

import java.util.Properties;
import groovy.sql.Sql
import java.sql.* 
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask

import com.marklogic.util.StringUtil
import com.marklogic.util.DbUtils

/***********************************************************************************
 * Create a DHF Entity descriptor from a DB2 database, given a table name
 ***********************************************************************************/
public class EntityFromDB2 extends DefaultTask {

    @Input
    String entity
	
    @Input
    String tableName

    @Input
    String outputFilename

    @Input
    @Optional
    Boolean camelCase
	
	@TaskAction
	void getColumnDescription() {

        File outFile = new File(outputFilename)
        
        if (camelCase == null) {
            camelCase = project.hasProperty("camelCase") ?
                    project.property("camelCase") : false;
        }
    
		def sql = DbUtils.createDbInstance(project);

        def sqlString = "select distinct colno, name, coltype, nulls, keyseq from sysibm.syscolumns where tbname = ${tableName} order by colno"
        def requiredList = []
		def primaryKey = null;
        def noOfLines = 0;

        outFile.write "{\n"
		outFile << "  \"info\": {\n"
		outFile << "    \"title\": \"${entity}\", \n"
		outFile << "    \"version\": \"1.0\",\n"
		outFile << "    \"baseUri\": \"myorg.com/myproject\" \n"
		outFile << "  },"
		outFile << "  \"definitions\": {\n"
		outFile << "    \"${entity}\": {\n"
		outFile << "      \"properties\": {\n"

        sql.eachRow(sqlString) { rs ->
            String propName = (camelCase) ? StringUtil.toCamelCase(rs.name.toLowerCase()) : rs.name;
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
        outFile << "        }\n"
		outFile << "      },\n"
		outFile << "     \"required\": ${requiredList},\n"
		outFile << "     \"primaryKey\": \"${primaryKey}\"\n"
		outFile << "    }\n"
		outFile << "  }\n"
		outFile << "}\n"

		//println groovy.json.JsonOutput.prettyPrint(outFile.text);
        sql.close()
	}
}