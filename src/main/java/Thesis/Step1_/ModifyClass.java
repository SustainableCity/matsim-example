package Thesis.Step1_;

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

import java.io.*;
import java.util.*;


public class ModifyClass {

    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_2011_onlyAuto_inMUC.xml";
    private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_2011_onlyAuto_inMUC_inclGarages.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static final String Garages = "C:/matsimfiles/input/testgarages.csv";
//    private static final String garagePath = "C:/matsimfiles/output/Garages.xml";    //The output file of demand generation
//    private static final String COUNTIES = "C:/matsimfiles/input/lkr_ex.shp";

    private static Population popInitial;
    private static Population popModified;
    private static Scenario scenario;
    private static Scenario scenarioNew;

    private static final Map<Integer,Integer> garageListCapacity = new HashMap<>();
    private static final Map<Integer,Double> garageListX = new HashMap<>();
    private static final Map<Integer,Double> garageListY = new HashMap<>();

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

            garageListCapacity.put(garageNo, garageCapacity);
            garageListX.put(garageNo, coordX);
            garageListY.put(garageNo, coordY);

        }

        System.out.println("Garage list: " + garageListX.get(1));
        System.out.println("Garage list: " + garageListY); //.size()

    }

    public Coord selectGarage(int idGarage){

        //add condition or selection method
        //int idGarage = 1; //als Input Variable definieren


        double x = garageListX.get(idGarage);
        double y = garageListY.get(idGarage);
        return new Coord(x,y);  // Coord coordGarage = new Coord(x,y); //instead of coordNew
                                //return coordGarage;
    }

    public Integer chooseGarage (Coord HomeCoord) {
        int garagesize = garageListX.size();
        double[] distances = new double[garagesize];
        double distance;
        Coord cG;
        OptionalDouble minDis;
        int garageid;

            for (int i = 0; i<garagesize+1; i++) {
            cG = selectGarage(i);
            distance = NetworkUtils.getEuclideanDistance(HomeCoord,cG);
            distances[i] = distance;
        }
        minDis = Arrays.stream(distances).min();
        garageid = Arrays.asList(distances).indexOf(minDis);
        System.out.println("Distance list: " + distances);
        return garagesize;
    }

    public void addGarage2Plan(Population popInitial, Scenario scenarioNew){


        for (Person person : popInitial.getPersons().values()){
            Id<Person> personId = Id.createPersonId(person.getId()); //get initial person id to new
            //System.out.println(personId);

            Plan plan = person.getSelectedPlan();

            int legSize = TripStructureUtils.getLegs(plan).size();
            //int actSize = plan.getPlanElements().size() - legSize;

            //System.out.println(legSize);
            //System.out.println(actSize);


            Person personNew = scenarioNew.getPopulation().getFactory().createPerson(personId);
            Plan planNew = scenarioNew.getPopulation().getFactory().createPlan();
            //Activity act1 = TripStructureUtils.getActivities .get(0); // CoordHome

            int curGarage = chooseGarage(CoordHome);
            Coord coordNew = selectGarage(curGarage); //Garage ID

            Activity garage1 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coordNew);

            double endHomeTime1 = 0;
            //Legloop1:
            for (Leg leg : TripStructureUtils.getLegs(plan)){
                endHomeTime1 = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                break;
            }
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

                planNew.addLeg(leg);

                if(oldLegCount == legSize){
                    Activity actOld2 = PlanUtils.getNextActivity(plan, leg);
                    Activity actNew2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord(actOld2.getType(), actOld2.getCoord());
                    actNew2.setEndTime(actOld2.getEndTime());
                    planNew.addActivity(actNew2);

                    planNew.addLeg(leg);
                }
                oldLegCount +=1;
            }

            Leg dropOff = scenarioNew.getPopulation().getFactory().createLeg("car");
            planNew.addLeg(dropOff);

            Coord coord2= selectGarage (curGarage); //
            Activity garage2 = scenarioNew.getPopulation().getFactory().createActivityFromCoord("garage", coord2);
            planNew.addActivity(garage2);

            personNew.addPlan(planNew);
            scenarioNew.getPopulation().addPerson(personNew);

        }

        //Write the population file to specified folder
        PopulationWriter pw = new PopulationWriter(scenarioNew.getPopulation(),scenarioNew.getNetwork());
        pw.write(PLANSFILEOUTPUT);

    }


}