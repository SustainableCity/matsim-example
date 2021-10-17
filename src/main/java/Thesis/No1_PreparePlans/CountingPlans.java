package Thesis.No1_PreparePlans;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

public class CountingPlans {

    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plansTest.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static String csvFile = "C:/matsimfiles/output/shopping3.csv";

    private static Population popInitial;
    private static Scenario scenario;
    Instant start = Instant.now();


    public CountingPlans() {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);
        scenario = ScenarioUtils.loadScenario(config);
        popInitial = scenario.getPopulation();
        new MatsimNetworkReader(scenario.getNetwork()).readFile(Network);

    }

    public static void main(String[] args) throws IOException {

        CountingPlans cp = new CountingPlans();
        cp.getShoppingTime(popInitial);

    }

    public double getDistance(Coord ShoopingCoord, Coord HomeCoord) {

        double distance;
        distance = NetworkUtils.getEuclideanDistance(HomeCoord, ShoopingCoord);

        return distance;

    }

    public void getShoppingTime(Population popInitial) throws FileNotFoundException {

        Coord ShoppingCoord;
        Coord HomeCoord;
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

            for (Leg leg : TripStructureUtils.getLegs(plan)) {
            int legSize = TripStructureUtils.getLegs(plan).size();

                //           for (int legCount =1; legCount <= legSize; legCount++) {
                if (PlanUtils.getNextActivity(plan, leg).getType().contains("shopping")) {
                    ShoppingEndTime = PlanUtils.getNextActivity(plan, leg).getEndTime();
                    ShoppingCoord = PlanUtils.getNextActivity(plan, leg).getCoord();
                    HomeEndTime = PlanUtils.getPreviousActivity(plan, leg).getEndTime();
                    HomeCoord = PlanUtils.getPreviousActivity(plan, leg).getCoord();

//                    Instant end = Instant.now();
//                    Duration timeElapsed = Duration.between(start, end);
//                    System.out.println("Time taken: "+ timeElapsed.toMillis() +" milliseconds");


                    double distance = getDistance(ShoppingCoord, HomeCoord);
//                    double totalTime = ShoppingEndTime - HomeEndTime;
                    TravelTime = distance / (50*1000/3600);
                    ShoppingStartTime = (HomeEndTime + TravelTime);

                    StringBuilder sb = new StringBuilder();
                    sb.append(personId).append(";");
                    sb.append(ShoppingStartTime).append(";");
                    sb.append(ShoppingEndTime).append(";");
                    sb.append(ShoppingEndTime-ShoppingStartTime).append(";");
                    pw.println(sb);
                    count+=1;
                    break;
                }
                System.out.println(count);
            }

        }
        pw.close();
    }
}


