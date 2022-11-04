package it.polimi.saefa.plan;

import com.google.ortools.linearsolver.MPSolver;
import it.polimi.saefa.plan.domain.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.util.ResourceUtils;

@Slf4j
@SpringBootApplication
@EnableFeignClients
public class PlanApplication {

    public static void main(String[] args) throws Exception {
        try {
            System.load(ResourceUtils.getFile("classpath:libjniortools.dylib").getAbsolutePath());
            System.load(ResourceUtils.getFile("classpath:libortools.9.dylib").getAbsolutePath());
        } catch (Exception e) {
            try {
                String libDir = args[0];
                System.out.println("Loading OR-Tools from "+libDir);
                System.load(libDir+"/libjniortools.dylib");
                System.load(libDir+"/libortools.9.dylib");
            } catch (Exception e2) {
                throw new RuntimeException("Error loading or-tools libraries", e2);
            }
        }
        SpringApplication.run(PlanApplication.class, args);
    }

}
