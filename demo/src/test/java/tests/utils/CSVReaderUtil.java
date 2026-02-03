package tests.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.testng.annotations.DataProvider;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReaderUtil {
    
    @DataProvider(name = "issueTestData")
    public static Object[][] getIssueTestData() {
        List<Object[]> testData = new ArrayList<>();
        String filePath = "src/test/resources/test-data/issues.csv";
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> allData = reader.readAll();
            
            // Пропускаем заголовок
            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);
                testData.add(new Object[]{
                    row[0], // projectId
                    row[1], // summary
                    row[2], // description
                    Boolean.parseBoolean(row[3]) // expectedSuccess
                });
            }
        } catch (IOException | CsvException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading CSV file: " + filePath, e);
        }
        
        return testData.toArray(new Object[0][0]);
    }
}