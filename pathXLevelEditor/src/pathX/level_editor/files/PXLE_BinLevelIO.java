package pathX.level_editor.files;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pathX.level_editor.model.PXLE_Level;
import pathX.level_editor.model.Intersection;
import pathX.level_editor.model.PXLE_Model;
import pathX.level_editor.model.Road;

/**
 * This class serves as a does the reading and writing of levels
 * to and from XML files.
 * 
 * @author  Richard McKenna
 */
public class PXLE_BinLevelIO implements PXLE_LevelIO
{
    /**
     * Constructor for making our importer/exporter. Note that it
     * initializes the XML utility for processing XML files and it
     * sets up the schema for use.
     */
    public PXLE_BinLevelIO()
    {
    }
    
    /**
     * Reads the level data found in levelFile into levelToLoad.
     */
    @Override
    public boolean loadLevel(File levelFile, PXLE_Model model)
    {
        try
        {
            // WE'LL FILL IN SOME OF THE LEVEL OURSELVES
            PXLE_Level levelToLoad = model.getLevel();
            levelToLoad.reset();

            // LET'S USE A FAST LOADING TECHNIQUE. WE'LL LOAD ALL OF THE
            // BYTES AT ONCE INTO A BYTE ARRAY, AND THEN PICK THAT APART.
            // THIS IS FAST BECAUSE IT ONLY HAS TO DO FILE READING ONCE
            byte[] bytes = new byte[Long.valueOf(levelFile.length()).intValue()];
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            FileInputStream fis = new FileInputStream(levelFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            
            // HERE IT IS, THE ONLY READY REQUEST WE NEED
            bis.read(bytes);
            bis.close();
            
            // NOW WE NEED TO LOAD THE DATA FROM THE BYTE ARRAY
            DataInputStream dis = new DataInputStream(bais);
            
            // NOTE THAT WE NEED TO LOAD THE DATA IN THE SAME
            // ORDER AND FORMAT AS WE SAVED IT
            
            // FIRST READ THE ALGORITHM NAME TO USE FOR THE LEVEL
            String levelName = dis.readUTF();
            levelToLoad.setLevelName(levelName);
            
            // THEN GET THE BACKGROUND IMAGE NAME
            String bgImageName = dis.readUTF();
            model.updateBackgroundImage(bgImageName);
            
            // THEN LET'S LOAD THE LIST OF ALL THE INTERSECTIONS
            loadIntersectionsList(dis, levelToLoad);
            ArrayList<Intersection> intersections = levelToLoad.getIntersections();
            
            // AND NOW CONNECT ALL THE REGIONS TO EACH OTHER
            loadRoadsList(dis, levelToLoad);
            
            // LOAD THE START INTERSECTION
            int startId = dis.readInt();
            String startImageName = dis.readUTF();
            Intersection startingIntersection = intersections.get(startId);
            levelToLoad.setStartingLocation(startingIntersection);
            model.updateStartingLocationImage(startImageName);
            
            // LOAD THE DESTINATION
            int destId = dis.readInt();
            String destImageName = dis.readUTF();
            levelToLoad.setDestination(intersections.get(destId));
            model.updateDestinationImage(destImageName);
            
            // LOAD THE MONEY
            int money = dis.readInt();
            levelToLoad.setMoney(money);
            
            // LOAD THE NUMBER OF POLICE
            int numPolice = dis.readInt();
            levelToLoad.setNumPolice(numPolice);
            
            // LOAD THE NUMBER OF BANDITS
            int numBandits = dis.readInt();
            levelToLoad.setNumBandits(numBandits);
            
            // LOAD THE NUMBER OF ZOMBIES
            int numZombies = dis.readInt();
            levelToLoad.setNumZombies(numZombies);            
        }
        catch(IOException e)
        {
            // LEVEL DIDN'T LOAD PROPERLY
            return false;
        }
        // LEVEL LOADED PROPERLY
        return true;
    }
    
    // PRIVATE HELPER METHOD FOR LOADING INTERSECTIONS INTO OUR LEVEL
    private void loadIntersectionsList( DataInputStream dis, PXLE_Level levelToLoad)
            throws IOException
    {
        // GET THE NUMBER OF INTERSECTIONS
        int numIntersections = dis.readInt();
        
        // FIRST GET THE INTERSECTIONS LIST
        ArrayList<Intersection> intersections = levelToLoad.getIntersections();
        
        // AND THEN GO THROUGH AND ADD ALL THE LISTED REGIONS
        for (int i = 0; i < numIntersections; i++)
        {
            // GET THEIR DATA FROM THE DOC
            int x = dis.readInt();
            int y = dis.readInt();
            boolean isOpen = dis.readBoolean();
            
            // NOW MAKE AND ADD THE INTERSECTION
            Intersection newIntersection = new Intersection(x, y);
            newIntersection.open = isOpen;
            intersections.add(newIntersection);
        }
    }

    // PRIVATE HELPER METHOD FOR LOADING ROADS INTO OUR LEVEL
    private void loadRoadsList( DataInputStream dis, PXLE_Level levelToLoad)
            throws IOException
    {
        // FIRST GET THE ROADS AND INTERSECTIONS LISTS
        ArrayList<Road> roads = levelToLoad.getRoads();
        ArrayList<Intersection> intersections = levelToLoad.getIntersections();

        // THEN GET THE NUMBER OF ROADS
        int numRoads = dis.readInt();

        // AND THEN GO THROUGH AND ADD ALL THE LISTED ROADS
        for (int i = 0; i < numRoads; i++)
        {
            // GET THEIR DATA FROM THE DOC
            int int_id1 = dis.readInt();
            int int_id2 = dis.readInt();
            boolean oneWay = dis.readBoolean();
            int speedLimit = dis.readInt();
            
            // NOW MAKE AND ADD THE ROAD
            Road newRoad = new Road();
            newRoad.setNode1(intersections.get(int_id1));
            newRoad.setNode2(intersections.get(int_id2));
            newRoad.setOneWay(oneWay);
            newRoad.setSpeedLimit(speedLimit);
            roads.add(newRoad);
        }
    }
    
    /**
     * This method saves the level currently being edited to the levelFile. Note
     * that it will be saved as an .xml file, which is an XML-format that will
     * conform to the schema.
     */
    @Override
    public boolean saveLevel(File levelFile, PXLE_Level levelToSave)
    {
        try
        {
            // WE'LL WRITE EVERYTHING IN BINARY. NOTE THAT WE
            // NEED TO MAKE SURE WE SAVE THE DATA IN THE SAME
            // FORMAT AND ORDER WITH WHICH WE READ IT LATER
            FileOutputStream fos = new FileOutputStream(levelFile);
            DataOutputStream dos = new DataOutputStream(fos);

            // FIRST SAVE THE LEVEL NAME
            String levelName = levelFile.getName();
            levelName = levelName.substring(0, levelName.indexOf("."));
            dos.writeUTF(levelName);
            
            // THEN SAVE THE BACKGROUND IMAGE NAME
            String bgImageName = levelToSave.getBackgroundImageFileName();
            dos.writeUTF(bgImageName);
            
            // THEN LET'S SAVE THE LIST OF ALL THE INTERSECTIONS,
            // FIRST THE NUMBER OF INTERSECTIONS
            ArrayList<Intersection> intersections = levelToSave.getIntersections();
            int numIntersections = intersections.size();
            dos.writeInt(numIntersections);
            for (int i = 0; i < numIntersections; i++)
            {
                Intersection intersection = intersections.get(i);
                dos.writeInt(intersection.x);
                dos.writeInt(intersection.y);
                dos.writeBoolean(intersection.open);
            }
            
            // AND NOW SAVE ALL THE ROADS
            ArrayList<Road> roads = levelToSave.getRoads();
            int numRoads = roads.size();
            dos.writeInt(numRoads);
            for (int i = 0; i < numRoads; i++)
            {
                Road road = roads.get(i);
                dos.writeInt(intersections.indexOf(road.getNode1()));
                dos.writeInt(intersections.indexOf(road.getNode2()));
                dos.writeBoolean(road.isOneWay());
                dos.writeInt(road.getSpeedLimit());
            }
            
            // SAVE THE START INTERSECTION
            dos.writeInt(intersections.indexOf(levelToSave.getStartingLocation()));
            dos.writeUTF(levelToSave.getStartingLocationImageFileName());
           
            // LOAD THE DESTINATION
            dos.writeInt(intersections.indexOf(levelToSave.getDestination()));
            dos.writeUTF(levelToSave.getDestinationImageFileName());
            
            // SAVE THE MONEY
            dos.writeInt(levelToSave.getMoney());
            
            // SAVE THE NUMBER OF POLICE
            dos.writeInt(levelToSave.getNumPolice());
            
            // SAVE THE NUMBER OF BANDITS
            dos.writeInt(levelToSave.getNumBandits());
            
            // SAVE THE NUMBER OF ZOMBIES
            dos.writeInt(levelToSave.getNumZombies());

            // SUCCESS
            return true;
        }
        catch(IOException ex)
        {
            // SOMETHING WENT WRONG
            return false;
        }    
    }   
    
    // THIS HELPER METHOD BUILDS ELEMENTS (NODES) FOR US TO HELP WITH
    // BUILDING A Doc WHICH WE WOULD THEN SAVE TO A FILE.
    private Element makeElement(Document doc, Element parent, String elementName, String textContent)
    {
        Element element = doc.createElement(elementName);
        element.setTextContent(textContent);
        parent.appendChild(element);
        return element;
    }
}