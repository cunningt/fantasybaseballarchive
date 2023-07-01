///usr/bin/env jbang "$0" "$@" ; exit $?
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:1.11.0.Final}@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS commons-io:commons-io
//DEPS org.apache.commons:commons-csv:1.10.0
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN

import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.Quarkus;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.FileUtils;;

@CommandLine.Command
public class steamerseparate implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The steamer csv file to separate", defaultValue = "data/steamerhitting.csv")
    String steamerFile;

    @CommandLine.Parameters(index = "0", description = "The minor league csv file", defaultValue = "data/minors.csv")
    String minorsFile;

    @Inject
    CommandLine.IFactory factory;

    private final SeparationService separationService;

    public steamerseparate(SeparationService separationService) {
        this.separationService = separationService;
    }

    @Override
    public void run() {
        try {
            separationService.separate(steamerFile, minorsFile);

        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}

@Dependent
class SeparationService {
    private File steamerFile;
    private File minorsFile;

    HashMap<String, String> minorsPlayers = new HashMap<String, String>();

    public void readInFiles() throws IOException {
        List<String> steamerContents = FileUtils.readLines(steamerFile, "UTF-8");
        List<String> minorsContents = FileUtils.readLines(minorsFile, "UTF-8");


        for (String minorsPlayer : minorsContents) {
            String[] fields = minorsPlayer.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            String levels = fields[2];
            String id = fields[fields.length-1];

            levels = levels.replaceAll("\"", "");
            if (levels.contains(",")) {
                String[] templevels = levels.split(",");
                levels = templevels[templevels.length-1];
            }


            System.out.println("id " + id + " levels " + levels);

            minorsPlayers.put(id, levels);
        }

        boolean header = true;
        File minors = new File("steamerminors.csv");
        if (minors.exists()) {
            minors.delete();
        }
        for (String steamerPlayer : steamerContents) {
            if (header) {
                FileUtils.writeStringToFile(minors, steamerPlayer);
                header = false;
            } else {

                String[] fields = steamerPlayer.split(",");
                String levels = fields[2];
                String id = fields[fields.length-1];

                if (id.startsWith("\"sa")) {
                    FileUtils.writeStringToFile(minors, steamerPlayer);
                }
            }
        }

        File aFile = new File("a.csv");
        aFile.delete();
        File aplusFile = new File("a+.csv");
        aplusFile.delete();
        File aaFile = new File("aa.csv");
        aaFile.delete();
        File aaaFile = new File("aaa.csv");
        aaaFile.delete();
        header = true;
        for (String steamerPlayer : steamerContents) {
            if (header) {
                FileUtils.writeStringToFile(aFile, steamerPlayer + "\n");
                FileUtils.writeStringToFile(aplusFile, steamerPlayer + "\n");
                FileUtils.writeStringToFile(aaFile, steamerPlayer + "\n");
                FileUtils.writeStringToFile(aaaFile, steamerPlayer + "\n");
                header = false;
            } else {

                String[] fields = steamerPlayer.split(",");
                String levels = fields[2];
                String id = fields[fields.length-1];

                if (id.startsWith("\"sa")) {
                    String level = minorsPlayers.get(id);
                    if (level != null) {
                        switch(level) {
                            case "A" : 
                                FileUtils.writeStringToFile(aFile, steamerPlayer + "\n", true);
                                break;
                            case "A+" :
                                FileUtils.writeStringToFile(aplusFile, steamerPlayer + "\n", true);
                                break;
                            case "AA" :
                                FileUtils.writeStringToFile(aaFile, steamerPlayer + "\n", true);
                                break;
                            case "AAA" :
                                FileUtils.writeStringToFile(aaaFile, steamerPlayer + "\n", true);
                                break;
                        }
                    }
                }
            }
        }
    }

    public void writeAbbreviatedCSV() throws FileNotFoundException, IOException {
        String[] levels = {"a.csv", "a+.csv", "aa.csv", "aaa.csv"};
        for (String level : levels) {
            Reader reader = new InputStreamReader(new BOMInputStream(new FileInputStream(level)), "UTF-8");

            CSVPrinter printer = new CSVPrinter(new FileWriter("abbreviated/" + level), CSVFormat.EXCEL);
            printer.printRecord("Name", "Team", "ISO", "BB%", "K%", "AVG", "OBP", "SLG", "OPS", "wRC+");
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(reader);
            int i = 1;
            for (CSVRecord record : records) {
                String name = record.get("Name");
                String team = record.get("Team");
                String iso = record.get("ISO");
                String bb = record.get("BB%");
                String k = record.get("K%");
                String avg = record.get("AVG");
                String obp = record.get("OBP");
                String slg = record.get("SLG");
                String ops = record.get("OPS");
                String wrc = record.get("wRC+");
                printer.printRecord(name, team, iso, bb, k, avg, obp, slg, ops, wrc);
                i++;
            }
        }
    }

    void separate(String steamerFileName, String minorsFileName) throws FileNotFoundException, IOException {
        System.out.println("steamerFileName=" + steamerFileName + " minorsFileName=" + minorsFileName);

        steamerFile = new File(steamerFileName);
        minorsFile = new File(minorsFileName);

        if (!steamerFile.exists()) {
            throw new FileNotFoundException("Could not find the steamer csv file at " + steamerFileName);
        }

        if (!minorsFile.exists()) {
            throw new FileNotFoundException("Could not find the minors csv file at " + minorsFileName);
        }

        readInFiles();
        writeAbbreviatedCSV();
    }
}