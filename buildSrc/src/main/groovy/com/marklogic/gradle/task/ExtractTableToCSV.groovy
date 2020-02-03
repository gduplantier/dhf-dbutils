package com.marklogic.gradle.task

import groovy.sql.Sql
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import com.marklogic.util.DbUtils

/***********************************************************************************
 * Extract a DB2 table to CSV
 * TODO Use DBUtils functions
 ***********************************************************************************/

public class ExtractTableToCSV extends DefaultTask {

    @Input
    String outputFilepath

    @Input
    String tableName

    @Input
    @Optional
    String date
	
	@TaskAction
	void extractTableToCSV() {

        println "Running task with outputFilepath ${outputFilepath} and tableName ${tableName}"
        
		def sql = DbUtils.createDbInstance(project);

        def printColNames = {meta, outFile ->
            (1..meta.columnCount).each {
                outFile << "\"${meta.getColumnLabel(it)}\""
                if(it < meta.columnCount) outFile << ","
            }
            outFile << "\n"
        }

        def printRow = {row, outFile ->
            def rowNum = 1
            def values = row.toRowResult().values()
            values.each {
                def valueStr = it.toString().replaceAll("\"", "'")
                outFile << "\"${valueStr}\""
                if(rowNum < values.size()) outFile << ","
                rowNum++
            }
            outFile << "\n"
        }

        try {
            File outFile = new File("${outputFilepath}${tableName}.csv")
            def rowNum = 1;
            def sqlStr = (date == null) ? 
                "select * from " + tableName : 
                "select * from " + tableName + " where UPD_DTTM > '${date}'"
            println sqlStr
            outFile.write ""
            sql.eachRow(sqlStr) { row ->
                if(rowNum == 1) printColNames(row.getMetaData(), outFile)
                printRow(row, outFile)
                rowNum++
            }
        } catch (Exception e) {
            println "Exception occurred querying Table, ${tableName}. Unable to load."
            println e
            outFile.delete()
        }
        
	}
}