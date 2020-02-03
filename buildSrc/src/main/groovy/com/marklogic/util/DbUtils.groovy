package com.marklogic.util

import java.util.Properties
import groovy.sql.Sql
import groovy.sql.GroovyRowResult
import java.sql.ResultSetMetaData

class DbUtils {
    private DbUtils() {}

    //Create a Grooby DB Instance based on project properties in gradle-local.properties
    static Sql createDbInstance(def project) {
		Sql.loadDriver(project.property("dbDriverClassName"));

        Properties properties = new java.util.Properties();
        properties.put("user",  project.property("dbUserName"));
        properties.put("password",  project.property("dbUserPwd"));

        //Additional properties for DB2 driver
        if(project.property("dbDriverClassName") == "com.ibm.db2.jcc.DB2Driver") {
            properties.put("securityMechanism", "9");
            properties.put("encryptionAlgorithm", "2");
        }
    
		return Sql.newInstance(project.property("dbDriverUrl") , properties);
	}

    static void printColNames(File outFile, ResultSetMetaData meta) {
		(1..meta.columnCount).each {
            outFile << "\"${meta.getColumnLabel(it)}\""
            if(it < meta.columnCount) outFile << ","
        }
        outFile << "\n"
	}

    static void printRow(GroovyRowResult row, File outFile) {
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
}