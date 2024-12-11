package org.edu_sharing.service.lifecycle;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;


public class OrganisationDeleteProtocolServiceCSV extends OrganisationDeleteProtocolService{

    Logger logger = Logger.getLogger(OrganisationDeleteProtocolServiceCSV.class);


    CSVWriter csvWriter = null;

    public static String MIMETYPE = "text/csv";

    public OrganisationDeleteProtocolServiceCSV(String organisation){

        super(organisation);

        try {
            FileWriter writer = new FileWriter(tempFile);
            csvWriter = new CSVWriter(writer);

            ContentReader contentReader = getContentReader();
            if(contentReader != null && contentReader.getSize() > 0){
                CSVReader csvReader = new CSVReader(new StringReader(contentReader.getContentString()));
                csvReader.forEach(a -> {
                    csvWriter.writeNext(a);
                });
            }else{
                csvWriter.writeNext(OrganisationDeleteProtocol.getHeader());
            }
        }catch (Throwable e){
            logger.error(e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

    public void writeProtocolEntry(OrganisationDeleteProtocol protEntry) throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        csvWriter.writeNext(protEntry.getArray());
        csvWriter.flushQuietly();
    }

    public void cleanUp(){
        try {
            if(csvWriter != null) csvWriter.close();
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

    public void protocolEntry(OrganisationDeleteProtocol protEntry) {
        try {
            writeProtocolEntry(protEntry);
        } catch (CsvRequiredFieldEmptyException e) {
            logger.error(e.getMessage(), e);
        } catch (CsvDataTypeMismatchException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getProtcolFormatSuffix() {
        return "csv";
    }
}
