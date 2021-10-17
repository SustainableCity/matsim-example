package Thesis.No3_Analysis;

import com.vividsolutions.jts.geom.Geometry;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

public class Print2CSV {
    private static final String PLANSFILEINPUT = "C:/matsimfiles/output/plans_2011_onlyAuto_MUC.xml";
    private static String csvFile = "C:/matsimfiles/output/plans_2011_onlyAuto_MUC.checkOD.csv" ;


    public static void main(String[] args) throws FileNotFoundException {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        final Population pop = scenario.getPopulation();
        System.out.println(pop.getPersons().size());
        PrintWriter pw = new PrintWriter(new File(csvFile));
        pw.println("id,x1,y1,x2,y2,activity");

        int count=1;

        for (Iterator<? extends Person> it = pop.getPersons().values().iterator(); it.hasNext();){
            Person person = it.next();
            Plan plan = person.getSelectedPlan();

            Legloop:
            for (Leg leg : TripStructureUtils.getLegs(plan)){
                StringBuilder sb = new StringBuilder();
                sb.append(person.getId()).append(",");
                sb.append(PlanUtils.getPreviousActivity(plan,leg).getCoord().getX()).append(",");
                sb.append(PlanUtils.getPreviousActivity(plan,leg).getCoord().getY()).append(",");
                sb.append(PlanUtils.getNextActivity(plan,leg).getCoord().getX()).append(",");
                sb.append(PlanUtils.getNextActivity(plan,leg).getCoord().getY()).append(",");
                sb.append(PlanUtils.getNextActivity(plan,leg).getType()).append(",");
                count +=1;
                pw.println(sb);
            }
        }
        pw.close();
    }

}
