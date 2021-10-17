package Thesis.No1_PreparePlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;

public class PrintActivities2CSV {
    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plansTest.xml";
    private static String csvFile = "C:/matsimfiles/output/TestTEst.csv" ;


    public static void main(String[] args) throws FileNotFoundException {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        final Population pop = scenario.getPopulation();
        System.out.println(pop.getPersons().size());
        PrintWriter pw = new PrintWriter(new File(csvFile));
        pw.println("id,x1,y1,x2,y2,activity");

        int count=1;
        int legCount = 1;
        StringBuilder sb = new StringBuilder();

        for (Iterator<? extends Person> it = pop.getPersons().values().iterator(); it.hasNext();){
            Person person = it.next();
            Plan plan = person.getSelectedPlan();

            int legSize = TripStructureUtils.getLegs(plan).size();
            sb.append(person.getId()).append(",");

            Legloop:
            for (Leg leg : TripStructureUtils.getLegs(plan)) {

                if (legCount == 1) {
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getX()).append(",");
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getY()).append(",");
                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getType()).append(",");
                } else if (legCount == 2 && legCount != legSize) {
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getX()).append(",");
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getY()).append(",");
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getType()).append(",");
//                    sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getX()).append(",");
//                    sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getY()).append(",");
                    sb.append(PlanUtils.getNextActivity(plan, leg).getType()).append(",");
                    System.out.println("legCount 2");
                } else if (legCount == 3 && legCount != legSize) {
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getX()).append(",");
//                sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getY()).append(",");
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getType()).append(",");
//                sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getX()).append(",");
//                sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getY()).append(",");
                    sb.append(PlanUtils.getNextActivity(plan, leg).getType()).append(",");
                    System.out.println("legCount 3");
                } else if (legCount == 4 && legCount !=legSize) {
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getX()).append(",");
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getCoord().getY()).append(",");
//                    sb.append(PlanUtils.getPreviousActivity(plan, leg).getType()).append(",");
//                    sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getX()).append(",");
//                    sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getY()).append(",");
                    sb.append(PlanUtils.getNextActivity(plan, leg).getType()).append(",");
                    System.out.println("legCount 4");
                } else if (legCount == legSize) {
//                    sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getX()).append(",");
//                    sb.append(PlanUtils.getNextActivity(plan, leg).getCoord().getY()).append(",");
                    sb.append(PlanUtils.getNextActivity(plan, leg).getType()).append(",");
                    break;
                }
                count +=1;
                pw.println(sb);
                legCount += 1;
            }
        }
        pw.close();
        System.out.println(count);
    }


}
