package Thesis.Step0_Preparation;

import Thesis.Step1_.ModifyClass;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FilterPlans2 {
    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_2011_onlyAuto_inMUC.xml";
    private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_2011_onlyTowardsGarageTrips.xml";
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

    public FilterPlans2() {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);
        scenario = ScenarioUtils.loadScenario(config);
        popInitial = scenario.getPopulation();

        scenarioNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        popModified = scenarioNew.getPopulation();
        new MatsimNetworkReader(scenarioNew.getNetwork()).readFile(Network);

    }

    public static void main(String[] args) throws IOException {

        FilterPlans2 mp = new FilterPlans2();
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

        double x = garageListX.get(idGarage);
        double y = garageListY.get(idGarage);
        return new Coord(x,y);
    }

    public Coord chooseGarageByDistance (Coord HomeCoord) {
        int garageSize = garageListX.size(); //No. of garages in the list
        ArrayList<Double> distances = new ArrayList<>();
        double distance;
        Coord coordGa; //Coord of the selected garage
        double minDistance;
        int garageMinDis = 0;

        for (int i = 1; i<garageSize+1; i++) {
            System.out.println(i);
            coordGa = selectGarage(i); // How to get the coordinates?
            distance = NetworkUtils.getEuclideanDistance(HomeCoord,coordGa);
            distances.add(i-1, distance);
        }
        System.out.println(distances);
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
            Id<Person> personId = Id.createPersonId(person.getId());
            Plan plan = person.getSelectedPlan();

            int legSize = TripStructureUtils.getLegs(plan).size();

            Person personNew = scenarioNew.getPopulation().getFactory().createPerson(personId);
            Plan planNew = scenarioNew.getPopulation().getFactory().createPlan();


            double endHomeTime1 = 0;
            Coord HomeLocation = new Coord();
            //Legloop1:
            for (Leg leg : TripStructureUtils.getLegs(plan)){
                endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                HomeLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
                break;
            }
            Coord curGarage = chooseGarageByDistance(HomeLocation);

            Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
            garage1.setEndTime(endHomeTime1);
            planNew.addActivity(garage1);

            Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
            planNew.addLeg(pickUp);


            int oldLegCount=1;
            //Activityloop:
            for (Leg leg : TripStructureUtils.getLegs(plan)){

                Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                actNew.setEndTime(actOld.getEndTime());
                planNew.addActivity(actNew);

//                planNew.addLeg(leg);

                if(oldLegCount == legSize){

                    Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                    Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                    actNew2.setEndTime(actOld2.getEndTime());
                    planNew.addActivity(actNew2);

                    Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                    planNew.addLeg(dropOff);

                    Coord DestinationLocation;
                    DestinationLocation = actNew2.getCoord();
                    Coord coord2 = chooseGarageByDistance(DestinationLocation);
                    Activity garage2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
                    planNew.addActivity(garage2);

                }
                oldLegCount +=1;
            }

            personNew.addPlan(planNew);
            scenarioNew.getPopulation().addPerson(personNew);

        }

        //Write the population file to specified folder
        PopulationWriter pw = new PopulationWriter(scenarioNew.getPopulation(),scenarioNew.getNetwork());
        pw.write(PLANSFILEOUTPUT);

    }


}
