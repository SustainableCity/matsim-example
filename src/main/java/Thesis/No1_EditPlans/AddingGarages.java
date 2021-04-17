
package Thesis.No1_EditPlans;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AddingGarages {

    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_CarCom_5.xml";
    private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_CarComGarages1_5.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static final String Garages = "C:/matsimfiles/input/testgarages1.csv";
    private static final String DISTRICTS = "C:/matsimfiles/input/MunichDistricts.shp";
    //    private static final String garagePath = "C:/matsimfiles/output/Garages.xml";    //The output file of demand generation
    private static final String COUNTIES = "C:/matsimfiles/input/lkr_ex.shp";

    private static Population popInitial;
    private static Population popModified;
    private static Scenario scenario;
    private static Scenario scenarioNew;

    private static final Map<Integer, Integer> garageListCapacity = new HashMap<>(40);
    private static final Map<Integer, Double> garageListX = new HashMap<>(40);
    private static final Map<Integer, Double> garageListY = new HashMap<>(40);
    private static final Map<Integer, Integer> garageDistricts = new HashMap<>();
    private static final Map<String, Double> garageDistances = new HashMap<>(40);
    int PersonsWithinMunich = 0;
    int CommutersStartingInMunich = 0;
    int CommutersEndingInMunich = 0;
    int PersonsWithinMunichG = 0;
    int CommutersStartingInMunichG = 0;
    int CommutersEndingInMunichG = 0;

    public AddingGarages() {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);
        scenario = ScenarioUtils.loadScenario(config);
        popInitial = scenario.getPopulation();
        scenarioNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        popModified = scenarioNew.getPopulation();
        new MatsimNetworkReader(scenarioNew.getNetwork()).readFile(Network);

    }

    public static void main(String[] args) throws IOException {

        AddingGarages mp = new AddingGarages();
        mp.readGarageCSV();
        mp.addGarage2Plan(popInitial, scenarioNew); //scenario deleted

    }

    public void readGarageCSV() throws IOException {

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
    }

    public Coord selectGarage(int idGarage) {

        //System.out.println(garageListX);
        double x = garageListX.get(idGarage);
        double y = garageListY.get(idGarage);

        int newCapacity = garageListCapacity.get(idGarage) - 1;
        garageListCapacity.put(idGarage, newCapacity); //

        return new Coord(x, y);
    }

    //Read in shapefile
    public static Map<String, Geometry> readShapeFile(String filename, String attrString) {
        Map<String, Geometry> shapeMap = new HashMap<String, Geometry>();
        for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
            GeometryFactory geometryFactory = new GeometryFactory();
            WKTReader wktReader = new WKTReader(geometryFactory);
            Geometry geometry;
            try {
                geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
                shapeMap.put(ft.getAttribute(attrString).toString(), geometry);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return shapeMap;
    }

    public Coord chooseGarageByDistance(String personTag, Coord HomeCoord) {
        int garageSize = garageListX.size(); //No. of garages in the list
        ArrayList<Double> distances = new ArrayList<>();
        double distance;
        Coord coordGa; //Coord of the selected garage
        double minDistance;
        int garageMinDis = 0;

        for (int i = 1; i < garageSize + 1; i++) {
            System.out.println(i);

            if (garageListCapacity.get(i) > 0) {
                coordGa = selectGarage(i); //
                distance = NetworkUtils.getEuclideanDistance(HomeCoord, coordGa);
                distances.add(i - 1, distance);
            } else {
                distances.add(i - 1, 9999999.9); //
            }
        }

        garageDistances.put(personTag, Collections.min(distances));

        return selectGarage(distances.indexOf(Collections.min(distances)) + 1);
    }

/* public Coord chooseGarageByDistrict (Coord HomeCoord) {
            Map<String, Geometry> shapeMap;
            shapeMap = readShapeFile(DISTRICTS, "SCH");
            Geometry stadtteil = shapeMap.get("09162");

        if (stadtteil.contains(HomeCoord)){
            if (capacity OKAY) {
                return garageCoord of stadtteil;
            } else if {
                chooseGarageByDistance
            }
        } else if (stadtteil.contains(HomeCoord)){
            return garageCoord of stadtteil
        } else if (){

        } else {

        }




    }*/



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

    int countPlans;

    public void addGarage2Plan(Population popInitial, Scenario scenarioNew) {


        for (Person person : popInitial.getPersons().values()) {
            Id<Person> personId = Id.createPersonId(person.getId()); //get initial person id to new
            Plan plan = person.getSelectedPlan();
            Person personNew = scenarioNew.getPopulation().getFactory().createPerson(personId);
            Plan planNew = scenarioNew.getPopulation().getFactory().createPlan();

            double random = Math.random();
            int legSize = TripStructureUtils.getLegs(plan).size();
            //int actSize = plan.getPlanElements().size() - legSize;
            double endHomeTime1;
            Coord HomeLocation;
            Coord curGarage;
            Map<String, Geometry> shapeMap;
            shapeMap = readShapeFile(COUNTIES, "SCH");
            Geometry munich = shapeMap.get("09162");
            Point p1;
            Point p2;
            double x1;
            double y1;
            double x2;
            double y2;
            double time1;
            double time2;

            if (random >= 0.7) {
                int oldLegCount = 1;

                for (Leg leg : TripStructureUtils.getLegs(plan)) {
                    x1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getX();
                    y1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getY();
                    x2 = PlanUtils.getNextActivity(plan, leg).getCoord().getX();
                    y2 = PlanUtils.getNextActivity(plan, leg).getCoord().getY();
                    p1 = MGC.xy2Point(x1, y1);
                    p2 = MGC.xy2Point(x2, y2);

                    if (munich.contains(p1) && munich.contains(p2)) {
                        if (oldLegCount == 1) {
                            endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                            HomeLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();

                            curGarage = chooseGarageByDistance(personId + "departure ", HomeLocation);

                            Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            garage1.setEndTime(endHomeTime1 - garageDistances.get(personId + "departure ") / (30/3.6)); // determine average speed

                            planNew.addActivity(garage1);
                            //System.out.print("davor ");
                            Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(pickUp);

                            PersonsWithinMunichG = PersonsWithinMunichG + 1;

/*                            time1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                            time2 = PlanUtils.getNextActivity(plan, leg).getEndTime();
                            if (time2 - time1 >= 2.25 * 3600) {
                                Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                                Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                                actNew2.setEndTime(actOld2.getEndTime());
                                actNew2.setMaximumDuration(90);
                                planNew.addActivity(actNew2);

                                Leg dropOffI = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(dropOffI);

                                Coord DestinationLocation;
                                DestinationLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
                                Coord coord2 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                                Activity garageIntermediate = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
                                garageIntermediate.setEndTime(time2 - 90);
                                planNew.addActivity(garageIntermediate);

                                Leg pickUpI = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(pickUpI);
                            }*/

                        }

                        //Copy OldPlan and Adding Garage:
                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);
                        //System.out.print("inzwischen ");

                        if (oldLegCount == legSize) {
                            Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                            Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                            //actNew2.setEndTime(actOld2.getEndTime());
                            actNew2.setMaximumDuration(90);
                            planNew.addActivity(actNew2);

                            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(dropOff);

                            Coord DestinationLocation;
                            DestinationLocation = actNew2.getCoord();
                            Coord coord2 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                            Activity garage2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
                            planNew.addActivity(garage2);
                            break;
                        }
                        oldLegCount += 1;

                    } else if (munich.contains(p1) && !munich.contains(p2)) {

                        if (oldLegCount == 1) {
                            endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                            HomeLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();

                            curGarage = chooseGarageByDistance(personId + "departure ", HomeLocation);

                            Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            garage1.setEndTime(endHomeTime1 - garageDistances.get(personId + "departure ") / (30/3.6)); // determine average speed

                            planNew.addActivity(garage1);

                            Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(pickUp);

                            CommutersStartingInMunichG = CommutersStartingInMunichG + 1;
                            //System.out.println("StarteinMUCG ");
                        }

                        //Copy OldPlan:
                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);
                        //System.out.println("StarteinMUCH ");
                        planNew.addLeg(leg);

                        if (oldLegCount == legSize) {

                            Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                            Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                            //actNew2.setEndTime(actOld2.getEndTime());
                            actNew2.setMaximumDuration(90);
                            planNew.addActivity(actNew2);
                            break;
                        }
                        oldLegCount += 1;

                    } else {

                        //EndInMuc: Copy OldPlan and Adding Garage:

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);

                        if (oldLegCount == legSize) {

                            Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                            Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                            //actNew2.setEndTime(actOld2.getEndTime());
                            actNew2.setMaximumDuration(90);
                            planNew.addActivity(actNew2);

                            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(dropOff);

                            Coord DestinationLocation;
                            DestinationLocation = actNew2.getCoord();
                            Coord coord2 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                            Activity garage2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
                            planNew.addActivity(garage2);
                            //System.out.println("EndeinMUCG ");
                            CommutersEndingInMunichG = CommutersEndingInMunichG + 1;
                            break;
                        }
                        oldLegCount += 1;
                    }
                }

            } else {
                int oldLegCount = 1;
                //random <0.7 (only) Copy OldPlan:
                for (Leg leg : TripStructureUtils.getLegs(plan)) {
                    x1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getX();
                    y1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getY();
                    x2 = PlanUtils.getNextActivity(plan, leg).getCoord().getX();
                    y2 = PlanUtils.getNextActivity(plan, leg).getCoord().getY();
                    p1 = MGC.xy2Point(x1, y1);
                    p2 = MGC.xy2Point(x2, y2);

                    if (munich.contains(p1) && munich.contains(p2)) {
                        PersonsWithinMunich = PersonsWithinMunich + 1;

                    } else if (munich.contains(p1) && !munich.contains(p2)) {
                        CommutersStartingInMunich = CommutersStartingInMunich + 1;

                    } else {
                        CommutersEndingInMunich = CommutersEndingInMunich + 1;
                    }
                    //System.out.println("KopierePlan ");
                    Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                    Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                    actNew.setEndTime(actOld.getEndTime());
                    planNew.addActivity(actNew);

                    planNew.addLeg(leg);

                    if (oldLegCount == legSize) {

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        actNew2.setEndTime(actOld2.getEndTime());
                        planNew.addActivity(actNew2);
                        break;
                    }
                    oldLegCount += 1;

                }
            }


           /* if (random >= 0.7 && munich.contains(p1)) {
                //Legloop1:
                for (Leg leg : TripStructureUtils.getLegs(plan)) {
                    endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                    HomeLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();

                    curGarage = chooseGarageByDistance(personId + "departure ", HomeLocation);

                    Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                    garage1.setEndTime(endHomeTime1 - garageDistances.get(personId + "departure ") / 30); // determine average speed

                    planNew.addActivity(garage1);

                    Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                    planNew.addLeg(pickUp);
                    break;
                }

                int oldLegCount = 1;
                //Activityloop:
                for (Leg leg : TripStructureUtils.getLegs(plan)) {

                    Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                    Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                    actNew.setEndTime(actOld.getEndTime());
                    planNew.addActivity(actNew);

                    planNew.addLeg(leg);

//                    if (oldLegCount == legSize && munich.contains(p2)) {

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        //actNew2.setEndTime(actOld2.getEndTime());
                        actNew2.setMaximumDuration(90);
                        planNew.addActivity(actNew2);

                        Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(dropOff);

                        Coord DestinationLocation;
                        DestinationLocation = actNew2.getCoord();
                        Coord coord2 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                        Activity garage2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
                        planNew.addActivity(garage2);

                    }
                    oldLegCount += 1;
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

                    if (oldLegCount == legSize) {

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        actNew2.setEndTime(actOld2.getEndTime());
                        planNew.addActivity(actNew2);
                        break;
                    }
                    oldLegCount += 1;
                }*/

                //System.out.println("SchreibePlan ");
                personNew.addPlan(planNew);
                scenarioNew.getPopulation().addPerson(personNew);
                System.out.println("GaragelistCapacity " + garageListCapacity);
                System.out.println("PersonID " + personId);
                countPlans = countPlans + 1;
                System.out.println("CountedPlan " + countPlans);
            }

            //Write the population file to specified folder
            PopulationWriter pw = new PopulationWriter(scenarioNew.getPopulation(), scenarioNew.getNetwork());
            pw.write(PLANSFILEOUTPUT);
            int noPersons = popInitial.getPersons().size();
            System.out.println("Number of Persons");
            System.out.println(noPersons);
            int noPersonsN = popModified.getPersons().size();
            System.out.println(noPersonsN);
            System.out.println("GaragelistCapacity " + garageListCapacity);
    }
}


