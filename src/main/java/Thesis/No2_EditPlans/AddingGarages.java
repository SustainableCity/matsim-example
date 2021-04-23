package Thesis.No2_EditPlans;

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
    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_CarCom.xml";
    private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_CCG.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static final String Garages = "C:/matsimfiles/input/testgarages2.csv";
    //    private static final String garagePath = "C:/matsimfiles/output/Garages.xml";    //The output file of demand generation
    private static final String COUNTIES = "C:/matsimfiles/input/lkr_ex.shp";

    private static Population popInitial;
    private static Population popModified;
    private static Scenario scenario;
    private static Scenario scenarioNew;

    private static final Map<Integer, Integer> garageListCapacity = new HashMap<>();
    private static final Map<Integer, Double> garageListX = new HashMap<>();
    private static final Map<Integer, Double> garageListY = new HashMap<>();
    private static final Map<Integer, Integer> garageDistricts = new HashMap<>();
    private static final Map<String, Double> garageDistances = new HashMap<>();
    int WithinMunichG = 0;
    int WithinMunich = 0;
    int StartingInMunichG = 0;
    int StartingInMunich = 0;
    int HeadingToMunichG = 0;
    int HeadingToMunich = 0;
    double AvSpeed = 30 / 3.6; // value in km/h and converted in m/s
    int countPlans;

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

        AddingGarages ag = new AddingGarages();
        ag.readGarageCSV();
        ag.addGarage2Plan(popInitial, scenarioNew); //scenario deleted
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

        //System.out.println(garageListCapacity);

        int newCapacity = garageListCapacity.get(idGarage) - 1;
        garageListCapacity.put(idGarage, newCapacity);

        //System.out.println(garageListCapacity);

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

    public int chooseGarageByDistance(String personTag, Coord HomeCoord) {
        int garageSize = garageListX.size(); //No. of garages in the list
        ArrayList<Double> distances = new ArrayList<>();
        double distance;
        Coord coordGa; //Coord of the selected garage
        double minDistance;
        int garageMinDis = 0;

        for (int i = 1; i < garageSize + 1; i++) {
            //System.out.println(i);

            if (garageListCapacity.get(i) > 0) {
                coordGa = selectGarage(i); //
                distance = NetworkUtils.getEuclideanDistance(HomeCoord, coordGa);
                distances.add(i - 1, distance);
            } else {
                distances.add(i - 1, 9999999.9); //
            }
        }

        garageDistances.put(personTag, Collections.min(distances));


        return distances.indexOf(Collections.min(distances)) + 1;
    }


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
            double endSecondTime;
            double travelTime;
            double firstDistance;
            double secondDistance;
            Coord FirstLocation;
            Coord SecondLocation;

            Map<String, Geometry> shapeMap;
            shapeMap = readShapeFile(COUNTIES, "SCH");
            Geometry munich = shapeMap.get("09162");
            Point p1;
            Point p2;
            double x1;
            double y1;
            double x2;
            double y2;

            for (Leg leg : TripStructureUtils.getLegs(plan)) {
                x1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getX();
                y1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getY();
                x2 = PlanUtils.getNextActivity(plan, leg).getCoord().getX();
                y2 = PlanUtils.getNextActivity(plan, leg).getCoord().getY();
                p1 = MGC.xy2Point(x1, y1);
                p2 = MGC.xy2Point(x2, y2);

                endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                endSecondTime = PlanUtils.getNextActivity(plan, leg).getEndTime();
                FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
                SecondLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
                int curGarageId = chooseGarageByDistance(personId + "departure ", FirstLocation);

                int interGarageId = chooseGarageByDistance(personId + "departure ", SecondLocation);

                travelTime = NetworkUtils.getEuclideanDistance(FirstLocation, SecondLocation) / AvSpeed;


                if (random >= 0.0) {
                    Coord curGarage;
                    Coord interGarage;
                    System.out.println(garageListCapacity);
                    if (munich.contains(p1) && munich.contains(p2)) {
                        WithinMunichG = WithinMunichG + 1;

                        curGarage = selectGarage(curGarageId);
                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                        garage1.setEndTime(endHomeTime1 - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);

                        Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(pickUp);

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);
                            break;
                        } else {
                            if ((endSecondTime - endHomeTime1 + travelTime) >= 2.0 * 3600) {
                                Activity dropOffPoint = scenarioNew.getPopulation().getFactory().createActivityFromCoord("dropOffPoint", actOld2.getCoord());

                                dropOffPoint.setMaximumDuration(90);
                                planNew.addActivity(dropOffPoint);

                                Leg toGarage2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(toGarage2);

                                interGarage = selectGarage(interGarageId);
                                Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                                garageI.setEndTime(actOld2.getEndTime() - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
                                planNew.addActivity(garageI);

                                Leg pickUp2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(pickUp2);

                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);

                                Leg dropOff2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(dropOff2);

                                curGarage = selectGarage(curGarageId);
                                Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                                planNew.addActivity(garage4);
                                break;
                            } else {

                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);

                                planNew.addLeg(leg);

                                //Activity 3 (Home)
                                Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                                actNew3.setMaximumDuration(90);
                                planNew.addActivity(actNew3);

                                Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(dropOff);

                                curGarage = selectGarage(curGarageId);
                                Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                                planNew.addActivity(garage4);
                                break;
                            }
                        }
                    } else if (munich.contains(p1) && !munich.contains(p2)) {
                        StartingInMunichG = StartingInMunichG + 1;

                        curGarage = selectGarage(curGarageId);
                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                        garage1.setEndTime(endHomeTime1 - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
                        planNew.addActivity(garage1);

                        Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(pickUp);

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);
                            break;
                        } else {
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            planNew.addLeg(leg);

                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            actNew3.setMaximumDuration(90);
                            planNew.addActivity(actNew3);

                            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(dropOff);

                            curGarage = selectGarage(curGarageId);
                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            planNew.addActivity(garage4);
                            break;
                        }

                    } else if (!munich.contains(p1) && munich.contains(p2)) {
                        HeadingToMunichG = HeadingToMunichG + 1;

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);

                        //Second Activity:
                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            actNew2.setMaximumDuration(30);
                            planNew.addActivity(actNew2);

                            Leg toGarage = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(toGarage);

                            interGarage = selectGarage(interGarageId);
                            Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                            planNew.addActivity(garageI);
                            break;
                        } else {
                            Activity dropOffPoint = scenarioNew.getPopulation().getFactory().createActivityFromCoord("dropOffPoint", actOld2.getCoord());
                            dropOffPoint.setMaximumDuration(30);
                            planNew.addActivity(dropOffPoint);

                            planNew.addLeg(leg);

                            interGarage = selectGarage(interGarageId);
                            Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                            garageI.setEndTime(actOld2.getEndTime() - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
                            planNew.addActivity(garageI);

                            planNew.addLeg(leg);

                            //Second Activity
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            planNew.addLeg(leg);

                            Activity actNew4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            planNew.addActivity(actNew4);
                            break;
                        }
                    }

                    System.out.println(garageListCapacity);

                } else {
                    //random <0.7 (only) Copy OldPlan:

                    if (munich.contains(p1) && munich.contains(p2)) {
                        WithinMunich = WithinMunich + 1;

                    } else if (munich.contains(p1) && !munich.contains(p2)) {
                        StartingInMunich = StartingInMunich + 1;

                    } else {
                        HeadingToMunich = HeadingToMunich + 1;
                    }
                    Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                    Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                    actNew.setEndTime(actOld.getEndTime());
                    planNew.addActivity(actNew);

                    planNew.addLeg(leg);

                    Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                    Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                    actNew2.setEndTime(actOld2.getEndTime());
                    planNew.addActivity(actNew2);

                    if (legSize == 1) {
                        break;
                    } else {
                        planNew.addLeg(leg);
                        Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        planNew.addActivity(actNew3);
                        break;
                    }
                }
            }


            personNew.addPlan(planNew);
            scenarioNew.getPopulation().addPerson(personNew);
            //System.out.println("GaragelistCapacity " + garageListCapacity);
            System.out.println("PersonID " + personId);
            countPlans = countPlans + 1;
            System.out.println("CountedPlan " + countPlans);
        }

        PopulationWriter pw = new PopulationWriter(scenarioNew.getPopulation(), scenarioNew.getNetwork());
        pw.write(PLANSFILEOUTPUT);
        int noPersons = popInitial.getPersons().size();
        System.out.println("Number of Persons " + noPersons);
        int noPersonsN = popModified.getPersons().size();
        System.out.println("Number of Persons " + noPersonsN);
        System.out.println("GaragelistCapacity " + garageListCapacity);
        System.out.println("Within MunichG " + WithinMunichG);
        System.out.println("Within Munich " + WithinMunich);
        System.out.println("Commuters starting in MunichG " + StartingInMunichG);
        System.out.println("Commuters starting in Munich " + StartingInMunich);
        System.out.println("Commuters heading to MunichG " + HeadingToMunichG);
        System.out.println("Commuters heading to Munich " + HeadingToMunich);
    }
}
