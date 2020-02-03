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
 * Create a DHF Entity descriptor from an Oracle database, given a table name
 ***********************************************************************************/

public class EntityFromOracle extends DefaultTask {

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

        def sqlString = "select column_id, column_name, data_type, nullable from all_tab_columns where UPPER(table_name) = UPPER(${tableName}) order by column_id"
        def requiredList = []
        def noOfLines = 0;

        outFile.write "{\n"
		outFile << "  \"info\": {\n"
		outFile << "    \"title\": \"${entity}\", \n"
		outFile << "    \"version\": \"1.0\",\n"
		outFile << "    \"baseUri\": \"http://myorg.com/myproject\" \n"
		outFile << "  },"
		outFile << "  \"definitions\": {\n"
		outFile << "    \"${entity}\": {\n"
		outFile << "      \"properties\": {\n"

        sql.eachRow(sqlString) { rs ->
            String propName = (camelCase) ? StringUtil.toCamelCase(rs.COLUMN_NAME.toLowerCase()) : rs.COLUMN_NAME;
            String colType = rs.DATA_TYPE.replace("\"", "").trim();
            String nullAllowed = rs.NULLABLE.replace("\"", "").trim();
            
            if(nullAllowed == "N") requiredList.add("\"${propName}\"");

            if(noOfLines > 0) {
                outFile << "        },\n"
            }

            outFile << "        \"${propName}\": {\n";

            if(colType == 'CHAR' || colType == 'VARCHAR2' || colType == 'NCHAR' || colType == 'NVARCHAR2') {
                outFile << "          \"datatype\": \"string\",  \n";
                outFile << "          \"collation\": \"http://marklogic.com/collation/codepoint\" \n"
            } else if(colType == 'TIMESTAMP') {
                outFile << "          \"datatype\": \"dateTime\"  \n";
            } else if(colType == 'DATE') {
                outFile << "          \"datatype\": \"date\"  \n";
            } else if(colType == 'INT' || colType == 'INTEGER' || colType == 'BIGINT' || colType == 'SMALLINT') {
                outFile << "          \"datatype\": \"integer\"  \n";
            } else if(colType == 'LONG' || colType == 'RAW') {
                outFile << "          \"datatype\": \"long\"  \n";
            } else if(colType == 'NUMBER' || colType == 'BINARY_FLOAT' || colType == 'BINARY_DOUBLE' || colType == 'DECIMAL' || colType == 'DEC'  || colType == 'NUMERIC') {
                outFile << "          \"datatype\": \"decimal\"  \n";
            } else {
                outFile << "          \"datatype\": \"unknown\" \n";
            }
            
            noOfLines++;
        }
        outFile << "        }\n"
		outFile << "      },\n"
		outFile << "     \"required\": ${requiredList},\n"
		outFile << "    }\n"
		outFile << "  }\n"
		outFile << "}\n"

		//println groovy.json.JsonOutput.prettyPrint(outFile.text);
        sql.close()
	}

}