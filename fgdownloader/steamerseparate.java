///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS commons-io:commons-io:2.13.0
//DEPS org.apache.commons:commons-csv:1.10.0

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.FileUtils;;

@CommandLine.Command
public class steamerseparate {

    @Parameters(index = "0", description = "The steamer csv file to separate", defaultValue = "data/steamerhitting.csv")
    String steamerFile;

    @Parameters(index = "1", description = "The steamer csv file to separate", defaultValue = "data/steamerpitching.csv")
    String steamerPitchingFile;

    @Parameters(index = "2", description = "The minor league csv file", defaultValue = "data/minors.csv")
    String minorsFile;

    @Parameters(index = "3", description = "The minor league csv file", defaultValue = "data/minorspitching.csv")
    String minorsPitchingFile;

    private SeparationService separationService = new SeparationService();

    public void separate() {
        try {
            separationService.separate(steamerFile, minorsFile);
            separationService.pitchingSeparate(steamerPitchingFile, minorsPitchingFile);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main (String...args) {
        steamerseparate ss = new steamerseparate();
        int exitCode = new CommandLine(ss).execute(args);
        ss.separate();
        System.exit(exitCode);
    }
}

class SeparationService {
    private File steamerFile;
    private File minorsFile;
    private File pitchingSteamerFile;
    private File pitchingMinorsFile;

    HashMap<String, String> minorsPlayers = new HashMap<String, String>();
    HashMap<String, String> pitchingMinorsPlayers = new HashMap<String, String>();


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
   public void pitchingReadInFiles() throws IOException {
        List<String> pitchingSteamerContents = FileUtils.readLines(pitchingSteamerFile, "UTF-8");
        List<String> pitchingMinorsContents = FileUtils.readLines(pitchingMinorsFile, "UTF-8");

        System.out.println("pitchingMinorsFile " + pitchingMinorsFile.getAbsolutePath());
        System.out.println("pitchingSteamerFile " + pitchingSteamerFile.getAbsolutePath());

        for (String minorsPlayer : pitchingMinorsContents) {
            String[] fields = minorsPlayer.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            String levels = fields[2];
            String id = fields[fields.length-1];

            levels = levels.replaceAll("\"", "");
            if (levels.contains(",")) {
                String[] templevels = levels.split(",");
                levels = templevels[templevels.length-1];
            }

            //System.out.println("id " + id + " fields " + Arrays.toString(fields) + " levels " + levels);

            pitchingMinorsPlayers.put(id, levels);
        }

        boolean header = true;
        File minors = new File("steamerpitchingminors.csv");
        if (minors.exists()) {
            minors.delete();
        }
        for (String steamerPlayer : pitchingSteamerContents) {
            if (header) {
                FileUtils.writeStringToFile(minors, steamerPlayer);
                header = false;
            } else {

                String[] fields = steamerPlayer.split(",");
                String id = fields[fields.length-1];

                if (id.startsWith("\"sa")) {
                    FileUtils.writeStringToFile(minors, steamerPlayer);
                }
            }
        }

        File aFile = new File("aPitching.csv");
        aFile.delete();
        File aplusFile = new File("a+Pitching.csv");
        aplusFile.delete();
        File aaFile = new File("aaPitching.csv");
        aaFile.delete();
        File aaaFile = new File("aaaPitching.csv");
        aaaFile.delete();
        header = true;
        for (String steamerPlayer : pitchingSteamerContents) {
            if (header) {
                FileUtils.writeStringToFile(aFile, steamerPlayer + "\n");
                FileUtils.writeStringToFile(aplusFile, steamerPlayer + "\n");
                FileUtils.writeStringToFile(aaFile, steamerPlayer + "\n");
                FileUtils.writeStringToFile(aaaFile, steamerPlayer + "\n");
                header = false;
            } else {

                String[] fields = steamerPlayer.split(",");
                String id = fields[fields.length-1];


                if (id.startsWith("\"sa")) {
                    String level = pitchingMinorsPlayers.get(id);

                    //System.out.println("id " + id + " fields " + Arrays.toString(fields) + " level " + level);

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
            printer.close(true);
        }
    }

    public void writePitchingAbbreviatedCSV() throws FileNotFoundException, IOException {
        String[] levels = {"aPitching.csv", "a+Pitching.csv", "aaPitching.csv", "aaaPitching.csv"};
        for (String level : levels) {
            Reader reader = new InputStreamReader(new BOMInputStream(new FileInputStream(level)), "UTF-8");

            CSVPrinter printer = new CSVPrinter(new FileWriter("abbreviated/" + level), CSVFormat.EXCEL);
            printer.printRecord("Name", "Team", "IP", "K/9", "BB/9", "K-BB%", "WHIP", "ERA", "FIP");
            //printer.printRecord("Name", "Team", "IP", "WHIP", "ERA", "FIP");

            CSVParser parser = CSVParser.parse(reader, CSVFormat.RFC4180.withFirstRecordAsHeader());
            int i = 1;

            //System.out.println(parser.getHeaderMap().keySet());

            for (CSVRecord record : parser.getRecords()) {
                String name = record.get("Name");
                String team = record.get("Team");
                String ip = record.get("IP");
                String bb = record.get("BB/9"); // BB%
                String k = record.get("K/9"); // K%
                String whip = record.get("WHIP");
                String era = record.get("ERA");
                String fip = record.get("FIP");

                double kbb = (Double.valueOf(k) * 20/7.5) - (Double.valueOf(bb) * 20/7.5);

                printer.printRecord(name, team, ip, k, bb, kbb, whip, era, fip);
                i++;
            }
            printer.close(true);
        }
    }

    void separate(String steamerFileName, String minorsFileName) throws FileNotFoundException, IOException {

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

    void pitchingSeparate(String steamerFileName, String minorsFileName) throws FileNotFoundException, IOException {

        pitchingSteamerFile = new File(steamerFileName);
        pitchingMinorsFile = new File(minorsFileName);

        if (!pitchingSteamerFile.exists()) {
            throw new FileNotFoundException("Could not find the steamer csv file at " + steamerFileName);
        }

        if (!pitchingMinorsFile.exists()) {
            throw new FileNotFoundException("Could not find the minors csv file at " + minorsFileName);
        }

        pitchingReadInFiles();
        try {
        writePitchingAbbreviatedCSV();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}