package Thesis.Step1_;

import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Geometry;

import java.awt.*;
import java.io.*;
import java.util.*;

import static Thesis.Step0_Preparation.FilterPlans.readShapeFile;


public class ModifyClass {

    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_2011_onlyAuto_inMUC.xml";
    private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_2011_onlyAuto_inMUC_inclGarages1.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static final String Garages = "C:/matsimfiles/input/testgarages.csv";
    private static final String DISTRICTS = "C:/matsimfiles/input/MunichDistricts.shp";
//    private static final String garagePath = "C:/matsimfiles/output/Garages.xml";    //The output file of demand generation
//    private static final String COUNTIES = "C:/matsimfiles/input/lkr_ex.shp";

    private static Population popInitial;
    private static Population popModified;
    private static Scenario scenario;
    private static Scenario scenarioNew;

    private static final Map<Integer,Integer> garageListCapacity = new HashMap<>();
    private static final Map<Integer,Double> garageListX = new HashMap<>();
    private static final Map<Integer,Double> garageListY = new HashMap<>();
    private static final Map<Integer, Integer> garageDistricts = new HashMap<>();
    private static final Map<String, Double> garageDistances = new HashMap<>();

    public ModifyClass() {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);
        scenario = ScenarioUtils.loadScenario(config);
        popInitial = scenario.getPopulation();
        scenarioNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        popModified = scenarioNew.getPopulation();
        new MatsimNetworkReader(scenarioNew.getNetwork()).readFile(Network);

    }

    public static void main(String[] args) throws IOException {

        ModifyClass mp = new ModifyClass();
        mp.readGarageCSV();
        mp.addGarage2Plan(popInitial, scenarioNew); //scenario deleted

    }

    public void readGarageCSV() throws IOException {

        System.out.println("Start reading list of garages");

        BufferedReader garageReader = new BufferedReader(new FileReader(Garages)); // garagePath
        garageReader.readLine();

        String line;
        while ((line = garageReader.readLine()) != null) {

            int garageNo = Integer.parseInt((line.split(",")[0]));
            int garageCapacity = Integer.parseInt((line.split(",")[1]));
            double coordX = Double.parseDouble((line.split(",")[2]));
            double coordY = Double.parseDouble((line.split(",")[3]));
            int districts = Integer.parseInt(line.split(",")[4]);

            garageListCapacity.put(garageNo, garageCapacity);
            garageListX.put(garageNo, coordX);
            garageListY.put(garageNo, coordY);
            garageDistricts.put(garageNo, districts);

        }

        System.out.println("Garage list: " + garageListX.get(1));
        System.out.println("Garage list: " + garageListY); //.size()

    }

    public Coord selectGarage(int idGarage){

        System.out.println(garageListX);
        double x = garageListX.get(idGarage);
        double y = garageListY.get(idGarage);

        int newCapacity = garageListCapacity.get(idGarage)-1;
        garageListCapacity.put(idGarage, newCapacity); //

        return new Coord(x,y);
    }

    public Coord chooseGarageByDistance (String personTag, Coord HomeCoord) {
        int garageSize = garageListX.size(); //No. of garages in the list
        ArrayList<Double> distances = new ArrayList<>();
        double distance;
        Coord coordGa; //Coord of the selected garage
        double minDistance;
        int garageMinDis = 0;

        for (int i = 1; i<garageSize+1; i++) {
            System.out.println(i);

            if (garageListCapacity.get(i) > 0) {
                coordGa = selectGarage(i); //
                distance = NetworkUtils.getEuclideanDistance(HomeCoord,coordGa);
                distances.add(i-1, distance);
            } else
            {
                distances.add(i-1, 9999999.9); //
            }
        }
 //       System.out.println(distances);
        garageDistances.put(personTag, Collections.min(distances));

        return selectGarage(distances.indexOf(Collections.min(distances))+1);

       // System.out.println("Distance list: " + distances);  // print a list of all distances
    }

/*    public Coord chooseGarageByDistrict (Coord HomeCoord) {
        Map<String, com.vividsolutions.jts.geom.Geometry> shapeMap;
        shapeMap = readShapeFile(DISTRICTS, "Borough");
        int district = 0;
        Point p1;
        for (int i = 1; i <= shapeMap.size(); i++) {
            Geometry munich = (Geometry) shapeMap.get(i);
            p1 = MGC.xy2Point(HomeCoord.getX(), HomeCoord.getY());
            if (munich.contains((DirectPosition) p1)) {
                district = i;
            }
        }
        for (int i = 1; i <= garageDistricts.size(); i++) {
            int garageId = garageDistricts.get(i);

        }
        return selectGarage(district);
        //break; // return GarageID

    }*/

    public void addGarage2Plan(Population popInitial, Scenario scenarioNew){

        for (Person person : popInitial.getPersons().values()){
            Id<Person> personId = Id.createPersonId(person.getId()); //get initial person id to new
            //System.out.println(personId);

            Plan plan = person.getSelectedPlan();

            double random = Math.random();
            int legSize = TripStructureUtils.getLegs(plan).size();
            //int actSize = plan.getPlanElements().size() - legSize;

            //System.out.println(legSize);
            //System.out.println(actSize);

            Person personNew = scenarioNew.getPopulation().getFactory().createPerson(personId);
            Plan planNew = scenarioNew.getPopulation().getFactory().createPlan();


            double endHomeTime1;
            Coord HomeLocation;
            Coord curGarage;

            if (random>=0.7) {
                //Legloop1:
                for (Leg leg : TripStructureUtils.getLegs(plan)){
                    endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                    HomeLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();

                    curGarage = chooseGarageByDistance(personId+"departure ", HomeLocation);

                    Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                    garage1.setEndTime(endHomeTime1-garageDistances.get(personId+"departure ")/40);

                    planNew.addActivity(garage1);

//                  System.out.print("davor ");
                    System.out.println(legSize);

                    Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                    planNew.addLeg(pickUp);
                    break;
                }

                int oldLegCount=1;
                //Activityloop:
                for (Leg leg : TripStructureUtils.getLegs(plan)){

                    Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                    Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                    actNew.setEndTime(actOld.getEndTime());
                    planNew.addActivity(actNew);

                    planNew.addLeg(leg);

//                System.out.print("inzwischen ");
                    System.out.println(TripStructureUtils.getLegs(planNew).size());

                    if(oldLegCount == legSize) {

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        actNew2.setEndTime(actOld2.getEndTime());
                        planNew.addActivity(actNew2);

                        Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(dropOff);

                        Coord DestinationLocation;
                        DestinationLocation = actNew2.getCoord();
//                    System.out.println(DestinationLocation);
                        Coord coord2 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                        Activity garage2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
                        planNew.addActivity(garage2);

                    }
                     oldLegCount +=1;
                }
            } else {

                int oldLegCount = 1;
                //Activityloop:
                for (Leg leg : TripStructureUtils.getLegs(plan)) {

                    Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                    Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                    actNew.setEndTime(actOld.getEndTime());
                    planNew.addActivity(actNew);

                    planNew.addLeg(leg);

//                System.out.print("inzwischen ");
                    System.out.println(TripStructureUtils.getLegs(planNew).size());

                    if (oldLegCount == legSize) {

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        actNew2.setEndTime(actOld2.getEndTime());
                        planNew.addActivity(actNew2);

                    }
                    oldLegCount += 1;
                }
            }

/*            //Legloop1:
            for (Leg leg : TripStructureUtils.getLegs(plan)){
                endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                HomeLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
                break;

            Coord curGarage;
            if (random>=0.7) {
                curGarage = chooseGarageByDistance(personId+"departure ", HomeLocation);
            } else {
                curGarage = HomeLocation;
            }

            //Coord curGarage = chooseGarageByDistance(personId+"departure ", HomeLocation);

            Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);

            if (random>=0.7) {
                garage1.setEndTime(endHomeTime1-garageDistances.get(personId+"departure ")/40);
            } else {
                garage1.setEndTime(endHomeTime1);
            }

            //garage1.setEndTime(endHomeTime1-garageDistances.get(personId+"departure ")/40);
            planNew.addActivity(garage1);

//            System.out.print("davor ");
            System.out.println(legSize);

            Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
            planNew.addLeg(pickUp);*/

//            System.out.print("danach ");
            /*System.out.println(TripStructureUtils.getLegs(planNew).size());

            int oldLegCount=1;
            //Activityloop:
            for (Leg leg : TripStructureUtils.getLegs(plan)){

                Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                actNew.setEndTime(actOld.getEndTime());
                planNew.addActivity(actNew);

                planNew.addLeg(leg);

//                System.out.print("inzwischen ");
                System.out.println(TripStructureUtils.getLegs(planNew).size());

                if(oldLegCount == legSize){

                    Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                    Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                    actNew2.setEndTime(actOld2.getEndTime());
                    planNew.addActivity(actNew2);

                    if (random>=0.7) {

                        Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(dropOff);

                        Coord DestinationLocation;
                        DestinationLocation = actNew2.getCoord();
//                    System.out.println(DestinationLocation);
                        Coord coord2 = chooseGarageByDistance(personId+"returning ", DestinationLocation);
                        Activity garage2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
                        planNew.addActivity(garage2);

                    } else {
                        break;
                    }
                }
                oldLegCount +=1;
            }*/

            personNew.addPlan(planNew);
            scenarioNew.getPopulation().addPerson(personNew);

        }

        //Write the population file to specified folder
        PopulationWriter pw = new PopulationWriter(scenarioNew.getPopulation(),scenarioNew.getNetwork());
        pw.write(PLANSFILEOUTPUT);


    }


}