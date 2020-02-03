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
 * Read a text file with table names on each line.  Create a header row for a
 * CSV from each table.
 * TODO function should accept an optional schema name
 ***********************************************************************************/

public class ExtractHeaders extends DefaultTask {
	
    @Input
    String outputFilepath

    @Input
    String tableListFile
	
	@TaskAction
	void getHeaderRow() {
        File inFile = new File(tableListFile)
        def line
        def sql = DbUtils.createDbInstance(project);
        inFile.withReader { reader ->
			while ((line = reader.readLine()) != null) {
                def tableName = line.trim()
                def sqlString = "select colname from syscat.columns where tabschema = 'ADWINST2' and tabname = ${tableName} order by colno"
                File outFile = new File("${outputFilepath}${tableName}.header.csv")
                def noOfLines = 0;
                
                sql.eachRow(sqlString) { rs ->
                    if(noOfLines <= 0) {
                        outFile.write "\"${rs.colname}\""
                    } else {
                        outFile << ", \"${rs.colname}\""
                    }
                    noOfLines++;
                }
            }
        }
        sql.close()
	}
}