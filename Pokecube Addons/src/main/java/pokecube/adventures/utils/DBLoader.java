package pokecube.adventures.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import pokecube.adventures.PokecubeAdv;
import pokecube.adventures.entity.trainers.TypeTrainer;
import pokecube.core.database.Database;

public class DBLoader
{
    public static boolean      FORCECOPY  = true;
    private static String      DBLOCATION = "/assets/pokecube_adventures/database/";
    public static String       CONFIGLOC  = "";

    private static PrintWriter out;

    private static FileWriter  fwriter;

    static String              female     = "female:,Alice,Bridget,Carrie,Connie,Dana,Ellen,Krise,Laura,Linda,Michelle,Shannon,Gina,Brooke,Cindy,Debra,Edna,Erin,Heidi,Hope,Liz,Sharon,Tanya,Tiffany,Beth,Carol,Emma,Fran,Cara,Jenn,Kate,Cybil,Gwen,Irene,Kelly,Joyce,Lola,Megan,Quinn,Reena,Valerie";

    static String              male       = "male:,Anthony,Bailey,Benjamin,Daniel,Erik,Jim,Kenny,Leonard,Michael,Parry,Philip,Russell,Sidney,Tim,Timothy,Wade,Al,Arnie,Benny,Don,Doug,Ed,Josh,Ken,Rob,Joey,Mikey,Albert,Gordon,Ian,Jason,Jimmy,Owen,Samuel,Warren,Aaron,Allen,Blake,Brian,Abe";

    public static void checkConfigFiles(FMLPreInitializationEvent evt)
    {
        File file = evt.getSuggestedConfigurationFile();
        String seperator = System.getProperty("file.separator");

        String folder = file.getAbsolutePath();
        String name = file.getName();
        folder = folder.replace(name, "pokecube" + seperator + "trainers" + seperator + "");

        CONFIGLOC = folder;
        File temp = new File(CONFIGLOC);
        if (!temp.exists())
        {
            temp.mkdirs();
        }
        copyDatabaseFile("trainers.xml");
        copyDatabaseFile("trades.xml");
        copyDatabaseFile("names.csv");
        DBLOCATION = CONFIGLOC;
        return;
    }

    private static ArrayList<ArrayList<String>> getRows(String file)
    {
        InputStream res = (DBLoader.class).getResourceAsStream(file);
        ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try
        {

            br = new BufferedReader(new InputStreamReader(res));
            int n = 0;
            while ((line = br.readLine()) != null)
            {

                String[] row = line.split(cvsSplitBy);
                rows.add(new ArrayList<String>());
                for (int i = 0; i < row.length; i++)
                {
                    rows.get(n).add(row[i]);
                }
                n++;
            }

        }
        catch (FileNotFoundException e)
        {
        }
        catch (NullPointerException e)
        {

            try
            {
                FileReader temp = new FileReader(new File(file));
                br = new BufferedReader(temp);
                int n = 0;
                while ((line = br.readLine()) != null)
                {

                    String[] row = line.split(cvsSplitBy);
                    rows.add(new ArrayList<String>());
                    for (int i = 0; i < row.length; i++)
                    {
                        rows.get(n).add(row[i]);
                    }
                    n++;
                }
                temp.close();
            }
            catch (FileNotFoundException e1)
            {
                System.err.println("Missing a Database file " + file);
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }

            // e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return rows;
    }

    public static void preInit(FMLPreInitializationEvent evt)
    {
        checkConfigFiles(evt);
    }

    public static void load()
    {
        try
        {
            File file;
            TrainerEntryLoader.loadDatabase(file = new File(DBLOCATION + "trainers.xml"));
            TrainerEntryLoader.makeEntries(file);
            TradeEntryLoader.loadDatabase(file = new File(DBLOCATION + "trades.xml"));
            TradeEntryLoader.makeEntries(file);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        loadNames();
        TypeTrainer.postInitTrainers();
    }

    public static void loadNames()
    {
        String s = PokecubeAdv.CUSTOMTRAINERFILE.replace("trainers.xml", "names.csv");

        File file = new File(s);
        if (!file.exists())
        {
            try
            {
                writeDefaultNames(file);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        ArrayList<ArrayList<String>> rows = getRows(s);

        for (ArrayList<String> row : rows)
        {
            if (row.isEmpty()) continue;
            String name = row.get(0);
            if (name.equalsIgnoreCase("female:"))
            {
                for (int i = 1; i < row.size(); i++)
                {
                    TypeTrainer.femaleNames.add(row.get(i));
                }
                continue;
            }
            if (name.equalsIgnoreCase("male:"))
            {
                for (int i = 1; i < row.size(); i++)
                {
                    TypeTrainer.maleNames.add(row.get(i));
                }
                continue;
            }
        }
    }

    private static void copyDatabaseFile(String name)
    {
        File temp1 = new File(CONFIGLOC + name);
        if (temp1.exists() && !FORCECOPY)
        {
            System.out.println(" Not Overwriting old database " + name);
            return;
        }
        ArrayList<String> rows = Database.getFile(DBLOCATION + name);
        try
        {
            fwriter = new FileWriter(CONFIGLOC + name);
            out = new PrintWriter(fwriter);
            for (int i = 0; i < rows.size(); i++)
                out.println(rows.get(i));
            out.close();
            fwriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void writeDefaultNames(File file) throws IOException
    {
        fwriter = new FileWriter(file);
        out = new PrintWriter(fwriter);
        out.println(female);
        out.println(male);
        out.close();
        fwriter.close();
    }

}
