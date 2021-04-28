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
import java.util.*;

public class AddGaragesParkingFreeCentre {

    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_CarCom_WithoutCentre.xml";
    private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_CarCom_WithoutCentre_Garages.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static final String Garages = "C:/matsimfiles/input/testgarages.csv";
    private static final String DISTRICTS = "C:/matsimfiles/input/CityCentre.shp";
    //    private static final String garagePath = "C:/matsimfiles/output/Garages.xml";    //The output file of demand generation
    private static final String COUNTIES = "C:/matsimfiles/input/lkr_ex.shp";
    Random randomObj = new Random(5);

    private static Population popInitial;
    private static Population popModified;
    private static Scenario scenario;
    private static Scenario scenarioNew;

    private static final Map<Integer, Integer> garageListCapacity = new HashMap<>();
    private static final Map<Integer, Double> garageListX = new HashMap<>();
    private static final Map<Integer, Double> garageListY = new HashMap<>();
    private static final Map<Integer, Integer> garageDistricts = new HashMap<>();
    private static final Map<String, Double> garageDistances = new HashMap<>();

    int PersonsWithinCentre = 0;

    int CentreToMunichG = 0;
    int CentreToMunichGToCentre = 0;
    int CentreToMunichSToCentre = 0;
    int CentreToRegion = 0;
    int CentreToRegionToCentre = 0;
    int MunichGToCentre = 0;
    int MunichGToCentreToMunichG = 0;
    int MunichGToMunichG = 0;
    int MunichGToMunichGToMunichG = 0;
    int MunichGToRegion = 0;
    int MunichGToRegionToMunichG = 0;
    int RegionToCentre = 0;
    int RegionToCentreToRegion = 0;
    int RegionToMunichG = 0;
    int RegionToMunichGToRegion = 0;

    int CentreToSomewhere = 0;
    int CentreToSomewhereToCentre = 0;
    int SomewhereToCentre = 0;
    int SomewhereToCentreToSomewhere = 0;
    int MunichToMunich = 0;
    int MunichToMunichToMunich = 0;
    int MunichToRegion = 0;
    int MunichToRegionToMunich = 0;
    int RegionToMunich = 0;
    int RegionToMunichToRegion = 0;
    int SomethingElse = 0;
    double AvSpeed = 30/3.6;


    public AddGaragesParkingFreeCentre() {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);
        scenario = ScenarioUtils.loadScenario(config);
        popInitial = scenario.getPopulation();
        scenarioNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        popModified = scenarioNew.getPopulation();
        new MatsimNetworkReader(scenarioNew.getNetwork()).readFile(Network);

    }

    public static void main(String[] args) throws IOException {

        AddGaragesParkingFreeCentre agpc = new AddGaragesParkingFreeCentre();
        agpc.readGarageCSV();
        agpc.addGarage2Plan(popInitial, scenarioNew); //scenario deleted

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
    public Coord selectGarage2(int idGarage) {

        //System.out.println(garageListX);
        double x = garageListX.get(idGarage);
        double y = garageListY.get(idGarage);


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
                coordGa = selectGarage2(i); //
                distance = NetworkUtils.getEuclideanDistance(HomeCoord, coordGa);
                distances.add(i - 1, distance);
            } else {
                distances.add(i - 1, 9999999.9); //
            }
        }

        garageDistances.put(personTag, Collections.min(distances));

        return selectGarage(distances.indexOf(Collections.min(distances)) + 1);
    }


    int countPlans;
    public void addGarage2Plan(Population popInitial, Scenario scenarioNew) {

        Map<String, Geometry> shapeMap;
        Map<String, Geometry> shapeMap3;
        shapeMap = readShapeFile(DISTRICTS, "Borough");
        shapeMap3 = readShapeFile(DISTRICTS, "Borough");
        //shapeMap2 = readShapeFile(COUNTIES, "SCH");
        //Geometry munich = shapeMap2.get("09162");
        Geometry centre = shapeMap.get("1"); //,2,3,4,5,6,8
        Geometry munich = shapeMap.get("20");

        for (Person person : popInitial.getPersons().values()) {
            Id<Person> personId = Id.createPersonId(person.getId()); //get initial person id to new
            Plan plan = person.getSelectedPlan();
            Person personNew = scenarioNew.getPopulation().getFactory().createPerson(personId);
            Plan planNew = scenarioNew.getPopulation().getFactory().createPlan();

            double random = randomObj.nextDouble();
            int legSize = TripStructureUtils.getLegs(plan).size();
            //int actSize = plan.getPlanElements().size() - legSize;
            double endHomeTime;
            double endSecondTime;
            double intermediateTime;
            double firstDistance;
            double secondDistance;
            Coord FirstLocation;
            Coord SecondLocation;
            Coord curGarage;
            Coord interGarage;

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

                endHomeTime = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                endSecondTime = PlanUtils.getNextActivity(plan, leg).getEndTime();
                FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
                SecondLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
                curGarage = chooseGarageByDistance(personId + "departure ", FirstLocation);
                interGarage = chooseGarageByDistance(personId + "departure ", SecondLocation);
                intermediateTime = NetworkUtils.getEuclideanDistance(FirstLocation, SecondLocation) / AvSpeed;
                firstDistance = NetworkUtils.getEuclideanDistance(FirstLocation, curGarage);
                secondDistance = NetworkUtils.getEuclideanDistance(SecondLocation, interGarage);

                if (random >= 0.7) {
                    if (centre.contains(p1) && munich.contains(p2)) {

                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", curGarage);
                        garage1.setEndTime(endHomeTime - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);

                        Leg halfway = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(halfway);

                        //Second Activity:
                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            actNew2.setMaximumDuration(90);
                            planNew.addActivity(actNew2);

                            Leg toGarage1 = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(toGarage1);

                            Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                            planNew.addActivity(garageI);
                            CentreToMunichG = CentreToMunichG +1;
                            break;
                        } else {

                            if ((endSecondTime - endHomeTime + intermediateTime) >= 2.0 * 3600) {
                                Activity dropOffPoint = scenarioNew.getPopulation().getFactory().createActivityFromCoord("dropOffPoint", actOld2.getCoord());

                                dropOffPoint.setMaximumDuration(60);
                                planNew.addActivity(dropOffPoint);

                                Leg toGarage2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(toGarage2);

                                Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                                garageI.setEndTime(actOld2.getEndTime() - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
                                planNew.addActivity(garageI);

                                Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(pickUp);

                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);
                                Leg halfway2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(halfway2);

                                Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", curGarage);
                                planNew.addActivity(garage4);
                                CentreToMunichGToCentre = CentreToMunichGToCentre +1;
                                break;
                            } else {
                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);

                                Leg halfway2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(halfway2);

                                Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", curGarage);
                                planNew.addActivity(garage4);
                                CentreToMunichSToCentre = CentreToMunichSToCentre +1;
                                break;
                            }
                        }

                    } else if (centre.contains(p1) && !munich.contains(p2)) {

                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", curGarage);
                        garage1.setEndTime(endHomeTime - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);

                        Leg halfway = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(halfway);
//                        }

                        //Second Activity - Copy:
                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            planNew.addActivity(actNew2);
                            CentreToRegion = CentreToRegion +1;
                            break;
                        } else {
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            Leg halfway2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(halfway2);

                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", curGarage);
                            planNew.addActivity(garage4);
                            CentreToRegionToCentre = CentreToRegionToCentre +1;
                            break;
                        }

                    } else if (munich.contains(p1) && centre.contains(p2)) {

                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                        garage1.setEndTime(endHomeTime - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);

                        Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(pickUp);

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        Leg halfway = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(halfway);

                        Activity parkAndRide = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", interGarage);


                        if (legSize == 1) {
                            planNew.addActivity(parkAndRide);
                            MunichGToCentre = MunichGToCentre + 1;
                            break;
                        } else {

                            parkAndRide.setMaximumDuration(60); // neglectable value -> leg = 0 m to the next activity
                            planNew.addActivity(parkAndRide);

                            planNew.addLeg(leg); // 0 m !

                            // not writing last activity and instead:
                            Activity garage3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", interGarage);
                            garage3.setEndTime(endSecondTime + garageDistances.get(personId + "departure ") / AvSpeed); ///+ Travel time
                            planNew.addActivity(garage3);

                            planNew.addLeg(leg);

                            Activity actOld3 = PlanUtils.getPreviousActivity(plan, leg);
                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld3.getType(), actOld3.getCoord());
                            actNew3.setMaximumDuration(90);
                            planNew.addActivity(actNew3);

                            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(dropOff);

                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            planNew.addActivity(garage4);
                            MunichGToCentreToMunichG = MunichGToRegionToMunichG +1;
                            break;
                        }

                    } else if (munich.contains(p1) && munich.contains(p2)) {

                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                        garage1.setEndTime(endHomeTime - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);
                        //System.out.print("davor ");
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
                            actNew2.setMaximumDuration(60);
                            planNew.addActivity(actNew2);

                            planNew.addLeg(leg);

                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            planNew.addActivity(garage4);
                            MunichGToMunichG = MunichGToMunichG +1;
                            break;
                        } else {
                            if ((endSecondTime - endHomeTime + intermediateTime) >= 2.0 * 3600) {
                                Activity dropOffPoint = scenarioNew.getPopulation().getFactory().createActivityFromCoord("DropOffPoint", actOld2.getCoord());

                                dropOffPoint.setMaximumDuration(90);
                                planNew.addActivity(dropOffPoint);

                                Leg toGarage2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(toGarage2);

                                Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                                garageI.setEndTime(actOld2.getEndTime() - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
                                planNew.addActivity(garageI);

                                Leg pickUp2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(pickUp2);

                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);

                                Leg dropOff2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(dropOff2);

                                Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                                planNew.addActivity(garage4);
                                break;
                            } else {
                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);

                                planNew.addLeg(leg);

                                //Activity 3 (Home)
                                Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                                actNew3.setMaximumDuration(60);
                                planNew.addActivity(actNew3);

                                Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(dropOff);

                                Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                                planNew.addActivity(garage4);
                                MunichGToMunichGToMunichG = MunichGToMunichGToMunichG +1;
                                break;
                            }
                        }
                    } else if (munich.contains(p1) && !munich.contains(p2)) {

                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                        garage1.setEndTime(endHomeTime - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
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
                            MunichGToRegion = MunichGToRegion +1;
                            break;
                        } else {
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            planNew.addLeg(leg);

                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            actNew3.setMaximumDuration(60);
                            planNew.addActivity(actNew3);

                            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(dropOff);

                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            planNew.addActivity(garage4);
                            MunichGToRegionToMunichG = MunichGToRegionToMunichG +1;
                            break;
                        }

                    } else if (!munich.contains(p1) && centre.contains(p2)) {

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), FirstLocation);
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);

                        Activity parkAndRide = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", interGarage);

                        if (legSize == 1) {
                            planNew.addActivity(parkAndRide);
                            RegionToCentre = RegionToCentre + 1;
                            break;

                        } else {
                            parkAndRide.setMaximumDuration(30);
                            planNew.addActivity(parkAndRide);

                            planNew.addLeg(leg); //  0 m !

                            Activity garage3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", interGarage);
                            garage3.setEndTime(endSecondTime + garageDistances.get(personId + "departure ") / AvSpeed); ///CHECK""""
                            planNew.addActivity(garage3);

                            planNew.addLeg(leg);

                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            planNew.addActivity(actNew3);
                            RegionToCentreToRegion = RegionToCentreToRegion +1;
                            break;
                        }
                    } else if (!munich.contains(p1) && munich.contains(p2)) {

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);

                        //Second Activity:
                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            actNew2.setMaximumDuration(60);
                            planNew.addActivity(actNew2);

                            Leg toGarage = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(toGarage);

                            Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                            planNew.addActivity(garageI);
                            RegionToMunichG = RegionToMunichG +1;
                            break;
                        } else {
                            Activity dropOffPoint = scenarioNew.getPopulation().getFactory().createActivityFromCoord("dropOffPoint", actOld2.getCoord());
                            dropOffPoint.setMaximumDuration(60);
                            planNew.addActivity(dropOffPoint);

                            planNew.addLeg(leg);

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
                            RegionToMunichGToRegion = RegionToMunichGToRegion +1;
                            break;
                        }


                    } else {
                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        SomethingElse = SomethingElse +1;

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
                } else {
                    if (centre.contains(p1)) {
//                    endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
//                    FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
//                    SecondLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
//                    curGarage = chooseGarageByDistance(personId + "departure ", FirstLocation);
//                    interGarage = chooseGarageByDistance(personId + "departure ", SecondLocation);

                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", curGarage);
                        garage1.setEndTime(endHomeTime - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);

                        Leg halfway = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(halfway);

                        //Second Activity:
                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            planNew.addActivity(actNew2);
                            CentreToSomewhere = CentreToSomewhere +1;
                            break;
                        } else {

                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            Leg halfway2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(halfway2);

                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", curGarage);
                            planNew.addActivity(garage4);
                            CentreToSomewhereToCentre = CentreToSomewhereToCentre +1;
                            break;
                        }

/*
                    } else if (centre.contains(p1) && !munich.contains(p2)) {
//                    endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
//                    FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
//                    curGarage = chooseGarageByDistance(personId + "departure ", FirstLocation);

                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkandride", curGarage);
                        garage1.setEndTime(endHomeTime1 - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);

                        Leg halfway = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(halfway);

                        //Second Activity - Copy:
                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        if (legSize == 1) {
                            planNew.addActivity(actNew2);
                            break;
                        } else {
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            Leg halfway2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(halfway2);

                            //Activity actOld3 = PlanUtils.getPreviousActivity(plan, leg);
                            //Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld3.getType(), actOld3.getCoord());
                            // not writing last activity and instead:
                            //Coord DestinationLocation;
                            //DestinationLocation = actNew3.getCoord();
                            //Coord coord4 = chooseGarageByDistance(personId + "returning ", curGarage);
                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkandride", curGarage);
                            planNew.addActivity(garage4);
                            break;
                        }
*/

/*
                    } else if (munich.contains(p1) && centre.contains(p2)) {
//                    endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
//                    endSecondTime = PlanUtils.getNextActivity(plan, leg).getEndTime();
//                    FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
//                    SecondLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
//                    curGarage = chooseGarageByDistance(personId + "departure ", FirstLocation);
//                    interGarage = chooseGarageByDistance(personId + "departure ", SecondLocation);

                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        Leg halfway = scenarioNew.getPopulation().getFactory().createLeg("car");
                        planNew.addLeg(halfway);

                        TripsFromMunichToCentre = TripsFromMunichToCentre + 1;

                        //Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                        //Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                        // not writing last activity and instead:
                        //Coord DestinationLocation;
                        //DestinationLocation = actNew2.getCoord();
                        //Coord coord2 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                        Activity parkandride = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkandride", interGarage);

                        if (legSize == 1) {
                            planNew.addActivity(parkandride);

                            break;
                        } else {

                            parkandride.setMaximumDuration(90); // neglectable value -> leg = 0 m to the next activity
                            planNew.addActivity(parkandride);

                            planNew.addLeg(leg); // 0 m !


                            // not writing last activity and instead:
//                        Coord DestinationLocation2;
//                        DestinationLocation = actNew2.getCoord();
//                        Coord coord3 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                            Activity garage3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkandride", interGarage);
                            garage3.setEndTime(endSecondTime + garageDistances.get(personId + "departure ") / AvSpeed); ///+ Travel time
                            planNew.addActivity(garage3);

                            planNew.addLeg(leg);

//                            Activity actOld3 = PlanUtils.getPreviousActivity(plan, leg);
                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            planNew.addActivity(actNew3);

//                        Coord DestinationLocation4;
//                        DestinationLocation4 = actNew3.getCoord();
//                        Coord coord4 = chooseGarageByDistance(personId + "returning ", HomeLocation);
//                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
//                            planNew.addActivity(garage4);
                            break;
                        }

                    } else if (munich.contains(p1) && munich.contains(p2)) {
//                    endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
//                    FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
//                    SecondLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
//                    curGarage = chooseGarageByDistance(personId + "departure ", FirstLocation);
//                    interGarage = chooseGarageByDistance(personId + "departure ", SecondLocation);
//                    firstDistance = NetworkUtils.getEuclideanDistance(FirstLocation, curGarage);
//                    secondDistance = NetworkUtils.getEuclideanDistance(FirstLocation, interGarage);


                        Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                        garage1.setEndTime(endHomeTime1 - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed

                        planNew.addActivity(garage1);
                        //System.out.print("davor ");
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
                            if ((endSecondTime - endHomeTime1 + intermediateTime) >= 2.0 * 3600) {
                                Activity dropOff = scenarioNew.getPopulation().getFactory().createActivityFromCoord("dropoff", actOld2.getCoord());

                                dropOff.setMaximumDuration(90);
                                planNew.addActivity(dropOff);

                                Leg toGarage2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(toGarage2);

                                //interGarage = chooseGarageByDistance(personId + "departure ", actOld2.getCoord());
                                Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                                garageI.setEndTime(actOld2.getEndTime() - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
                                planNew.addActivity(garageI);

                                Leg pickUp2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(pickUp2);

                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);

                                Leg dropOff2 = scenarioNew.getPopulation().getFactory().createLeg("car");
                                planNew.addLeg(dropOff2);

                                Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                                planNew.addActivity(garage4);
                                break;
                            } else {
                                actNew2.setEndTime(actOld2.getEndTime());
                                planNew.addActivity(actNew2);
                            }
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            planNew.addLeg(leg);

                            //Activity 3 (Home)
                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            actNew3.setMaximumDuration(90);
                            planNew.addActivity(actNew3);

                            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(dropOff);

                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            planNew.addActivity(garage4);
                            break;
                        }
                    } else if (munich.contains(p1) && !munich.contains(p2)) {
//                    endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
//                    FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
//                    curGarage = chooseGarageByDistance(personId + "departure ", FirstLocation);

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

                            //Activity actOld3 = PlanUtils.getPreviousActivity(plan, leg);
                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            actNew3.setMaximumDuration(90);
                            planNew.addActivity(actNew3);

                            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
                            planNew.addLeg(dropOff);

//                        Coord DestinationLocation2;
//                        DestinationLocation2 = actNew3.getCoord();
//                        Coord coord4 = chooseGarageByDistance(personId + "returning ", HomeLocation);
                            Activity garage4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", curGarage);
                            planNew.addActivity(garage4);
                            break;
                        }
*/

                    } else if (centre.contains(p2)) {
//                    FirstLocation = PlanUtils.getPreviousActivity(plan, leg).getCoord();
//                    SecondLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
//                    interGarage = chooseGarageByDistance(personId + "departure ", SecondLocation);


                        Activity actOld = PlanUtils.getPreviousActivity(plan, leg);
                        Activity actNew = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), FirstLocation);
                        actNew.setEndTime(actOld.getEndTime());
                        planNew.addActivity(actNew);

                        planNew.addLeg(leg);

                        Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
//                    Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());

//                    Coord DestinationLocation;
//                    DestinationLocation = actNew2.getCoord();
//                    Coord coord2 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                        Activity parkAndRide = scenarioNew.getPopulation().getFactory().createActivityFromCoord("ParkAndRide", interGarage);

                        if (legSize == 1) {
                            planNew.addActivity(parkAndRide);
                            SomewhereToCentre = SomewhereToCentre +1;
                            break;

                        } else {
                            parkAndRide.setMaximumDuration(60);
                            planNew.addActivity(parkAndRide);

                            planNew.addLeg(leg); //  0 m !


                            // not writing last activity and instead:
//                        Coord DestinationLocation2;
//                        DestinationLocation = actNew2.getCoord();
//                        Coord coord3 = chooseGarageByDistance(personId + "returning ", DestinationLocation);
                            Activity garage3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("parkAndRide", interGarage);
                            garage3.setEndTime(actOld2.getEndTime() + garageDistances.get(personId + "departure ") / AvSpeed); ///CHECK""""
                            planNew.addActivity(garage3);

                            planNew.addLeg(leg);

                            //Activity actOld3 = PlanUtils.getPreviousActivity(plan, leg);
                            Activity actNew3 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            planNew.addActivity(actNew3);
                            SomewhereToCentreToSomewhere = SomewhereToCentreToSomewhere +1;
                            break;
                        }
/*
                    } else if (munich.contains(p2)) {
//                    SecondLocation = PlanUtils.getNextActivity(plan, leg).getCoord();
//                    interGarage = chooseGarageByDistance(personId + "departure ", SecondLocation);

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

                            Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                            planNew.addActivity(garageI);
                            break;
                        } else {
                            Activity dropOff = scenarioNew.getPopulation().getFactory().createActivityFromCoord("dropoff", actOld2.getCoord());
                            dropOff.setMaximumDuration(30);
                            planNew.addActivity(dropOff);

//                        Leg toGarage = scenarioNew.getPopulation().getFactory().createLeg("car");
//                        planNew.addLeg(toGarage);
                            planNew.addLeg(leg);

                            Activity garageI = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", interGarage);
                            garageI.setEndTime(actOld2.getEndTime() - garageDistances.get(personId + "departure ") / AvSpeed); // determine average speed
                            planNew.addActivity(garageI);

                            //Leg pickUp = scenarioNew.getPopulation().getFactory().createLeg("car");
                            //planNew.addLeg(pickUp);
                            planNew.addLeg(leg);

                            //Second Activity
                            actNew2.setEndTime(actOld2.getEndTime());
                            planNew.addActivity(actNew2);

                            planNew.addLeg(leg);

                            //Activity actOld4 = PlanUtils.getPreviousActivity(plan, leg);
                            Activity actNew4 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld.getType(), actOld.getCoord());
                            planNew.addActivity(actNew4);
                            break;
                        }
*/


                    } else {

                        if (munich.contains(p1) && munich.contains(p2)) {
                            if (legSize==1) {
                                MunichToMunich = MunichToMunich +1;
                            } else {
                                MunichToMunichToMunich = MunichToMunichToMunich +1;
                            }

                        } else if (munich.contains(p1) && !munich.contains(p2)) {
                            if (legSize==1) {
                                MunichToRegion = MunichToRegion +1;
                            } else {
                                MunichToRegionToMunich = MunichToRegionToMunich +1;
                            }

                        } else if (!munich.contains(p1) && munich.contains(p2)) {
                            if (legSize==1) {
                                RegionToMunich = RegionToMunich +1;
                            } else {
                                RegionToMunichToRegion = RegionToMunichToRegion +1;
                            }
                        } else {
                            SomethingElse = SomethingElse +1;
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
            }

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
        System.out.println("Initial number of persons " + noPersons);
        int noPersonsN = popModified.getPersons().size();
        System.out.println("Number of persons modified: "+ noPersonsN);
        System.out.println("Capacities of garages " + garageListCapacity);
        System.out.println("Counted plans total " + countPlans);

        System.out.println("Persons within centre " + PersonsWithinCentre);

        System.out.println("CentreToMunichG " + CentreToMunichG);
        System.out.println("CentreToMunichGToCentre " + CentreToMunichGToCentre);
        System.out.println("CentreToMunichSToCentre " + CentreToMunichSToCentre);
        System.out.println("CentreToRegion " + CentreToRegion);
        System.out.println("CentreToRegionToCentre " + CentreToRegionToCentre);
        System.out.println("MunichGToCentre " + MunichGToCentre);
        System.out.println("MunichGToCentreToMunichG " + MunichGToCentreToMunichG);
        System.out.println("MunichGToMunichG " + MunichGToMunichG);
        System.out.println("MunichGToMunichGToMunichG " + MunichGToMunichGToMunichG);
        System.out.println("MunichGToRegion " + MunichGToRegion);
        System.out.println("MunichGToRegionToMunichG " + MunichGToRegionToMunichG);
        System.out.println("RegionToCentre " + RegionToCentre);
        System.out.println("RegionToCentreToRegion " + RegionToCentreToRegion);
        System.out.println("RegionToMunichG " + RegionToMunichG);
        System.out.println("RegionToMunichGToRegion " + RegionToMunichGToRegion);

        System.out.println("CentreToSomewhere " + CentreToSomewhere);
        System.out.println("CentreToSomewhereToCentre " + CentreToSomewhereToCentre);
        System.out.println("SomewhereToCentre " + SomewhereToCentre);
        System.out.println("SomewhereToCentreToSomewhere " + SomewhereToCentreToSomewhere);
        System.out.println("MunichToMunich " + MunichToMunich);
        System.out.println("MunichToMunichToMunich " + MunichToMunichToMunich);
        System.out.println("MunichToRegion " + MunichToRegion);
        System.out.println("MunichToRegionToMunich " + MunichToRegionToMunich);
        System.out.println("RegionToMunich " + RegionToMunich);
        System.out.println("RegionToMunichToRegion " + RegionToMunichToRegion);

        System.out.println("SomethingElse " + SomethingElse);

    }
}
