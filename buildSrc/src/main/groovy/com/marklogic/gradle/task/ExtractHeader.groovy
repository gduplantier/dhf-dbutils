package com.marklogic.gradle.task

import java.util.Properties;
import groovy.sql.Sql
import java.sql.* 
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask

import com.marklogic.util.DbUtils

/***********************************************************************************
 * Create a header row for a csv from a DB2 database given a table name.
 * TODO Need to accept a schema name also
 ***********************************************************************************/

public class ExtractHeader extends DefaultTask {
	
    @Input
    String tableName

    @Input
    String outputFilepath
	
	@TaskAction
	void getHeaderRow() {
        File outFile = new File("${outputFilepath}${tableName}.header.csv")
        def sql = DbUtils.createDbInstance(project);
        def sqlString = "select colname from syscat.columns where tabschema = 'ADWINST2' and tabname = ${tableName} order by colno"
        def noOfLines = 0;
		
        sql.eachRow(sqlString) { rs ->
            if(noOfLines <= 0) {
                outFile.write rs.colname
            } else {
                outFile << ", ${rs.colname}"
            }
            noOfLines++;
        }
        sql.close()
	}
}