package it.polimi.saefa.monitor.rest;

import it.polimi.saefa.monitor.domain.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path="/rest")
public class MonitorRestController {
    @Autowired
    private MonitorService monitorService;

    @GetMapping("/notifyFinishedIteration")
    public void notifyFinishedIteration() {
        monitorService.setLoopIterationFinished(true);
    }




    // Configuration endpoints
    @GetMapping("/")
    public GetInfoResponse getInfo() {
        return new GetInfoResponse(getSchedulingPeriod(), isRoutineRunning());
    }

    @GetMapping("/schedulingPeriod")
    public int getSchedulingPeriod() {
        return monitorService.getSchedulingPeriod();
    }

    @PutMapping("/schedulingPeriod")
    public void changeSchedulingPeriod(@RequestParam("period") int period) {
        monitorService.changeSchedulingPeriod(period);
    }

    @GetMapping("/startRoutine")
    public void startRoutine() {
        monitorService.startRoutine();
    }

    @GetMapping("/stopRoutine")
    public void stopRoutine() {
        monitorService.stopRoutine();
    }

    @GetMapping("/isRoutineRunning")
    public boolean isRoutineRunning() {
        return monitorService.getMonitorRoutine() != null;
    }



    // TODO remove after test
    @GetMapping("/break")
    public String debug() {
        monitorService.breakpoint();
        return "Hello from Monitor Service";
    }
}
