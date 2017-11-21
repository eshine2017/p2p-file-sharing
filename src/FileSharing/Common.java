package FileSharing;

import java.io.*;

/**
 * read in Common.cfg, initialize the configurations
 */
public class Common {

    /** Common.cfg configurations */
    public static int NumberOfPreferredNeighbors;
    public static int UnchokingInterval;
    public static int OptimisticUnchokingInterval;
    public static String FileName;
    public static int FileSize;
    public static int PieceSize;

    /** read in configurations */
    public Common() {
        setConfig();
//        System.out.println(NumberOfPreferredNeighbors + " " + UnchokingInterval + " " + OptimisticUnchokingInterval
//                + " " + FileName + " " + FileSize + " " + PieceSize);
    }

    /* read Common.cfg and set all the specified configurations */
    private void setConfig() {

        try {
            String filePath = System.getProperty("user.dir") + File.separator;
            BufferedReader br = new BufferedReader(new FileReader(filePath + "Common.cfg"));

            // read in configurations one by one
            String line = br.readLine();
            String[] words = line.split(" ");
            NumberOfPreferredNeighbors = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            UnchokingInterval = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            OptimisticUnchokingInterval = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            FileName = words[1];

            line = br.readLine();
            words = line.split(" ");
            FileSize = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            PieceSize = Integer.parseInt(words[1]);

            br.close();

        } catch (FileNotFoundException e) {
            System.err.println("Common.cfs does not exist!");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
