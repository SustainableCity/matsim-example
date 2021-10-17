package Thesis.No1_PreparePlans;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class Counting {
    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_2011_5.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static String csvFile = "C:/matsimfiles/output/shopping2.csv";

    private static Population popInitial;
    private static Scenario scenario;



    public Counting() {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);
        scenario = ScenarioUtils.loadScenario(config);
        popInitial = scenario.getPopulation();
        new MatsimNetworkReader(scenario.getNetwork()).readFile(Network);

    }

    public static void main(String[] args) throws IOException {

        Counting cp = new Counting();
        cp.getShoppingTime(popInitial);

    }

    public double getDistance(Coord ShoopingCoord, Coord HomeCoord) {

        double distance;
        distance = NetworkUtils.getEuclideanDistance(HomeCoord, ShoopingCoord);

        return distance;

    }

    public void getShoppingTime(Population popInitial) throws IOException {

        Coord ShoppingCoord;
        Coord HomeCoord;
        String Activity1;
        String Activity2;
        String Activity3;
        double HomeEndTime;
        double ShoppingEndTime;
        double ShoppingStartTime;
        double TravelTime;

        PrintWriter pw = new PrintWriter(new File(csvFile));
        pw.println("PersonID, ShoppingStartTime, ShoppingEndTime");
        int count=0;

        for (Person person : popInitial.getPersons().values()) {
            Id<Person> personId = Id.createPersonId(person.getId()); //get initial person id to new
            Plan plan = person.getSelectedPlan();

            int legCount = 1;
            StringBuilder sb = new StringBuilder();

            for (Leg leg : TripStructureUtils.getLegs(plan)) {
                int legSize = TripStructureUtils.getLegs(plan).size();
/*
                for (int legCount =1; legCount <= legSize; legCount++) {
                if (legSize == 2) {
                    System.out.println("2 "+personId);
                }
                if (legSize == 3) {
                    System.out.println("3 " +personId);
                }*/

                if (legCount == 1) {
                    Activity1 = PlanUtils.getPreviousActivity(plan, leg).getType();
                    Activity2 = PlanUtils.getNextActivity(plan, leg).getType();
                    sb.append(Activity1).append(",");
                    sb.append(Activity2).append(",");
                }if (legCount == 2) {
                    Activity3 = PlanUtils.getNextActivity(plan, leg).getType();
                    sb.append(Activity3).append(",");
                    break;
                }
//                pw.println(sb);
                legCount += 1;

//                count+=1;
                }
            pw.println(sb);
//                count = count +1;
//                System.out.println(count);
//            }
        }
        pw.close();
    }

}
